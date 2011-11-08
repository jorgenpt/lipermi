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

import java.io.Serializable;

/**
 * Class that holds informations about a remote instance,
 * making the instance unique in all remote JVM.
 * All remote instances have a generated random UUID,
 * except the global ones (registered with {@link net.sf.lipermi.handler.CallHandler#registerGlobal CallHandler}).
 * 
 * @date   05/10/2006 
 * @author lipe
 */

public class RemoteInstance implements Serializable {

	private static final long serialVersionUID = -4597780264243542810L;

	String instanceId;
	
	String className;

	public String getClassName() {
		return className;
	}
	
	public String getInstanceId() {
		return instanceId;
	}

	public RemoteInstance(String instanceId, String className) {
		this.instanceId = instanceId;
		this.className = className;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RemoteInstance) {
			RemoteInstance ri = (RemoteInstance) obj;
			boolean instanceId = (getInstanceId() == ri.getInstanceId() || (getInstanceId() != null && getInstanceId().equals(ri.getInstanceId())));
			boolean className = (getClassName().equals(ri.getClassName()));
			return (className && instanceId);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return className.hashCode();
	}
	
}
