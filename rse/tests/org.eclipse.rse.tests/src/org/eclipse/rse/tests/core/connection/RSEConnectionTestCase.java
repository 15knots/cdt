/* *******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Don Yantzi (IBM) - initial contribution.
 * David Dykstal (IBM) - initial contribution.
 * *******************************************************************************/
package org.eclipse.rse.tests.core.connection;

import java.util.Properties;

import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.tests.RSETestsPlugin;
import org.eclipse.rse.ui.ISystemPreferencesConstants;
import org.eclipse.rse.ui.RSEUIPlugin;

/**
 * Basic connection tests.
 */
public class RSEConnectionTestCase extends RSEBaseConnectionTestCase {

	public RSEConnectionTestCase(String name) {
		super(name);
	}

	/**
	 * Test creation of connections.
	 */
	public void testConnectionCreation() {
		if (!RSETestsPlugin.isTestCaseEnabled("RSEConnectionTestCase.testConnectionCreation")) return; //$NON-NLS-1$

		Properties properties = new Properties();
		properties.setProperty(IRSEConnectionProperties.ATTR_PROFILE_NAME, "TestProfile"); //$NON-NLS-1$
		properties.setProperty(IRSEConnectionProperties.ATTR_NAME, "TestHost1"); //$NON-NLS-1$
		properties.setProperty(IRSEConnectionProperties.ATTR_ADDRESS, "localhost"); //$NON-NLS-1$
		properties.setProperty(IRSEConnectionProperties.ATTR_SYSTEM_TYPE, "Unix"); //$NON-NLS-1$
		properties.setProperty(IRSEConnectionProperties.ATTR_USERID, "userid"); //$NON-NLS-1$
		properties.setProperty(IRSEConnectionProperties.ATTR_PASSWORD, "password"); //$NON-NLS-1$
		
		IRSEConnectionProperties props = getConnectionManager().loadConnectionProperties(properties, false);
		IHost	connection = getConnectionManager().findOrCreateConnection(props);
		assertNotNull("Failed to create connection " + props.getProperty(IRSEConnectionProperties.ATTR_NAME), connection); //$NON-NLS-1$
		
		props.setProperty(IRSEConnectionProperties.ATTR_NAME, "TestHost2"); //$NON-NLS-1$
		connection = getConnectionManager().findOrCreateConnection(props);
		assertNotNull("Failed to create connection " + props.getProperty(IRSEConnectionProperties.ATTR_NAME), connection); //$NON-NLS-1$
		
		props.setProperty(IRSEConnectionProperties.ATTR_NAME, "TestHost3"); //$NON-NLS-1$
		connection = getConnectionManager().findOrCreateConnection(props);
		assertNotNull("Failed to create connection " + props.getProperty(IRSEConnectionProperties.ATTR_NAME), connection); //$NON-NLS-1$
		
		props.setProperty(IRSEConnectionProperties.ATTR_NAME, "TestHost4"); //$NON-NLS-1$
		connection = getConnectionManager().findOrCreateConnection(props);
		assertNotNull("Failed to create connection " + props.getProperty(IRSEConnectionProperties.ATTR_NAME), connection); //$NON-NLS-1$
		
		props.setProperty(IRSEConnectionProperties.ATTR_NAME, "TestHost5"); //$NON-NLS-1$
		connection = getConnectionManager().findOrCreateConnection(props);
		assertNotNull("Failed to create connection " + props.getProperty(IRSEConnectionProperties.ATTR_NAME), connection); //$NON-NLS-1$
		
		props.setProperty(IRSEConnectionProperties.ATTR_NAME, "TestHost6"); //$NON-NLS-1$
		connection = getConnectionManager().findOrCreateConnection(props);
		assertNotNull("Failed to create connection " + props.getProperty(IRSEConnectionProperties.ATTR_NAME), connection); //$NON-NLS-1$
	}
	
