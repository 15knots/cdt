/********************************************************************************
 * Copyright (c) 2006 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors:
 * Uwe Stieber (Wind River) - initial API and implementation.
 * Martin Oberhuber (Wind River) - fix build against 3.2.1, fix javadoc errors
 ********************************************************************************/
package org.eclipse.rse.tests.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;
import junit.framework.TestResult;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.rse.core.SystemBasePlugin;
import org.eclipse.rse.tests.RSETestsPlugin;
import org.eclipse.rse.tests.core.RSEWaitAndDispatchUtil.IInterruptCondition;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.osgi.framework.Bundle;

/**
 * Core RSE test case infra structure implementation.
 */
public class RSECoreTestCase extends TestCase {
	// Test properties storage.
	private final Properties properties = new Properties();
	
	// Internal. Used to remember view zoom state changes.
	private final String PROP_RSE_SYSTEMS_VIEW_ZOOM_STATE_CHANGED = "rseSystemsViewZoomStateChanged"; //$NON-NLS-1$
	
	/**
	 * Constructor.
	 */
	public RSECoreTestCase() {
		this(null);
	}

	/**
	 * Constructor.
	 * 
	 * @param name The test name.
	 */
	public RSECoreTestCase(String name) {
		super(name);
		// clear out all properties on construction.
		properties.clear();
		// initialize the core test properties
		initializeProperties();
	}

	// ***** Test properties management and support methods *****
	
	/**
	 * Initialize the core test properties. Override to modify core
	 * test properties or to add additional ones.
	 */
	protected void initializeProperties() {
		setProperty(IRSECoreTestCaseProperties.PROP_MAXIMIZE_REMOTE_SYSTEMS_VIEW, false);
		setProperty(IRSECoreTestCaseProperties.PROP_SWITCH_TO_PERSPECTIVE, "org.eclipse.rse.ui.view.SystemPerspective"); //$NON-NLS-1$
		setProperty(IRSECoreTestCaseProperties.PROP_FORCE_BACKGROUND_EXECUTION, false);
		setProperty(IRSECoreTestCaseProperties.PROP_PERFORMANCE_TIMING_INCLUDE_SETUP_TEARDOWN, false);
		setProperty(PROP_RSE_SYSTEMS_VIEW_ZOOM_STATE_CHANGED, false);
	}
	
