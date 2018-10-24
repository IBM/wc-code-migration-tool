package com.ibm.commerce.qcheck.tools;

/*
 *-----------------------------------------------------------------
 * Copyright 2018 Trent Hoeppner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *-----------------------------------------------------------------
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.ibm.commerce.qcheck.core.ExternalValidatorResource;
import com.ibm.commerce.qcheck.core.ValidationException;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.TimeEnum;

import junit.framework.TestCase;

/**
 * ValidatorRunnerTest tests the ValidatorRunner class.
 * 
 * @author Trent Hoeppner
 */
public class ValidatorRunnerTest extends TestCase {

	/**
	 * The number of threads to run concurrently.
	 */
	private static final int NUM_THREADS = 10;

	/**
	 * The total number of threads that were canceled.
	 */
	private int totalCanceled = 0;

	/**
	 * Constructor for this.
	 */
	public ValidatorRunnerTest() {
		// do nothing
	}

	/**
	 * Adds one to the count of canceled threads.
	 */
	private synchronized void addCanceled() {
		totalCanceled++;
	}

	/**
	 * Returns the number of canceled threads.
	 *
	 * @return The number of canceled threads. Will be &gt;= 0.
	 */
	private synchronized int getNumCanceled() {
		return totalCanceled;
	}

	/**
	 * Starts up several threads to run the validator on a file. All threads
	 * block before the ValidatorRunner class is called, and they are all
	 * released at the same time to test how the system responds. All threads
	 * should be canceled except one.
	 */
	public void testConcurrentRuns() {
		CyclicBarrier barrier = new CyclicBarrier(NUM_THREADS, new Runnable() {

			@Override
			public void run() {
				System.out.println("barrier broken");
			}

		});

		for (int i = 0; i < NUM_THREADS; i++) {
			new Thread(new BarrierObeyingValidatorRunner(barrier)).start();
		}

		// the current thread was started by JUnit. When this thread finishes
		// the test is over, and all the threads that were spawned will have no
		// effect after that. So, we sleep here to make sure all threads have a
		// chance to run.
		try {
			System.out.println("Sleeping.");
			Thread.currentThread().sleep(50 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(NUM_THREADS - 1, getNumCanceled());
		System.out.println("Finished with " + getNumCanceled() + " canceled.");
	}

	/**
	 * BarrierObeyingValidatorRunner is made to start the ValidatorRunner, but
	 * only after waiting until all threads are ready, enforced by a barrier.
	 */
	private class BarrierObeyingValidatorRunner implements Runnable {

		/**
		 * The barrier to use to wait for other threads. This value will never
		 * be null.
		 */
		private CyclicBarrier barrier;

		/**
		 * Constructor for BarrierObeyingValidatorRunner.
		 *
		 * @param barrier
		 *            The barrier to use to wait for other threads. Cannot be
		 *            null.
		 */
		private BarrierObeyingValidatorRunner(CyclicBarrier barrier) {
			this.barrier = barrier;
		}

		/**
		 * First waits for the barrier, then runs the ValidatorRunner on a
		 * single class.
		 */
		@Override
		public void run() {
			try {
				barrier.await();

				ValidatorResource resource = new ExternalValidatorResource(new File("testData\\TestClass.java"));
				List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
				resources.add(resource);

				ValidatorRunner.runValidators(resources, TimeEnum.ASYOUTYPE, new FakeProblemActionFactory(),
						new NullProgressMonitor());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (OperationCanceledException e) {
				addCanceled();
			} catch (ValidationException e) {
				e.printStackTrace();
			}

		}

	}
}
