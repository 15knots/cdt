/* *******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Uwe Stieber (Wind River) - initial contribution.
 * *******************************************************************************/
package org.eclipse.rse.tests.core;

import org.eclipse.swt.widgets.Display;

/**
 * RSE unit test framework helper class providing common functionality
 * to hold the current thread from execution time out and/or condition
 * based.
 * <p>
 * <b>Note:</b> The class cannot be instanciated as all provided methods
 * are declared static! 
 */
public final class RSEWaitAndDispatchUtil {

	/**
	 * Private constructor.
	 */
	private RSEWaitAndDispatchUtil() {
		// nothing to do. The class cannot be instanciated.
	}
	
	/**
	 * Blocks the calling thread from execution till the specified
	 * time out has exceeded. If the calling thread is an display thread,
	 * the display event dispatching will be kept going during this time.
	 * The method will return immediatelly if any time out less or equal
	 * to 0 is specified.
	 * 
	 * @param timeout The time to wait till the method return in milli seconds. Must be larger than 0.
	 */
	public static void waitAndDispatch(long timeout) {
		assert timeout > 0;
		if (timeout > 0) {
			long start = System.currentTimeMillis();
			Display display = Display.findDisplay(Thread.currentThread());
			if (display != null) {
				// ok, we are running within a display thread --> keep the
				// display event dispatching running.
				long current = System.currentTimeMillis();
				while ((current - start) < timeout) {
					if (!display.readAndDispatch()) display.sleep();
					current = System.currentTimeMillis();
				}
			} else {
				// we are not running within a display thread --> we can
				// just block the thread here
				try { Thread.sleep(timeout); } catch (InterruptedException e) { /* ignored on purpose */ }
			}
		}
	}
	
	/**
	 * Public interface used to interrupt waiting for a condition to
	 * come true and/or a timeout occurs.
	 */
	public interface IInterruptCondition {
		/**
		 * Test if the interrupt condition is <code>true</code>.
		 * 
		 * @return <code>true</code> if the condition is fulfilled and the wait method should return, <code>false</code> otherwise.
		 */
		public boolean isTrue();
		
		/**
		 * Dispose the interrupt condition. Cleanup whatever necessary.
		 * This method will be called only once just before the wait
		 * method returns.
		 */
		public void dispose();
	}
	
	/**
	 * Blocks the calling thread from execution till the specified
	 * time out has exceeded or the specified interrupt condition is <code>true</code>.
	 * If the calling thread is an display thread, the display event dispatching will
	 * be kept going during this time. The method will return immediatelly if any time
	 * out less than 0 is specified or the interrupt condition is <code>true</code> from
	 * the beginning. If a time out of 0 is specified, the method will be wait indefinite
	 * amount of time till the interrupt condition ever becomes <code>true</code>.
	 * 
	 * @param timeout The time to wait till the method return in milli seconds. Must be larger or equals than 0.
	 * @param condition The interrupt condition to test. Must be not <code>null</code>.
	 * @return <code>True</code> if the method returned because of the timeout, <code>false</code> if the
	 *         method returned because of the condition became true.
	 */
	public static boolean waitAndDispatch(long timeout, IInterruptCondition condition) {
		assert timeout >= 0 && condition != null;
		
		boolean isTimedOut= false;
		if (timeout >= 0 && condition != null) {
			long start = System.currentTimeMillis();
			Display display = Display.findDisplay(Thread.currentThread());
			if (display != null) {
				// ok, we are running within a display thread --> keep the
				// display event dispatching running.
				long current = System.currentTimeMillis();
				while (timeout == 0 || (current - start) < timeout) {
					if (condition.isTrue()) break;
					if (!display.readAndDispatch()) display.sleep();
					current = System.currentTimeMillis();
				}
				isTimedOut = (current - start) >= timeout && timeout > 0;
			} else {
				// ok, we are not running within a display thread --> we can
				// just block the thread here
				long current = System.currentTimeMillis();
				while (timeout == 0 || (current - start) < timeout) {
					if (condition.isTrue()) break;
					try { Thread.sleep(50); } catch (InterruptedException e) { /* ignored on purpose */ }
					current = System.currentTimeMillis();
				}
				isTimedOut = (current - start) >= timeout && timeout > 0;
			}
		}
		
		// Signal the interrupt condition that we are done here
		// and it can cleanup whatever necessary.
		condition.dispose();
		
		return isTimedOut;
	}
}
