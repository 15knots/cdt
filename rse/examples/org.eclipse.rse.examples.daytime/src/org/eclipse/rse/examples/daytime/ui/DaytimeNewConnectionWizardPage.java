package org.eclipse.rse.examples.daytime.ui;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.examples.daytime.DaytimeResources;
import org.eclipse.rse.ui.wizards.AbstractSystemNewConnectionWizardPage;

public class DaytimeNewConnectionWizardPage extends
		AbstractSystemNewConnectionWizardPage {

	
	public DaytimeNewConnectionWizardPage(IWizard wizard, ISubSystemConfiguration parentConfig)
	{
		super(wizard, parentConfig);
	}
	
	public Control createContents(Composite parent) {
		Text field = new Text(parent, SWT.NONE);
		field.setText(DaytimeResources.DaytimeWizard_TestFieldText);
		
		// TODO Auto-generated method stub
		return field;
	}

}
