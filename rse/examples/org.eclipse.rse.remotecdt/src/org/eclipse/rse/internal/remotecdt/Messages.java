/*******************************************************************************
 * Copyright (c) 2006 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Martin Oberhuber (Wind River) - initial API and implementation 
 *******************************************************************************/
package org.eclipse.rse.internal.remotecdt;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.rse.internal.remotecdt.messages"; //$NON-NLS-1$

	public static String Gdbserver_name_textfield_label;

	public static String Gdbserver_Settings_Tab_Name;

	public static String Port_number_textfield_label;

	public static String Remote_GDB_Debugger_Options;

	public static String RemoteCMainTab_Program;
	public static String RemoteCMainTab_SkipDownload;
	public static String RemoteCMainTab_ErrorNoProgram;
	public static String RemoteCMainTab_ErrorNoConnection;
	public static String RemoteCMainTab_Connection;
	public static String RemoteCMainTab_New;

	public static String RemoteRunLaunchDelegate_RemoteShell;
	public static String RemoteRunLaunchDelegate_1;
	public static String RemoteRunLaunchDelegate_3;
	public static String RemoteRunLaunchDelegate_4;
	public static String RemoteRunLaunchDelegate_5;
	public static String RemoteRunLaunchDelegate_6;
	public static String RemoteRunLaunchDelegate_7;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
