/*******************************************************************************
 * Copyright (c) 2006 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Martin Oberhuber (Wind River) - initial API and implementation
 * Dave Dykstal (IBM) - fixing bug 162510: correctly process filter strings  
 *******************************************************************************/

package org.eclipse.rse.services.ssh.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import org.eclipse.rse.services.Mutex;
import org.eclipse.rse.services.clientserver.FileTypeMatcher;
import org.eclipse.rse.services.clientserver.IMatcher;
import org.eclipse.rse.services.clientserver.NamePatternMatcher;
import org.eclipse.rse.services.clientserver.PathUtility;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.files.AbstractFileService;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.files.IHostFile;
import org.eclipse.rse.services.files.RemoteFileIOException;
import org.eclipse.rse.services.files.RemoteFileSecurityException;
import org.eclipse.rse.services.ssh.Activator;
import org.eclipse.rse.services.ssh.ISshService;
import org.eclipse.rse.services.ssh.ISshSessionProvider;
import org.eclipse.rse.services.ssh.SshServiceResources;

public class SftpFileService extends AbstractFileService implements IFileService, ISshService
{
	//private SshConnectorService fConnector;
	private ISshSessionProvider fSessionProvider;
	private ChannelSftp fChannelSftp;
	private String fUserHome;
	private Mutex fDirChannelMutex = new Mutex();
	private long fDirChannelTimeout = 5000; //max.5 seconds to obtain dir channel 
	
//	public SftpFileService(SshConnectorService conn) {
//		fConnector = conn;
//	}

	public SftpFileService(ISshSessionProvider sessionProvider) {
		fSessionProvider = sessionProvider;
	}
	
	public String getName() {
		return SshServiceResources.SftpFileService_Name;
	}
	
	public String getDescription() {
		return SshServiceResources.SftpFileService_Description;
	}
	
	public void connect() throws SystemMessageException {
		Activator.trace("SftpFileService.connecting..."); //$NON-NLS-1$
		try {
			Session session = fSessionProvider.getSession();
		    Channel channel=session.openChannel("sftp"); //$NON-NLS-1$
		    channel.connect();
		    fChannelSftp=(ChannelSftp)channel;
			fUserHome = fChannelSftp.pwd();
			Activator.trace("SftpFileService.connected"); //$NON-NLS-1$
		} catch(Exception e) {
			Activator.trace("SftpFileService.connecting failed: "+e.toString()); //$NON-NLS-1$
			throw makeSystemMessageException(e);
		}
	}
	
	/**
	 * Check if the main ssh session is still connected.
	 * Notify ConnectorService of lost session if necessary.
	 * @return <code>true</code> if the session is still healthy.
	 */
	protected boolean checkSessionConnected() {
		Session session = fSessionProvider.getSession();
		if (session==null) {
			// ConnectorService has disconnected already. Nothing to do.
			return false;
		} else if (session.isConnected()) {
			// Session still healthy.
			return true;
		} else {
			// Session was lost, but ConnectorService doesn't know yet.
			// notify of lost session. May reconnect asynchronously later.
			fSessionProvider.handleSessionLost();
			return false;
		}
	}
	
	protected ChannelSftp getChannel(String task) throws SystemMessageException
	{
		Activator.trace(task);
		if (fChannelSftp==null || !fChannelSftp.isConnected()) {
			Activator.trace(task + ": channel not connected: "+fChannelSftp); //$NON-NLS-1$
			if (checkSessionConnected()) {
				//session connected but channel not: try to reconnect
				//(may throw Exception)
				connect();
			} else {
				//session was lost: returned channelSftp will be invalid.
				//This will lead to jsch exceptions (NPE, or disconnected)
				//which are ignored for now since the connection is about
				//to be disconnected anyways.
				throw makeSystemMessageException(new IOException(SshServiceResources.SftpFileService_Error_JschSessionLost));
			}
		}
		return fChannelSftp;
	}
	
