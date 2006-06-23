/********************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation. All rights reserved.
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

package org.eclipse.rse.internal.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.rse.logging.IRemoteSystemsLogging;

/**
 * Log Listener is a sink for messages coming from Logger.
 */
public class RemoteSystemLogListener implements ILogListener, IPropertyChangeListener {

	private PrintWriter log = null;
	private File outputFile = null;
	private boolean log_to_stdout = false;
	private Plugin plugin = null;

	/**
	 * Create a new log listener for a plugin.
	 * @param plugin The plugin for which to create a log listener.
	 */
	public RemoteSystemLogListener(Plugin plugin) {
		this.plugin = plugin;
		IPath path = plugin.getStateLocation().addTrailingSeparator().append(".log");
		outputFile = path.toFile();
		if ((outputFile != null) && (outputFile.exists())) {
			outputFile.delete();
		}
		initialize();
	}

	/**
	 * Initialize the logger. Retrieves the logging location preference and sets up the logger
	 * to log to that location.
	 */
	private void initialize() {
		try {
			Preferences store = plugin.getPluginPreferences();
			String log_location = store.getString(IRemoteSystemsLogging.LOG_LOCATION);
			if ((log_location != null) && (log_location.equalsIgnoreCase(IRemoteSystemsLogging.LOG_TO_STDOUT))) {
				doLogToView();
			} else {
				doLogToFile();
			}
		} catch (Exception e) {
			log = null;
			System.err.println("Exception in RemoteSystemLogListener.initialize(): " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Logs to standard output. 
	 */
	private void doLogToView() {
		// make sure we free resources first
		freeResources();
		// log
		log = new PrintWriter(System.out, true);
		// cach last state
		log_to_stdout = true;
	}

	private void doLogToFile() throws Exception {
		// make sure we free resources first
		freeResources();
		// log
		log =
			new PrintWriter(
				new BufferedWriter(new FileWriter(outputFile.toString(), true)),
				true);
		// cache last state
		log_to_stdout = false;
	}

	public void logging(IStatus status) {
		if (log == null)
			return;
		else {
			// Need a to string here, because we need to be able to compate dates.
			String date = new Date().toString();
			log.println(date);
			int severity = status.getSeverity();
			if (severity == IStatus.ERROR) {
				log.print("ERROR");
			} else if (severity == IStatus.WARNING) {
				log.print("WARNING");
			} else if (severity == IStatus.INFO) {
				log.print("INFO");
			} else if (severity == IStatus.OK) {
				log.print("DEBUG");
			}

			log.print(" ");
			log.print(status.getPlugin());
			// removed for now because we do not use Error codes.
			//log.print("  ");
			//log.print(status.getCode());
			log.print("  ");
			log.println(status.getMessage());
			if (status.getException() != null)
				status.getException().printStackTrace(log);
			if (status.isMultiStatus()) {
				IStatus[] children = status.getChildren();
				for (int i = 0; i < children.length; i++)
					loggingChild(children[i]);
			}
			log.println("--------------------------------------------");
		}

	}

	public void logging(IStatus status, String plugin) {
		logging(status);
	}

	/**
	 * @param tmp org.eclipse.core.runtime.IStatus
	 */
	private void loggingChild(IStatus status) {
		if (log == null)
			return;
		else {
			log.print("\t\t");
			log.println(status.getMessage());
			if (status.getException() != null)
				status.getException().printStackTrace(log);
			if (status.isMultiStatus()) {
				IStatus[] children = status.getChildren();
				for (int i = 0; i < children.length; i++)
					logging(children[i]);
			}
		}
	}

	/**
	 * Handle changes from Preferences page.
	 */
	public synchronized void propertyChange(PropertyChangeEvent event) {
		// refresh the log location from plugin Preference store
		initialize();
	}

	public void freeResources() {
		if (log == null)
			return;

		// make sure to not close std_out. A closed stream can *not*
		// br re-opened!
		if (log_to_stdout)
			return;

		log.flush();
		log.close();
		log = null;

	}
}