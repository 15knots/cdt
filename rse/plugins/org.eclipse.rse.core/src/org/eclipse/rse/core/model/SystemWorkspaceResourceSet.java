/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * {Name} (company) - description of contribution.
 *******************************************************************************/

package org.eclipse.rse.core.model;

import java.util.List;

import org.eclipse.core.resources.IResource;

public class SystemWorkspaceResourceSet extends AbstractSystemResourceSet {

	public SystemWorkspaceResourceSet() {
		super();
	}

	public SystemWorkspaceResourceSet(List resources) {
		super(resources);
	}

	public SystemWorkspaceResourceSet(IResource[] resources) {
		super(resources);
	}
}
