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

import java.util.Calendar;
import java.util.TimerTask;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.update.ui.utils.UpdateUtil;

/**
 * The class is used to run the method check() of class {@link CheckAvailable}
 * periodically, which extends TimerTask.
 * 
 * @author Trent Hoeppner
 */
public class CheckAvailableTimerTask extends TimerTask {

	/**
	 * The milliseconds between now and the checking time.
	 */
	private long margin = 0;

	/**
	 * The constructor.
	 */
	public CheckAvailableTimerTask() {
		Calendar now = Calendar.getInstance();
		Calendar gmtCalendar = UpdateUtil.convertToGMT(now);
		margin = UpdateUtil.getMargin(gmtCalendar);
	}

	/**
	 * The getter method of margin.
	 * 
	 * @return The variable margin.
	 */
	public long getMargin() {
		return margin;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		try {
			CheckAvailable.getInstance().check();
		} catch (Exception e) {
			Debug.FRAMEWORK.log(e);
		}
	}
}
