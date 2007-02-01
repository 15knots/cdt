/********************************************************************************
 * Copyright (c) 2006 IBM Corporation. All rights reserved.
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

package org.eclipse.rse.services.dstore.util;



import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dstore.core.model.DE;
import org.eclipse.dstore.core.model.DataElement;
import org.eclipse.dstore.core.model.DataStore;
import org.eclipse.dstore.extra.internal.extra.DomainEvent;
import org.eclipse.dstore.extra.internal.extra.IDomainListener;
import org.eclipse.rse.dstore.universal.miners.IUniversalDataStoreConstants;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.services.dstore.ServiceResources;
import org.eclipse.swt.widgets.Display;

public class DownloadListener implements IDomainListener,IUniversalDataStoreConstants
{

	private DataElement _status;
	private IProgressMonitor _monitor;
	private DataStore _dataStore;
	private File _localFile;
	private Display _display;

	private boolean _networkDown = false;
	private boolean _isDone = false;
	private boolean _isCancelled = false;
	private long _totalBytesNotified = 0;
	private long _totalLength;

	private static String _percentMsg = SystemMessage.sub(SystemMessage.sub(SystemMessage.sub(ServiceResources.DStore_Service_Percent_Complete_Message, "&0", "{0}"), "&1", "{1}"), "&2", "{2}");	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

	public DownloadListener(IProgressMonitor monitor, DataElement status, File localFile, String remotePath, long totalLength)
	{
		_monitor = monitor;
		_status = status;
		_totalLength = totalLength;
		_display = Display.getCurrent();

		if (_status == null)
		{
		    System.out.println("Status is null!"); //$NON-NLS-1$
		}
		
		_dataStore = _status.getDataStore();
		_dataStore.getDomainNotifier().addDomainListener(this);
		
		_localFile = localFile;

		if (monitor != null)
		{
			/* DKM - DO WE NEED THIS?!!
			while (_display!=null && _display.readAndDispatch()) {
				//Process everything on event queue
			}
			*/
		}
		if (_status.getValue().equals("done")) //$NON-NLS-1$
		{
			updateDownloadState();
			setDone(true);
		}
	
	}
	public boolean isCancelled()
	{
		return _isCancelled;
	}

	public DataElement getStatus()
	{
		return _status;
	}

	/**
	 * @see IDomainListener#listeningTo(DomainEvent)
	 */
	public boolean listeningTo(DomainEvent event)
	{
		if (_status == null)
		{
			return false;
		}

		if (_status == event.getParent())
		{
			return true;
		}

		return false;
	}

	/**
	 * @see IDomainListener#domainChanged(DomainEvent)
	 */
	public void domainChanged(DomainEvent event)
	{
		if (_status.getValue().equals("done")) //$NON-NLS-1$
		{
			if (_status == event.getParent())
			{
				setDone(true);
			}
		}
		else
		{
			updateDownloadState();
		}
	}

	private void updateDownloadState()
	{

		if (_monitor != null)
		{
			long currentLength =  _localFile.length();
			long delta = currentLength - _totalBytesNotified;
			if (delta > 0)
			{
				//System.out.println(_status.getAttribute(DE.A_SOURCE));
				_monitor.worked((int)delta);
				
				try
				{
					long percent = (currentLength * 100) / _totalLength;
												
						
					StringBuffer current = new StringBuffer();
					current.append(currentLength /KB_IN_BYTES);
					current.append(" KB"); //$NON-NLS-1$
					
					StringBuffer total = new StringBuffer();
					total.append(_totalLength / KB_IN_BYTES);
					total.append(" KB"); //$NON-NLS-1$
					
					StringBuffer percentBuf = new StringBuffer();
					percentBuf.append(percent);
					percentBuf.append("%"); //$NON-NLS-1$
					
					String str = MessageFormat.format(_percentMsg, new Object[] {current, total, percentBuf});
					
					_monitor.subTask(str);
					
					/* DKM - DO WE NEED THIS?!!
					while (_display != null && _display.readAndDispatch()) {
						//Process everything on event queue
					}
					*/
				}
				catch (Exception e)
				{
				}
				_totalBytesNotified = currentLength;
			}
		}
		
		if (!_status.getDataStore().getStatus().getName().equals("okay")) //$NON-NLS-1$
		{
			_networkDown = true;
		}
	}

	/**
	 * setDone(boolean)
	 */
	public void setDone(boolean done)
	{
		this._isDone = done;
		if (done)
		{
			updateDownloadState();
			_status.getDataStore().getDomainNotifier().removeDomainListener(this);

		}
	}



	/**
	 * 
	 */
	public boolean wasCancelled()
	{
		return _isCancelled;
	}



	/**
     * Wait for the the status DataElement to be refreshed
     *
     * @return The status DataElement after it has been updated, or the user
     *         has pressed cancel
     *
     * @throws InterruptedException if the thread was interrupted.
     */
	public DataElement waitForUpdate() throws InterruptedException
	{
		return waitForUpdate(0); //No diagnostic
	}

	/**
	 * Wait for the the status DataElement to be refreshed
	 *
	 * @param wait threshold for starting diagnostic. Default is 60 seconds; a zero means to use the default.
	 *             -1 means to force a timeout; mainly for testing purpose.  
	 *
	 * @return The status DataElement after it has been updated, or the user
	 *         has pressed cancel
	 *
	 * @throws InterruptedException if the thread was interrupted.
	 */
	public DataElement waitForUpdate(int wait) throws InterruptedException
	{
		Display display = _display;

		if (wait > 0)
		{
		}
		else if (wait == -1)
		{
		}

		
		
		if (display != null)
		{
			
			// Current thread is UI thread
			while (!_isDone && !_isCancelled && !_networkDown)
			{
				while (display.readAndDispatch()) {
					//Process everything on event queue
				}

				if ((_monitor != null) && (_monitor.isCanceled()))
				{
					// cancel remote request		
					cancelDownload();								
					_isCancelled = true;
					setDone(true);
				}
				else if (_networkDown)
				{
					_isCancelled = true;
					setDone(true);
					throw new InterruptedException();
				}

				if (getStatus().getAttribute(DE.A_NAME).equals("done")) //$NON-NLS-1$
				{
					setDone(true);
				}
				else
				{
				    Thread.sleep(100);
					updateDownloadState();
				}
			}

		}
		else
		{
			// Current thread is not UI thread
			while (!_isDone && !_isCancelled && !_networkDown)
			{
				if ((_monitor != null) && (_monitor.isCanceled()))
				{
					cancelDownload();
					_isCancelled = true;
					setDone(true);
				}
				else if (_networkDown)
				{
					_isCancelled = true;
					setDone(true);
					throw new InterruptedException();
				}
				if (getStatus().getAttribute(DE.A_NAME).equals("done")) //$NON-NLS-1$
				{
					setDone(true); 
				}
				else
				{
				    Thread.sleep(100);
					updateDownloadState();
				}
			}
		}
		return _status;
	}

	public void cancelDownload()
		{
			DataElement status = _status;
			if (status != null)
			{
				DataElement command = status.getParent();
				DataStore dataStore = command.getDataStore();
				DataElement cmdDescriptor = command.getDescriptor();
				DataElement cancelDescriptor = dataStore.localDescriptorQuery(cmdDescriptor, "C_CANCEL"); //$NON-NLS-1$
				if (cancelDescriptor != null)
				{
					dataStore.command(cancelDescriptor, command);
				}
				_localFile.delete();
			}
			if (_monitor != null)
			{
				_monitor.done();
			}
		}
}