/********************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others. All rights reserved.
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
 * Uwe Stieber (Wind River) - Dynamic system type provider extension.
 * Martin Oberhuber (Wind River) - [184095] Replace systemTypeName by IRSESystemType
 * Martin Oberhuber (Wind River) - [186640] Add IRSESystemType.testProperty() 
 ********************************************************************************/
package org.eclipse.rse.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.PlatformObject;
import org.osgi.framework.Bundle;

/**
 * Abstract base class holding core functionality of a system type.
 * 
 * Extenders must override {@link IRSESystemType#getSubsystemConfigurationIds()}
 * according to their strategy of finding subsystem configuration id's that
 * match their system type.
 * 
 * Extenders may override any other method.
 */
public abstract class AbstractRSESystemType extends PlatformObject implements IRSESystemType {

	protected String id = null;
	protected String name = null;
	protected String label = null;
	protected String description = null;
	protected Bundle definingBundle = null;
	protected Map properties;
	
	/**
	 * Default constructor.
	 * Only subclasses may call this if set the id, name, label,
	 * description and properties attributes themselves. 
	 */
	protected AbstractRSESystemType()
	{
		super();
	}
	
	/**
	 * Constructor for an object representing a system type.
	 * @param id unique id of this system type. Must be system unique.
	 * @param name a name of this system type to be used for internal checks.
	 * @param label a user-visible label of this system type. 
	 *     May be <code>null</code> and falls back to the name in this case.
	 * @param description a user-visible description of this system type.
	 *     May be <code>null</code> and falls back to the label in this case.
	 */
	public AbstractRSESystemType(String id, String name, String label, String description, Bundle definingBundle)
	{
		super();
		this.id = id;
		this.name = name;
		this.label = label == null ? name : label;
		this.description = description == null ? "" : description; //$NON-NLS-1$
		this.definingBundle = definingBundle;
		this.properties = new HashMap();
	}

	/**
	 * Checks whether two system types are the same.
	 * 
	 * System types are considered the same if they have the same ID.
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IRSESystemType) {
			return id.equals( ((IRSESystemType)obj).getId() );
		}
		return false;
	}

	/**
	 * Returns the hashCode for this system type.
	 * 
	 * The hashCode is the hashCode of its ID.
	 */
	public int hashCode() {
		return id.hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.IRSESystemType#getId()
	 */
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.IRSESystemType#getLabel()
	 */
	public String getLabel() {
		// For default RSE system types, the UI label is equal to the
		// name. Therefore, fallback to the name if the label is not
		// explicitly set.
		if (label == null) return getName();
		return label;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.IRSESystemType#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.IRSESystemType#getDescription()
	 */
	public String getDescription() {
		return description;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.IRSESystemType#getDefiningBundle()
	 */
	public Bundle getDefiningBundle() {
		return definingBundle;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.IRSESystemType#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		return (String) (properties.get(key));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.IRSESystemType#getProperty(java.lang.String, boolean)
	 */
	public boolean testProperty(String key, boolean defaultValue) {
		Object val = properties.get(key);
		if (val instanceof String) {
			return Boolean.valueOf((String)val).booleanValue();
		}
		return defaultValue;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.IRSESystemType#isLocal()
	 */
	public boolean isLocal() {
		return IRSESystemType.SYSTEMTYPE_LOCAL_ID.equals(getId())
		   || testProperty(IRSESystemType.PROPERTY_IS_LOCAL, true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.IRSESystemType#isLocal()
	 */
	public boolean isWindows() {
		return IRSESystemType.SYSTEMTYPE_WINDOWS_ID.equals(getId())
		   || (isLocal() && System.getProperty("os.name").toLowerCase().startsWith("win")) //$NON-NLS-1$ //$NON-NLS-2$
		   || testProperty(IRSESystemType.PROPERTY_IS_WINDOWS, true);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getLabel() + " (" + getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}