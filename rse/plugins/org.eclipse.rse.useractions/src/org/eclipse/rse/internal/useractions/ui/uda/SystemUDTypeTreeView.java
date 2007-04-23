/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * Martin Oberhuber (Wind River) - [168975] Move RSE Events API to Core
 *******************************************************************************/
package org.eclipse.rse.internal.useractions.ui.uda;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.rse.core.events.ISystemModelChangeEvents;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.swt.widgets.Composite;

/**
 * In the Work With User Defined File Types dialog, this is the
 *  tree view for showing the existing types.
 */
public class SystemUDTypeTreeView extends SystemUDBaseTreeView {
	/**
	 * Constructor when we have a subsystem
	 */
	public SystemUDTypeTreeView(Composite parent, ISystemUDWorkWithDialog editPane, ISubSystem ss) {
		/* FIXME - UDA not coupled with subsystem API anymore */
		super(parent, editPane, ss, /*ss.getUDActionSubsystem().getUDTypeManager()*/null);
	}

	/**
	 * Constructor when we have a subsystem factory and profile
	 */
	public SystemUDTypeTreeView(Composite parent, ISystemUDWorkWithDialog editPane, ISubSystemConfiguration ssFactory, ISystemProfile profile) {
		super(parent, editPane, ssFactory, profile,
		/* FIXME - UDA not coupled with subsystem API anymore
		 ((ISubsystemFactoryAdapter)ssFactory.getAdapter(ISubsystemFactoryAdapter.class)).getActionSubSystem(ssFactory, null).getUDTypeManager()
		 */
		null);
	}

	/**
	 * Return types manager
	 */
	public SystemUDTypeManager getTypeManager() {
		return (SystemUDTypeManager) super.getDocumentManager();
	}

	/**
	 * Get the selected type name.
	 * Returns "" if nothing selected
	 */
	public String getSelectedTypeName() {
		return super.getSelectedElementName();
	}

	/**
	 * Get the selected type domain.
	 * Returns -1 if nothing selected or domains not supported
	 */
	public int getSelectedTypeDomain() {
		return super.getSelectedElementDomain();
	}

	/**
	 * Return message for delete confirmation
	 */
	protected SystemMessage getDeleteConfirmationMessage() {
		return RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONFIRM_DELETE_USERTYPE);
	}

	/**
	 * Return the {@link org.eclipse.rse.core.events.ISystemModelChangeEvents} constant representing the resource type managed by this tree.
	 * This is a parent class override.
	 */
	protected int getResourceType() {
		return ISystemModelChangeEvents.SYSTEM_RESOURCETYPE_NAMEDTYPE;
	}

	/**
	 * Parent override.
	 * Restore the selected type to its IBM-supplied default value.
	 */
	public void doRestore() {
		SystemXMLElementWrapper selectedElement = getSelectedElement();
		if ((selectedElement == null) || !(selectedElement instanceof SystemUDTypeElement)) return;
		SystemUDTypeElement type = (SystemUDTypeElement) selectedElement;
		boolean ok = getDocumentManager().getActionSubSystem().restoreDefaultType(type, type.getDomain(), type.getOriginalName());
		if (ok) {
			type.setUserChanged(false);
			getDocumentManager().saveUserData(profile);
			selectElement(selectedElement);
			String[] allProps = { IBasicPropertyConstants.P_TEXT, IBasicPropertyConstants.P_IMAGE };
			update(selectedElement, allProps);
		}
	}

	// ------------------------------------
	// HELPER METHODS CALLED FROM EDIT PANE
	// ------------------------------------
	/**
	 * Select the given type
	 */
	public void selectType(SystemUDTypeElement element) {
		super.selectElement(element);
	}

	/**
	 * Refresh the parent of the given action.
	 * That is, find the parent and refresh the children.
	 * If the parent is not found, assume it is because it is new too,
	 *  so refresh the whole tree.
	 */
	public void refreshTypeParent(SystemUDTypeElement element) {
		super.refreshElementParent(element);
	}
}
