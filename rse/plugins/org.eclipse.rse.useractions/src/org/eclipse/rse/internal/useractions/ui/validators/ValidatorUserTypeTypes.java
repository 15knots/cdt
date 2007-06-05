/********************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation. All rights reserved.
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
 * David Dykstal (IBM) - [186589] move user types, user actions, and compile commands
 *                                API to the user actions plugin
 ********************************************************************************/

package org.eclipse.rse.internal.useractions.ui.validators;

import org.eclipse.rse.internal.useractions.IUserActionsMessageIds;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.validators.ISystemValidator;

/**
 * This class is used to verify a user defined action's comment
 */
public class ValidatorUserTypeTypes implements ISystemValidator {
	public static final int MAX_UDTTYPES_LENGTH = 512;

	protected SystemMessage emptyMsg, invalidMsg, currentMessage;

	/**
	 * Constructor to use when wanting to specify the "value required" error message,
	 * but use the default for the "Value not valid" error message
	 */
	public ValidatorUserTypeTypes() {
		setErrorMessages(RSEUIPlugin.getPluginMessage(IUserActionsMessageIds.MSG_VALIDATE_UDTTYPES_EMPTY), RSEUIPlugin.getPluginMessage(IUserActionsMessageIds.MSG_VALIDATE_UDTTYPES_NOTVALID));
	}

	/**
	 * Set the error messages, overriding the defaults
	 */
	public void setErrorMessages(SystemMessage emptyMsg, SystemMessage invalidMsg) {
		this.emptyMsg = emptyMsg;
		this.invalidMsg = invalidMsg;
	}

	// ---------------------------
	// ISystemValidator methods...
	// ---------------------------    

	/**
	 * @see org.eclipse.jface.viewers.ICellEditorValidator#isValid(java.lang.Object)
	 */
	public String isValid(Object input) {
		currentMessage = null;
		if (!(input instanceof String)) {
			return null;
		} else
			return isValid((String) input);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IInputValidator#isValid(java.lang.String)
	 * @see #getSystemMessage()
	 */
	public String isValid(String input) {
		currentMessage = null;
		if ((input == null) || (input.length() == 0)) {
			currentMessage = emptyMsg;
		} else {
			if (input.length() > MAX_UDTTYPES_LENGTH) currentMessage = invalidMsg;
		}
		return (currentMessage == null) ? null : currentMessage.getLevelOneText();
	}

	/**
	 * When isValid returns non-null, call this to get the SystemMessage object for the error 
	 *  versus the simple string message.
	 */
	public SystemMessage getSystemMessage() {
		return currentMessage;
	}

	/**
	 * Return the max length for comments
	 */
	public int getMaximumNameLength() {
		return MAX_UDTTYPES_LENGTH;
	}

	/**
	 * For convenience, this is a shortcut to calling:
	 * <pre><code>
	 *  if (isValid(text) != null)
	 *    msg = getSystemMessage();
	 * </code></pre>
	 */
	public SystemMessage validate(String text) {
		if (isValid(text) != null)
			return currentMessage;
		else
			return null;
	}

}