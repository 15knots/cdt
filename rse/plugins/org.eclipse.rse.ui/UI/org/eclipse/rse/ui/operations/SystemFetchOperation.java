/********************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 ********************************************************************************/

package org.eclipse.rse.ui.operations;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.subsystems.SubSystem;
import org.eclipse.rse.core.subsystems.SubSystem.DisplayErrorMessageJob;
import org.eclipse.rse.internal.ui.GenericMessages;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
import org.eclipse.rse.ui.view.IContextObject;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.progress.IElementCollector;


/**
 * @author dmcknigh
 */
public class SystemFetchOperation extends JobChangeAdapter implements IRunnableWithProgress
{
    protected IWorkbenchPart _part;
    protected Object _remoteObject;
    protected IElementCollector _collector;
    private IRunnableContext context;
    protected ISystemViewElementAdapter _adapter;
    protected boolean _canRunAsJob;
    protected InvocationTargetException _exc;
 
    public SystemFetchOperation(IWorkbenchPart part, Object remoteObject, ISystemViewElementAdapter adapter, IElementCollector collector) 
    {
		_part = part;
		_remoteObject = remoteObject;
		_collector = collector;
		_adapter = adapter;
		_canRunAsJob = false;
	}
    
    public SystemFetchOperation(IWorkbenchPart part, Object remoteObject, ISystemViewElementAdapter adapter, IElementCollector collector, boolean canRunAsJob) 
    {
		_part = part;
		_remoteObject = remoteObject;
		_collector = collector;
		_adapter = adapter;
		_canRunAsJob = canRunAsJob;
	}
    
    public void setException(InvocationTargetException exc)
    {
    	_exc = exc;
    }
    
	/**
	 * Return the part that is associated with this operation.
	 * 
	 * @return Returns the part or <code>null</code>
	 */
	public IWorkbenchPart getPart() {
		return _part;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		startOperation();
		try {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, 100);
			monitor.setTaskName(getTaskName());
			execute(Policy.subMonitorFor(monitor, 100));
			endOperation();
		} catch (Exception e) {
			// TODO: errors may not be empty (i.e. endOperation has not been executed)
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}
	
	protected void startOperation() {
		//statusCount = 0;
		//resetErrors();
		//confirmOverwrite = true;
	}
	
	protected void endOperation() {
		//handleErrors((IStatus[]) errors.toArray(new IStatus[errors.size()]));
	}
	
	
	public class PromptForPassword implements Runnable
	{
		public SubSystem _ss;
		public PromptForPassword(SubSystem ss)
		{
			_ss = ss;
		}
		
		public void run()
		{
			try
			{
				_ss.promptForPassword();
			}
			catch (Exception e)
			{
				
			}
		}
	}
	
	public class UpdateRegistry implements Runnable
	{
		private SubSystem _ss;
		public UpdateRegistry(SubSystem ss)
		{
			_ss = ss;
		}
		
		public void run()
		{
			ISystemRegistry registry = RSECorePlugin.getTheSystemRegistry();
			registry.connectedStatusChange(_ss, true, false);
		}
	}
	
	/**
	 * Subclasses must override this method to perform the operation.
	 * Clients should never call this method directly.
	 * 
	 * @param monitor
	 * @throws Exception
	 * @throws InterruptedException
	 */
	protected void execute(IProgressMonitor monitor) throws Exception, InterruptedException
	{
		SubSystem ss = null;
		if (_remoteObject instanceof IContextObject)
		{
			ss = (SubSystem)((IContextObject)_remoteObject).getSubSystem();
		}
		else
		{
			ss = (SubSystem)_adapter.getSubSystem(_remoteObject);
		}
		synchronized (ss.getConnectorService())
		{
		if (!ss.isConnected())
		{
			
			Display dis = Display.getDefault();
			dis.syncExec(new PromptForPassword(ss));
			try
			{
				ss.getConnectorService().connect(monitor);
				if (_exc != null)
				{
					showOperationErrorMessage(null, _exc, ss);
				}
			}
			catch (InvocationTargetException exc)
			{
          	  	showOperationErrorMessage(null, exc, ss);
			}
			catch (Exception e)
			{
				showOperationErrorMessage(null, e, ss);
			}
			
			dis.asyncExec(new UpdateRegistry(ss));
			
		}
		}
		Object[] children = null;
		if (_remoteObject instanceof IContextObject)
		{
			children = _adapter.getChildren((IContextObject)_remoteObject, monitor);
		}
		else
		{
			children = _adapter.getChildren((IAdaptable)_remoteObject, monitor);
		}
	    _collector.add(children, monitor);
	    monitor.done();
	}
	
