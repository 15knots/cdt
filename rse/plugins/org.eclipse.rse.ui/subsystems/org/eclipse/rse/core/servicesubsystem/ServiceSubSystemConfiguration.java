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

package org.eclipse.rse.core.servicesubsystem;

import java.util.Vector;

import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.model.ISystemNewConnectionWizardPage;
import org.eclipse.rse.core.subsystems.AbstractConnectorService;
import org.eclipse.rse.core.subsystems.AbstractConnectorServiceManager;
import org.eclipse.rse.core.subsystems.IServiceSubSystemConfiguration;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.core.subsystems.SubSystemConfiguration;
import org.eclipse.rse.internal.ui.view.SubSystemConfigurationAdapter;
import org.eclipse.swt.widgets.Shell;



/**
 * This class is to be used by subsystem-providers that do not desire to use MOF/EMF. It is 
 * therefore recommended starting base class for subsystem providers. 
 * <br>
 * To use this class, simply subclass it and override the appropriate methods in it, such as:</p>
 * <ul>
 *    <li>any of the supportsXXX() configuration methods you wish to change.
 *    <li>{@link #createSubSystemInternal(org.eclipse.rse.model.IHost)}, to instantiate your subsystem class.
 * </ul>
 * <p>
 * For additional customization of the subsystem, you may supply a {@link SubSystemConfigurationAdapter},
 * which allows you to
 *    <li>supply your own subsystem popup menu actions via {@link SubSystemConfigurationAdapter#getAdditionalSubSystemActions(ISubSystemConfiguration, ISubSystem, Shell)}, 
 *    <li>supply your own New->Filter popup menu action via {@link SubSystemConfigurationAdapter#getNewFilterPoolFilterAction(ISubSystemConfiguration, ISystemFilterPool, Shell)}, and 
 *    <li>supply your own Change Filter popup menu action via {@link SubSystemConfigurationAdapter#getChangeFilterAction(ISubSystemConfiguration, ISystemFilter,Shell)}.
 * </ul>
 * <p>
 * This class is typically used together with:</p>
 * <ul>
 *   <li>{@link org.eclipse.rse.core.servicesubsystem.ServiceSubSystem} for the subsystem
 *   <li>{@link AbstractConnectorService} for the connector service
 *   <li>{@link AbstractConnectorServiceManager} for the connector service manager
 *   <li>{@link org.eclipse.rse.core.subsystems.AbstractResource} for the individual remote resources
 * </ul>
 * <p>
 * In general, for what methods to override, only worry about the non-generated methods in
 * this class, and ignore the hundreds in {@link org.eclipse.rse.core.subsystems.SubSystemConfiguration}
 * 
 * @see org.eclipse.rse.core.servicesubsystem.ServiceSubSystem
 * @see AbstractConnectorService
 * @see AbstractConnectorServiceManager
 */

public abstract class ServiceSubSystemConfiguration extends SubSystemConfiguration implements IServiceSubSystemConfiguration  
{

	protected ServiceSubSystemConfiguration()
	{
		super();
	}

	// ------------------------------------------------------
	// CONFIGURATION METHODS THAT ARE OVERRIDDEN FROM PARENT
	// WE ASSUME TYPICAL DEFAULTS, BUT CHILDREN CAN OVERRIDE
	// ------------------------------------------------------
	/**
	 * <i>Overridable configuration method. Default is <b>false</b></i><br>
	 * Return true if instance of this factory's subsystems support connect and disconnect actions.
	 */
	public boolean supportsSubSystemConnect()
	{
		return true;
	}

    /**
	 * <i>Overridable configuration method. Default is <b>true</b></i><br>
     * Return true (default) or false to indicate if subsystems of this factory support user-editable
     *  port numbers.
     */
    public boolean isPortEditable()
    {
    	return true;
    }
    /**
	 * <i>Overridable configuration method. Default is <b>true</b></i><br>
     * Required method for subsystem factory child classes. Return true if you support filters, false otherwise.
     * If you support filters, then some housekeeping will be done for you automatically. Specifically, they
     * will be saved and restored for you automatically.
     */
    public boolean supportsFilters()
    {
    	return true;
    }
    /**
	 * <i>Overridable configuration method. Default is <b>false</b></i><br>
     * Do we allow filters within filters?
     */
    public boolean supportsNestedFilters()
    {
    	return false;
    }
	/**
	 * <i>COverridable configuration method. Default is <b>false</b></i><br>
	 * Return true if you support user-defined actions for the remote system objects returned from expansion of
	 *  subsystems created by this subsystem factory
	 */
	public boolean supportsUserDefinedActions()
	{
		return false;
	}
	/**
	 * <i>Overridable configuration method. Default is <b>false</b></i><br>
	 * Return true if you support user-defined/managed named file types
	 */
	public boolean supportsFileTypes()
	{
		return false;
	}
	/**
	 * <i>Overridable configuration method. Default is <b>false</b></i><br>
	 * Tell us if filter strings are case sensitive. 
	 */
	public boolean isCaseSensitive()
	{
		return false;
	}
	/**
	 * <i>Overridable configuration method. Default is <b>false</b></i><br>
	 * Tell us if duplicate filter strings are supported per filter.
	 */
	public boolean supportsDuplicateFilterStrings()
	{
		return false;
	}
	
