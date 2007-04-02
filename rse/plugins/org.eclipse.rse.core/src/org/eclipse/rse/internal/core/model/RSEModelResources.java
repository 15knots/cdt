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

package org.eclipse.rse.internal.core.model;

import org.eclipse.osgi.util.NLS;

/**
 * This class contains bundle resources for model objects.
 */
public class RSEModelResources extends NLS {

	private static String BUNDLE_NAME = "org.eclipse.rse.internal.core.model.RSEModelResources"; //$NON-NLS-1$

	public static String RESID_MODELOBJECTS_MODELOBJECT_DESCRIPTION;
	public static String RESID_MODELOBJECTS_REFERENCINGOBJECT_DESCRIPTION;
	public static String RESID_MODELOBJECTS_FILTERSTRING_DESCRIPTION;
	public static String RESID_MODELOBJECTS_HOSTPOOL_DESCRIPTION;
	public static String RESID_MODELOBJECTS_PROFILE_DESCRIPTION;
	public static String RESID_MODELOBJECTS_SERVERLAUNCHER_DESCRIPTION;
	public static String RESID_MODELOBJECTS_FILTER_DESCRIPTION;
	public static String RESID_MODELOBJECTS_FILTERPOOL_DESCRIPTION;
	
	public static String RESID_PROP_SERVERLAUNCHER_MEANS_LABEL;
	public static String RESID_PROP_SERVERLAUNCHER_PATH;
	public static String RESID_PROP_SERVERLAUNCHER_INVOCATION;
	public static String RESID_CONNECTION_DAEMON_PORT_LABEL;
	public static String RESID_CONNECTION_PORT_LABEL;
	public static String RESID_SUBSYSTEM_AUTODETECT_LABEL;


	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, RSEModelResources.class);
	}


}