	public void disconnect() {
		//disconnect-service may be called after the session is already
		//disconnected (due to event handling). Therefore, don't try to
		//check the session and notify.
		Activator.trace("SftpFileService.disconnect"); //$NON-NLS-1$
		if (fChannelSftp!=null && fChannelSftp.isConnected()) {
			fChannelSftp.disconnect();
		}
		fDirChannelMutex.interruptAll();
		fChannelSftp = null;
	}
	
	
	private SystemMessageException makeSystemMessageException(Exception e) {
		if (e instanceof SystemMessageException) {
			//dont wrap SystemMessageException again
			return (SystemMessageException)e;
		}
		else if (e instanceof SftpException) {
			//Some extra handling to keep Sftp messages
			//TODO more user-friendly messages for more Sftp exception types
			SystemMessageException messageException;
			SftpException sftpe = (SftpException)e;
			if (sftpe.id == ChannelSftp.SSH_FX_PERMISSION_DENIED) {
				messageException = new RemoteFileSecurityException(e);
			} else {
				messageException = new RemoteFileIOException(e);
			}
			messageException.getSystemMessage().makeSubstitution("Sftp: "+sftpe.toString()); //$NON-NLS-1$ //Dont translate since the exception isnt translated either
			return messageException;
		}
		return new RemoteFileIOException(e);
	}
	
	public IHostFile getFile(IProgressMonitor monitor, String remoteParent, String fileName) throws SystemMessageException
	{
		//TODO getFile() must return a dummy even for non-existent files,
		//or the move() operation will fail. This needs to be described in 
		//the API docs.
		SftpHostFile node = null;
		SftpATTRS attrs = null;
		if (fDirChannelMutex.waitForLock(monitor, fDirChannelTimeout)) {
			try {
				attrs = getChannel("SftpFileService.getFile").stat(remoteParent+'/'+fileName); //$NON-NLS-1$
				Activator.trace("SftpFileService.getFile done"); //$NON-NLS-1$
				node = makeHostFile(remoteParent, fileName, attrs);
			} catch(Exception e) {
				Activator.trace("SftpFileService.getFile failed: "+e.toString()); //$NON-NLS-1$
				if ( (e instanceof SftpException) && ((SftpException)e).id==ChannelSftp.SSH_FX_NO_SUCH_FILE) {
					//We MUST NOT throw an exception here. API requires that an empty IHostFile
					//is returned in this case.
				} else {
					throw makeSystemMessageException(e);
				}
			} finally {
				fDirChannelMutex.release();
			}
		}
		if (node==null) {
			node = new SftpHostFile(remoteParent, fileName, false, false, false, 0, 0);
			node.setExists(false);
		}
		return node;
	}
	
	public boolean isConnected() {
		try {
			return getChannel("SftpFileService.isConnected()").isConnected(); //$NON-NLS-1$
		} catch(Exception e) {
			/*cannot be connected when we cannot get a channel*/
		}
		return false;
	}
	
