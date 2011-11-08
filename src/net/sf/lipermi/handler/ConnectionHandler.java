/*
 * LipeRMI - a light weight Internet approach for remote method invocation
 * Copyright (C) 2006  Felipe Santos Andrade
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * For more information, see http://lipermi.sourceforge.net/license.php
 * You can also contact author through lipeandrade@users.sourceforge.net
 */

package net.sf.lipermi.handler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.lipermi.call.IRemoteMessage;
import net.sf.lipermi.call.RemoteCall;
import net.sf.lipermi.call.RemoteInstance;
import net.sf.lipermi.call.RemoteReturn;
import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.filter.IProtocolFilter;


/**
 * A ConnectionHandler is object which can call remote
 * methods, receive remote calls and dispatch its returns.
 *   
 * @author lipe
 * @date   05/10/2006
 * 
 * @see	   net.sf.lipermi.handler.CallHandler
 * @see	   net.sf.lipermi.call.RemoteInstance
 * @see	   net.sf.lipermi.call.RemoteCall
 * @see	   net.sf.lipermi.call.RemoteReturn
 * @see	   net.sf.lipermi.net.Client
 * @see	   net.sf.lipermi.net.Server
 * @see	   net.sf.lipermi.handler.filter.DefaultFilter
 */
public class ConnectionHandler implements Runnable {

	public static ConnectionHandler createConnectionHandler(Socket socket, CallHandler callHandler, IProtocolFilter filter) {
		ConnectionHandler connectionHandler = new ConnectionHandler(socket, callHandler, filter);

		String threadName = String.format("ConnectionHandler (%s:%d)", socket.getInetAddress().getHostAddress(), socket.getPort()); //$NON-NLS-1$
		Thread connectionHandlerThread = new Thread(connectionHandler, threadName);
		connectionHandlerThread.setDaemon(true);
		connectionHandlerThread.start();
		
		return connectionHandler;
	}
	
	public static ConnectionHandler createConnectionHandler(Socket socket, CallHandler callHandler, IProtocolFilter filter, IConnectionHandlerListener listener) {
		ConnectionHandler connectionHandler = createConnectionHandler(socket, callHandler, filter);
		connectionHandler.addConnectionHandlerListener(listener);
		return connectionHandler;
	}

	private CallHandler callHandler;
	
	private Socket socket;
	
	private ObjectOutputStream output;

	private static AtomicLong callId = new AtomicLong(0L);
	
	private IProtocolFilter filter;
	
	private List<IConnectionHandlerListener> listeners = new LinkedList<IConnectionHandlerListener>();
	
	private Map<RemoteInstance, Object> remoteInstanceProxys = new HashMap<RemoteInstance, Object>();
	
	private List<RemoteReturn> remoteReturns = new LinkedList<RemoteReturn>();
	
	public void addConnectionHandlerListener(IConnectionHandlerListener listener) {
		listeners.add(listener);
	}

	public void removeConnectionHandlerListener(IConnectionHandlerListener listener) {
		listeners.remove(listener);
	}

	private ConnectionHandler(Socket socket, CallHandler callHandler, IProtocolFilter filter) {
		this.callHandler = callHandler;
		this.socket = socket;
		this.filter = filter;
	}
	
