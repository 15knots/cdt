package org.eclipse.rse.useractions.files.uda;

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
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.useractions.ui.uda.ISystemUDAEditPaneHoster;
import org.eclipse.rse.useractions.ui.uda.ISystemUDTreeView;
import org.eclipse.rse.useractions.ui.uda.ISystemUDTypeEditPaneTypesSelector;
import org.eclipse.rse.useractions.ui.uda.SystemUDTypeEditPane;
import org.eclipse.swt.widgets.Composite;

/**
 * @author coulthar
 *
 * This is a refinement of the generic edit pane for defining
 *   new named file types, specifialized for universal files.
 * Specifically, it overrides the editor used to select/specify
 *  the individual types that constitute the named type.
 */
public class UDTypesEditPaneFiles extends SystemUDTypeEditPane {
	/**
	 * Constructor for UDTypesEditPaneFiles.
	 * @param ss
	 * @param parent
	 * @param tv
	 */
	public UDTypesEditPaneFiles(ISubSystem ss, ISubSystemConfiguration ssf, ISystemProfile profile, ISystemUDAEditPaneHoster parent, ISystemUDTreeView tv) {
		super(ss, ssf, profile, parent, tv);
	}

	/**
	 * Overridable exit point.
	 * Return true if the types are to be auto-uppercased. 
	 * We return false
	 */
	protected boolean getAutoUpperCaseTypes() {
		return false;
	}

	/**
	 * Overridable exit point from parent.
	 * Create the edit widgets that will allow the user to see and
	 *  edit the list of file types that constitute this named type.
	 * <p>
	 * To better facilitate this, the only requirement is that this
	 *  "editor" meet the minimal interface 
	 *   {@link org.eclipse.rse.useractions.ui.uda.ISystemUDTypeEditPaneTypesSelector}
	 * <p>
	 * Our implementation is to return an instance of our custom UDDTypesEditorFiles class.
	 * 
	 * @param parent - the parent composite where the widgets are to go
	 * @param nbrColumns - the number of columns in the parent composite, which these
	 *   widgets should span
	 * @return a class implementing the required interface
	 */
	protected ISystemUDTypeEditPaneTypesSelector createTypesListEditor(Composite parent, int nbrColumns) {
		UDTypesEditorFiles ourEditor = new UDTypesEditorFiles(parent, nbrColumns);
		//ourEditor.setAutoUpperCase(getAutoUpperCaseTypes());
		return ourEditor;
	}
}
