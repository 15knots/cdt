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

package org.eclipse.rse.ui.view;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;

/**
 * Abstraction for any object that wishes to be a roots-provider for the SystemView tree viewer.
 */
public interface ISystemViewInputProvider extends IAdaptable {
	
	/**
	 * @return the children objects to consistute the root elements in the system view tree
	 */
	public Object[] getSystemViewRoots();

	/**
	 * @return true if {@link #getSystemViewRoots()} will return a non-empty list
	 */
	public boolean hasSystemViewRoots();

	/**
	 * @return true if we are listing connections or not, so we know whether we are interested in 
	 * connection-add events
	 */
	public boolean showingConnections();

	/**
	 * This method is called by the connection adapter when the user expands
	 * a connection. This method must return the child objects to show for that
	 * connection.
	 * @param selectedConnection the connection undergoing expansion
	 * @return the list of objects under the connection
	 */
	public Object[] getConnectionChildren(IHost selectedConnection);

	/**
	 * This method is called by the connection adapter when deciding to show a plus-sign
	 * or not beside a connection.
	 * @param selectedConnection the connection being shown in the viewer 
	 * @return true if this connection has children to be shown.
	 */
	public boolean hasConnectionChildren(IHost selectedConnection);

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapterType);

	/**
	 * Set the shell in case it is needed for anything.
	 * The label and content provider will call this.
	 * @param shell the shell being used by the viewer using this provider
	 */
	// TODO should possibly be deprecated or removed
	public void setShell(Shell shell);

	/**
	 * @return the shell of the viewer we are currently associated with
	 */
	// TODO should possibly be deprecated or removed
	public Shell getShell();

	/**
	 * Set the viewer in case it is needed for anything.
	 * The label and content provider will call this.
	 * @param viewer the viewer that uses this provider
	 */
	// TODO should possibly be deprecated or removed
	public void setViewer(Viewer viewer);

	/**
	 * @return the viewer we are currently associated with
	 */
	// TODO should possibly be deprecated or removed
	public Viewer getViewer();

	/**
	 * @return true to show the action bar (ie, toolbar) above the viewer.
	 * The action bar contains connection actions, predominantly.
	 */
	public boolean showActionBar();

	/**
	 * @return true to show the button bar above the viewer.
	 * The tool bar contains "Get List" and "Refresh" buttons and is typicall
	 * shown in dialogs that list only remote system objects.
	 */
	public boolean showButtonBar();

	/**
	 * @return true to show right-click popup actions on objects in the tree.
	 */
	public boolean showActions();

}