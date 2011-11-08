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

package net.sf.lipermi.call;

/**
 * Class that holds method call informations.
 * 
 * @date   05/10/2006 
 * @author lipe
 */
public class RemoteCall implements IRemoteMessage {

	private static final long serialVersionUID = -4057457700512552099L;

	/**
	 * Instance will receive the call
	 */
	RemoteInstance remoteInstance;
	
	/**
	 * Method's name
	 */
	String methodId;
	
	/**
	 * Method's arguments
	 */
	Object[] args;
	
	/**
	 * The id is a number unique in client and server to identify the call
	 */
	Long callId;

	public Object[] getArgs() {
		return args;
	}

	public Long getCallId() {
		return callId;
	}

	public RemoteInstance getRemoteInstance() {
		return remoteInstance;
	}

	public String getMethodId() {
		return methodId;
	}

	public RemoteCall(RemoteInstance remoteInstance, String methodId, Object[] args, Long callId) {
		this.remoteInstance = remoteInstance;
		this.methodId = methodId;
		this.args = args;
		this.callId = callId;
	}
	
}
