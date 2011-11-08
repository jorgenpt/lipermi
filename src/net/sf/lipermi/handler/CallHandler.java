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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.sf.lipermi.call.RemoteCall;
import net.sf.lipermi.call.RemoteInstance;
import net.sf.lipermi.call.RemoteReturn;
import net.sf.lipermi.exception.LipeRMIException;


/**
 * A handler who know a RemoteInstance and its
 * local implementations. Used to delegate calls to
 * correct implementation objects.
 * 
 * Local implementation objects must register with
 * methods {@link net.sf.lipermi.handler.CallHandler#registerGlobal registerGlobal} and
 * {@link net.sf.lipermi.handler.CallHandler#exportObject exportObject} to work remotelly.
 * 
 * @author lipe
 * @date   05/10/2006
 * 
 * @see	   net.sf.lipermi.call.RemoteInstance
 */
public class CallHandler {
		
	private Map<RemoteInstance, Object> exportedObjects = new HashMap<RemoteInstance, Object>();
	
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	public void registerGlobal(Class cInterface, Object objImplementation) throws LipeRMIException {
		exportObject(cInterface, objImplementation, null);
	}
	
	public void exportObject(Class<?> cInterface, Object exportedObject) throws LipeRMIException {
		UUID objUUID = java.util.UUID.randomUUID();
		String instanceId = objUUID.toString();

		exportObject(cInterface, exportedObject, instanceId);
	}
	
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	private void exportObject(Class cInterface, Object objImplementation, String instanceId) throws LipeRMIException {
		if (!cInterface.isAssignableFrom(objImplementation.getClass()))
			throw new LipeRMIException(String.format("Class %s is not assignable from %s", objImplementation.getClass().getName(), cInterface.getName())); //$NON-NLS-1$

		for (RemoteInstance remoteInstance : exportedObjects.keySet()) {
			if ((remoteInstance.getInstanceId() == instanceId || (remoteInstance.getInstanceId() != null && remoteInstance.getInstanceId().equals(instanceId))) && remoteInstance.getClassName().equals(cInterface.getName())) {
				throw new LipeRMIException(String.format("Class %s already has a implementation class", cInterface.getName()));				 //$NON-NLS-1$
			}
		}
		
		RemoteInstance remoteInstance = new RemoteInstance(instanceId, cInterface.getName());
		exportedObjects.put(remoteInstance, objImplementation);
	}	
	
	public RemoteReturn delegateCall(RemoteCall remoteCall) throws LipeRMIException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException {
		Object implementator = exportedObjects.get(remoteCall.getRemoteInstance());
		if (implementator == null)
			throw new LipeRMIException(String.format("Class %s doesn't have implementation", remoteCall.getRemoteInstance().getClassName())); //$NON-NLS-1$

		RemoteReturn remoteReturn;

		Method implementationMethod = null;
		
		for (Method method : implementator.getClass().getMethods()) {
			String implementationMethodId = method.toString();
			implementationMethodId = implementationMethodId.replace(implementator.getClass().getName(), remoteCall.getRemoteInstance().getClassName());
			
			if (implementationMethodId.endsWith(remoteCall.getMethodId())) {
				implementationMethod = method;
				break;
			}
		}
		
		if (implementationMethod == null)
			throw new NoSuchMethodException(remoteCall.getMethodId());
		
		try {
			Object methodReturn = null;
			implementationMethod.setAccessible(true);
			methodReturn = implementationMethod.invoke(implementator, remoteCall.getArgs());
			if (exportedObjects.containsValue(methodReturn))
				methodReturn = getRemoteReference(methodReturn);
			
			remoteReturn = new RemoteReturn(false, methodReturn, remoteCall.getCallId());
		} catch (InvocationTargetException e) {
			remoteReturn = new RemoteReturn(true, e, remoteCall.getCallId());
		}
		
		return remoteReturn;
	}

	
	RemoteInstance getRemoteReference(Object obj) {
		for (RemoteInstance remoteInstance : exportedObjects.keySet()) {
			Object exportedObj = exportedObjects.get(remoteInstance);
			if (exportedObj == obj)
				return remoteInstance;
		}
		return null;
	}
	
	public static Class<?>[] typeFromObjects(Object[] objects) {
		Class<?>[] argClasses = null;
		if (objects != null) {
			argClasses = new Class[objects.length];
			for (int n = 0; n < objects.length; n++) {
				Object obj = objects[n];
				argClasses[n++] = obj.getClass();
			}
		}		
		return argClasses;
	}
}
