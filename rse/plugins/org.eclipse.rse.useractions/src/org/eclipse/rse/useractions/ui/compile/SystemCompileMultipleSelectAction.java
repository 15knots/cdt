/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.rse.useractions.ui.compile;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.SystemAdapterHelpers;
import org.eclipse.rse.internal.ui.view.SystemTableViewProvider;
import org.eclipse.rse.ui.GenericMessages;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.rse.ui.view.ISystemEditableRemoteObject;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;
import org.eclipse.rse.useractions.UserActionsResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.AdaptableList;
import org.eclipse.ui.model.WorkbenchContentProvider;

public class SystemCompileMultipleSelectAction extends SystemBaseAction {
	/**
	 * Constructor for multiple select compile action
	 */
	public SystemCompileMultipleSelectAction(Shell shell) {
		super(UserActionsResources.ACTION_COMPILE_NOPROMPT_LABEL, UserActionsResources.ACTION_COMPILE_NOPROMPT_TOOLTIP, (ImageDescriptor) null, shell);
		allowOnMultipleSelection(true);
		setAccelerator(SWT.CTRL | SWT.SHIFT | 'c');
	}

	/**
	 * The default implementation runs the last used compile command for each selected resource.
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		if (checkDirtyEditors()) {
			Object element = getFirstSelection();
			boolean ok = true;
			while (ok && (element != null)) {
				ISystemRemoteElementAdapter rmtAdapter = SystemAdapterHelpers.getRemoteAdapter(element);
				/* FIXME - compile actions not coupled with subsystem API anymore
				 ISubSystem ss = rmtAdapter.getSubSystem(element);
				 ss.getParentSubSystemFactory().getCompileManager().setSystemConnection(ss.getHost());
				 */
				String srcType = null;
				srcType = rmtAdapter.getRemoteSourceType(element);
				if (srcType == null) {
					srcType = "null"; //$NON-NLS-1$
				} else if (srcType.equals("")) { //$NON-NLS-1$
					srcType = "blank"; //$NON-NLS-1$
				}
				/* FIXME - compile actions not coupled with subsystem API anymore
				 ISubSystem subsystem = rmtAdapter.getSubSystem(element);
				 ISystemProfile profile = subsystem.getSystemProfile();
				 
				 // get the compile profile
				 SystemCompileProfile compileProfile = subsystem.getParentSubSystemFactory().getCompileManager().getCompileProfile(profile);
				 
				 // add any contributions from compile extension points
				 // compileProfile.addContributions(element);
				 
				 // get the compile type for the current resource
				 SystemCompileType compType = (SystemCompileType)compileProfile.getCompileType(srcType);
				 
				 
				 // get the last used compile command for that type
				 SystemCompileCommand compileCmd = compType.getLastUsedCompileCommand();
				 
				 SystemCompilableSource compilableSrc = compType.getParentProfile().getCompilableSourceObject(getShell(), element, compileCmd, false, viewer);
				 
				 ok = compilableSrc.runCompileCommand();
				 */
				if (ok) {
					element = getNextSelection();
				}
			}
		}
	}

	protected List getDirtyEditors() {
		IStructuredSelection sel = getSelection();
		List selection = sel.toList();
		List dirtyEditors = new ArrayList();
		for (int i = 0; i < selection.size(); i++) {
			Object selected = selection.get(i);
			if (selected instanceof IAdaptable) {
				ISystemEditableRemoteObject editable = getEditableFor((IAdaptable) selected);
				if (editable != null) {
					try {
						// is the file being edited?
						if (editable.checkOpenInEditor() == 0) {
							// reference the editing editor
							editable.openEditor();
							// file is open in editor - prompt for save
							if (editable.isDirty()) {
								dirtyEditors.add(editable);
							}
						}
					} catch (Exception e) {
					}
				}
			}
		}
		return dirtyEditors;
	}

	protected ISystemEditableRemoteObject getEditableFor(IAdaptable selected) {
		ISystemRemoteElementAdapter adapter = (ISystemRemoteElementAdapter) selected.getAdapter(ISystemRemoteElementAdapter.class);
		if (adapter.canEdit(selected)) {
			ISystemEditableRemoteObject editable = adapter.getEditableRemoteObject(selected);
			try {
				editable.setLocalResourceProperties();
			} catch (Exception e) {
			}
			return editable;
		}
		return null;
	}

	protected boolean checkDirtyEditors() {
		List dirtyEditors = getDirtyEditors();
		if (dirtyEditors.size() > 0) {
			AdaptableList input = new AdaptableList();
			for (int i = 0; i < dirtyEditors.size(); i++) {
				ISystemEditableRemoteObject rmtObj = (ISystemEditableRemoteObject) dirtyEditors.get(i);
				input.add(rmtObj.getRemoteObject());
			}
			WorkbenchContentProvider cprovider = new WorkbenchContentProvider();
			SystemTableViewProvider lprovider = new SystemTableViewProvider(null);
			// TODO: Cannot use WorkbenchMessages -- it's internal
			ListSelectionDialog dlg = new ListSelectionDialog(getShell(), input, cprovider, lprovider, GenericMessages.EditorManager_saveResourcesMessage);
			dlg.setInitialSelections(input.getChildren());
			// TODO: Cannot use WorkbenchMessages -- it's internal
			dlg.setTitle(GenericMessages.EditorManager_saveResourcesTitle);
			int result = dlg.open();
			//Just return false to prevent the operation continuing
			if (result == IDialogConstants.CANCEL_ID) return false;
			Object[] filesToSave = dlg.getResult();
			for (int s = 0; s < filesToSave.length; s++) {
				IAdaptable rmtObj = (IAdaptable) filesToSave[s];
				ISystemEditableRemoteObject editable = getEditableFor(rmtObj);
				editable.doImmediateSaveAndUpload();
			}
		}
		return true;
	}
}