	// ------------------------------------------------
	// FRAMEWORKD METHODS TO BE OVERRIDDEN IF APPROPRIATE.
	// THESE ARE CALLED BY OUR OWN PARENT
	// ------------------------------------------------

    /**
	 * <i>Overridable lifecycle method. Not typically overridden.</i><br>
     * After a new subsystem instance is created, the framework calls this method
     * to initialize it. This is your opportunity to set default attribute values.
     * 
     * <p>The reason for the connect wizard pages parm is in case your factory contributes a page to that wizard,
     * whose values are needed to set the subsystem's initial state. For example, you might decide to add a 
     * page to the connection wizard to prompt for a JDBC Driver name. If so, when this method is called at 
     * the time a new connection is created apres the wizard, your page will have the user's value. You can
     * thus use it here to initialize that subsystem property. Be use to use instanceof to find your particular
     * page. 
     * </p>
     * 
     * <p>
     * If you override this, <i>PLEASE CALL SUPER TO DO DEFAULT INITIALIZATION!</i>
     * 
     * @param subsys - The subsystem that was created via createSubSystemInternal
     * @param yourNewConnectionWizardPages - The wizard pages you supplied to the New Connection wizard, via the
     *            {@link org.eclipse.rse.internal.ui.view.SubSystemConfigurationAdapter#getNewConnectionWizardPages(org.eclipse.rse.core.subsystems.ISubSystemConfiguration, org.eclipse.jface.wizard.IWizard)} 
     *            method or null if you didn't override this method.
     *            Note there may be more pages than you originally supplied, as you are passed all pages contributed
     *            by this factory object, including subclasses. Null on a clone operation.
     * 
     * @see org.eclipse.rse.internal.ui.view.SubSystemConfigurationAdapter#getNewConnectionWizardPages(org.eclipse.rse.core.subsystems.ISubSystemConfiguration, org.eclipse.jface.wizard.IWizard)
     */
    protected void initializeSubSystem(ISubSystem subsys,ISystemNewConnectionWizardPage[] yourNewConnectionWizardPages)
    {
    	super.initializeSubSystem(subsys, yourNewConnectionWizardPages);
    }
    
	// --------------------------------
	// METHODS FOR SUPPLYING ACTIONS...
	// --------------------------------
    /**
	 * <i>Overridable method for getting Remote System view popup menu actions.</i><br>
	 * Called by {@link org.eclipse.rse.internal.ui.view.SystemView SystemView} when constructing 
	 * the popup menu for a selected subsystem.
	 * <p>
     * For contributing popup menu actions to <b>subsystem objects</b>, beyond the
     * default actions already supplied by our parent class. This method is only called with
     * subsystems created by this subsystem factory.
     * <p>
     * Returns null by default. Override to show your own popup menu actions for your own subsystems.
     * 
     * @return Vector of IAction objects, which usually are subclasses of {@link org.eclipse.rse.ui.actions.SystemBaseAction SystemBaseAction} or
     * {@link org.eclipse.rse.ui.actions.SystemBaseDialogAction SystemBaseDialogAction} or
     * {@link org.eclipse.rse.ui.actions.SystemBaseWizardAction SystemBaseWizardAction} or
     * {@link org.eclipse.rse.ui.actions.SystemBaseSubMenuAction SystemBaseSubMenuAction}.
     */
    protected Vector getAdditionalSubSystemActions(ISubSystem selectedSubSystem, Shell shell)
    {
    	return null;
    }


    /**
     * <i>Optionally overridable method affecting the visual display of objects within subsystems created by this factory.</i><br>
     * Return the translated string to show in the property sheet for the "type" property, for the selected
     *  filter. This method is only called for filters within subsystems created by this subsystem factory.
     * <p>
     * Returns a default string, override if appropriate.
     */
    public String getTranslatedFilterTypeProperty(ISystemFilter selectedFilter)
    {
    	return super.getTranslatedFilterTypeProperty(selectedFilter);
    }
    /**
	 * <i>Overridable method for getting Remote System view popup menu actions. Called by {@link org.eclipse.rse.internal.ui.view.SystemView SystemView}
	 *  when constructing the popup menu for a selected filter.</i><br>
     * This method is only called for filters within subsystems created by this subsystem factory.<br>
     * By default, this returns null. Override if appropriate.
     * 
     * @see org.eclipse.rse.core.subsystems.SubSystemConfiguration#getFilterActions(ISystemFilter,Shell)
     * 
     * @return Vector of IAction objects, which usually are subclasses of {@link org.eclipse.rse.ui.actions.SystemBaseAction SystemBaseAction} or
     * {@link org.eclipse.rse.ui.actions.SystemBaseDialogAction SystemBaseDialogAction} or
     * {@link org.eclipse.rse.ui.actions.SystemBaseWizardAction SystemBaseWizardAction} or
     * {@link org.eclipse.rse.ui.actions.SystemBaseSubMenuAction SystemBaseSubMenuAction}.
     */
    protected Vector getAdditionalFilterActions(ISystemFilter selectedFilter, Shell shell)
    {
    	return null;
    }




} 