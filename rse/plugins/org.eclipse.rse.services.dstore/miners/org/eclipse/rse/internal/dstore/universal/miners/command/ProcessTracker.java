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

package org.eclipse.rse.internal.dstore.universal.miners.command;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.dstore.core.model.Handler;


public class ProcessTracker extends Handler
{

	private class ProcessDescriptor
	{
		private String _pid;
		private String _cmd;

		public ProcessDescriptor(String pid, String cmd)
		{
			_pid = pid;
			_cmd = cmd;
		}

		public String getPID()
		{
			return _pid;
		}

		public String getCMD()
		{
			return _cmd;
		}

		public String toString()
		{
			String result = getPID() + " " + getCMD(); //$NON-NLS-1$
			if (ProcessDescriptor.this == _newestProcess)
			{
				result += " *"; //$NON-NLS-1$
			}
			return result;
		}

		public boolean hasCMD(String cmdname)
		{
			StringTokenizer tokenizer = new StringTokenizer(_cmd, "/"); //$NON-NLS-1$

			while (tokenizer.hasMoreTokens())
			{
				String token = tokenizer.nextToken();
				if (!tokenizer.hasMoreElements())
				{
					if (token.equals(cmdname))
					{
						return true;
					}
				}
			}
			return false;
		}

		public boolean equals(Object obj)
		{
			if (obj instanceof ProcessDescriptor)
			{
				ProcessDescriptor des = (ProcessDescriptor) obj;
				if (des.getPID().equals(_pid))
				{
					return true;
				}
			}

			return false;
		}
	}

	private Process _psShell;

	private BufferedReader _psReader;
	private BufferedWriter _psWriter;

	private boolean _updateFlag;
	private ProcessDescriptor _newestProcess;
	private String _psCommand;
	private boolean _isEnabled;

	private List _currentProcesses = new ArrayList();

	public ProcessTracker()
	{
		super();
		init();
	}

	private String getFormatOptions(String theOS)
	{
		String formatOptions = ""; //$NON-NLS-1$
		if (theOS.startsWith("z")) //$NON-NLS-1$
		{
			formatOptions = "-o pid,comm"; //$NON-NLS-1$
		}
		else if (theOS.startsWith("linux")) //$NON-NLS-1$
		{
			formatOptions = "--format pid,ucomm"; //$NON-NLS-1$
		}
		else if (theOS.startsWith("aix")) //$NON-NLS-1$
		{
			formatOptions = "-F pid,ucomm"; //$NON-NLS-1$
		}
		return formatOptions;
	}

	private void init()
	{
		String userID = System.getProperty("user.name"); //$NON-NLS-1$
		String userOptions = "-u " + userID; //$NON-NLS-1$

		String theOS = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
		String formatOptions = getFormatOptions(theOS);
		if (formatOptions.length() == 0)
		{
			_isEnabled = false;
		}
		else
		{
			_isEnabled = true;
		}
		if (_isEnabled)
		{

			_psCommand = "ps " + userOptions + " " + formatOptions; //$NON-NLS-1$ //$NON-NLS-2$
			try
			{

				if (_psShell == null)
				{
					_psShell = Runtime.getRuntime().exec("sh"); //$NON-NLS-1$

					String specialEncoding = System.getProperty("dstore.stdin.encoding"); //$NON-NLS-1$
					if (specialEncoding != null)
					{
						_psReader = new BufferedReader(new InputStreamReader(_psShell.getInputStream(), specialEncoding));
						try
						{
							_psWriter = new BufferedWriter(new OutputStreamWriter(_psShell.getOutputStream(), specialEncoding));
						}
						catch (UnsupportedEncodingException e)
						{
							_psWriter = new BufferedWriter(new OutputStreamWriter(_psShell.getOutputStream()));
						}
					}
					else
					{
						_psReader = new BufferedReader(new InputStreamReader(_psShell.getInputStream()));
						_psWriter = new BufferedWriter(new OutputStreamWriter(_psShell.getOutputStream()));
					}
				}
			}
			catch (Exception e)
			{
			}
		}
	}

