/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tm.internal.terminal.remote.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tm.internal.terminal.remote.Activator;
import org.eclipse.tm.internal.terminal.remote.messages.Messages;
import org.eclipse.tm.terminal.remote.IRemoteTerminalConstants;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class RemoteTerminalPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		addField(new StringFieldEditor(IRemoteTerminalConstants.PREF_TERMINAL_SHELL_COMMAND,
				Messages.RemoteTerminalPreferencePage_0, parent));
	}

	@Override
	public IPreferenceStore doGetPreferenceStore() {
		return new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.getUniqueIdentifier());
	}

	public void init(IWorkbench workbench) {
		// Nothing
	}
}