    protected void showOperationErrorMessage(Shell shell, Throwable exc, SubSystem ss)
    {
    	SystemMessage sysMsg = null;
    	if (exc instanceof SystemMessageException)
    	{
    	        displayAsyncMsg(ss, (SystemMessageException)exc);
    	  //sysMsg = ((SystemMessageException)exc).getSystemMessage();
    	} 
    	else
    	{
    	  String excMsg = exc.getMessage();
    	  if ((excMsg == null) || (excMsg.length()==0))
    	    excMsg = "Exception " + exc.getClass().getName(); //$NON-NLS-1$
          sysMsg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_OPERATION_FAILED);
          sysMsg.makeSubstitution(excMsg);
    	
    	
    	 SystemMessageDialog msgDlg = new SystemMessageDialog(shell, sysMsg);
         msgDlg.setException(exc);
    	 msgDlg.open();
         //RSEUIPlugin.logError("Operation failed",exc); now done successfully in msgDlg.open()
    	}
 
    }
    
	/**
	 * Display message on message thread
	 */
	protected void displayAsyncMsg(SubSystem ss, org.eclipse.rse.services.clientserver.messages.SystemMessageException msg)
	{
		DisplayErrorMessageJob job = new DisplayErrorMessageJob(null, msg);
		job.setPriority(Job.INTERACTIVE);
		job.setSystem(true);
		job.schedule();
	}

	protected String getTaskName()
	{
	    return GenericMessages.RSEQuery_task;
	}
	
	/**
	 * Run the operation in a context that is determined by the {@link #canRunAsJob()}
	 * hint. If this operation can run as a job then it will be run in a background thread.
	 * Otherwise it will run in the foreground and block the caller.
	 */
	public final void run() throws InvocationTargetException, InterruptedException {
		if (shouldRun()) {
			getRunnableContext().run(this);
		}
	}

	/**
	 * This method is invoked from the <code>run()</code> method before
	 * the operation is run in the operation's context. Subclasses may
	 * override in order to perform prechecks to determine if the operation
	 * should run. This may include prompting the user for information, etc.
	 * 
	 * @return whether the operation should be run.
	 */
	protected boolean shouldRun() {
		return true;
	}

	/**
	 * Returns the scheduling rule that is to be obtained before this
	 * operation is executed by it's context or <code>null</code> if
	 * no scheduling rule is to be obtained. If the operation is run 
	 * as a job, the schdulin rule is used as the schduling rule of the
	 * job. Otherwise, it is obtained before execution of the operation
	 * occurs.
	 * <p>
	 * By default, no scheduling
	 * rule is obtained. Sublcasses can override to in order ot obtain a
	 * scheduling rule or can obtain schduling rules withing their operation
	 * if finer grained schduling is desired.
	 * 
	 * @return the schduling rule to be obtained by this operation
	 * or <code>null</code>.
	 */
	protected ISchedulingRule getSchedulingRule() {
		return null;
	}
	
	/**
	 * Return whether the auto-build should be postponed until after
	 * the operation is complete. The default is to postpone the auto-build.
	 * subclas can override.
	 * 
	 * @return whether to postpone the auto-build while the operation is executing.
	 */
	protected boolean isPostponeAutobuild() {
		return true;
	}
	
	
	/**
	 * If this operation can safely be run in the background, then subclasses can
	 * override this method and return <code>true</code>. This will make their
	 * action run in a {@link  org.eclipse.core.runtime.jobs.Job}. 
	 * Subsclass that override this method should 
	 * also override the <code>getJobName()</code> method.
	 * 
	 * @return <code>true</code> if this action can be run in the background and
	 * <code>false</code> otherwise.
	 */
	protected boolean canRunAsJob() {
		return _canRunAsJob;
	}
	
	/**
	 * Return the job name to be used if the action can run as a job. (i.e.
	 * if <code>canRunAsJob()</code> returns <code>true</code>).
	 * 
	 * @return the string to be used as the job name
	 */
	protected String getJobName() {
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * This method is called to allow subclasses to configure an action that could be run to show
	 * the results of the action to the user. Default is to return null.
	 * 
	 * @return an action that could be run to see the results of this operation
	 */
	protected IAction getGotoAction() {
		return null;
	}
	
	/**
	 * This method is called to allow subclasses to configure an icon to show when running this
	 * operation.
	 * 
	 * @return an URL to an icon
	 */
	protected URL getOperationIcon() {
		return null;
	}
	
	/**
	 * This method is called to allow subclasses to have the operation remain in the progress
	 * indicator even after the job is done.
	 * 
	 * @return <code>true</code> to keep the operation and <code>false</code> otherwise.
	 */
	protected boolean getKeepOperation() {
		return false;
	}
	
	/**
	 * Return a shell that can be used by the operation to display dialogs, etc.
	 * 
	 * @return a shell
	 */
	protected Shell getShell() 
	{
	    return SystemBasePlugin.getActiveWorkbenchShell();
	}
	
	private ISystemRunnableContext getRunnableContext() {
		if (context == null && canRunAsJob()) {
			SystemJobRunnableContext context = new SystemJobRunnableContext(getJobName(), getOperationIcon(), getGotoAction(), getKeepOperation(), this, getSite());
			context.setPostponeBuild(isPostponeAutobuild());
			context.setSchedulingRule(getSchedulingRule());
			return context;
		} else {
			SystemProgressDialogRunnableContext context = new SystemProgressDialogRunnableContext(getShell());
			context.setPostponeBuild(isPostponeAutobuild());
			context.setSchedulingRule(getSchedulingRule());
			if (this.context != null) {
				context.setRunnableContext(this.context);
			}
			return context;
		}
	}
	
	

	
	private IWorkbenchSite getSite() {
		IWorkbenchSite site = null;
		if(_part != null) {
			site = _part.getSite();
		}
		return site;
	}
}