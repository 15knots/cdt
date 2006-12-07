/*******************************************************************************
 * Copyright (c) 2006 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Martin Oberhuber (Wind River) - initial API and implementation 
 * Martin Oberhuber (Wind River) - copy dialogs from team.cvs.ui 
 *******************************************************************************/

package org.eclipse.rse.connectorservice.ssh;

import org.eclipse.osgi.util.NLS;

public class SshConnectorResources extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.rse.connectorservice.ssh.SshConnectorResources"; //$NON-NLS-1$
	static {
		NLS.initializeMessages(BUNDLE_NAME, SshConnectorResources.class);
	}
	private SshConnectorResources() {
	}

	public static String SshConnectorService_Name;
	public static String SshConnectorService_Description;
	
	public static String SshConnectorService_ErrorDisconnecting;
	public static String SshConnectorService_Info;
	public static String SshConnectorService_Warning;

	//These are from org.eclipse.team.cvs.ui.CVSUIMessages
	public static String UserValidationDialog_required;
	public static String UserValidationDialog_labelUser;
	public static String UserValidationDialog_labelPassword;
	public static String UserValidationDialog_password;
	public static String UserValidationDialog_user;
	public static String UserValidationDialog_5;
	public static String UserValidationDialog_6;
	public static String UserValidationDialog_7;

	public static String KeyboardInteractiveDialog_message;
	public static String KeyboardInteractiveDialog_labelConnection;

	//These are from  cvs/messages.properties
	public static String Socket_timeout;

}
