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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.sf.lipermi.call.RemoteInstance;

/**
 * A dynamic proxy which delegates interface
 * calls to a ConnectionHandler
 *  
 * @author lipe
 * @date   05/10/2006
 * 
 * @see	   net.sf.lipermi.handler.CallHandler
 */
public class CallProxy implements InvocationHandler  {

	private ConnectionHandler connectionHandler;
	
	/**
	 * Create new CallProxy with a ConnectionHandler which will
	 * transport invocations on this Proxy
	 *  
	 * @param connectionHandler
	 */
	public CallProxy(ConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}
	
	/**
	 * Delegates call to this proxy to it's ConnectionHandler
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return connectionHandler.remoteInvocation(proxy, method, args);
	}
	
	/**
	 * Build a proxy to a {@see net.sf.lipermi.call.RemoteInstance RemoteInstance}
	 * specifing how it could be reached (i.e., through a ConnectionHandler)
	 * 
	 * @param  remoteInstance
	 * @param  connectionHandler
	 * @return dymamic proxy for RemoteInstance
	 * @throws ClassNotFoundException
	 */
	public static Object buildProxy(RemoteInstance remoteInstance, ConnectionHandler connectionHandler) throws ClassNotFoundException {
		Class<?> clazz = Class.forName(remoteInstance.getClassName());
		return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, new CallProxy(connectionHandler));
	}
}
