/********************************************************************************
 * Copyright (c) 2007 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * David Dykstal (IBM) - 168977: refactoring IConnectorService
 ********************************************************************************/
package org.eclipse.rse.ui.subsystems;

import org.eclipse.rse.core.PasswordPersistenceManager;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.SystemSignonInformation;
import org.eclipse.rse.core.subsystems.AbstractCredentialsProvider;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ICredentials;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.core.subsystems.util.ISubSystemConfigurationAdapter;
import org.eclipse.rse.logging.Logger;
import org.eclipse.rse.logging.LoggerFactory;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.dialogs.ICredentialsValidator;
import org.eclipse.rse.ui.dialogs.ISystemPasswordPromptDialog;
import org.eclipse.rse.ui.dialogs.SystemChangePasswordDialog;
import org.eclipse.rse.ui.dialogs.SystemPasswordPromptDialog;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
import org.eclipse.rse.ui.validators.ISystemValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * The {@link StandardCredentialsProvider} is an extension of
 * {@link AbstractCredentialsProvider} that provides for the prompting of a userid
 * and password.
 * <p>
 * It uses a {@link PasswordPersistenceManager} to store the passwords in the
 * keychain keyed by {@link IHost} and possibly by {@link ISubSystemConfiguration}.
 * <p>
 * This is suitable for use by subclasses of {@link StandardConnectorService}
 * that wish to provide prompting and persistence for userids and passwords when
 * connecting.
 * <p>
 * This class is may be subclassed. Typically to provide connector service
 * specific prompting.
 */
public class StandardCredentialsProvider extends AbstractCredentialsProvider {
	/**
	 * A runnable that will actually perform the prompting.
	 */
	private class PromptForCredentials implements Runnable {
		private boolean canceled = false;

		/**
		 * @return true if prompting was canceled.
		 */
		public boolean isCanceled() {
			return canceled;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			ISystemPasswordPromptDialog dialog = getPasswordPromptDialog(getShell());
			dialog.setSystemInput(getConnectorService());
			dialog.setForceToUpperCase(forcePasswordToUpperCase());
			dialog.setSignonValidator(getSignonValidator());
			if (supportsUserId()) {
				dialog.setUserIdValidator(getUserIdValidator());
			}
			if (supportsPassword()) {
				dialog.setSavePassword(savePassword);
				dialog.setPassword(password);
				dialog.setPasswordValidator(getPasswordValidator());
			}
			try {
				dialog.open();
			} catch (Exception e) {
				logException(e);
			}
			canceled = dialog.wasCancelled();
			if (!canceled) {
				userId = dialog.getUserId();
				password = dialog.getPassword();
				saveUserId = dialog.getIsUserIdChangePermanent();
				savePassword = dialog.getIsSavePassword();
			}
		}
	}

	/**
	 * A runnable that will prompt for a new password. Typically used when 
	 * a password has expired.
	 */
	private class PromptForNewPassword implements Runnable {
		private SystemMessage message;
		private boolean canceled = false;

		public PromptForNewPassword(SystemMessage message) {
			this.message = message;
		}

		public boolean isCancelled() {
			return canceled;
		}

		public void run() {
			SystemChangePasswordDialog dlg = new SystemChangePasswordDialog(getShell(), getConnectorService().getHostName(), getUserId(), message);
			dlg.setSavePassword(savePassword);
			dlg.open();
			canceled = dlg.wasCancelled();
			if (!canceled) {
				password = dlg.getNewPassword();
				savePassword = dlg.getIsSavePassword();
			}
		}
	}

	private String userId = null;
	private String password = null;
	private boolean savePassword = false;
	private boolean saveUserId = false;

	/**
	 * Creates a standard credentials provider for a connector service. 
	 * @param connectorService the connector service associated with this 
	 * provider.
	 * @see IConnectorService
	 */
	public StandardCredentialsProvider(IConnectorService connectorService) {
		super(connectorService);
	}

