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

package org.eclipse.rse.internal.model;

import org.eclipse.rse.model.IRSEModelObject;
import org.eclipse.rse.ui.SystemResources;



/**
 * Provides common support for local RSE model objects
 * Extenders inherit property set support
 * @author dmcknigh
 *
 */
public abstract class RSEModelObject extends PropertySetContainer implements IRSEModelObject
{
	protected boolean _isDirty = true;
	protected boolean _wasRestored = false;
	
	
	public boolean isDirty()
	{
		return _isDirty;
	}

	public void setDirty(boolean flag)
	{
		_isDirty = flag;
	}


	public boolean wasRestored() 
	{
		return _wasRestored;
	}

	public void setWasRestored(boolean flag) 
	{
		_wasRestored = flag;
	}
	
	public String getDescription()
	{
		return SystemResources.RESID_MODELOBJECTS_MODELOBJECT_DESCRIPTION;
	}
}