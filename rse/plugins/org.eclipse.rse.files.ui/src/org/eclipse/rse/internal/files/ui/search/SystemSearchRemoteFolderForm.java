/********************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation. All rights reserved. 
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Kushal Munir (IBM) - initial API and implementation.
 ********************************************************************************/
package org.eclipse.rse.internal.files.ui.search;

import org.eclipse.rse.files.ui.widgets.SystemSelectRemoteFileOrFolderForm;
import org.eclipse.rse.internal.ui.view.SystemSelectRemoteObjectAPIProviderImpl;
import org.eclipse.rse.subsystems.files.core.model.ISystemFileRemoteTypes;
import org.eclipse.rse.ui.messages.ISystemMessageLine;

/**
 * The selection form to use is search selection dialogs.
 */
public class SystemSearchRemoteFolderForm extends SystemSelectRemoteFileOrFolderForm {

	/**
	 * Constructor.
	 * @param msgLine the message line.
	 * @param caller the caller of the contructor for callbacks.
	 */
	public SystemSearchRemoteFolderForm(ISystemMessageLine msgLine, Object caller) {
		super(msgLine, caller, false);
	}

	/**
	 * Returns an instance of the search input provider {@link SystemSearchRemoteObjectAPIProvider}
	 * @see org.eclipse.rse.files.ui.widgets.SystemSelectRemoteFileOrFolderForm#getInputProvider()
	 */
	protected SystemSelectRemoteObjectAPIProviderImpl getInputProvider() {
		
		if (inputProvider == null) {
		    // create the input provider that drives the contents of the tree
			inputProvider = new SystemSearchRemoteObjectAPIProvider(null, ISystemFileRemoteTypes.TYPECATEGORY, true, null); // show new connection prompt, no system type restrictions
		}
		
		return inputProvider;
	}
}
