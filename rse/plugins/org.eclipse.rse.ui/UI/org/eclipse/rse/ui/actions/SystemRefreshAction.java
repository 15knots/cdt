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
 * Michael Berger (IBM) - 146339 Added refresh action graphic.
 ********************************************************************************/

package org.eclipse.rse.ui.actions;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.model.ISystemContainer;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.model.ISystemResourceChangeEvents;
import org.eclipse.rse.model.ISystemResourceChangeListener;
import org.eclipse.rse.model.SystemRegistry;
import org.eclipse.rse.model.SystemResourceChangeEvent;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.swt.widgets.Shell;


/**
 * The action allows users to refresh the selected node in the Remote System Explorer tree view
 */
public class SystemRefreshAction extends SystemBaseAction 
                                 //
{
	private IStructuredSelection _selection = null;
	
	/**
	 * Constructor
	 */
	public SystemRefreshAction(Shell parent) 
	{
		super(SystemResources.ACTION_REFRESH_LABEL, SystemResources.ACTION_REFRESH_TOOLTIP,
				RSEUIPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SYSTEM_REFRESH_ID), // D54577
		      	parent);
        allowOnMultipleSelection(true);
		setContextMenuGroup(ISystemContextMenuConstants.GROUP_BUILD);
		setHelp(RSEUIPlugin.HELPPREFIX+"actn0017"); //$NON-NLS-1$
		setAvailableOffline(true);
	}

	/**
	 * @see SystemBaseAction#updateSelection(IStructuredSelection)
	 */
	public boolean updateSelection(IStructuredSelection selection)
	{
		boolean enable = true;
		_selection = selection;
		return enable;
	}

	/**
	 * This is the method called when the user selects this action.
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() 
	{
		SystemRegistry sr = RSEUIPlugin.getTheSystemRegistry();
		if (_selection != null)
		{
			Vector parents = new Vector();
			
			Iterator iter = _selection.iterator();
			
			while(iter.hasNext())
			{
				Object obj = iter.next();
				
				if (obj instanceof ISystemContainer)
				{
					((ISystemContainer)obj).markStale(true);
				}
				
				// get the adapter and find out if it's a leaf node. If so, refresh the parent as well.
				ISystemViewElementAdapter adapter = getViewAdapter(obj);
				
				if (adapter != null) {
					Object parent = adapter.getParent(obj);
					boolean hasChildren = adapter.hasChildren((IAdaptable)obj);
					
					if ((parent != null) && !hasChildren && (!parents.contains(parent))) {
						parents.add(parent);
						sr.fireEvent(new SystemResourceChangeEvent(parent, ISystemResourceChangeEvents.EVENT_REFRESH, parent));						
					}
					
					sr.fireEvent(new SystemResourceChangeEvent(obj, ISystemResourceChangeEvents.EVENT_REFRESH, obj));
				}
				else {
					sr.fireEvent(new SystemResourceChangeEvent(obj, ISystemResourceChangeEvents.EVENT_REFRESH, obj));
				}
			}
		}
		else
		{
			if ((viewer != null) && (viewer instanceof ISystemResourceChangeListener))
			{			
			  sr.fireEvent((ISystemResourceChangeListener)viewer,
			               new SystemResourceChangeEvent(sr, 
			                    ISystemResourceChangeEvents.EVENT_REFRESH_SELECTED, null));
			}
			else
			  sr.fireEvent(new SystemResourceChangeEvent(sr, ISystemResourceChangeEvents.EVENT_REFRESH_SELECTED, null));
		}
	}		
}