	/**
	 * <i>Do not override.</i>
	 * Acquires the credentials (userid and password) for this connector service.
	 * The search order for the password is as follows:</p>
	 * <ol>
	 * <li>First check if the password is already known by this
	 * connector service and that it is still valid.
	 * <li>If password not known then look in the password
	 * store and verify that it is still valid.
	 * <li>If a valid password is not found or is to be reacquired
	 * then prompt the user
	 * </ol>
	 * @param reacquire if true then present the prompt even
	 * if the password was found and is valid.
	 * @throws InterruptedException if user is prompted and user
	 * cancels that prompt or if {@link #isSuppressed()} is true.
	 */
	public final void acquireCredentials(boolean reacquire) throws InterruptedException {
		if (isSuppressed()) {
			throw new InterruptedException();
		}
		ISubSystem subsystem = getPrimarySubSystem();
		IHost host = subsystem.getHost();
		String hostName = host.getHostName();
		String hostType = host.getSystemType();
		savePassword = false;
		if (supportsUserId()) {
			boolean sameHost = hostName.equalsIgnoreCase(getConnectorService().getHostName());
			if (!sameHost) {
				clearCredentials();
			}
			getUserId();
		}
		if (supportsPassword()) {
			if (password == null) {
				PasswordPersistenceManager ppm = PasswordPersistenceManager.getInstance();
				SystemSignonInformation savedSignonInformation = ppm.find(hostType, hostName, userId);
				if (savedSignonInformation != null) {
					password = savedSignonInformation.getPassword();
					savePassword = true;
				}
			}
		}
		ICredentialsValidator validator = getSignonValidator();
		boolean signonValid = true;
		if (validator != null) {
			SystemMessage m = validator.validate(getCredentials());
			signonValid = (m == null);
		}
		// If we ran into an invalid password we need to tell the user.
		// Not sure this is necessary, shouldn't this message show up on the password prompt itself?
		if (!signonValid) {
			SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_COMM_PWD_INVALID);
			msg.makeSubstitution(userId, getConnectorService().getHostName());
			SystemMessageDialog dialog = new SystemMessageDialog(getShell(), msg);
			dialog.open();
		}
		if (supportsPassword() || supportsUserId()) {
			boolean passwordNeeded = supportsPassword() && password == null;
			boolean userIdNeeded = supportsUserId() && userId == null;
			if (passwordNeeded || userIdNeeded || reacquire) {
				promptForCredentials();
				if (savePassword) {
					getConnectorService().savePassword();
				} else {
					getConnectorService().removePassword();
				}
				if (saveUserId) {
					getConnectorService().saveUserId();
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.ICredentialsProvider#clearCredentials()
	 */
	public final void clearCredentials() {
		password = null;
		userId = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.ICredentialsProvider#clearPassword()
	 */
	final public void clearPassword() {
		password = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.ICredentialsProvider#getCredentials()
	 */
	public final ICredentials getCredentials() {
		IHost host = getConnectorService().getHost();
		String hostName = host.getHostName();
		String systemType = host.getSystemType();
		SystemSignonInformation result = new SystemSignonInformation(hostName, userId, password, systemType);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.ICredentialsProvider#getUserId()
	 */
	final public String getUserId() {
		if (supportsUserId()) {
			if (userId == null) {
				userId = getSubSystemUserId();
			}
		}
		return userId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.ICredentialsProvider#repairCredentials(org.eclipse.rse.services.clientserver.messages.SystemMessage)
	 */
	public final void repairCredentials(SystemMessage prompt) throws InterruptedException {
		promptForNewPassword(prompt);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.ICredentialsProvider#setPassword(java.lang.String)
	 */
	public final void setPassword(String password) {
		this.password = password;
	}

	/**
	 * <i>Useful utility method. Fully implemented, no need to override.</i><br>
	 * Set the password if you got it from somewhere
	 * @param matchingUserId the user for which to set the password
	 * @param password the password to set for this userid
	 * @param persist true if the password is to be persisted as well
	 */
	public final void setPassword(String matchingUserId, String password, boolean persist) {
		if (getPrimarySubSystem().forceUserIdToUpperCase()) {
			matchingUserId = matchingUserId.toUpperCase();
		}
		IConnectorService cs = getConnectorService();
		String systemType = cs.getHostType();
		String hostName = cs.getHostName();
		SystemSignonInformation signonInformation = new SystemSignonInformation(hostName, matchingUserId, password, systemType);
		setSignonInformation(signonInformation);
		if (persist) { // if password should be persisted, then add to disk
			PasswordPersistenceManager.getInstance().add(signonInformation, true, true);
		} else { // otherwise, remove from both memory and disk
			PasswordPersistenceManager.getInstance().remove(systemType, hostName, userId);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.IConnectorService#setUserId(java.lang.String)
	 */
	final public void setUserId(String newId) {
		if (userId == null || !userId.equals(newId)) {
			userId = newId;
		}
	}

	/**
	 * <i>A default implementation is supplied, but can be overridden if desired.</i><br>
	 * Instantiates and returns the dialog to prompt for the userId and password.
	 * @return An instance of a dialog class that implements {@link ISystemPasswordPromptDialog}
	 */
	protected ISystemPasswordPromptDialog getPasswordPromptDialog(Shell shell) {
		ISystemPasswordPromptDialog dlg = new SystemPasswordPromptDialog(shell, requiresUserId(), requiresPassword());
		return dlg;
	}

	/**
	 * <i>Optionally overridable, not implemented by default.</i><br>
	 * Get the credentails validator to use to validate the credentials as entered
	 * in the dialog. This should only do a local validation.
	 * @return null indicating that no signon validator exists.
	 */
	protected ICredentialsValidator getSignonValidator() {
		return null;
	}

	private boolean forcePasswordToUpperCase() {
		return getPrimarySubSystem().forceUserIdToUpperCase();
	}

	private ISystemValidator getPasswordValidator() {
		ISubSystemConfiguration ssFactory = getPrimarySubSystem().getSubSystemConfiguration();
		ISubSystemConfigurationAdapter adapter = (ISubSystemConfigurationAdapter) ssFactory.getAdapter(ISubSystemConfigurationAdapter.class);
		return adapter.getPasswordValidator(ssFactory);
	}

	private ISubSystem getPrimarySubSystem() {
		return getConnectorService().getPrimarySubSystem();
	}

	private Shell getShell() {
		Display display = Display.getDefault();
		Shell shell = new Shell(display, SWT.APPLICATION_MODAL);
		return shell;
	}

	private String getSubSystemUserId() {
		ISubSystem ss = getPrimarySubSystem();
		String result = ss.getUserId();
		return result;
	}

	private ISystemValidator getUserIdValidator() {
		ISubSystemConfiguration ssFactory = getPrimarySubSystem().getSubSystemConfiguration();
		ISubSystemConfigurationAdapter adapter = (ISubSystemConfigurationAdapter) ssFactory.getAdapter(ISubSystemConfigurationAdapter.class);
		ISystemValidator validator = adapter.getUserIdValidator(ssFactory);
		return validator;
	}

	private void logException(Throwable t) {
		Logger log = LoggerFactory.getLogger(RSEUIPlugin.getDefault());
		log.logError("Unexpected exception", t); //$NON-NLS-1$
	}

	private void promptForCredentials() throws InterruptedException {
		PromptForCredentials runnable = new PromptForCredentials();
		Display.getDefault().syncExec(runnable);
		if (runnable.isCanceled()) {
			throw new InterruptedException();
		}
	}

	private void promptForNewPassword(SystemMessage prompt) throws InterruptedException {
		PromptForNewPassword runnable = new PromptForNewPassword(prompt);
		Display.getDefault().syncExec(runnable);
		if (runnable.isCancelled()) {
			throw new InterruptedException();
		}
	}

	private void setSignonInformation(SystemSignonInformation signonInfo) {
		password = signonInfo.getPassword();
		userId = signonInfo.getUserId();
	}
}
