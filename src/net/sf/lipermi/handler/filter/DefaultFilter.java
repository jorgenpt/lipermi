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

package net.sf.lipermi.handler.filter;

import net.sf.lipermi.call.IRemoteMessage;

/**
 * Default protocol filter implementation.
 * Only forwards the data.
 * 
 * @see net.sf.lipermi.handler.filter.GZipFilter
 */
public class DefaultFilter implements IProtocolFilter {

	public IRemoteMessage readObject(Object obj) {
		return (IRemoteMessage) obj;
	}

	public Object prepareWrite(IRemoteMessage message) {
		return message;
	}

}
