/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.rse.core.filters;

import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.logging.Logger;

public interface ISystemFilterStartHere {

	/**
	 * Factory method to return an instance populated with defaults.
	 * You can then simply override whatever is desired via setXXX methods.
	 */
	public IRSEFilterNamingPolicy createSystemFilterNamingPolicy();

	/**
	 * Factory to create a filter pool manager, when you do NOT want it to worry about 
	 *  saving and restoring the filter data to disk. Rather, you will save and restore
	 *  yourself.
	 * @param logger A logging object into which to log errors as they happen in the framework
	 * @param caller Objects which instantiate this class should implement the
	 *   SystemFilterPoolManagerProvider interface, and pass "this" for this parameter.
	 *   Given any filter framework object, it is possible to retrieve the caller's
	 *   object via the getProvider method call.
	 * @param name the name of the filter pool manager. Not currently used but you may
	 *   find a use for it.
	 * @param allowNestedFilters true if filters inside filter pools in this manager are
	 *   to allow nested filters. This is the default, but can be overridden at the 
	 *   individual filter pool level.
	 */
	public ISystemFilterPoolManager createSystemFilterPoolManager(ISystemProfile profile, Logger logger, ISystemFilterPoolManagerProvider caller, String name, boolean allowNestedFilters);

	/**
	 * Create a SystemFilterPoolReferenceManager instance, when you  do NOT want it 
	 *  to be saved and restored to its own file. Rather, you will save and restore it
	 *  yourself.
	 * @param caller Objects which instantiate this class should implement the
	 *   SystemFilterPoolReferenceManagerProvider interface, and pass "this" for this parameter.
	 *   Given any filter framework object, it is possible to retrieve the caller's
	 *   object via the getProvider method call.
	 * @param relatedPoolMgrProvider The creator of the managers that own the master list of filter pools that 
	 *   this manager will contain references to.
	 * @param name the name of the filter pool reference manager. This is not currently 
	 *   used, but you may find a use for it. 
	 * @param namingPolicy the naming policy object which will return the name of that one file.
	 */
	public ISystemFilterPoolReferenceManager createSystemFilterPoolReferenceManager(ISystemFilterPoolReferenceManagerProvider caller, ISystemFilterPoolManagerProvider relatedPoolMgrProvider,
			String name, IRSEFilterNamingPolicy namingPolicy);
}
