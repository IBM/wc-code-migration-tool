package com.ibm.commerce.qcheck.core;

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
 * Debug is used to log messages about parts of the validation plug-in. Each
 * logger can be active or not. This is safe for using both in an Eclipse
 * environment and in a normal Java environment (for example, during unit
 * testing). The default configuration is to use Eclipse logging. To change it
 * to a normal Java environment this, use {@link #setInEclipse(boolean)}.
 * 
 * @author Trent Hoeppner
 */
public enum Debug {
	/**
	 * This value is used to log configuration events and information.
	 */
	CONFIG,

	/**
	 * This value is used to log information when processing JavaDoc comments.
	 */
	COMMENT,

	/**
	 * This value is used to log information about the framework, normally in
	 * the core plug-in.
	 */
	FRAMEWORK,

	/**
	 * This value is used to log information about the validators, normally in
	 * the tools plug-in and extensions.
	 */
	VALIDATOR,

	/**
	 * This value is used to log information about the check-in wizard, normally
	 * in the wizard.ui plug-in.
	 */
	WIZARD;

	private static boolean inEclipse = true;

	/**
	 * Sets whether this is being used in an Eclipse environment. This is
	 * normally used for unit testing, where the whole Eclipse environment is
	 * not necessary for the test.
	 *
	 * @param inEclipse
	 *            True indicates that this is being used in Eclipse, false
	 *            indicates that it is not.
	 */
	public static void setInEclipse(boolean inEclipse) {
		Debug.inEclipse = inEclipse;
	}

	/**
	 * Returns whether this is being used in an Eclipse environment.
	 *
	 * @return True if this is being used in an Eclipse environment, false
	 *         otherwise.
	 */
	public static boolean isInEclipse() {
		return inEclipse;
	}

	private boolean active;

	/**
	 * Returns whether this logger is active. A call to this can be used to
	 * check if logging is allowed before calling the logger methods.
	 *
	 * @return True if this is actively recording messages, false otherwise.
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Sets whether this logger is active.
	 *
	 * @param active
	 *            True indicates that logging should be done, false indicates
	 *            that logging should not be done.
	 */
	void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * Converts the given objects to strings, concatenates them, and logs them
	 * with a line separator.
	 *
	 * @param strings
	 *            The objects to convert to strings. Cannot be null, but may be
	 *            empty.
	 */
	public void log(Object... strings) {
		if (isInEclipse()) {
			EclipseUtil.getDefault().log(strings);
		} else {
			log(null, strings);
		}
	}

	/**
	 * Converts the given objects to strings, concatenates them, and logs them
	 * with the given error message, followed by a line separator.
	 *
	 * @param e
	 *            The error to log. If null, it will be ignored.
	 * @param strings
	 *            The objects to convert to strings. Cannot be null, but may be
	 *            empty.
	 */
	public void log(Throwable e, Object... strings) {
		if (isInEclipse()) {
			EclipseUtil.getDefault().log(e, strings);
		} else {
			StringBuffer buf = new StringBuffer();
			if (e != null) {
				buf.append("Error:");
				buf.append(e.getMessage());
				buf.append(", ");
			}

			for (Object object : strings) {
				String string = String.valueOf(object);
				buf.append(string);
			}

			System.out.println(buf.toString());
		}

	}

}
