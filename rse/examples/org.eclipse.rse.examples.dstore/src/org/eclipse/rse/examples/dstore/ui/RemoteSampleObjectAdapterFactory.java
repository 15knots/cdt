/********************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is 
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * Martin Oberhuber (Wind River) - Adapted original tutorial code to Open RSE.
 ********************************************************************************/

package org.eclipse.rse.examples.dstore.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.rse.examples.dstore.subsystems.RemoteSampleObject;
import org.eclipse.rse.ui.view.AbstractSystemRemoteAdapterFactory;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.ui.views.properties.IPropertySource;

/**
 * This factory maps requests for an adapter object from a given remote object.
 */
public class RemoteSampleObjectAdapterFactory extends AbstractSystemRemoteAdapterFactory
		implements IAdapterFactory
{

	private SampleAdapter sampleAdapter = new SampleAdapter();
	
	
	/**
	 * Constructor for DeveloperAdapterFactory.
	 */
	public RemoteSampleObjectAdapterFactory() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.view.AbstractSystemRemoteAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		ISystemViewElementAdapter adapter = null;
		if (adaptableObject instanceof RemoteSampleObject)
		  adapter = sampleAdapter;
		// these lines are very important! 
		if ((adapter != null) && (adapterType == IPropertySource.class))
		  adapter.setPropertySourceInput(adaptableObject);
		return adapter;
	}

}
