/********************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation. All rights reserved.
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
 * David Dykstal (IBM) - created and used RSEPreferencesManager
 *                     - moved SystemPreferencesManager to a new plugin
 ********************************************************************************/

package org.eclipse.rse.internal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.RSEPreferencesManager;
import org.eclipse.rse.core.SystemBasePlugin;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.model.ISystemProfileManager;
import org.eclipse.rse.model.SystemStartHere;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.validators.ISystemValidator;
import org.eclipse.rse.ui.validators.ValidatorProfileName;

/**
 * A class that manages a list of SystemProfile objects.
 * This should be used as a singleton.
 */
public class SystemProfileManager implements ISystemProfileManager {
	private List _profiles = null;
	private String[] profileNames = null;
	private Vector profileNamesVector = null;
	private static ISystemProfileManager singleton = null;
	private static final String PROFILE_FILE_NAME = "profile"; //$NON-NLS-1$

	/**
	 * Ordinarily there should be only one instance of a SystemProfileManager
	 * created on the system, so the static method {@link #getSystemProfileManager()} is 
	 * preferred to using this.
	 */
	private SystemProfileManager() {
		super();
	}

	/**
	 * @return (and create if necessary) the singleton instance of this class.
	 */
	public static ISystemProfileManager getSystemProfileManager() {
		if (singleton == null) {
			singleton = new SystemProfileManager();
			RSEUIPlugin.getThePersistenceManager().restore(singleton); // restores all of RSE
		}
		return singleton;
	}

	/**
	 * Clear the default after a team sychronization say
	 */
	public static void clearDefault() {
		singleton = null;
	}