	/**
	 * Test removal of connections
	 */
	public void testConnectionRemoval() {
		if (!RSETestsPlugin.isTestCaseEnabled("RSEConnectionTestCase.testConnectionRemoval")) return; //$NON-NLS-1$

		String profileName = "TestProfile"; //$NON-NLS-1$
		
		getConnectionManager().removeConnection(profileName, "TestHost1"); //$NON-NLS-1$
		getConnectionManager().removeConnection(profileName, "TestHost2"); //$NON-NLS-1$
		getConnectionManager().removeConnection(profileName, "TestHost3"); //$NON-NLS-1$
		getConnectionManager().removeConnection(profileName, "TestHost4"); //$NON-NLS-1$
		getConnectionManager().removeConnection(profileName, "TestHost5"); //$NON-NLS-1$
		getConnectionManager().removeConnection(profileName, "TestHost6"); //$NON-NLS-1$
	}

	/**
	 * Test the connect and disconnect methods
	 */
	public void testConnect() {
		if (!RSETestsPlugin.isTestCaseEnabled("RSEConnectionTestCase.testConnect")) return; //$NON-NLS-1$

		Exception exception = null;
		String cause = null;
		
		IHost connection = getLocalSystemConnection();
		ISubSystem subsystem = null;
		try {
			subsystem = getConnectionManager().getFileSubSystem(connection, "local.files"); //$NON-NLS-1$
		} catch(Exception e) {
			exception = e;
			cause = e.getLocalizedMessage();
		}
		assertNull("Failed to get local.files subsystem! Possible cause: " + cause, exception); //$NON-NLS-1$
		assertNotNull("No local.files subystem", subsystem); //$NON-NLS-1$

		RSEUIPlugin.getDefault().getPreferenceStore().setValue(ISystemPreferencesConstants.ALERT_SSL, false);
		RSEUIPlugin.getDefault().getPreferenceStore().setValue(ISystemPreferencesConstants.ALERT_NONSSL, false);
		
		exception = null;
		cause = null;
		
		try {
			subsystem.connect();
		} catch(Exception e) {
			exception = e;
			cause = e.getLocalizedMessage();
		}
		assertNull("Failed to connect local.files subsystem! Possible cause: " + cause, exception); //$NON-NLS-1$
		assertTrue("local.files subsystem is not connected!", subsystem.isConnected()); //$NON-NLS-1$
		
		exception = null;
		cause = null;
		
		try {
			subsystem.disconnect();
		} catch(Exception e) {
			exception = e;
			cause = e.getLocalizedMessage();
		}
		assertNull("Failed to discconnect local.files subsystem! Possible cause: " + cause, exception); //$NON-NLS-1$
		// The local.files subsystem should be not disconnectable!
		assertTrue("local.files subsystem is not connected but is expected to!", subsystem.isConnected()); //$NON-NLS-1$
	}

	/**
	 * Test resolving a filter string.
	 */
	public void testResolveFilterString() {
		if (!RSETestsPlugin.isTestCaseEnabled("RSEConnectionTestCase.testResolveFilterString")) return; //$NON-NLS-1$

		Exception exception = null;
		String cause = null;

		IHost connection = getLocalSystemConnection();
		ISubSystem subsystem = null;
		try {
			subsystem = getConnectionManager().getFileSubSystem(connection, "local.files"); //$NON-NLS-1$
		} catch(Exception e) {
			exception = e;
			cause = e.getLocalizedMessage();
		}
		assertNull("Failed to get local.files subsystem! Possible cause: " + cause, exception); //$NON-NLS-1$
		assertNotNull("No local.files subystem", subsystem); //$NON-NLS-1$

		exception = null;
		cause = null;

		try {
			subsystem.connect();
		} catch(Exception e) {
			exception = e;
			cause = e.getLocalizedMessage();
		}
		assertNull("Failed to connect local.files subsystem! Possible cause: " + cause, exception); //$NON-NLS-1$
		assertTrue("local.files subsystem is not connected!", subsystem.isConnected()); //$NON-NLS-1$
		
		exception = null;
		cause = null;

		Object[] objects = null;
		try {
			objects = subsystem.resolveFilterString(null, "/bin/*"); //$NON-NLS-1$
		} catch(Exception e) {
			exception = e;
			cause = e.getLocalizedMessage();
		} finally {
			try { subsystem.disconnect(); } catch (Exception e) { /* ignored */ }
		}
		assertNull("Failed to resolve filter string for local.files subsystem! Possible cause: " + cause, exception); //$NON-NLS-1$
		assertNotNull("Unexpected return value null for resolveFilterString!", objects); //$NON-NLS-1$
	}	
}
