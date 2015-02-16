package org.eclipse.launchbar.core.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.launchbar.core.ILaunchConfigurationProvider;

public class LaunchConfigProviderInfo {
	private final String launchConfigTypeId;
	private IConfigurationElement element;
	private ILaunchConfigurationProvider provider;

	public LaunchConfigProviderInfo(IConfigurationElement element) {
		this.launchConfigTypeId = element.getAttribute("launchConfigurationType");
		this.element = element;
	}

	public String getLaunchConfigTypeId() {
		return launchConfigTypeId;
	}

	public ILaunchConfigurationProvider getProvider() throws CoreException {
		if (provider == null) {
			provider = (ILaunchConfigurationProvider) element.createExecutableExtension("class");
			element = null;
		}
		return provider;
	}
}