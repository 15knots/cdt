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

package org.eclipse.rse.ui.validators;
import java.util.Vector;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.rse.core.SystemPlugin;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;


/**
 * This class is used to verify a user defined action's name.
 */
public class ValidatorUserActionName extends ValidatorUniqueString 
       implements ISystemMessages, ISystemValidator
{
	public static final int MAX_UDANAME_LENGTH = 256; // max name for an action
		
	protected boolean  fUnique;
	protected SystemMessage   msg_Invalid;
	protected IWorkspace workspace = ResourcesPlugin.getWorkspace();
	
	/**
	 * Use this constructor when the name must be unique. Give the
	 * ctor a vector containing a list of existing names to compare against.
	 */
	public ValidatorUserActionName(Vector existingNameList)
	{
		super(existingNameList, CASE_SENSITIVE); // case sensitive uniqueness
        init();
	}
	/**
	 * Use this constructor when the name must be unique. Give the
	 * ctor a string array of existing names to compare against.
	 */
	public ValidatorUserActionName(String existingNameList[])
	{
		super(existingNameList, CASE_SENSITIVE); // case sensitive uniqueness
        init();
	}
	
	/**
	 * Use this constructor when the name need not be unique, and you just want
	 * the syntax checking.
	 */
	public ValidatorUserActionName()
	{
		super(new String[0], CASE_SENSITIVE);
        init();
	}	
	
	private void init()
	{
		super.setErrorMessages(SystemPlugin.getPluginMessage(MSG_VALIDATE_UDANAME_EMPTY),
		                       SystemPlugin.getPluginMessage(MSG_VALIDATE_UDANAME_NOTUNIQUE));  
		fUnique = true;
		msg_Invalid = SystemPlugin.getPluginMessage(MSG_VALIDATE_UDANAME_NOTVALID);				
	}
	/**
	 * Supply your own error message text. By default, messages from SystemPlugin resource bundle are used.
	 * @param error message when entry field is empty
	 * @param error message when value entered is not unique
	 * @param error message when syntax is not valid
	 */
	public void setErrorMessages(SystemMessage msg_Empty, SystemMessage msg_NonUnique, SystemMessage msg_Invalid)
	{
		super.setErrorMessages(msg_Empty, msg_NonUnique);
		this.msg_Invalid = msg_Invalid;		
	}

	/**
	 * Overridable method for invalidate character check, beyond what this class offers
	 * @return true if valid, false if not
	 */
	protected boolean checkForBadCharacters(String newText)
	{
		return ((newText.indexOf('&') == -1) && // causes problems in menu popup as its a mnemonic character
		         (newText.indexOf('@') == -1)); // defect 43950
	}
	
	public String toString()
	{
		return "UserActionNameValidator class";
	}

    // ---------------------------
    // Parent Overrides...
    // ---------------------------
	/**
	 * Validate each character. 
	 * Override of parent method.
	 * Override yourself to refine the error checking.	 
	 */
	public SystemMessage isSyntaxOk(String newText)
	{	       
	   if (newText.length() > getMaximumNameLength())
	     currentMessage = msg_Invalid; 
	   else
	     currentMessage = checkForBadCharacters(newText) ? null: msg_Invalid;
	   return currentMessage;
	}

    
    // ---------------------------
    // ISystemValidator methods...
    // ---------------------------
    
    /**
     * Return the max length for folder names: 256
     */
    public int getMaximumNameLength()
    {
    	return MAX_UDANAME_LENGTH;
    }

	
}