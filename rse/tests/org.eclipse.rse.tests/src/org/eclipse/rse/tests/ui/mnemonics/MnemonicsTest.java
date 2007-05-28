/********************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * David Dykstal (IBM) - initial API and implementation.
 * Martin Oberhuber (Wind River) - [184095] Replace systemTypeName by IRSESystemType
 * Martin Oberhuber (Wind River) - [177523] Unify singleton getter methods
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 ********************************************************************************/

package org.eclipse.rse.tests.ui.mnemonics;

import org.eclipse.rse.tests.core.RSECoreTestCase;
import org.eclipse.rse.ui.Mnemonics;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemPreferencesManager;

/**
 * Tests for {@link SystemPreferencesManager}.
 * Test various aspects of mnemonic generation and assignment.
 */
public class MnemonicsTest extends RSECoreTestCase {
	
	/* (non-Javadoc)
	 * @see org.eclipse.rse.tests.core.RSECoreTestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.rse.tests.core.RSECoreTestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testDefaultGeneration() {
		Mnemonics mn = new Mnemonics();
		mn.clear("abcde");
		String result = mn.setUniqueMnemonic("A...");
		assertEquals("A...", result);
		result = mn.setUniqueMnemonic("F...");
		assertEquals("&F...", result);
	}
	
	public void testAppendPolicies() {
		setLocalePattern(".*"); // match all locales
		Mnemonics mn = new Mnemonics();
		mn.clear("abcde");
		String result = mn.setUniqueMnemonic("A...");
		assertEquals("A(&F)...", result);
		result = mn.setUniqueMnemonic("F...");
		assertEquals("F(&G)...", result);
		result = mn.setUniqueMnemonic("H...");
		assertEquals("&H...", result);
	}
	
	private void setLocalePattern(String pattern) {
		RSEUIPlugin.getDefault().getPluginPreferences().setValue(Mnemonics.APPEND_MNEMONICS_PATTERN_PREFERENCE, pattern);
	}
	
}
