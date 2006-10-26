/********************************************************************************
 * Copyright (c) 2006 IBM Corporation. All rights reserved.
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
 * {Name} (company) - description of contribution.
 ********************************************************************************/

package org.eclipse.rse.persistence;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.rse.persistence.dom.RSEDOM;


/**
 * This is the interface that needs to be implemented when providing an extension
 * using the RSE persistence provider extension point. 
 * 
 * Implement this class to provide a specialized means of 
 * saving and restoring RSEDOM
 * 
 */
public interface IRSEPersistenceProvider 
{
	/**
	 * Restores an RSE DOM given a profileName. 
	 * 
	 * @param profileName name of the Profile to load
	 * @param monitor 
	 * @return the RSE DOM for the specified profile
	 */
	public RSEDOM loadRSEDOM(String profileName, IProgressMonitor monitor);

	/**
	 * Persists an RSE DOM.
	 *  
	 * @param dom the RSE DOM to persist
	 * @param monitor
	 * @return true if succcessful
	 */
	public boolean saveRSEDOM(RSEDOM dom, IProgressMonitor monitor);
	
	/**
	 * @return The names of the profiles that have been saved by this persistence provider.
	 */
	public String[] getSavedProfileNames();
	
	/**
	 * Removes a profile. Does nothing if the profile is not found.
	 * @param profileName the name of the profile to remove
	 * @param monitor the monitor for the operation
	 */
	public IStatus deleteProfile(String profileName, IProgressMonitor monitor);
}