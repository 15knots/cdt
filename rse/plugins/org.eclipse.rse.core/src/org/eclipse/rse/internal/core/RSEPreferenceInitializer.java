/********************************************************************************
 * Copyright (c) 2007 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * David Dykstal (IBM) - initial API and implementation
 ********************************************************************************/
package org.eclipse.rse.internal.core;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.rse.core.IRSEPreferenceNames;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.RSEPreferencesManager;

public class RSEPreferenceInitializer extends AbstractPreferenceInitializer {

	public void initializeDefaultPreferences() {
		// the complex preferences
		RSEPreferencesManager.initDefaults();
		
		// the simple preferences
		Preferences prefs = RSECorePlugin.getDefault().getPluginPreferences();
		prefs.setDefault(IRSEPreferenceNames.DEFAULT_PERSISTENCE_PROVIDER, "org.eclipse.rse.persistence.PropertyFileProvider"); //$NON-NLS-1$
	}

}
