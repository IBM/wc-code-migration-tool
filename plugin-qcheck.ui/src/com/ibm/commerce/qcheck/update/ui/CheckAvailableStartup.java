package com.ibm.commerce.qcheck.update.ui;

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

import java.util.Timer;

import org.eclipse.ui.IStartup;

/**
 * This class is used to activate the plug in when the workbench window starts.
 * As this plug in, we need to make the Timer start. To do this, firstly
 * implements the IStartup, and then put codes which start the Timer in the
 * earlyStartup method.
 * 
 * @author Trent Hoeppner
 */
public class CheckAvailableStartup implements IStartup {

	/**
	 * The timer, which will be run at 8:00 GMT every day.
	 */
	private static Timer timer = new Timer();

	/**
	 * Get the timer variable.
	 *
	 * @return The instant variable, timer.
	 */
	public static Timer getTimer() {
		return timer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void earlyStartup() {
		// At first,check for new availables manually.
		// CheckAvailable.getInstance().check();

		// CheckAvailableTimerTask task = new CheckAvailableTimerTask();
		// timer.schedule(task, task.getMargin(), 24 * 60 * 60 * 1000);
		// timer.schedule(task, 5000, 20000);
		//
		// Options.ensureLoaded();
		// Short secondsBetweenChecks =
		// Options.Attributes.UPDATE_FREQUENCY.getValue();
		// timer.schedule(task, 10 * 1000, secondsBetweenChecks * 1000);
		// timer.schedule(new InitializeButtonsTimerTask(), 2 * 1000);
	}

}
