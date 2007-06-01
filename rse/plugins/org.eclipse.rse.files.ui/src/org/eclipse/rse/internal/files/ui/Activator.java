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
 * Martin Oberhuber (Wind River) - [180519][api] declaratively register adapter factories
 ********************************************************************************/

package org.eclipse.rse.internal.files.ui;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.rse.files.ui.resources.SystemUniversalTempFileListener;
import org.eclipse.rse.internal.files.ui.propertypages.SystemCachePreferencePage;
import org.eclipse.rse.internal.files.ui.propertypages.UniversalPreferencePage;
import org.eclipse.rse.internal.files.ui.resources.SystemRemoteEditManager;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The main plugin class to be used in the desktop.
 */
public class Activator extends AbstractUIPlugin
{

	//The shared instance.
	private static Activator plugin;
	
	private static SystemUniversalTempFileListener _tempFileListener;
	
	/**
	 * The constructor.
	 */
	public Activator() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);		

	    // refresh the remote edit project at plugin startup, to ensure
	    // it's never closed
		SystemRemoteEditManager.getInstance().refreshRemoteEditProject();

	    // universal temp file listener
	    _tempFileListener = SystemUniversalTempFileListener.getListener();	
	  	// add listener for temp files
    	int eventMask = IResourceChangeEvent.POST_CHANGE;	
    	IWorkspace ws = SystemBasePlugin.getWorkspace();
    	ws.addResourceChangeListener(_tempFileListener, eventMask);   
    	
    	// DKM - workaround for issue in  175295
    	// I had tried SystemFilePreferenceInitializer but it was not being started by the platform because
    	// the preference store is rse.ui. In order to fix that, we'd have to migrate the
    	// preferences for files to the files.ui preference store. 
    	// Instead calling this directly at startup.
    	initializeDefaultRSEPreferences();
	}

	/**
	 * Initializes preferences.
	 */
	public void initializeDefaultRSEPreferences()
	{
		IPreferenceStore store = RSEUIPlugin.getDefault().getPreferenceStore();
		SystemCachePreferencePage.initDefaults(store);
		UniversalPreferencePage.initDefaults(store);	
	}
	
	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception 
	{
		super.stop(context);
		
		IWorkspace ws = SystemBasePlugin.getWorkspace();
		ws.removeResourceChangeListener(_tempFileListener);
  	  	_tempFileListener = null;
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("org.eclipse.rse.files.ui", path); //$NON-NLS-1$
	}
	
}