/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * {Name} (company) - description of contribution.
 *******************************************************************************/

package org.eclipse.rse.subsystems.files.core.subsystems;

import org.eclipse.rse.services.files.IHostFile;

/**
 * A root node used to drive a CheckboxTreeAndListGroup, or any viewer which
 * takes a root which should return a single IRemoteFile object in the initial
 * getChildren query.
 */
public class RemoteFileRoot extends RemoteFile 
{
	private IRemoteFile rootFile;
	private IRemoteFile[] rootFiles;

	/**
	 * Constructor when root is known
	 */
	public RemoteFileRoot(IRemoteFile rootFile) 
	{
		super(new RemoteFileContext(null,null,null));
	}
	/**
	 * Constructor when root is not known
	 */
	public RemoteFileRoot() 
	{
		super(new RemoteFileContext(null,null,null));
	}

    /**
     * Return the root file node
     */
    public IRemoteFile getRootFile()
    {
    	return rootFile;
    }
    
    /**
     * Reset the root file node
     */
    public void setRootFile(IRemoteFile rootFile)
    {
    	this.rootFile = rootFile;
    	rootFiles = new IRemoteFile[1];
    	rootFiles[0] = rootFile;
    }

    /**
     * Return the root file node as an array of 1
     */
    public IRemoteFile[] getRootFiles()
    {
    	return rootFiles;
    }
    
	public String getName()
	{
		return "dummy"; //$NON-NLS-1$
	}
	
	public int compareTo(Object o)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	public boolean isVirtual()
	{
		// TODO Auto-generated method stub
		return false;
	}
	public boolean showBriefPropertySet()
	{
		// TODO Auto-generated method stub
		return false;
	}
	public String getParentPath()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public String getParentNoRoot()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public String getRoot()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public String getParentName()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public boolean isRoot()
	{
		// TODO Auto-generated method stub
		return false;
	}
	public boolean isDirectory()
	{
		// TODO Auto-generated method stub
		return false;
	}
	public boolean isFile()
	{
		// TODO Auto-generated method stub
		return false;
	}
	public boolean isHidden()
	{
		// TODO Auto-generated method stub
		return false;
	}
	public boolean canRead()
	{
		// TODO Auto-generated method stub
		return false;
	}
	public boolean canWrite()
	{
		// TODO Auto-generated method stub
		return false;
	}
	public boolean exists()
	{
		// TODO Auto-generated method stub
		return false;
	}
	public long getLastModified()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	public long getLength()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	public boolean showReadOnlyProperty()
	{
		// TODO Auto-generated method stub
		return false;
	}
	public String getClassification()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public String getCanonicalPath()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public IHostFile getHostFile()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
