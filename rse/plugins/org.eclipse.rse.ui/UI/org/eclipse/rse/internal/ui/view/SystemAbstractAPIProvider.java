/********************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * Tobias Schwarz   (Wind River) - [173267] "empty list" should not be displayed 
 ********************************************************************************/

package org.eclipse.rse.internal.ui.view;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.ISystemMessageObject;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.model.SystemMessageObject;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.ISystemPreferencesConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.rse.ui.view.ISystemViewInputProvider;
import org.eclipse.rse.ui.view.SystemAdapterHelpers;
import org.eclipse.swt.widgets.Shell;



/**
 * This is a base class that a provider of root nodes to the remote systems tree viewer part can
 * use as a parent class.
 */
public abstract class SystemAbstractAPIProvider 
       implements ISystemViewInputProvider
{


	protected Shell shell;
	protected Viewer viewer;
	protected ISystemRegistry sr;
	
	protected Object[] emptyList = new Object[0];
	protected Object[] msgList   = new Object[1];
	/**
	 * @deprecated Use {@link #checkForEmptyList(Object[], Object, boolean)} instead.
	 */
	protected SystemMessageObject nullObject     = null;
	protected SystemMessageObject canceledObject = null;	
	protected SystemMessageObject errorObject    = null;	
	
	private Preferences fPrefStore = null;
	
	/**
	 * Constructor 
	 */
	public SystemAbstractAPIProvider()
	{
		super();
		sr = RSECorePlugin.getTheSystemRegistry();
	}
	
    /**
	 * This is the method required by the IAdaptable interface.
	 * Given an adapter class type, return an object castable to the type, or
	 *  null if this is not possible.
	 */
    public Object getAdapter(Class adapterType)
    {
   	    return Platform.getAdapterManager().getAdapter(this, adapterType);	
    }           

    /**
     * Set the shell in case it is needed for anything.
     * The label and content provider will call this.
     */
    public void setShell(Shell shell)
    {
    	this.shell = shell;
    }
    
    /**
     * Return the shell of the current viewer
     */
    public Shell getShell()
    {
    	return shell;
    }

    /**
     * Set the viewer in case it is needed for anything.
     * The label and content provider will call this.
     */
    public void setViewer(Viewer viewer)
    {
    	this.viewer = viewer;
    }
    
    /**
     * Return the viewer we are currently associated with
     */
    public Viewer getViewer()
    {
    	return viewer;
    }

    protected final void initMsgObjects()
 	{
 		nullObject     = new SystemMessageObject(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_EXPAND_EMPTY),ISystemMessageObject.MSGTYPE_EMPTY, null);
 		canceledObject = new SystemMessageObject(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_LIST_CANCELLED),ISystemMessageObject.MSGTYPE_CANCEL, null);
 		errorObject    = new SystemMessageObject(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_EXPAND_FAILED),ISystemMessageObject.MSGTYPE_ERROR, null);
 	}

    /**
     * <i>Callable by subclasses. Do not override</i><br>
     * In getChildren, return <samp>checkForEmptyList(children, parent, true/false)<.samp>
     * versus your array directly. This method checks for a null array which is
     * not allowed and replaces it with an empty array. 
     * If true is passed then it returns the "Empty list" message object if the array is null or empty
     * 
     * @param children The list of children.
     * @param parent The parent for the children.
     * @param returnNullMsg <code>true</code> if an "Empty List" message should be returned.
     * @return The list of children, a list with the "Empty List" message object or an empty list.
     */
    protected Object[] checkForEmptyList(Object[] children, Object parent, boolean returnNullMsg) {
    	if ((children == null) || (children.length == 0)) {
    		if (fPrefStore == null) {
    			fPrefStore = RSEUIPlugin.getDefault().getPluginPreferences();
    		}
    		if (!returnNullMsg
    				|| (fPrefStore != null && !fPrefStore
    						.getBoolean(ISystemPreferencesConstants.SHOW_EMPTY_LISTS))) {
    			return emptyList;
    		} else {
    			return new Object[] {
    				new SystemMessageObject(
    					RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_EXPAND_EMPTY),
    					ISystemMessageObject.MSGTYPE_EMPTY, 
    					parent)};
    		}
    	}
    	return children;
    }
    
    /**
     * In getChildren, return checkForNull(children, true/false) vs your array directly.
     * This method checks for a null array which not allow and replaces it with an empty array.
     * If true is passed then it returns the "Empty list" message object if the array is null or empty
     * 
     * @deprecated Use {@link #checkForEmptyList(Object[], Object, boolean)} instead.
     */
    protected Object[] checkForNull(Object[] children, boolean returnNullMsg)
    {
	   if ((children == null) || (children.length==0))
	   {
	   	 if (!returnNullMsg)
           return emptyList;
         else
         {
	 	   if (nullObject == null)
	 	     initMsgObjects();
	 	   msgList[0] = nullObject;
	 	   return msgList;
         }
	   }
       else
         return children;
    }

    /**
     * Return the "Operation cancelled by user" msg as an object array so can be used to answer getChildren()
     */
    protected Object[] getCancelledMessageObject()
    {    	
		 if (canceledObject == null)
		   initMsgObjects();
		 msgList[0] = canceledObject;
		 return msgList;
    }    

    /**
     * Return the "Operation failed" msg as an object array so can be used to answer getChildren()
     */
    protected Object[] getFailedMessageObject()
    {    	
		 if (errorObject == null)
		   initMsgObjects();
		 msgList[0] = errorObject;
		 return msgList;
    }    

	/**
	 * Return true if we are listing connections or not, so we know whether we are interested in 
	 *  connection-add events
	 */
	public boolean showingConnections()
	{
		return false;
	}

	// ------------------
	// HELPER METHODS...
	// ------------------	
    /**
     * Returns the implementation of ISystemViewElement for the given
     * object.  Returns null if the adapter is not defined or the
     * object is not adaptable.
     */
    protected ISystemViewElementAdapter getViewAdapter(Object o) 
    {
    	return SystemAdapterHelpers.getViewAdapter(o);
    }
    
    /**
     * Returns the implementation of ISystemRemoteElement for the given
     * object.  Returns null if this object does not adaptable to this.
     */
    protected ISystemRemoteElementAdapter getRemoteAdapter(Object o) 
    {
    	return SystemAdapterHelpers.getRemoteAdapter(o);
    }
}