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

package org.eclipse.rse.ui.actions;
import java.util.Iterator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.SystemPlugin;
import org.eclipse.rse.model.IHost;
import org.eclipse.rse.model.ISystemProfile;
import org.eclipse.rse.model.ISystemRegistry;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.SystemResources;
import org.eclipse.rse.ui.SystemSortableSelection;
import org.eclipse.swt.widgets.Shell;


/**
 * The action allows users to move the current connection down in the list
 */
public class SystemMoveDownConnectionAction extends SystemBaseAction 
                                 
{
	
	private ISystemProfile prevProfile = null;
	
	/**
	 * Constructor for SystemMoveDownAction
	 */
	public SystemMoveDownConnectionAction(Shell parent) 
	{
		super(SystemResources.ACTION_MOVEDOWN_LABEL, SystemResources.ACTION_MOVEDOWN_TOOLTIP,
		      SystemPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SYSTEM_MOVEDOWN_ID),
		      parent);
        allowOnMultipleSelection(true);
		setContextMenuGroup(ISystemContextMenuConstants.GROUP_REORDER);        
		setHelp(SystemPlugin.HELPPREFIX+"actn0002");
	}

	/**
	 * We override from parent to do unique checking...
	 * <p>
	 * We intercept to ensure only connections from the same profile are selected.
	 * <p>
	 * @see SystemBaseAction#updateSelection(IStructuredSelection)
	 */
	public boolean updateSelection(IStructuredSelection selection)
	{
		boolean enable = true;
		ISystemRegistry sr = SystemPlugin.getDefault().getSystemRegistry();		
		prevProfile = null;
		Iterator e= ((IStructuredSelection) selection).iterator();		
		while (enable && e.hasNext())
		{
			Object selectedObject = e.next();
			if (selectedObject instanceof IHost)
			{
			  IHost conn = (IHost)selectedObject;
		      int connCount = sr.getHostCountWithinProfile(conn)-1;
			  if (prevProfile == null)
			    prevProfile = conn.getSystemProfile();
			  else
			    enable = (prevProfile == conn.getSystemProfile());
			  if (enable)
			  {
		        enable = (sr.getHostPosition(conn) < connCount);
		        prevProfile = conn.getSystemProfile();
			  }
			}
			else
			  enable = false;
		}
		return enable;
	}

	/**
	 * This is the method called when the user selects this action.
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() 
	{
		ISystemRegistry sr = SystemPlugin.getDefault().getSystemRegistry();	
		SystemSortableSelection[] sortableArray = SystemSortableSelection.makeSortableArray(getSelection());
		IHost conn = null;
		for (int idx=0; idx<sortableArray.length; idx++)
		{
		   conn = (IHost)sortableArray[idx].getSelectedObject();
           sortableArray[idx].setPosition(sr.getHostPosition(conn));
		}
		SystemSortableSelection.sortArray(sortableArray);
		IHost[] conns = (IHost[])SystemSortableSelection.getSortedObjects(sortableArray, new IHost[sortableArray.length]);
		sr.moveHosts(prevProfile.getName(),conns,1);
	}	
}