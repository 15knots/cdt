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

package org.eclipse.rse.core.references;

/**
 * Interface that any master object that is referenced must implement.
 */
public interface IRSEBaseReferencedObject {
	/**
	 * Add a reference, increment reference count, return new count
	 * @param ref the referencing object from which this object will now be referenced.
	 * @return new count of how many referencing objects reference this object.
	 */
	public int addReference(IRSEBaseReferencingObject ref);

	/**
	 * Remove a reference, decrement reference count, return new count
	 * @param ref the referencing object from which this object is no longer referenced.
	 * @return new count of how many referencing objects reference this object.
	 */
	public int removeReference(IRSEBaseReferencingObject ref);

	/**
	 * @return a count of how many referencing objects reference this object.
	 */
	public int getReferenceCount();

	/**
	 * Clear the list of referenced objects.
	 */
	public void removeAllReferences();

	/**
	 * @return a list of all referencing objects of this object
	 */
	public IRSEBaseReferencingObject[] getReferencingObjects();
}