	public void run() {
		ObjectInputStream input;
		
		try {
			input = new ObjectInputStream(socket.getInputStream());

			while (socket.isConnected()) {
				Object objFromStream = input.readUnshared();
				
				IRemoteMessage remoteMessage = filter.readObject(objFromStream);
				
				if (remoteMessage instanceof RemoteCall) {

					final RemoteCall remoteCall = (RemoteCall) remoteMessage;
					if (remoteCall.getArgs() != null) {
						for (int n = 0; n < remoteCall.getArgs().length; n++) {
							Object arg = remoteCall.getArgs()[n];
							if (arg instanceof RemoteInstance) {
								RemoteInstance remoteInstance = (RemoteInstance) arg;
								remoteCall.getArgs()[n] = getProxyFromRemoteInstance(remoteInstance);
							}
						}
					}
					
					Thread delegator = new Thread(new Runnable() {
						public void run() {
							CallLookup.handlingMe(ConnectionHandler.this);
							
							RemoteReturn remoteReturn;
							try {
//								System.out.println("remoteCall: " + remoteCall.getCallId() + " - " + remoteCall.getRemoteInstance().getInstanceId());
//								System.out.println("(" + remoteCall.getCallId() + ") " + remoteCall.getArgs()[0]);
								remoteReturn = callHandler.delegateCall(remoteCall);
//								System.out.println("(" + remoteCall.getCallId() + ") " + remoteReturn.getRet());
								sendMessage(remoteReturn);
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							CallLookup.forgetMe();
						}
					}, "Delegator"); //$NON-NLS-1$
					delegator.setDaemon(true);
					delegator.start();
				}
				else if (remoteMessage instanceof RemoteReturn) {
					RemoteReturn remoteReturn = (RemoteReturn) remoteMessage;
					synchronized (remoteReturns) {
						remoteReturns.add(remoteReturn);
						remoteReturns.notifyAll();
					}
				}
				else
					throw new LipeRMIException("Unknown IRemoteMessage type"); //$NON-NLS-1$
			}
		} catch (Exception e) {
			try {
				socket.close();
			} catch (IOException e1) {}

			synchronized (remoteReturns) {
				remoteReturns.notifyAll();
			}
			
			for (IConnectionHandlerListener listener : listeners)
				listener.connectionClosed();
		}
	}
	
	private synchronized void sendMessage(IRemoteMessage remoteMessage) throws IOException {
		if (output == null)
			output = new ObjectOutputStream(socket.getOutputStream());

		Object objToWrite = filter.prepareWrite(remoteMessage);
		output.reset();
		output.writeUnshared(objToWrite);
		output.flush();
	}


	final synchronized Object remoteInvocation(final Object proxy, final Method method, final Object[] args) throws Throwable {
		final Long id = callId.getAndIncrement();
		 
		RemoteInstance remoteInstance;
		remoteInstance = getRemoteInstanceFromProxy(proxy);
		if (remoteInstance == null)
			remoteInstance = new RemoteInstance(null, proxy.getClass().getInterfaces()[0].getName());

		if (args != null) {
			for (int n = 0; n < args.length; n++) {
				RemoteInstance remoteRef = callHandler.getRemoteReference(args[n]);
				if (remoteRef != null)
					args[n] = remoteRef;
			}
		}

		String methodId = method.toString().substring(15);
		
		IRemoteMessage remoteCall = new RemoteCall(remoteInstance, methodId, args, id);
		sendMessage(remoteCall);
		
		RemoteReturn remoteReturn = null;
		
		boolean bReturned = false;
		do {
			synchronized (remoteReturns) {
				for (RemoteReturn ret : remoteReturns) {
					if (ret.getCallId().equals(id)) {
						bReturned = true;
						remoteReturn = ret;
						break;
					}
				}
				if (bReturned)
					remoteReturns.remove(remoteReturn);
				else {
					try {
						remoteReturns.wait();
					}
					catch (InterruptedException ie) {}
				}
			}
		}
		while (socket.isConnected() && !bReturned);
		
		if (!socket.isConnected() && !bReturned)
			throw new LipeRMIException("Connection aborted"); //$NON-NLS-1$
		
		if (remoteReturn.isThrowing() && remoteReturn.getRet() instanceof Throwable)
			throw ((Throwable) remoteReturn.getRet()).getCause();
		
		if (remoteReturn.getRet() instanceof RemoteInstance) {
			RemoteInstance remoteInstanceReturn = (RemoteInstance) remoteReturn.getRet();
			Object proxyReturn = remoteInstanceProxys.get(remoteInstanceReturn);
			if (proxyReturn == null) {
				proxyReturn = CallProxy.buildProxy(remoteInstanceReturn, this);
				remoteInstanceProxys.put(remoteInstanceReturn, proxyReturn);
			}
			return proxyReturn; 
		}		
		
		return remoteReturn.getRet();
	}
	
	private Object getProxyFromRemoteInstance(RemoteInstance remoteInstance) {
		Object proxy = remoteInstanceProxys.get(remoteInstance);
		if (proxy == null) {
			try {
				proxy = CallProxy.buildProxy(remoteInstance, this);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			remoteInstanceProxys.put(remoteInstance, proxy);
		}
		return proxy; 
	}
	
	private RemoteInstance getRemoteInstanceFromProxy(Object proxy) {
		for (RemoteInstance remoteInstance : remoteInstanceProxys.keySet()) {
			if (remoteInstanceProxys.get(remoteInstance) == proxy)
				return remoteInstance;
		}
		
		return null;
	}

	public Socket getSocket() {
		return socket;
	}
}