	private static String readLine(BufferedReader reader)
	{
		StringBuffer theLine = new StringBuffer();
		int ch;
		boolean done = false;
		while (!done)
		{
			try
			{
				synchronized (reader)
				{
					if (!reader.ready())
					{
						return theLine.toString();
					}
					ch = reader.read();
					switch (ch)
					{
						case -1 :
							if (theLine.length() == 0) //End of Reader 
								return null;
							done = true;
							break;
						case 65535 :
							if (theLine.length() == 0)
								return null;
							done = true;
							break;
						case 10 :
							done = true; //Newline
							break;
						case 13 :
							done = true;
							break; //Carriage Return
						default :
							char tch = (char) ch;
							if (!Character.isISOControl(tch))
							{
								theLine.append(tch); //Any other character
							}
							else
							{
								// ignore next char too
								if (reader.ready())
									reader.read();
							}
							break;
							//Any other character
					}
					//Check to see if the BufferedReader is still ready which means there are more characters 
					//in the Buffer...If not, then we assume it is waiting for input.
					if (!reader.ready())
					{
						done = true;
					}
				}
			}
			catch (IOException e)
			{
				return null;
			}
		}
		return theLine.toString();
	}

	private void getCurrentProcesses()
	{
		try
		{
			_psWriter.write(_psCommand);
			_psWriter.write("\n"); //$NON-NLS-1$
			_psWriter.flush();

			// skip first line
			String line = _psReader.readLine();
			ArrayList newPIDs = new ArrayList();
			line = readLine(_psReader);
			while (line != null && line.length() > 0)
			{
				line = line.trim();
				int firstBlank = line.indexOf(' ');
				if (firstBlank != -1)
				{
					String pid = line.substring(0, firstBlank);
					String cmd = line.substring(firstBlank + 1, line.length());
					ProcessDescriptor descriptor = new ProcessDescriptor(pid, cmd);
					if (!descriptor.hasCMD("ps")) //$NON-NLS-1$
					{
						newPIDs.add(descriptor);
					}
				}

				line = readLine(_psReader);
			}
			updateProcesses(newPIDs);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		//listProcesses();
	}

	public void finish()
	{
		_updateFlag = false;
		if (_isEnabled)
		{
			endTrackerProcess();
		}
		super.finish();
	}

	private void updateProcesses(ArrayList newPIDs)
	{
		boolean firstRun = _currentProcesses.size() == 0;
		// remove finished pids
		for (int c = _currentProcesses.size() - 1; c >= 0; c--)
		{
			ProcessDescriptor p = (ProcessDescriptor) _currentProcesses.get(c);
			if (newPIDs.contains(p))
			{
				newPIDs.remove(p);
			}
			else
			{
				_currentProcesses.remove(p);
			}
		}

		// add new pids
		for (int i = 0; i < newPIDs.size(); i++)
		{
			ProcessDescriptor p = (ProcessDescriptor) newPIDs.get(i);
			_currentProcesses.add(p);
			if (!firstRun)
			{
				_newestProcess = p;
			}
		}
	}

	public void endTrackerProcess()
	{
		if (_isEnabled)
		{
			try
			{
				_psWriter.write("exit"); //$NON-NLS-1$
				_psWriter.write("\n"); //$NON-NLS-1$
				_psWriter.flush();

				_psReader.close();
				_psWriter.close();
				_psShell.waitFor();
			}
			catch (Exception e)
			{
			}
		}
	}

	public ProcessDescriptor getNewestProcess()
	{
		return _newestProcess;
	}

	private ProcessDescriptor findLast(String cmd)
	{
		if (_newestProcess != null && _newestProcess.hasCMD(cmd))
		{
			return _newestProcess;
		}
		for (int i = _currentProcesses.size() - 1; i > 0; i--)
		{
			ProcessDescriptor descriptor = (ProcessDescriptor) _currentProcesses.get(i);
			if (descriptor.hasCMD(cmd))
			{
				return descriptor;
			}
		}
		return null;
	}

	public void killCommand(String cmd)
	{
		ProcessDescriptor descriptor = findLast(cmd);
		if (descriptor != null)
		{
			kill(descriptor);
		}
	}

	public void killLastest()
	{
		if (_newestProcess != null)
		{
			kill(_newestProcess);
		}
	}

	private void kill(ProcessDescriptor descriptor)
	{
		if (_isEnabled)
		{
			try
			{
				_psWriter.write("kill " + descriptor.getPID()); //$NON-NLS-1$
				_psWriter.write("\n"); //$NON-NLS-1$
				_psWriter.flush();
				_psReader.reset();
			}
			catch (Exception e)
			{
			}
			doUpdate();
		}
	}

	public void handle()
	{
		if (!_isEnabled)
		{
			finish();
		}
		if (_updateFlag)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (Exception e)
			{
			}
			getCurrentProcesses();
			_updateFlag = false;
		}
	}

	public void doUpdate()
	{
		if (_isEnabled)
			_updateFlag = true;
		else
			_updateFlag = false;
	}

	public synchronized void waitForInput()
	{
		try
		{
			Thread.sleep(100);
		}
		catch (Exception e)
		{
			
		}
	}
}