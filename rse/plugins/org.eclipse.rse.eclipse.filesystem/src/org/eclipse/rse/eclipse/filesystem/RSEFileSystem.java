/********************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * David Dykstal (IBM) - 168977: refactoring IConnectorService and ServerLauncher hierarchies
 ********************************************************************************/

package org.eclipse.rse.eclipse.filesystem;

import java.net.URI;
import java.util.HashMap;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.subsystems.files.core.model.RemoteFileUtility;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;
import org.eclipse.rse.ui.RSEUIPlugin;

public class RSEFileSystem extends FileSystem 
{
	private static RSEFileSystem _instance = new RSEFileSystem();
	private HashMap _fileStoreMap;
	
	public RSEFileSystem()
	{
		super();
		_fileStoreMap = new HashMap();
	}
	
	public static RSEFileSystem getInstance()
	{
		return _instance;
	}
	
	public boolean canDelete() 
	{
		return true;
	}
	
	public boolean canWrite() 
	{
		return true;
	}

	public static IHost getConnectionFor(String hostName)
	{
		ISystemRegistry sr = RSEUIPlugin.getTheSystemRegistry();
		IHost[] connections = sr.getHosts();
		IHost unconnected = null;
		for (int i = 0; i < connections.length; i++)
		{
			IHost con = connections[i];
			if (con.getHostName().equalsIgnoreCase(hostName))
			{
				boolean isConnected = false;
				IConnectorService[] connectorServices = con.getConnectorServices();
				for (int c = 0; c < connectorServices.length  && !isConnected; c++)
				{
					IConnectorService serv = connectorServices[c];
					isConnected = serv.isConnected();
				}
				if (isConnected)
					return con;
				else
					unconnected = con;
			}
		}
		return unconnected;
	}
	
	public static IRemoteFileSubSystem getRemoteFileSubSystem(IHost host)
	{
		return RemoteFileUtility.getFileSubSystem(host);
	}
	
	public URI getURIFor(IRemoteFile file)
	{
		IFileStore fstore = FileStoreConversionUtility.convert(null, file);
		return fstore.toURI();
	}
	
	public IFileStore getStore(URI uri)
	{
		Object obj = _fileStoreMap.get(uri);
		if (obj != null)
		{
			RSEFileStoreRemoteFileWrapper store = (RSEFileStoreRemoteFileWrapper)obj;
			IRemoteFileSubSystem ss = store.getRemoteFileSubSystem();
			if (!ss.isConnected())
			{
				
				try
				{
					ss.connect();
				}
				catch (Exception e)
				{			
					return null;
				}
			}
			return store;
		}
		try 
		{
			String path = uri.getPath();
			String hostName = uri.getHost();
			IHost con = getConnectionFor(hostName);
			if (con != null)
			{
				IRemoteFileSubSystem fs =  getRemoteFileSubSystem(con);
				if (fs != null)
				{
					
					if (!fs.isConnected())
					{
						
					
						fs.getConnectorService().acquireCredentials(false);
						fs.getConnectorService().connect(new NullProgressMonitor());
						//fs.connect(shell);
					}
					if (fs.isConnected())
					{
						IFileStore fstore = FileStoreConversionUtility.convert(null, fs.getRemoteFileObject(path));
						_fileStoreMap.put(uri, fstore);
						return fstore;
					}
				
				}
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			return EFS.getNullFileSystem().getStore(uri);
	
			//return FileStoreConversionUtility.convert(null, new RemoteFileEmpty());
		}
		return null;
	}
	
}