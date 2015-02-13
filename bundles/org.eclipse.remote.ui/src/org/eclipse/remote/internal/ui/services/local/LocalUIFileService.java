/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.remote.internal.ui.services.local;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteConnectionType;
import org.eclipse.remote.core.IRemoteConnectionType.Service;
import org.eclipse.remote.ui.IRemoteUIFileService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class LocalUIFileService implements IRemoteUIFileService {
	private final IRemoteConnectionType connectionType;
	private IRemoteConnection connection = null;

	public LocalUIFileService(IRemoteConnectionType connectionType) {
		this.connectionType = connectionType;
	}

	public static class Factory implements IRemoteConnectionType.Service.Factory {
		@SuppressWarnings("unchecked")
		@Override
		public <T extends Service> T getService(IRemoteConnectionType connectionType, Class<T> service) {
			if (IRemoteUIFileService.class.equals(service)) {
				return (T) new LocalUIFileService(connectionType);
			}
			return null;
		}
	}

	@Override
	public IRemoteConnectionType getConnectionType() {
		return connectionType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.remote.core.IRemoteFileManager#browseDirectory(org.eclipse
	 * .swt.widgets.Shell, java.lang.String, java.lang.String)
	 */
	@Override
	public String browseDirectory(Shell shell, String message, String filterPath, int flags) {
		DirectoryDialog dialog = new DirectoryDialog(shell);
		dialog.setText(message);
		if (filterPath != null) {
			File path = new File(filterPath);
			if (path.exists()) {
				dialog.setFilterPath(path.isFile() ? path.getParent() : filterPath);
			}
		}

		String path = dialog.open();
		if (path == null) {
			return null;
		}

		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.remote.core.IRemoteFileManager#browseFile(org.eclipse
	 * .swt.widgets.Shell, java.lang.String, java.lang.String)
	 */
	@Override
	public String browseFile(Shell shell, String message, String filterPath, int flags) {
		FileDialog dialog = new FileDialog(shell, SWT.SINGLE);
		dialog.setText(message);
		if (filterPath != null) {
			File path = new File(filterPath);
			if (path.exists()) {
				dialog.setFilterPath(path.isFile() ? path.getParent() : filterPath);
			}
		}

		String path = dialog.open();
		if (path == null) {
			return null;
		}

		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.remote.core.IRemoteFileManager#browseFile(org.eclipse
	 * .swt.widgets.Shell, java.lang.String, java.lang.String)
	 */
	@Override
	public List<String> browseFiles(Shell shell, String message, String filterPath, int flags) {
		FileDialog dialog = new FileDialog(shell, SWT.MULTI);
		dialog.setText(message);
		if (filterPath != null) {
			File path = new File(filterPath);
			if (path.exists()) {
				dialog.setFilterPath(path.isFile() ? path.getParent() : filterPath);
			}
		}

		String path = dialog.open();
		if (path == null) {
			return null;
		}

		return Arrays.asList(dialog.getFileNames());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.remote.ui.IRemoteUIFileManager#getConnection()
	 */
	@Override
	public IRemoteConnection getConnection() {
		return connection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.remote.ui.IRemoteUIFileManager#setConnection(org.eclipse
	 * .remote.core.IRemoteConnection)
	 */
	@Override
	public void setConnection(IRemoteConnection connection) {
		this.connection = connection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.remote.ui.IRemoteUIFileManager#showConnections(boolean)
	 */
	@Override
	public void showConnections(boolean enable) {
		// Not implemented
	}
}