	/**
	 * Enables or disables the specified property.
	 * 
	 * @param key The key of the property to enable or disable. Must be not <code>null</code>!
	 * @param enable Specify <code>true</code> to enable the property, <code>false</code> to disable the property.
	 */
	protected final void setProperty(String key, boolean enable) {
		setProperty(key, enable ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
	}
	
	/**
	 * Test if the specified property is equal to the specified value.
	 * 
	 * @param key The key of the property to test. Must be not <code>null</code>!
	 * @param value The value to compare the property with.
	 * @return <code>true</code> if the property is equal to the specified value, <code>false</code> otherwise.
	 */
	protected final boolean isProperty(String key, boolean value) {
		assert key != null;
		return (value ? Boolean.TRUE : Boolean.FALSE).equals(Boolean.valueOf(properties.getProperty(key, "false"))); //$NON-NLS-1$
	}
	
	/**
	 * Sets the specified string value for the specified property. If the specified
	 * value is <code>null</code>, the specified property will be removed.
	 * 
	 * @param key The key of the property to set. Must be not <code>null</code>!
	 * @param value The string value to set or <code>null</code>.
	 */
	protected final void setProperty(String key, String value) {
		assert key != null;
		if (key != null) {
			if (value != null) {
				properties.setProperty(key, value);
			} else {
				properties.remove(key);
			}
		}
	}
	
	/**
	 * Test if the specified property is equal to the specified value. If the specified
	 * value is <code>null</code>, this method returns <code>true</code> if the specified
	 * property key does not exist. The comparisation is case insensitive.
	 * 
	 * @param key The key of the property to test. Must be not <code>null</code>!
	 * @param value The value to compare the property with or <code>null</code>
	 * @return <code>true</code> if the property is equal to the specified value 
	 *         or the specified value is <code>null</code> and the property does not exist,
	 *         <code>false</code> otherwise.
	 */
	protected final boolean isProperty(String key, String value) {
		assert key != null;
		if (value != null) {
			return value.equalsIgnoreCase(properties.getProperty(key));
		}
		return !properties.containsKey(key);
	}

	/**
	 * Returns the configured string value of the specified property.
	 * 
	 * @param key The property key. Must be not <code>null</code>.
	 * @return The property value or <code>null</code> if the specified property does not exist.
	 */
	protected final String getProperty(String key) {
		assert key != null;
		return properties.getProperty(key, null);
	}
	
	// ***** Test case life cycle management and support methods *****

	final static QualifiedName BACKGROUND_TEST_EXECUTION_FINISHED = new QualifiedName(RSETestsPlugin.getDefault().getBundle().getSymbolicName(), "background_test_execution_finished"); //$NON-NLS-1$
	
	private final class RSEBackgroundTestExecutionJob extends Job {
		private final TestResult result;
		
		/**
		 * Constructor.
		 * 
		 * @param result The test result object the test is reporting failures to. Must be not <code>null</code>.
		 */
		public RSEBackgroundTestExecutionJob(TestResult result) {
			super("RSE JUnit Test Case Execution Job"); //$NON-NLS-1$
			setUser(false);
			setPriority(Job.INTERACTIVE);
			setRule(ResourcesPlugin.getWorkspace().getRoot());
			
			assert result != null;
			this.result = result;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Running test " + RSECoreTestCase.this.getName() + " ...", IProgressMonitor.UNKNOWN); //$NON-NLS-1$ //$NON-NLS-2$

			// Execute the test now.
			result.addListener(TEST_LISTENER);
			invokeTestCaseRunImpl(result);
			result.removeListener(TEST_LISTENER);
			
			monitor.done();
			
			setProperty(BACKGROUND_TEST_EXECUTION_FINISHED, Boolean.TRUE);

			// The job never fails. The test result is the real result.
			return Status.OK_STATUS;
		} 
	}
	
	private final static class RSEBackgroundTestExecutionJobWaiter implements IInterruptCondition {
		private final Job job;
		
		/**
		 * Constructor.
		 * 
		 * @param job The job to wait for the execution to finish. Must be not <code>null</code>.
		 */
		public RSEBackgroundTestExecutionJobWaiter(Job job) {
			assert job != null;
			this.job = job;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.rse.tests.core.RSEWaitAndDispatchUtil.IInterruptCondition#isTrue()
		 */
		public boolean isTrue() {
			// Interrupt the wait method if the job signaled that it has finished.
			return ((Boolean)job.getProperty(BACKGROUND_TEST_EXECUTION_FINISHED)).booleanValue();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.rse.tests.core.RSEWaitAndDispatchUtil.IInterruptCondition#dispose()
		 */
		public void dispose() { /* nothing to dispose here */ }
	}
	
	/**
	 * Internal accessor method to call the original <code>junit.
	 * framework.TestCase.run(TestResult) implementation.
	 * 
	 * @param result The test result object the test is reporting failures to. Must be not <code>null</code>.
	 */
	final void invokeTestCaseRunImpl(TestResult result) {
		super.run(result);
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#run(junit.framework.TestResult)
	 */
	public final void run(TestResult result) {
		if (isProperty(IRSECoreTestCaseProperties.PROP_FORCE_BACKGROUND_EXECUTION, false)
				|| !RSEWaitAndDispatchUtil.isDispatchThread()) {
			// do not force test execution into background, just call super.run(result)
			// from with the current thread.
			result.addListener(TEST_LISTENER);
			super.run(result);
			result.removeListener(TEST_LISTENER);
		} else {
			// Create the background job
			Job job = new RSEBackgroundTestExecutionJob(result);
			// Initialize the BACKGROUND_EXECUTION_TEST_RESULT property
			job.setProperty(BACKGROUND_TEST_EXECUTION_FINISHED, Boolean.FALSE);
			// schedule the job to run immediatelly
			job.schedule();
			
			// wait till the job finished executing
			RSEWaitAndDispatchUtil.waitAndDispatch(0, new RSEBackgroundTestExecutionJobWaiter(job));
		}
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#runBare()
	 */
	public void runBare() throws Throwable {
		// If PROP_PERFORMANCE_TIMING_INCLUDE_SETUP_TEARDOWN is set to true,
		// print the timing information including the tests setUp and tearDown methods.
		if (isProperty(IRSECoreTestCaseProperties.PROP_PERFORMANCE_TIMING_INCLUDE_SETUP_TEARDOWN, true)) {
			// Print timing information here
			long start = printTestStartInformation(getName());
			try {
				super.runBare();
			} finally {
				printTestEndInformation(getName(), start);
			}
		} else {
			// Do no print timing information
			super.runBare();
		}
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#runTest()
	 */
	protected void runTest() throws Throwable {
		// If PROP_PERFORMANCE_TIMING_INCLUDE_SETUP_TEARDOWN is set to false (default),
		// print the timing information only the test method itself.
		if (isProperty(IRSECoreTestCaseProperties.PROP_PERFORMANCE_TIMING_INCLUDE_SETUP_TEARDOWN, false)) {
			// Print timing information here and run the test.
			long start = printTestStartInformation(getName());
			try {
				super.runTest();
			} finally {
				printTestEndInformation(getName(), start);
			}
		} else {
			// Do no print timing information, just run the test
			super.runTest();
		}
	}

	// Local date format presenting long date and time format.
	private final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.getDefault());

	/**
	 * Print the start date and time of the specified test to stdout.
	 * 
	 * @param name The name of the starting test. Must be not <code>null</code>!
	 * @return The start time of the test in milliseconds.
	 */
	protected long printTestStartInformation(String name) {
		assert name != null;
		long startTime = System.currentTimeMillis();
		if (name != null) {
			System.out.println("\n=== " + name + " started at: " + DATE_FORMAT.format(new Date(startTime))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return startTime;
	}
	
	/**
	 * Print the end date and time as well as the delay of the specified test to stdout.
	 * 
	 * @param name The name of the finished test. Must be not <code>null</code>!
	 * @param startTime The start time of the test in milliseconds.
	 */
	protected void printTestEndInformation(String name, long startTime) {
		assert name != null;
		long endTime = System.currentTimeMillis();
		if (name != null) {
			long duration = endTime - startTime;
			System.out.println("=== " + name + " finished at: " + DATE_FORMAT.format(new Date(endTime)) + " (duration: " + duration + " ms)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		final String perspectiveId = getProperty(IRSECoreTestCaseProperties.PROP_SWITCH_TO_PERSPECTIVE);
		assertNotNull("Invalid null-value for test case perspective id!", perspectiveId); //$NON-NLS-1$
		
		// all view managment must happen in the UI thread!
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				// in case the test case is launched within a new workspace, the eclipse intro
				// view is hiding everything else. Find the intro page and hide it.
				hideView("org.eclipse.ui.internal.introview", perspectiveId); //$NON-NLS-1$
				
				// toggle the Remote Systems View zoom state.
				setProperty(PROP_RSE_SYSTEMS_VIEW_ZOOM_STATE_CHANGED, false);
				IViewPart part = showView(IRSEViews.RSE_REMOTE_SYSTEMS_VIEW_ID, perspectiveId);
				assertNotNull("RSE Remote System View is not available!", part); //$NON-NLS-1$
				// Unfortunately, for the zooming, we needs the view reference and not the view part :-(
				IViewReference reference = findView(IRSEViews.RSE_REMOTE_SYSTEMS_VIEW_ID, perspectiveId);
				assertNotNull("Failed to lookup view reference for RSE Remote Systems View!", reference); //$NON-NLS-1$
				if (reference.getPage().getPartState(reference) != IWorkbenchPage.STATE_MAXIMIZED
						&& isProperty(IRSECoreTestCaseProperties.PROP_MAXIMIZE_REMOTE_SYSTEMS_VIEW, true)) {
					reference.getPage().toggleZoom(reference);
					setProperty(PROP_RSE_SYSTEMS_VIEW_ZOOM_STATE_CHANGED, true);
				} else if (reference.getPage().getPartState(reference) == IWorkbenchPage.STATE_MAXIMIZED
										&& isProperty(IRSECoreTestCaseProperties.PROP_MAXIMIZE_REMOTE_SYSTEMS_VIEW, false)) {
					reference.getPage().toggleZoom(reference);
					setProperty(PROP_RSE_SYSTEMS_VIEW_ZOOM_STATE_CHANGED, true);
				}
			}
		});
		
		// Give the UI a chance to repaint if the view zoom state changed
		if (isProperty(PROP_RSE_SYSTEMS_VIEW_ZOOM_STATE_CHANGED, true)) RSEWaitAndDispatchUtil.waitAndDispatch(1000);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		// restore the original view zoom state
		if (isProperty(PROP_RSE_SYSTEMS_VIEW_ZOOM_STATE_CHANGED, true)) {
			final String perspectiveId = getProperty(IRSECoreTestCaseProperties.PROP_SWITCH_TO_PERSPECTIVE);
			assertNotNull("Invalid null-value for test case perspective id!", perspectiveId); //$NON-NLS-1$

			// all view managment must happen in the UI thread!
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					IViewReference reference = findView(IRSEViews.RSE_REMOTE_SYSTEMS_VIEW_ID, perspectiveId);
					assertNotNull("Failed to lookup view reference for RSE Remote Systems View!", reference); //$NON-NLS-1$
					if (reference.getPage().getPartState(reference) == IWorkbenchPage.STATE_MAXIMIZED
							&& isProperty(IRSECoreTestCaseProperties.PROP_MAXIMIZE_REMOTE_SYSTEMS_VIEW, true)) {
						reference.getPage().toggleZoom(reference);
					} else if (reference.getPage().getPartState(reference) != IWorkbenchPage.STATE_MAXIMIZED
							&& isProperty(IRSECoreTestCaseProperties.PROP_MAXIMIZE_REMOTE_SYSTEMS_VIEW, false)) {
						reference.getPage().toggleZoom(reference);
					}
					setProperty(PROP_RSE_SYSTEMS_VIEW_ZOOM_STATE_CHANGED, false);
				}
			});
		}
		
		super.tearDown();
	}
	
	// ***** View and perspective management and support methods *****	

	/**
	 * Finds the view reference for the view identified by the specified id.
	 * 
	 * @param viewId The unique view id. Must be not <code>null</code>.
	 * @param perspectiveId The unique perspective id within the view should be searched. Must be not <code>null</code>.
	 * @return The view reference instance to the view or <code>null</code> if not available.
	 */
	protected final IViewReference findView(String viewId, String perspectiveId) {
		assert viewId != null && perspectiveId != null;
		if (viewId == null || perspectiveId == null) return null;
		
		// First of all, we have to lookup the currently active workbench
		// of the currently active workbench window. 
		IWorkbench workbench = PlatformUI.getWorkbench();
		assertNotNull("Failed to query current workbench instance!", workbench); //$NON-NLS-1$
		// and the corresponding currently active workbench window.
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		assertNotNull("Failed to query currently active workbench window!", window); //$NON-NLS-1$
		
		// Now we have to switch to the specified perspecitve
		try {
			workbench.showPerspective(perspectiveId, window);
		} catch (WorkbenchException e) {
			SystemBasePlugin.logError("Failed to switch to requested perspective (id = " + perspectiveId + ")!", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		// From the active workbench window, we need the active workbench page
		IWorkbenchPage page = window.getActivePage();
		assertNotNull("Failed to query currently active workbench page!", page); //$NON-NLS-1$
		
		return page.findViewReference(viewId);
	}

	/**
	 * Shows and activate the view identified by the specified id.
	 * 
	 * @param viewId The unique view id. Must be not <code>null</code>.
	 * @param perspectiveId The unique perspective id within the view should be activated. Must be not <code>null</code>.
	 * @return The view part instance to the view or <code>null</code> if it cannot be shown.
	 */
	protected final IViewPart showView(String viewId, String perspectiveId) {
		assert viewId != null && perspectiveId != null;
		if (viewId == null || perspectiveId == null) return null;
		
		// First of all, we have to lookup the currently active workbench
		// of the currently active workbench window. 
		IWorkbench workbench = PlatformUI.getWorkbench();
		assertNotNull("Failed to query current workbench instance!", workbench); //$NON-NLS-1$
		// and the corresponding currently active workbench window.
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		assertNotNull("Failed to query currently active workbench window!", window); //$NON-NLS-1$
		
		// Now we have to switch to the specified perspecitve
		try {
			workbench.showPerspective(perspectiveId, window);
		} catch (WorkbenchException e) {
			SystemBasePlugin.logError("Failed to switch to requested perspective (id = " + perspectiveId + ")!", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		// From the active workbench window, we need the active workbench page
		IWorkbenchPage page = window.getActivePage();
		assertNotNull("Failed to query currently active workbench page!", page); //$NON-NLS-1$
		
		IViewPart part = null;
		try {
			part = page.showView(viewId);
		} catch (PartInitException e) {
			SystemBasePlugin.logError("Failed to show view (id = " + viewId + ")!", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		return part;
	}

	/**
	 * Hides the view identified by the specified id.
	 * 
	 * @param viewId The unique view id. Must be not <code>null</code>.
	 * @param perspectiveId The unique perspective id the view should be hidden from. Must be not <code>null</code>.
	 */
	protected final void hideView(String viewId, String perspectiveId) {
		assert viewId != null && perspectiveId != null;
		if (viewId == null || perspectiveId == null) return;
		
		IViewReference viewReference = findView(viewId, perspectiveId);
		if (viewReference != null) {
			// at this point we can safely asume that we can access the active page directly
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().hideView(viewReference);
			// give the UI a chance to execute the hideView and repaint
			RSEWaitAndDispatchUtil.waitAndDispatch(1000);
		}
	}

	// ***** Test data management and support methods *****
	
	/**
	 * Returns the absolute test data location path calculated out of the known
	 * test data location root (<i>org.eclipse.rse.tests plugin location + sub
	 * directory 'test.data'</i>), the specified relative path (<i>relative to the
	 * test data location root</i>) and the current execution host operating system
	 * string (<i>if requested</i>). The method will test the resulting location
	 * to be: <br>
	 * <ul>
	 * 		<li>an directory and</li>
	 * 		<li>is readable.</li>
	 * </ul><br>
	 * If the calculated test data location does not pass these conditions, the
	 * method will return <code>null</code>.
	 * 
	 * @param relativePath A path relative to the test data location root path. Must be not <code>null</code!
	 * @param appendHostOS <code>True</code> if to append the current execution host operating system string, <code>false</code> otherwise.
	 * 
	 * @return The root path to the test data location or <code>null</code> if the test data location does cannot be read or is not a directory.
	 */
	protected final IPath getTestDataLocation(String relativePath, boolean appendHostOS) {
		assert relativePath != null;
		IPath root = null;
		
		if (relativePath != null) {
			Bundle bundle = RSETestsPlugin.getDefault().getBundle();
			if (bundle != null) {
				// build up the complete relative path
				IPath relative = new Path ("test.data").append(relativePath); //$NON-NLS-1$
				if (appendHostOS) relative = relative.append(Platform.getOS());

				URL url = FileLocator.find(bundle, relative, null);
				if (url != null) {
					try {
						// Resolve the URL to an absolute path
						root = new Path(FileLocator.resolve(url).getFile());
						// test the resulting path element to be accessible
						if (!root.toFile().isDirectory() || !root.toFile().canRead()) {
							root = null;
						}
					} catch (IOException e) { /* ignored on purpose */ }
				}
			}
		}
		
		return root;
	}

	// ***** Test failures log collector management and support methods *****
	
	final TestListener TEST_LISTENER = new RSETestFailureListener();
	
	/**
	 * Listens to the test executions and collect the test log files
	 * through the known list of test log collector delegates in a test
	 * had an error or failed.
	 */
	class RSETestFailureListener implements TestListener {

		/* (non-Javadoc)
		 * @see junit.framework.TestListener#startTest(junit.framework.Test)
		 */
		public void startTest(Test test) {
			// nothing to do on start test
		}
		
		/* (non-Javadoc)
		 * @see junit.framework.TestListener#addError(junit.framework.Test, java.lang.Throwable)
		 */
		public synchronized void addError(Test test, Throwable error) {
			if (test != null && error != null) {
				// Log the error to the error log.
				IStatus status = new Status(IStatus.ERROR,
				                            RSETestsPlugin.getDefault().getBundle().getSymbolicName(),
				                            1,
				                            "RSE JUnit test case '" + test + "' failed with error. Possible cause: " + error.getLocalizedMessage(), //$NON-NLS-1$ //$NON-NLS-2$
				                            error
				                           );
				RSETestsPlugin.getDefault().getLog().log(status);
				
				// Collect the log files if at least one test log collector is known
				collectTestLogs(test);
			}
		}

		/* (non-Javadoc)
		 * @see junit.framework.TestListener#addFailure(junit.framework.Test, junit.framework.AssertionFailedError)
		 */
		public synchronized void addFailure(Test test, AssertionFailedError failure) {
			if (test != null && failure != null) {
				// Log the failure to the error log.
				IStatus status = new Status(IStatus.ERROR,
				                            RSETestsPlugin.getDefault().getBundle().getSymbolicName(),
				                            1,
				                            "RSE JUnit test case '" + test + "' failed. Failure: " + failure.getLocalizedMessage(), //$NON-NLS-1$ //$NON-NLS-2$
				                            failure
				                           );
				RSETestsPlugin.getDefault().getLog().log(status);
				
				// Collect the log files if at least one test log collector is known
				collectTestLogs(test);
			}
		}

		/* (non-Javadoc)
		 * @see junit.framework.TestListener#endTest(junit.framework.Test)
		 */
		public void endTest(Test test) {
			// nothing to do on end test
		}
	}
	
	/**
	 * Collect the test logs for the failed test.
	 * 
	 * @param test The failed test. Must be not <code>null</code>.
	 */
	protected final synchronized void collectTestLogs(Test test) {
		if (test != null) {
			// get a snapshot of the currently known test log collector delegates
			IRSETestLogCollectorDelegate[] delegates = RSETestsPlugin.getDefault().getTestLogCollectorDelegates();
			if (delegates.length > 0) {
				// Write the logs to the test plugins state location. Check if older archives with the same
				// name already exist and delete them.
				IPath stateLocation = RSETestsPlugin.getDefault().getStateLocation();
				if (stateLocation != null && stateLocation.toFile().isDirectory()) {
					// Build up the archives name
					String archiveName = "RSEUnittestFailureLogs_" + test + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
					IPath archivePath = stateLocation.append(archiveName);
					// Delete the target file if it exist.
					if (archivePath.toFile().exists()) archivePath.toFile().delete();

					// Now the file should not exist anymore --> open the new ZIP archive
					ZipOutputStream stream = null;
					try {
						if (archivePath.toFile().createNewFile()) {
							stream = new ZipOutputStream(new FileOutputStream(archivePath.toFile()));
							stream.setLevel(9);
							
							// cache the names of the entries added to the ZIP stream.
							// They needs to be unique!
							Set nameCache = new HashSet();
							
							// call each test log collector delegate for the absolute file names
							// and add each of the returned files to the ZIP archive.
							for (int i = 0; i < delegates.length; i++) {
								IRSETestLogCollectorDelegate delegate = delegates[i];
								// get the list of log files to collect from the delegate
								IPath[] locations = delegate.getAbsoluteLogFileLocations();
								if (locations != null && locations.length > 0) {
									for (int j = 0; j < locations.length; j++) {
										IPath location = locations[j];
										// The location is expected to be absolute, the file
										// must be a file and it must be readable.
										if (location != null && location.isAbsolute()
												&& location.toFile().isFile() && location.toFile().canRead()) {
											File file = location.toFile();
											String entryName = file.getName();
											if (nameCache.contains(entryName)) {
												// unify the name by prepending the directory elements in
												// front of the name till it is unique.
												IPath unifier = location.removeLastSegments(1);
												entryName = unifier.lastSegment() + "_" + entryName; //$NON-NLS-1$
												while (nameCache.contains(entryName) && !unifier.isEmpty()) {
													unifier = location.removeLastSegments(1);
													entryName = unifier.lastSegment() + "_" + entryName; //$NON-NLS-1$
												}
												
												// if the name is still not unique, append a count to it
												long count = 0;
												// force to make a copy of the current name
												String base = new String(entryName.getBytes());
												while (nameCache.contains(entryName)) {
													entryName = base + " (" + count + ")"; //$NON-NLS-1$ //$NON-NLS-2$
													count++;
												}
											} else {
												nameCache.add(entryName);
											}
											
											ZipEntry zipEntry = new ZipEntry(entryName);
											zipEntry.setTime(file.lastModified());
											stream.putNextEntry(zipEntry);
											
											// Read the file bytewise and write it bytewise to the ZIP
											BufferedInputStream fileStream = null;
											try {
												fileStream = new BufferedInputStream(new FileInputStream(file));
												int character = fileStream.read();
												while (character >= 0) {
													stream.write(character);
													character = fileStream.read();
												}
											} catch (IOException e) {
												if (Platform.inDebugMode()) e.printStackTrace();
											} finally {
												// Explicitly catch the possible IOException of the close() call here.
												// This keep the loop going, otherwise we would drop out of all.
												try { if (fileStream != null) fileStream.close(); } catch (IOException e) { if (Platform.inDebugMode()) e.printStackTrace(); }
												stream.closeEntry();
											}
										}
									}
								}
								
								// If done with the current test log collector delegate, signal the delegate to dispose himself.
								// This gives the delegate the chance to remove any possibly created temporary file.
								delegate.dispose();
							}
						}
					} catch(IOException e) { 
						 /* ignored on purpose */
					} finally {
						// always close the stream if open
						try { if (stream != null) stream.close(); } catch (IOException e) { if (Platform.inDebugMode()) e.printStackTrace(); }
					}
				}
			}
		}
	}
}
