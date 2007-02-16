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

package org.eclipse.rse.internal.ui.view.team;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.model.ISystemProfileManager;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.internal.model.SystemProfileManager;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemResources;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.swt.widgets.Shell;


/**
 * The action allows users to activate/de-activate a selected profile.
 * @deprecated
 * TODO: delete this action after next MRI rev, as it will be replaced by 
 * {@link org.eclipse.rse.internal.ui.view.team.SystemTeamViewActiveProfileAction} and 
 * {@link org.eclipse.rse.internal.ui.view.team.SystemTeamViewMakeInActiveProfileAction}.
 */
public class SystemTeamViewActiveProfileAction extends SystemBaseAction 
                                 
{
	
	/**
	 * Constructor 
	 */
	public SystemTeamViewActiveProfileAction(Shell parent) 
	{
		super(SystemResources.ACTION_PROFILE_ACTIVATE_LABEL,SystemResources.ACTION_PROFILE_ACTIVATE_TOOLTIP,
		      RSEUIPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SYSTEM_MAKEPROFILEACTIVE_ID),
		      parent);
        allowOnMultipleSelection(true); // as requested by WSDD team
		setContextMenuGroup(ISystemContextMenuConstants.GROUP_CHANGE);
		setHelp(RSEUIPlugin.HELPPREFIX+"actnactp"); //$NON-NLS-1$
		setChecked(false);
	}

	/**
	 * @see SystemBaseAction#updateSelection(IStructuredSelection)
	 */
	public boolean updateSelection(IStructuredSelection selection)
	{
		ISystemProfile profile = (ISystemProfile)getFirstSelection();
		if (profile == null)
		  return false;
		ISystemProfileManager mgr = SystemProfileManager.getSystemProfileManager();
		// todo... we need to have two actions, one to make active, and one to make inactive.
		while (profile != null)
		{
			setChecked(mgr.isSystemProfileActive(profile.getName()));
			profile = (ISystemProfile)getNextSelection();
		}			
		return true;
	}

	/**
	 * This is the method called when the user selects this action.
	 */
	public void run() 
	{
		ISystemRegistry sr = RSEUIPlugin.getTheSystemRegistry();
		ISystemProfile profile = (ISystemProfile)getFirstSelection();
		boolean check = isChecked();
		while (profile != null)
		{
			sr.setSystemProfileActive(profile, check);
			profile = (ISystemProfile)getNextSelection();
		}		
	}		
}