/********************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation. All rights reserved.
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

package org.eclipse.rse.files.ui.resources;
import org.eclipse.rse.files.ui.widgets.SystemSelectRemoteFileOrFolderForm;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.swt.widgets.Shell;


/**
 * @author mjberger
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class AddToArchiveDialog extends CombineDialog {

	private String[] _relativePaths;

	public AddToArchiveDialog(Shell shell) 
	{
		super(shell);
		setHelp(RSEUIPlugin.HELPPREFIX + "atad0000"); //$NON-NLS-1$
	}

	public AddToArchiveDialog(Shell shell, String title) {
		super(shell, title);
		setHelp(RSEUIPlugin.HELPPREFIX + "atad0000"); //$NON-NLS-1$
	}
	
	public AddToArchiveDialog(Shell shell, String title, String[] relativePaths) 
	{
		super(shell, title);
		setHelp(RSEUIPlugin.HELPPREFIX + "atad0000"); //$NON-NLS-1$
		_relativePaths = relativePaths;
		((AddToArchiveForm)form).setRelativePathList(_relativePaths);
	}

	public AddToArchiveDialog(
		Shell shell,
		String title,
		boolean prePopSelection) 
	{
		super(shell, title, prePopSelection);
		setHelp(RSEUIPlugin.HELPPREFIX + "atad0000"); //$NON-NLS-1$
	}
	
	public AddToArchiveDialog(
		Shell shell,
		String title,
		boolean prePopSelection,
		String[] relativePaths) 
	{
		super(shell, title, prePopSelection);
		setHelp(RSEUIPlugin.HELPPREFIX + "atad0000"); //$NON-NLS-1$
		_relativePaths = relativePaths;
		((AddToArchiveForm)form).setRelativePathList(_relativePaths);
	}


	protected SystemSelectRemoteFileOrFolderForm getForm(boolean fileMode)
	{
		super.getForm(fileMode);
		form = new AddToArchiveForm(getMessageLine(), this, fileMode, prePop, _relativePaths);
		return form;
	}
	
	public boolean getSaveFullPathInfo()
	{
		return ((AddToArchiveForm)form).getSaveFullPathInfo();
	}
	
	public String getRelativePath()
	{
		return ((AddToArchiveForm)form).getRelativePath();
	}
}