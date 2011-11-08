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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.ConnectionHandler;
import net.sf.lipermi.handler.IConnectionHandlerListener;
import net.sf.lipermi.handler.filter.DefaultFilter;
import net.sf.lipermi.handler.filter.IProtocolFilter;


/**
 * The LipeRMI server.
 * This object listen to a specific port and
 * when a client connects it delegates the connection
 * to a {@link net.sf.lipermi.handler.ConnectionHandler ConnectionHandler}.
 * 
 * @author lipe
 * @date   05/10/2006
 * 
 * @see    net.sf.lipermi.handler.CallHandler
 * @see    net.sf.lipermi.net.Client
 */
public class Server {

	private ServerSocket serverSocket;
	
	private boolean enabled;
	
	private List<IServerListener> listeners = new LinkedList<IServerListener>();
	
	public void addServerListener(IServerListener listener) {
		listeners.add(listener);
	}

	public void removeServerListener(IServerListener listener) {
		listeners.remove(listener);
	}
	
	public void close() {
		enabled = false;
	}
	
	public void bind(int port, CallHandler callHandler) throws IOException {
		bind(port, callHandler, new DefaultFilter());
	}
	
	public void bind(final int port, final CallHandler callHandler, final IProtocolFilter filter) throws IOException {
		serverSocket = new ServerSocket();
		serverSocket.setPerformancePreferences(1, 0, 2);
		enabled = true;

		serverSocket.bind(new InetSocketAddress(port));
		
		Thread bindThread = new Thread(new Runnable() {
			public void run() {
				while (enabled) {
					Socket acceptSocket = null;
					try {
						acceptSocket = serverSocket.accept();
						
						final Socket clientSocket = acceptSocket;
						ConnectionHandler.createConnectionHandler(clientSocket,
								callHandler,
								filter,
								new IConnectionHandlerListener() {
							
							public void connectionClosed() {
								for (IServerListener listener : listeners)
									listener.clientDisconnected(clientSocket);
							}
							
						});
						for (IServerListener listener : listeners)
							listener.clientConnected(clientSocket);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}, String.format("Bind (%d)", port)); //$NON-NLS-1$ //$NON-NLS-2$
		bindThread.start();
	}
	
}