	protected IHostFile[] internalFetch(IProgressMonitor monitor, String parentPath, String fileFilter, int fileType) throws SystemMessageException
	{
		if (fileFilter == null) {
			fileFilter = "*"; //$NON-NLS-1$
		}
		IMatcher filematcher = null;
		if (fileFilter.endsWith(",")) { //$NON-NLS-1$
			String[] types = fileFilter.split(","); //$NON-NLS-1$
			filematcher = new FileTypeMatcher(types, true);
		} else {
			filematcher = new NamePatternMatcher(fileFilter, true, true);
		}
		List results = new ArrayList();
		if (fDirChannelMutex.waitForLock(monitor, fDirChannelTimeout)) {
			try {
			    Vector vv=getChannel("SftpFileService.internalFetch").ls(parentPath); //$NON-NLS-1$
			    for(int ii=0; ii<vv.size(); ii++) {
			    	Object obj=vv.elementAt(ii);
			    	if(obj instanceof ChannelSftp.LsEntry){
			    		ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry)obj;
			    		String fileName = lsEntry.getFilename();
			    		if (".".equals(fileName) || "..".equals(fileName)) { //$NON-NLS-1$ //$NON-NLS-2$
			    			//don't show the trivial names
			    			continue;
			    		}
			    		if (filematcher.matches(fileName) || lsEntry.getAttrs().isDir()) {
			    			SftpHostFile node = makeHostFile(parentPath, fileName, lsEntry.getAttrs());
			    			if (isRightType(fileType, node)) {
			    				results.add(node);
			    			}
			    		}
			    	}
			    }
				Activator.trace("SftpFileService.internalFetch ok"); //$NON-NLS-1$
			} catch(Exception e) {
				//TODO throw new SystemMessageException.
				//We get a "2: No such file" exception when we try to get contents
				//of a symbolic link that turns out to point to a file rather than
				//a directory. In this case, the result is probably expected.
				//We should try to classify symbolic links as "file" or "dir" correctly.
				if (checkSessionConnected()) {
					Activator.trace("SftpFileService.internalFetch failed: "+e.toString()); //$NON-NLS-1$
					throw makeSystemMessageException(e);
				}
				//TODO if not session connected, do we need to throw?
				//Probably not, since the session is going down anyways.  
			} finally {
				fDirChannelMutex.release();
			}
		}
		return (IHostFile[])results.toArray(new IHostFile[results.size()]);
	}

	private SftpHostFile makeHostFile(String parentPath, String fileName, SftpATTRS attrs) {
		SftpATTRS attrsTarget = attrs;
		String linkTarget=null;
		if (attrs.isLink()) {
			//TODO remove comments as soon as jsch-0.1.29 is available 
			//	try {
			//		//Note: readlink() is supported only with jsch-0.1.29 or higher.
			//		//By catching the exception we remain backward compatible.
			//		linkTarget=getChannel("makeHostFile.readlink").readlink(node.getAbsolutePath()); //$NON-NLS-1$
			//		//TODO: Classify the type of resource linked to as file, folder or broken link
			//	} catch(Exception e) {}
			//check if the link points to a directory
			try {
				getChannel("makeHostFile.chdir").cd(parentPath+'/'+fileName); //$NON-NLS-1$
				linkTarget=getChannel("makeHostFile.chdir").pwd(); //$NON-NLS-1$
				if (linkTarget!=null && !linkTarget.equals(parentPath+'/'+fileName)) {
					attrsTarget = getChannel("SftpFileService.getFile").stat(linkTarget); //$NON-NLS-1$
				} else {
					linkTarget=null;
				}
			} catch(Exception e) {
				//dangling link?
				if (e instanceof SftpException && ((SftpException)e).id==ChannelSftp.SSH_FX_NO_SUCH_FILE) {
					linkTarget=":dangling link"; //$NON-NLS-1$
				}
			}
		}
		SftpHostFile node = new SftpHostFile(parentPath, fileName, attrsTarget.isDir(), false, attrs.isLink(), 1000L * attrs.getMTime(), attrs.getSize());
		if (linkTarget!=null) {
			node.setLinkTarget(linkTarget);
		}
		//Permissions: expect the current user to be the owner
		String perms = attrsTarget.getPermissionsString();
		if (perms.indexOf('r',1)<=0) {
			node.setReadable(false); //not readable by anyone
		}
		if (perms.indexOf('w',1)<=0) {
			node.setWritable(false); //not writable by anyone
		}
		if (node.isDirectory()) {
			if (perms.indexOf('x',1)<=0) {
				node.setWritable(false); //directories that are not executable are also not readable
			}
		} else {
			if (perms.indexOf('x',1)>0) {
				node.setExecutable(true); //executable by someone
			}
		}
		if (attrs.getExtended()!=null) {
			node.setExtendedData(attrs.getExtended());
		}
		return node;
	}
	
	public String getSeparator() {
		return "/"; //$NON-NLS-1$
	}
	
	public boolean upload(IProgressMonitor monitor, File localFile, String remoteParent, String remoteFile, boolean isBinary, String srcEncoding, String hostEncoding) throws SystemMessageException
	{
		//TODO what to do with isBinary?
		ChannelSftp channel = null;
		//Fixing bug 158534. TODO remove when bug 162688 is fixed.
		if (monitor==null) {
			monitor = new NullProgressMonitor();
		}
		try {
			SftpProgressMonitor sftpMonitor=new MyProgressMonitor(monitor);
			int mode=ChannelSftp.OVERWRITE;
			String dst = remoteParent;
			if( remoteFile!=null ) {
				if (!dst.endsWith("/")) { //$NON-NLS-1$
					dst += '/';
				}
				dst += remoteFile;
			}
			getChannel("SftpFileService.upload "+remoteFile); //check the session is healthy //$NON-NLS-1$ 
			channel=(ChannelSftp)fSessionProvider.getSession().openChannel("sftp"); //$NON-NLS-1$
		    channel.connect();
			channel.put(localFile.getAbsolutePath(), dst, sftpMonitor, mode); 
			Activator.trace("SftpFileService.upload "+remoteFile+ " ok"); //$NON-NLS-1$ //$NON-NLS-2$
			if (monitor.isCanceled()) {
				return false;
			} else {
				SftpATTRS attr = channel.stat(dst);
				attr.setACMODTIME(attr.getATime(), (int)(localFile.lastModified()/1000));
				////TODO check if we want to maintain permissions
				//if (!localFile.canWrite()) {
				//	attr.setPERMISSIONS( attr.getPermissions() & (~00400));
				//}
				channel.setStat(dst, attr);
				if (attr.getSize() != localFile.length()) {
					//Error: file truncated? - Inform the user!!
					//TODO test if this works
					throw makeSystemMessageException(new IOException(NLS.bind(SshServiceResources.SftpFileService_Error_upload_size,dst)));
					//return false;
				}
			}
		}
		catch (Exception e) {
			Activator.trace("SftpFileService.upload "+remoteFile+" failed: "+e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			throw makeSystemMessageException(e);
			//return false;
		}
		finally {
			if (channel!=null) channel.disconnect();
		}
		return true;
	}

	  public static class MyProgressMonitor implements SftpProgressMonitor
	  {
		  private IProgressMonitor fMonitor;
		  private double fWorkPercentFactor;
		  private Long fMaxWorkKB;
		  private long fWorkToDate;
		  
		  public MyProgressMonitor(IProgressMonitor monitor) {
			  fMonitor = monitor;
		  }
		  public void init(int op, String src, String dest, long max){
			  fWorkPercentFactor = 1.0 / max;
			  fMaxWorkKB = new Long(max / 1024L);
			  fWorkToDate = 0;
			  String srcFile = new Path(src).lastSegment();
			  //String desc = ((op==SftpProgressMonitor.PUT)? 
              //        "Uploading " : "Downloading ")+srcFile;
			  String desc = srcFile;
			  //TODO avoid cast from long to int
			  fMonitor.beginTask(desc, (int)max);
		  }
		  public boolean count(long count){
			  fWorkToDate += count;
			  Long workToDateKB = new Long(fWorkToDate / 1024L);
			  Double workPercent = new Double(fWorkPercentFactor * fWorkToDate);
			  String subDesc = MessageFormat.format(
					  SshServiceResources.SftpFileService_Msg_Progress,
					  new Object[] {
						workToDateKB, fMaxWorkKB, workPercent	  
					  });
			  fMonitor.subTask(subDesc);
		      fMonitor.worked((int)count);
		      return !(fMonitor.isCanceled());
		  }
		  public void end(){
			  fMonitor.done();
		  }
	}

	public boolean upload(IProgressMonitor monitor, InputStream stream, String remoteParent, String remoteFile, boolean isBinary, String hostEncoding) throws SystemMessageException
	{
		//TODO hack for now
		try
		{
			BufferedInputStream bis = new BufferedInputStream(stream);
			File tempFile = File.createTempFile("sftp", "temp"); //$NON-NLS-1$ //$NON-NLS-2$
			FileOutputStream os = new FileOutputStream(tempFile);
			BufferedOutputStream bos = new BufferedOutputStream(os);
	
			 byte[] buffer = new byte[1024];
			 int readCount;
			 while( (readCount = bis.read(buffer)) > 0) 
			 {
			      bos.write(buffer, 0, readCount);
			 }
			 bos.close();
			 upload(monitor, tempFile, remoteParent, remoteFile, isBinary, "", hostEncoding); //$NON-NLS-1$
		}
		catch (Exception e) {
			throw makeSystemMessageException(e);
			//return false;
		}
		return true;
	}
	
	public boolean download(IProgressMonitor monitor, String remoteParent, String remoteFile, File localFile, boolean isBinary, String hostEncoding) throws SystemMessageException
	{
		ChannelSftp channel = null;
		try {
			if (!localFile.exists()) {
				File localParentFile = localFile.getParentFile();
				if (!localParentFile.exists()) {
					localParentFile.mkdirs();
				}
				//localFile.createNewFile();
			}
			//TODO Ascii/binary?
			String remotePath = remoteParent+'/'+remoteFile;
			int mode=ChannelSftp.OVERWRITE;
			MyProgressMonitor sftpMonitor = new MyProgressMonitor(monitor);
			getChannel("SftpFileService.download "+remoteFile); //check the session is healthy //$NON-NLS-1$
			channel=(ChannelSftp)fSessionProvider.getSession().openChannel("sftp"); //$NON-NLS-1$
		    channel.connect();
			channel.get(remotePath, localFile.getAbsolutePath(), sftpMonitor, mode);
			Activator.trace("SftpFileService.download "+remoteFile+ " ok"); //$NON-NLS-1$ //$NON-NLS-2$
			if (monitor.isCanceled()) {
				return false;
			} else {
				SftpATTRS attr = channel.stat(remotePath);
				localFile.setLastModified(1000L * attr.getMTime());
				//TODO should we set the read-only status?
				//if (0==(attrs.getPermissions() & 00400)) localFile.setReadOnly();
				if (attr.getSize() != localFile.length()) {
					//Error: file truncated? - Inform the user!!
					//TODO test if this works
					throw makeSystemMessageException(new IOException(NLS.bind(SshServiceResources.SftpFileService_Error_download_size,remotePath)));
					//return false;
				}
			}
		}
		catch (Exception e) {
			//TODO improve message and handling when trying to download a symlink
			Activator.trace("SftpFileService.download "+remoteFile+" failed: "+e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			throw makeSystemMessageException(e);
			//Note: In case of an exception, the caller needs to ensure that in case
			//we downloaded to a temp file, the temp file is deleted again, or a 
			//broken incorrect file might be synchronized back to the source, thus
			//destroying the original file!!
			//return false;
		}
		finally {
			if (channel!=null) {
				channel.disconnect();
			}
		}
		return true;
	}
	
	public IHostFile getUserHome() {
		//TODO assert: this is only called after we are connected
		int lastSlash = fUserHome.lastIndexOf('/');
		String name = fUserHome.substring(lastSlash + 1);	
		String parent = fUserHome.substring(0, lastSlash);
		try {
			return getFile(null, parent, name);
		} catch(SystemMessageException e) {
			//Could not determine user home
			//return new SftpHostFile(".",".",true,false,false,0,0); //$NON-NLS-1$ //$NON-NLS-2$
			return new SftpHostFile("/", "/", true, true, false, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public IHostFile[] getRoots(IProgressMonitor monitor) {
		IHostFile root = new SftpHostFile("/", "/", true, true, false, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$
		return new IHostFile[] { root };
	}
	
	public IHostFile createFile(IProgressMonitor monitor, String remoteParent, String fileName) throws SystemMessageException 
	{
		IHostFile result = null;
		if (fDirChannelMutex.waitForLock(monitor, fDirChannelTimeout)) {
			try {
				String fullPath = remoteParent + '/' + fileName;
				OutputStream os = getChannel("SftpFileService.createFile").put(fullPath); //$NON-NLS-1$
				//TODO workaround bug 153118: write a single space
				//since jsch hangs when trying to close the stream without writing
				os.write(32); 
				os.close();
				SftpATTRS attrs = getChannel("SftpFileService.createFile.stat").stat(fullPath); //$NON-NLS-1$
				result = makeHostFile(remoteParent, fileName, attrs);
				Activator.trace("SftpFileService.createFile ok"); //$NON-NLS-1$
			} catch (Exception e) {
				Activator.trace("SftpFileService.createFile failed: "+e.toString()); //$NON-NLS-1$
				throw makeSystemMessageException(e);
			} finally {
				fDirChannelMutex.release();
			}
		}
		return result;
	}

	public IHostFile createFolder(IProgressMonitor monitor, String remoteParent, String folderName) throws SystemMessageException
	{
		IHostFile result = null;
		if (fDirChannelMutex.waitForLock(monitor, fDirChannelTimeout)) {
			try {
				String fullPath = remoteParent + '/' + folderName;
				getChannel("SftpFileService.createFolder").mkdir(fullPath); //$NON-NLS-1$
				SftpATTRS attrs = getChannel("SftpFileService.createFolder.stat").stat(fullPath); //$NON-NLS-1$
				result = makeHostFile(remoteParent, folderName, attrs);
				Activator.trace("SftpFileService.createFolder ok"); //$NON-NLS-1$
			} catch (Exception e) {
				Activator.trace("SftpFileService.createFolder failed: "+e.toString()); //$NON-NLS-1$
				throw makeSystemMessageException(e);
			} finally {
				fDirChannelMutex.release();
			}
		}
		return result;
	}

	public boolean delete(IProgressMonitor monitor, String remoteParent, String fileName) throws SystemMessageException
	{
		boolean ok=false;
		Activator.trace("SftpFileService.delete.waitForLock"); //$NON-NLS-1$
		if (fDirChannelMutex.waitForLock(monitor, fDirChannelTimeout)) {
			try {
				String fullPath = remoteParent + '/' + fileName;
				SftpATTRS attrs = null;
				try {
					attrs = getChannel("SftpFileService.delete").stat(fullPath); //$NON-NLS-1$
				} catch (SftpException e) {
					//bug 154419: test for dangling symbolic link 
					if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
						//simply try to delete --> if it really doesnt exist, this will throw an exception
						getChannel("SftpFileService.delete.rm").rm(fullPath); //$NON-NLS-1$
					} else {
						throw e;
					}
				}
				if (attrs==null) {
					//doesn't exist, nothing to do
					ok=true;
				} else if (attrs.isDir()) {
					try {
						getChannel("SftpFileService.delete.rmdir").rmdir(fullPath); //$NON-NLS-1$
						ok=true;
					} catch(SftpException e) {
						if(e.id==ChannelSftp.SSH_FX_FAILURE) {
							//Bug 153649: Recursive directory delete
							//throw new RemoteFolderNotEmptyException();
							String fullPathQuoted = PathUtility.enQuoteUnix(fullPath);
							int rv = runCommand(monitor, "rm -rf "+fullPathQuoted); //$NON-NLS-1$
							ok = (rv==0);
						} else {
							throw e;
						}
					}
				} else {
					getChannel("SftpFileService.delete.rm").rm(fullPath); //$NON-NLS-1$
					ok=true;
				}
				Activator.trace("SftpFileService.delete ok"); //$NON-NLS-1$
			} catch (Exception e) {
				Activator.trace("SftpFileService.delete: "+e.toString()); //$NON-NLS-1$
				throw makeSystemMessageException(e);
			} finally {
				fDirChannelMutex.release();
			}
		}
		return ok;
	}

	public boolean rename(IProgressMonitor monitor, String remoteParent, String oldName, String newName) throws SystemMessageException
	{
		boolean ok=false;
		if (fDirChannelMutex.waitForLock(monitor, fDirChannelTimeout)) {
			try {
				String fullPathOld = remoteParent + '/' + oldName;
				String fullPathNew = remoteParent + '/' + newName;
				getChannel("SftpFileService.rename").rename(fullPathOld, fullPathNew); //$NON-NLS-1$
				ok=true;
				Activator.trace("SftpFileService.rename ok"); //$NON-NLS-1$
			} catch (Exception e) {
				Activator.trace("SftpFileService.rename failed: "+e.toString()); //$NON-NLS-1$
				throw makeSystemMessageException(e);
			} finally {
				fDirChannelMutex.release();
			}
		}
		return ok;
	}
	
	public boolean rename(IProgressMonitor monitor, String remoteParent, String oldName, String newName, IHostFile oldFile) throws SystemMessageException {
		// TODO dont know how to update
		return rename(monitor, remoteParent, oldName, newName);
	}

	private boolean progressWorked(IProgressMonitor monitor, int work) {
		boolean cancelRequested = false;
		if (monitor!=null) {
			monitor.worked(work);
			cancelRequested = monitor.isCanceled();
		}
		return cancelRequested;
	}
	
	public int runCommand(IProgressMonitor monitor, String command) throws SystemMessageException
	{
		Activator.trace("SftpFileService.runCommand "+command); //$NON-NLS-1$
		int result = -1;
		if (monitor!=null) {
			monitor.beginTask(command, 20);
		}
		Channel channel = null;
		try {
			channel=fSessionProvider.getSession().openChannel("exec"); //$NON-NLS-1$
			((ChannelExec)channel).setCommand(command);

			//No user input
			channel.setInputStream(null);
			//Capture error output for exception text
			ByteArrayOutputStream err = new ByteArrayOutputStream();
			((ChannelExec)channel).setErrStream(err);
			InputStream in=channel.getInputStream();
			channel.connect();
			byte[] tmp=new byte[1024];
			while(!channel.isClosed()){
				if( progressWorked(monitor,1) ) {
					break;
				}
				while(in.available()>0){
					int i=in.read(tmp, 0, 1024);
					if(i<0)break;
					//System.out.print(new String(tmp, 0, i));
				}
				try{Thread.sleep(1000);}catch(Exception ee){}
			}
			result = channel.getExitStatus();
			if (result!=0) {
				String errorMsg = err.toString();
				Activator.trace("SftpFileService.runCommand ok, error: "+result+", "+errorMsg); //$NON-NLS-1$ //$NON-NLS-2$
				if (errorMsg.length()>0) {
					throw makeSystemMessageException(new IOException(errorMsg));
				}
			} else {
				Activator.trace("SftpFileService.runCommand ok, result: "+result); //$NON-NLS-1$
			}
		} catch(Exception e) {
			Activator.trace("SftpFileService.runCommand failed: "+e.toString()); //$NON-NLS-1$
			throw makeSystemMessageException(e);
		} finally {
			if (monitor!=null) {
				monitor.done();
			}
			if (channel!=null) {
				channel.disconnect();
			}
		}
		return result;
	}
	
	public boolean move(IProgressMonitor monitor, String srcParent, String srcName, String tgtParent, String tgtName) throws SystemMessageException
	{
		// move is not supported by sftp directly. Use the ssh shell instead.
		// TODO check if newer versions of sftp support move directly
		// TODO Interpret some error messages like "command not found" (use ren instead of mv on windows)
		// TODO mimic by copy if the remote does not support copying between file systems?
		Activator.trace("SftpFileService.move "+srcName); //$NON-NLS-1$
		String fullPathOld = PathUtility.enQuoteUnix(srcParent + '/' + srcName);
		String fullPathNew = PathUtility.enQuoteUnix(tgtParent + '/' + tgtName);
		int rv = runCommand(monitor, "mv "+fullPathOld+' '+fullPathNew); //$NON-NLS-1$
		return (rv==0);
	}

	public boolean copy(IProgressMonitor monitor, String srcParent, String srcName, String tgtParent, String tgtName) throws SystemMessageException {
		// copy is not supported by sftp directly. Use the ssh shell instead.
		// TODO check if newer versions of sftp support copy directly
		// TODO Interpret some error messages like "command not found" (use (x)copy instead of cp on windows)
		Activator.trace("SftpFileService.copy "+srcName); //$NON-NLS-1$
		String fullPathOld = PathUtility.enQuoteUnix(srcParent + '/' + srcName);
		String fullPathNew = PathUtility.enQuoteUnix(tgtParent + '/' + tgtName);
		int rv = runCommand(monitor, "cp -Rp "+fullPathOld+' '+fullPathNew); //$NON-NLS-1$
		return (rv==0);
	}
	
	public boolean copyBatch(IProgressMonitor monitor, String[] srcParents, String[] srcNames, String tgtParent) throws SystemMessageException 
	{
		Activator.trace("SftpFileService.copyBatch "+srcNames); //$NON-NLS-1$
		boolean ok = true;
		for (int i = 0; i < srcParents.length; i++)
		{
			//TODO check what should happen if one file throws an Exception 
			//should the batch job continue? 
			ok = ok && copy(monitor, srcParents[i], srcNames[i], tgtParent, srcNames[i]);
		}
		return ok;
	}

	public void initService(IProgressMonitor monitor) {
		Activator.trace("SftpFileService.initService"); //$NON-NLS-1$
		try
		{
			connect();
		}
		catch (Exception e)
		{			
		}
	}
	
	public void uninitService(IProgressMonitor monitor) {
		Activator.trace("SftpFileService.uninitService"); //$NON-NLS-1$
		disconnect();
	}

	public boolean isCaseSensitive() {
		//TODO find out whether remote is case sensitive or not
		return true;
	}

	public boolean setLastModified(IProgressMonitor monitor, String parent,
			String name, long timestamp) throws SystemMessageException 
	{
		// TODO implement this to set the timestamp on the specified file
		return false;
	}

	public boolean setReadOnly(IProgressMonitor monitor, String parent,
			String name, boolean readOnly) throws SystemMessageException {
		// TODO Auto-generated method stub
		return false;
	}

}
