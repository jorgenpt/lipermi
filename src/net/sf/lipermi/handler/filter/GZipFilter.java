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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.sf.lipermi.call.IRemoteMessage;


/**
 * GZip filter to compact data using GZip I/O streams.
 * 
 * @author lipe
 * @date   07/10/2006
 * 
 * @see net.sf.lipermi.handler.filter.DefaultFilter
 */
public class GZipFilter implements IProtocolFilter {
	
	public IRemoteMessage readObject(Object obj) {
		IRemoteMessage remoteMessage = null;
		GZIPInputStream gzis = null;
		ObjectInputStream ois = null;
		
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) obj);
			gzis = new GZIPInputStream(bais);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int b;
			while ((b = gzis.read()) != -1)
				baos.write(b);
			
			gzis.close();
			
			byte[] extractedObj = baos.toByteArray();
			
			bais = new ByteArrayInputStream(extractedObj);
			ois = new ObjectInputStream(bais);
			remoteMessage = (IRemoteMessage) ois.readUnshared();
			ois.close();
		}
		catch (Exception e) {
			throw new RuntimeException("Can't read message", e); //$NON-NLS-1$
		}
		finally {
			if (gzis != null)
				try {
					gzis.close();
				} catch (IOException e) {}
			
			if (ois != null)
				try {
					ois.close();
				} catch (IOException e) {}
		}
		return remoteMessage;
	}

	public Object prepareWrite(IRemoteMessage message) {
		Object objectToWrite = message;
		
		ObjectOutputStream oos = null;
		GZIPOutputStream gzos = null;
		try {
			// serialize obj
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			// oos.reset(); -- not needed here because the oos is
			//                 always a new instance, reseted.
			oos.writeUnshared(message);
			byte[] byteObj = baos.toByteArray();
			
			baos.reset();
			
			// compact the serialization
			gzos = new GZIPOutputStream(baos);
			gzos.write(byteObj);
			gzos.finish();
			byteObj = baos.toByteArray();
			
			objectToWrite = byteObj;
		}
		catch (Exception e) {
			throw new RuntimeException("Can't prepare message", e); //$NON-NLS-1$
		}
		finally {
			if (gzos != null)
				try {
					gzos.close();
				} catch (IOException e) {}
				
			if (oos != null)
				try {
					oos.close();
				} catch (IOException e) {}
		}
		return objectToWrite;
	}
}
