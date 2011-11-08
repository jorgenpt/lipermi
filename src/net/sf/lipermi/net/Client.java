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

package net.sf.lipermi.net;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.CallProxy;
import net.sf.lipermi.handler.ConnectionHandler;
import net.sf.lipermi.handler.IConnectionHandlerListener;
import net.sf.lipermi.handler.filter.DefaultFilter;
import net.sf.lipermi.handler.filter.IProtocolFilter;


/**
 * The LipeRMI client.
 * Connects to a LipeRMI Server in a address:port
 * and create local dynamic proxys to call remote
 * methods through a simple interface.
 * 
 * @author lipe
 * @date   05/10/2006
 * 
 * @see    net.sf.lipermi.handler.CallHandler
 * @see    net.sf.lipermi.net.Server
 */
public class Client {

	private Socket socket;
	
	private ConnectionHandler connectionHandler;
	
	private final IConnectionHandlerListener connectionHandlerListener = new IConnectionHandlerListener() {
		public void connectionClosed() {
			for (IClientListener listener : listeners)
				listener.disconnected();
		}
	};
	
	private List<IClientListener> listeners = new LinkedList<IClientListener>();
	
	public void addClientListener(IClientListener listener) {
		listeners.add(listener);
	}

	public void removeClientListener(IClientListener listener) {
		listeners.remove(listener);
	}	
	
	public Client(String address, int port, CallHandler callHandler) throws IOException {
		this(address, port, callHandler, new DefaultFilter());
	}
	
	public Client(String address, int port, CallHandler callHandler, IProtocolFilter filter) throws IOException {
		socket = new Socket(address, port);
		connectionHandler = ConnectionHandler.createConnectionHandler(socket, callHandler, filter, connectionHandlerListener);
	}
	
	public void close() throws IOException {
		socket.close();
	}
	
	public Object getGlobal(Class<?> clazz) {
		return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, new CallProxy(connectionHandler));
	}
}
