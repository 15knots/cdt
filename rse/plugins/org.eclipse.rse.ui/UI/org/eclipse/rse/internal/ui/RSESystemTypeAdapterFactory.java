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
package org.eclipse.rse.internal.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.ui.RSESystemTypeAdapter;

public class RSESystemTypeAdapterFactory implements IAdapterFactory {

	private RSESystemTypeAdapter adapter = new RSESystemTypeAdapter();

	public RSESystemTypeAdapterFactory() {
		super();
	}

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof IRSESystemType) {
			return adapter;
		}
		else {
			return null;
		}
	}

	public Class[] getAdapterList() {
	    return new Class[] {IRSESystemType.class};
	}
}