	/**
	 * Create a new profile with the given name, and add to the list.
	 * The name must be unique within the existing list.
	 * <p>
	 * The underlying folder is created in the file system.
	 * <p>
	 * @param name What to name this profile
	 * @param makeActive true if this profile is to be added to the active profile list.
	 * @return new profile, or null if name not unique.
	 * @see ISystemProfileManager#createSystemProfile(String, boolean)
	 */
	public ISystemProfile createSystemProfile(String name, boolean makeActive) {
		// FIXME - used to use MOF
		ISystemProfile existingProfile = getSystemProfile(name);
		if (existingProfile != null) {
			deleteSystemProfile(existingProfile, false); // replace the existing one with a new profile
		}

		ISystemProfile newProfile = internalCreateSystemProfileAndFolder(name);
		if (makeActive) {
			RSEPreferencesManager.addActiveProfile(name);
			((SystemProfile) newProfile).setActive(makeActive);
		}
		RSEUIPlugin.getThePersistenceManager().commit(this);
		return newProfile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#makeSystemProfileActive(org.eclipse.rse.core.model.ISystemProfile, boolean)
	 */
	public void makeSystemProfileActive(ISystemProfile profile, boolean makeActive) {
		boolean wasActive = isSystemProfileActive(profile.getName());
		if (wasActive && !makeActive)
			RSEPreferencesManager.deleteActiveProfile(profile.getName());
		else if (makeActive && !wasActive) RSEPreferencesManager.addActiveProfile(profile.getName());
		((SystemProfile) profile).setActive(makeActive);
	}

	/*
	 * private version that avoids name collision check
	 */
	private ISystemProfile internalCreateSystemProfile(String name) {
		ISystemProfile profile = new SystemProfile();
		initialize(profile, name);
		profile.setDefaultPrivate(name.equalsIgnoreCase(RSEPreferencesManager.getDefaultPrivateSystemProfileName()));
		return profile;
	}

	/*
	 * private version that avoids name collision check
	 */
	private ISystemProfile internalCreateSystemProfileAndFolder(String name) {
		ISystemProfile profile = internalCreateSystemProfile(name);
		// FIXME This is where the old style folders get created for profiles.
		// This is no longer needed but 
		// SystemResourceManager.getProfileFolder(profile); // creates proj/profileName folder
		return profile;
	}

	/*
	 * private method to initialize state for new profile
	 */
	private void initialize(ISystemProfile profile, String name) {
		profile.setName(name);
		profile.setProfileManager(this);
		getProfiles().add(profile);
		invalidateCache();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#getSystemProfiles()
	 */
	public ISystemProfile[] getSystemProfiles() {
		List profiles = getProfiles();

		// Ensure that one Profile is the default Profile - defect 48995 NH	
		boolean defaultProfileExist = false;
		for (int idx = 0; (!defaultProfileExist) && (idx < profiles.size()); idx++) {
			ISystemProfile profile = (ISystemProfile) profiles.get(idx);
			if (profile.isDefaultPrivate()) {
				defaultProfileExist = true;
			}
		}
		if (!defaultProfileExist) {
			// Check if the Profile exists with name same as the LocalMachine Name - this is the default we give
			// when creating connections. 	
			for (int idx = 0; (!defaultProfileExist) && (idx < profiles.size()); idx++) {
				ISystemProfile profile = (ISystemProfile) profiles.get(idx);
				String initProfileName = RSEPreferencesManager.getDefaultPrivateSystemProfileName();
				if (profile.getName().equalsIgnoreCase(initProfileName)) {
					profile.setDefaultPrivate(true);
					defaultProfileExist = true;
				}
			}

			// If did not find such a profile then the first profile found besides Team is set to be the default profile 	
			if (!defaultProfileExist) {
				for (int idx = 0; (!defaultProfileExist) && (idx < profiles.size()); idx++) {
					ISystemProfile profile = (ISystemProfile) profiles.get(idx);
					if (!profile.getName().equalsIgnoreCase(RSEPreferencesManager.getDefaultTeamProfileName())) {
						profile.setDefaultPrivate(true);

						RSEUIPlugin.getThePersistenceManager().commit(SystemStartHere.getSystemProfileManager());
						defaultProfileExist = true;
					}
				}
			}
			if (!defaultProfileExist) {
				// If Team is the only profile - then put a message in the log - do not make Team to be default
				if (profiles.size() == 1 && ((ISystemProfile) profiles.get(0)).getName().equalsIgnoreCase("Team")) //$NON-NLS-1$  
				{
					SystemBasePlugin.logWarning("Only one Profile Team exists - there is no Default Profile"); //$NON-NLS-1$
				} else {
					// sonething must have gone wrong - it should not come here
					SystemBasePlugin.logWarning("Something went wrong and the default profile is not set"); //$NON-NLS-1$
				}
			}
		}
		return (ISystemProfile[]) profiles.toArray(new ISystemProfile[profiles.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#getSystemProfileNames()
	 */
	public String[] getSystemProfileNames() {
		if (profileNames == null) {
			ISystemProfile[] profiles = getSystemProfiles();
			profileNames = new String[profiles.length];
			for (int idx = 0; idx < profiles.length; idx++)
				profileNames[idx] = profiles[idx].getName();
		}
		return profileNames;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#getSystemProfileNamesVector()
	 */
	public Vector getSystemProfileNamesVector() {
		if (profileNamesVector == null) {
			ISystemProfile[] profiles = getSystemProfiles();
			profileNamesVector = new Vector(profiles.length);
			for (int idx = 0; idx < profiles.length; idx++)
				profileNamesVector.addElement(profiles[idx].getName());
		}
		return profileNamesVector;
	}

	/**
	 * Something changed so invalide cache of profiles so it will be regenerated
	 */
	protected void invalidateCache() {
		//DY	profiles = null;
		profileNames = null;
		profileNamesVector = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#getSystemProfile(java.lang.String)
	 */
	public ISystemProfile getSystemProfile(String name) {
		ISystemProfile[] profiles = getSystemProfiles();
		if ((profiles == null) || (profiles.length == 0)) return null;
		ISystemProfile match = null;
		for (int idx = 0; (match == null) && (idx < profiles.length); idx++)
			if (profiles[idx].getName().equals(name)) match = profiles[idx];
		return match;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#renameSystemProfile(org.eclipse.rse.core.model.ISystemProfile, java.lang.String)
	 */
	public void renameSystemProfile(ISystemProfile profile, String newName) {
		boolean isActive = isSystemProfileActive(profile.getName());
		String oldName = profile.getName();
		profile.setName(newName);
		if (isActive) RSEPreferencesManager.renameActiveProfile(oldName, newName);
		invalidateCache();
		// FIXME RSEUIPlugin.getThePersistenceManager().save(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#deleteSystemProfile(org.eclipse.rse.core.model.ISystemProfile, boolean)
	 */
	public void deleteSystemProfile(ISystemProfile profile, boolean persist) {
		String oldName = profile.getName();
		boolean isActive = isSystemProfileActive(oldName);
		getProfiles().remove(profile);
		/* FIXME in EMF the profiles are "owned" by the Resource, and only referenced by the profile manager,
		 * so just removing it from the manager is not enough, it must also be removed from its resource.
		 * No longer needed since EMF is not in use.
		 * Resource res = profile.eResource();
		 * if (res != null)
		 * res.getContents().remove(profile);
		 */
		if (isActive) RSEPreferencesManager.deleteActiveProfile(oldName);
		invalidateCache();
		if (persist) {
			RSEUIPlugin.getThePersistenceManager().deleteProfile(oldName);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#cloneSystemProfile(org.eclipse.rse.core.model.ISystemProfile, java.lang.String)
	 */
	public ISystemProfile cloneSystemProfile(ISystemProfile profile, String newName) {
		ISystemProfile newProfile = createSystemProfile(newName, false);
		return newProfile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#isSystemProfileActive(java.lang.String)
	 */
	public boolean isSystemProfileActive(String profileName) {
		String[] activeProfiles = getActiveSystemProfileNames();
		boolean match = false;
		for (int idx = 0; !match && (idx < activeProfiles.length); idx++) {
			if (activeProfiles[idx].equals(profileName)) match = true;
		}
		return match;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#getActiveSystemProfiles()
	 */
	public ISystemProfile[] getActiveSystemProfiles() {
		String[] profileNames = getActiveSystemProfileNames();
		ISystemProfile[] profiles = new ISystemProfile[profileNames.length];
		for (int idx = 0; idx < profileNames.length; idx++) {
			profiles[idx] = getOrCreateSystemProfile(profileNames[idx]);
			((SystemProfile) profiles[idx]).setActive(true);
		}
		return profiles;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#getActiveSystemProfileNames()
	 */
	public String[] getActiveSystemProfileNames() {
		String[] activeProfileNames = RSEPreferencesManager.getActiveProfiles();
		// dy: defect 48355, need to sync this with the actual profile list.  If the user
		// imports old preference settings or does a team sync and a profile is deleted then
		// it is possible an active profile no longer exists.
		// String[] systemProfileNames = getSystemProfileNames();
		ISystemProfile[] systemProfiles = getSystemProfiles();
		boolean found;
		boolean found_team = false;
		boolean found_private = false;
		boolean changed = false;
		String defaultProfileName = RSEPreferencesManager.getDefaultPrivateSystemProfileName();

		for (int activeIdx = 0; activeIdx < activeProfileNames.length; activeIdx++) {
			// skip Team and Private profiles
			String activeProfileName = activeProfileNames[activeIdx];
			if (activeProfileName.equals(defaultProfileName)) {
				found_private = true;
			} else if (activeProfileName.equals(RSEPreferencesManager.getDefaultTeamProfileName())) {
				found_team = true;
			} else {
				found = false;
				for (int systemIdx = 0; systemIdx < systemProfiles.length && !found; systemIdx++) {
					if (activeProfileNames[activeIdx].equals(systemProfiles[systemIdx].getName())) {
						found = true;
					}
				}

				if (!found) {
					// The active profile no longer exists so remove it from the active list
					RSEPreferencesManager.deleteActiveProfile(activeProfileNames[activeIdx]);
					changed = true;
				}
			}
		}

		for (int systemIdx = 0; systemIdx < systemProfiles.length && !changed; systemIdx++) {
			boolean matchesBoth = false;
			String name = systemProfiles[systemIdx].getName();

			for (int activeIdx = 0; activeIdx < activeProfileNames.length && !matchesBoth; activeIdx++) {
				String aname = activeProfileNames[activeIdx];
				if (name.equals(aname)) {
					matchesBoth = true;
				}

			}
			if (!matchesBoth && found_private) {
				if (systemProfiles[systemIdx].isActive() || systemProfiles[systemIdx].isDefaultPrivate()) {
					RSEPreferencesManager.addActiveProfile(name);
					RSEPreferencesManager.deleteActiveProfile(RSECorePlugin.getLocalMachineName());
					activeProfileNames = RSEPreferencesManager.getActiveProfiles();
				}
			}
		}

		// the active profiles list needed to be changed because of an external update, also
		// check if Default profile needs to be added back to the list
		if (changed || !found_team || !found_private) {
			if (systemProfiles.length == 0) {
				// First time user, make sure default is in the active list, the only time it wouldn't
				// be is if the pref_store.ini was modified (because the user imported old preferences)
				if (!found_team) {
					RSEPreferencesManager.addActiveProfile(RSEPreferencesManager.getDefaultTeamProfileName());
					changed = true;
				}

				if (!found_private) {
					RSEPreferencesManager.addActiveProfile(RSECorePlugin.getLocalMachineName());
					changed = true;
				}
			} else {
				ISystemProfile defaultProfile = getDefaultPrivateSystemProfile();
				if (defaultProfile != null && !found_private) {
					RSEPreferencesManager.addActiveProfile(defaultProfile.getName());
					changed = true;
				}
			}

			if (changed) {
				activeProfileNames = RSEPreferencesManager.getActiveProfiles();
			}
		}

		return activeProfileNames;
	}

	/**
	 * @return the profile names currently selected by the user as "active" profiles
	 */
	public Vector getActiveSystemProfileNamesVector() {
		String[] profileNames = RSEPreferencesManager.getActiveProfiles();
		Vector v = new Vector(profileNames.length);
		for (int idx = 0; idx < profileNames.length; idx++)
			v.addElement(profileNames[idx]);
		return v;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#getActiveSystemProfilePosition(java.lang.String)
	 */
	public int getActiveSystemProfilePosition(String profileName) {
		String[] profiles = getActiveSystemProfileNames();
		int pos = -1;
		for (int idx = 0; (pos < 0) && (idx < profiles.length); idx++) {
			if (profiles[idx].equals(profileName)) pos = idx;
		}
		return pos;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#getDefaultPrivateSystemProfile()
	 */
	public ISystemProfile getDefaultPrivateSystemProfile() {
		return getSystemProfile(RSEPreferencesManager.getDefaultPrivateSystemProfileName());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#getDefaultTeamSystemProfile()
	 */
	public ISystemProfile getDefaultTeamSystemProfile() {
		return getSystemProfile(RSEPreferencesManager.getDefaultTeamProfileName());
	}

	/**
	 * Instantiate a user profile given its name.
	 * @param userProfileName the name of the profile to find or create
	 * @return the profile that was found or created.
	 */
	protected ISystemProfile getOrCreateSystemProfile(String userProfileName) {
		ISystemProfile userProfile = getSystemProfile(userProfileName);
		if (userProfile == null) {
			userProfile = internalCreateSystemProfileAndFolder(userProfileName);
		}
		return userProfile;
	}

	/**
	 * No longer used.
	 * @param profileName the name of the profile from which to construct the name
	 * @return the unqualified save file name with the extension .xmi
	 */
	public static String getSaveFileName(String profileName) {
		return null;
		//FIXME SystemMOFHelpers.getSaveFileName(getRootSaveFileName(profileName)); no longer needed.
	}

	/**
	 * No longer used.
	 * @param profile the profile from which to construct the name 
	 * @return the unqualified save file name with the extension .xmi
	 */
	public static String getSaveFileName(ISystemProfile profile) {
		return null;
		//FIXME SystemMOFHelpers.getSaveFileName(getRootSaveFileName(profile)); no longer needed.
	}

	/**
	 * No longer used.
	 * @param profile the profile from which to retrieve the root.
	 * @return the root save file name without the extension .xmi
	 */
	protected static String getRootSaveFileName(ISystemProfile profile) {
		return getRootSaveFileName(profile.getName());
	}

	/**
	 * No longer used.
	 * @param profileName the name of the profile
	 * @return the root save file name without the extension .xmi
	 */
	protected static String getRootSaveFileName(String profileName) {
		//String fileName = profileName; // may be a bad idea to include manager name in it!
		String fileName = PROFILE_FILE_NAME;
		return fileName;
	}

	/**
	 * Reusable method to return a name validator for renaming a profile.
	 * @param profileName the current profile name on updates. Can be null for new profiles. Used
	 * to remove from the existing name list the current connection.
	 * @return the validator
	 */
	public ISystemValidator getProfileNameValidator(String profileName) {
		//Vector v = getActiveSystemProfileNamesVector();
		Vector v = getSystemProfileNamesVector();
		if (profileName != null) v.removeElement(profileName);
		ISystemValidator nameValidator = new ValidatorProfileName(v);
		return nameValidator;
	}

	/**
	 * Reusable method to return a name validator for renaming a profile.
	 * @param profile the current profile object on updates. Can be null for new profiles. Used
	 * to remove from the existing name list the current connection.
	 * @return the validator
	 */
	public ISystemValidator getProfileNameValidator(ISystemProfile profile) {
		if (profile != null)
			return getProfileNameValidator(profile.getName());
		else
			return getProfileNameValidator((String) null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemProfileManager#getProfiles()
	 */
	public List getProfiles() {
		if (_profiles == null) {
			ISystemProfile profile = new SystemProfile();
			String initProfileName = RSEPreferencesManager.getDefaultPrivateSystemProfileName();
			profile.setName(initProfileName);
			profile.setDefaultPrivate(true);
			_profiles = new ArrayList();
			_profiles.add(profile);
			//profiles = null;//FIXME new EObjectResolvingeList(SystemProfile.class, this, ModelPackage.SYSTEM_PROFILE_MANAGER__PROFILES);
		}
		return _profiles;
	}

}