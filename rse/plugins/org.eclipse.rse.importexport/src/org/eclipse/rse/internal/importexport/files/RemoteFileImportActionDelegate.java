/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.rse.internal.importexport.files;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.rse.core.SystemBasePlugin;
import org.eclipse.rse.internal.importexport.RemoteImportExportProblemDialog;
import org.eclipse.rse.internal.importexport.RemoteImportExportResources;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
import org.eclipse.ui.PlatformUI;

/**
 * This class is a remote file import action. 
 */
public class RemoteFileImportActionDelegate extends RemoteFileImportExportActionDelegate {
	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		IFile[] descriptions = getDescriptionFiles(getSelection());
		MultiStatus mergedStatus;
		int length = descriptions.length;
		if (length < 1) {
			return;
		}
		// create read multi status		
		String message;
		if (length > 1) {
			message = RemoteImportExportResources.IMPORT_EXPORT_ERROR_CREATE_FILES_FAILED;
		} else {
			message = RemoteImportExportResources.IMPORT_EXPORT_ERROR_CREATE_FILE_FAILED;
		}
		MultiStatus readStatus = new MultiStatus(RSEUIPlugin.getDefault().getSymbolicName(), 0, message, null);
		RemoteFileImportData[] importDatas = readImportDatas(descriptions, readStatus);
		if (importDatas.length > 0) {
			IStatus status = importFiles(importDatas);
			if (status == null) {
				return;
			}
			if (readStatus.getSeverity() == IStatus.ERROR) {
				message = readStatus.getMessage();
			} else {
				message = status.getMessage();
			}
			// create new status because we want another message - no API to set message
			mergedStatus = new MultiStatus(RSEUIPlugin.getDefault().getSymbolicName(), status.getCode(), readStatus.getChildren(), message, null);
			mergedStatus.merge(status);
		} else {
			mergedStatus = readStatus;
		}
		if (!mergedStatus.isOK()) {
			RemoteImportExportProblemDialog.open(getShell(), RemoteImportExportResources.IMPORT_EXPORT_IMPORT_ACTION_DELEGATE_TITLE, null, mergedStatus);
		}
	}

	private RemoteFileImportData[] readImportDatas(IFile[] descriptions, MultiStatus readStatus) {
		List importDataList = new ArrayList(descriptions.length);
		for (int i = 0; i < descriptions.length; i++) {
			RemoteFileImportData importData = readImportData(descriptions[i], readStatus);
			if (importData != null) {
				importDataList.add(importData);
			}
		}
		return (RemoteFileImportData[]) importDataList.toArray(new RemoteFileImportData[importDataList.size()]);
	}

	/**
	 * Reads the file import data from a file.
	 */
	protected RemoteFileImportData readImportData(IFile description, MultiStatus readStatus) {
		Assert.isLegal(description.isAccessible());
		Assert.isNotNull(description.getFileExtension());
		Assert.isLegal(description.getFileExtension().equals(Utilities.IMPORT_DESCRIPTION_EXTENSION));
		RemoteFileImportData importData = new RemoteFileImportData();
		IRemoteFileImportDescriptionReader reader = null;
		try {
			reader = importData.createImportDescriptionReader(description.getContents());
			// read export data
			reader.read(importData);
			// do not save settings again
			importData.setSaveSettings(false);
		} catch (CoreException ex) {
			String message = MessageFormat.format(RemoteImportExportResources.IMPORT_EXPORT_ERROR_DESCRIPTION_READ, new Object[] { description.getFullPath(), ex.getStatus().getMessage() });
			addToStatus(readStatus, message, ex);
			return null;
		} finally {
			if (reader != null) {
				readStatus.addAll(reader.getStatus());
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (CoreException ex) {
				String message = MessageFormat.format(RemoteImportExportResources.IMPORT_EXPORT_ERROR_DESCRIPTION_CLOSE, new Object[] { description.getFullPath() });
				addToStatus(readStatus, message, ex);
			}
		}
		return importData;
	}

	private IStatus importFiles(RemoteFileImportData[] importDatas) {
		IStatus status = null;
		for (int i = 0; i < importDatas.length; i++) {
			RemoteFileImportOperation op = new RemoteFileImportOperation(importDatas[i], FileSystemStructureProvider.INSTANCE, new RemoteFileOverwriteQuery());
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, true, op);
				status = op.getStatus();
			} catch (InvocationTargetException e) {
				SystemBasePlugin.logError("Error occured trying to import", e); //$NON-NLS-1$
				status = new Status(IStatus.ERROR, RSEUIPlugin.getDefault().getBundle().getSymbolicName(), 0, "", e); //$NON-NLS-1$
			} catch (InterruptedException e) {
				SystemBasePlugin.logError("Error occured trying to import", e); //$NON-NLS-1$
				status = new Status(IStatus.OK, RSEUIPlugin.getDefault().getBundle().getSymbolicName(), 0, "", e); //$NON-NLS-1$
			}
			if (!status.isOK()) {
				SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.FILEMSG_IMPORT_FAILED);
				msg.makeSubstitution(status);
				SystemMessageDialog dlg = new SystemMessageDialog(getShell(), msg);
				dlg.openWithDetails();
				return null;
			}
		}
		return null;
	}

	protected void addToStatus(MultiStatus multiStatus, String defaultMessage, CoreException ex) {
		IStatus status = ex.getStatus();
		String message = ex.getLocalizedMessage();
		if (message == null || message.length() < 1) {
			status = new Status(status.getSeverity(), status.getPlugin(), status.getCode(), defaultMessage, ex);
		}
		multiStatus.add(status);
	}
}
