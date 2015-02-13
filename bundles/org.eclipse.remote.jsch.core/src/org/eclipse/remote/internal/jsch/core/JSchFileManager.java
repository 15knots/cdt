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
package org.eclipse.remote.internal.jsch.core;

import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteFileService;
import org.eclipse.remote.core.IRemoteProcessService;
import org.eclipse.remote.core.IRemoteConnection.Service;

public class JSchFileManager implements IRemoteFileService {

	private final IRemoteConnection fConnection;

	public JSchFileManager(IRemoteConnection connection) {
		fConnection = connection;
	}

	public static class Factory implements IRemoteFileService.Factory {
		@SuppressWarnings("unchecked")
		@Override
		public <T extends Service> T getService(IRemoteConnection remoteConnection, Class<T> service) {
			if (IRemoteFileService.class.equals(service)) {
				return (T) new JSchFileManager(remoteConnection);
			}
			return null;
		}
	}

	@Override
	public IRemoteConnection getRemoteConnection() {
		return fConnection;
	}

	@Override
	public String getDirectorySeparator() {
		return "/"; //$NON-NLS-1$
	}

	@Override
	public IFileStore getResource(String pathStr) {
		IPath path = new Path(pathStr);
		if (!path.isAbsolute()) {
			path = new Path(getBaseDirectory()).append(path);
		}
		return JschFileStore.getInstance(JSchFileSystem.getURIFor(fConnection.getName(), path.toString()));
	}

	@Override
	public String getBaseDirectory() {
		return fConnection.getService(IRemoteProcessService.class).getWorkingDirectory();
	}

	@Override
	public void setBaseDirectory(String path) {
		fConnection.getService(IRemoteProcessService.class).setWorkingDirectory(path);
	}

	@Override
	public String toPath(URI uri) {
		return uri.getPath();
	}

	@Override
	public URI toURI(IPath path) {
		try {
			return JSchFileSystem.getURIFor(fConnection.getName(), path.toString());
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public URI toURI(String path) {
		return toURI(new Path(path));
	}

}
