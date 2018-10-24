package com.ibm.commerce.cmt;

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

/**
 * This class helps to log time intervals for debugging performance problems.
 * 
 * @author Trent Hoeppner
 */
public class TimeLogger {

	/**
	 * The time that the logger was created.
	 */
	private long startTime = System.currentTimeMillis();

	/**
	 * Logs the amount of time in milliseconds since the logger was created.
	 * 
	 * @param intervalName
	 *            The name of the interval, such as "time to query the
	 *            database". This value cannot be null.
	 */
	public void logTotal(String intervalName) {
		long endTime = System.currentTimeMillis();
		System.out.println(intervalName + " took " + (endTime - startTime) + " ms");
	}
}
