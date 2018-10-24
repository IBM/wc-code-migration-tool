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

import java.util.List;
import java.util.Map;

/**
 * This class helps check parameter values.
 * 
 * @author Trent Hoeppner
 */
public class Check {

	/**
	 * Checks that a string is not null or empty.
	 * 
	 * @param variable
	 *            The variable to check.
	 * @param name
	 *            The name of the variable, used in exception messages.
	 * 
	 * @throws NullPointerException
	 *             If the variable is null.
	 * @throws IllegalArgumentException
	 *             If the variable is empty.
	 */
	public static void notNullOrEmpty(String variable, String name) {
		notNull(variable, name);
		notEmpty(variable, name);
	}

	/**
	 * Checks that an object is not null.
	 * 
	 * @param variable
	 *            The variable to check.
	 * @param name
	 *            The name of the variable, used in exception messages.
	 * 
	 * @throws NullPointerException
	 *             If the variable is null.
	 */
	public static void notNull(Object variable, String name) {
		if (variable == null) {
			throw new NullPointerException(name + " cannot be null.");
		}
	}

	/**
	 * Checks that a string is not empty.
	 * 
	 * @param variable
	 *            The variable to check.
	 * @param name
	 *            The name of the variable, used in exception messages.
	 * 
	 * @throws IllegalArgumentException
	 *             If the variable is empty.
	 */
	public static void notEmpty(String variable, String name) {
		if (variable.isEmpty()) {
			throw new IllegalArgumentException(name + " cannot be empty.");
		}
	}

	/**
	 * Checks that a list is not empty.
	 * 
	 * @param variable
	 *            The variable to check.
	 * @param name
	 *            The name of the variable, used in exception messages.
	 * 
	 * @throws IllegalArgumentException
	 *             If the variable is empty.
	 */
	public static void notEmpty(List<?> variable, String name) {
		if (variable.isEmpty()) {
			throw new IllegalArgumentException(name + " cannot be empty.");
		}
	}

	/**
	 * Checks that a map is not empty.
	 * 
	 * @param variable
	 *            The variable to check.
	 * @param name
	 *            The name of the variable, used in exception messages.
	 * 
	 * @throws IllegalArgumentException
	 *             If the variable is empty.
	 */
	public static void notEmpty(Map<?, ?> variable, String name) {
		if (variable.isEmpty()) {
			throw new IllegalArgumentException(name + " cannot be empty.");
		}
	}

	/**
	 * Checks that a list is not null or empty.
	 * 
	 * @param variable
	 *            The variable to check.
	 * @param name
	 *            The name of the variable, used in exception messages.
	 * 
	 * @throws NullPointerException
	 *             If the variable is null.
	 * @throws IllegalArgumentException
	 *             If the variable is empty.
	 */
	public static void notNullOrEmpty(List<?> variable, String name) {
		notNull(variable, name);
		notEmpty(variable, name);
	}

	/**
	 * Checks that a map is not null or empty.
	 * 
	 * @param variable
	 *            The variable to check.
	 * @param name
	 *            The name of the variable, used in exception messages.
	 * 
	 * @throws NullPointerException
	 *             If the variable is null.
	 * @throws IllegalArgumentException
	 *             If the variable is empty.
	 */
	public static void notNullOrEmpty(Map<?, ?> variable, String name) {
		notNull(variable, name);
		notEmpty(variable, name);
	}

}
