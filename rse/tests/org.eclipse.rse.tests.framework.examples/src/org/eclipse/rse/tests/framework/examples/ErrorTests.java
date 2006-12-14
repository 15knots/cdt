/* *******************************************************************************
 * Copyright (c) 2006 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * David Dykstal (IBM) - initial contribution.
 * *******************************************************************************/
package org.eclipse.rse.tests.framework.examples;

public class ErrorTests extends AbstractTest {

	public void testError01() {
		sleep(1000);
		throw new RuntimeException("generic runtime exception 01"); //$NON-NLS-1$
	}
	
	public void testError02() {
		sleep(1000);
		throw new RuntimeException("generic runtime exception 02"); //$NON-NLS-1$
	}
	
	public void testError03() {
		sleep(1000);
		throw new RuntimeException("generic runtime exception 03"); //$NON-NLS-1$
	}
	
}


