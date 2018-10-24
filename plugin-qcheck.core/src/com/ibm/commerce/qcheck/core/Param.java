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

import java.util.Collection;

/**
 * <code>Param</code> helps check constraints on method and constructor
 * parameters. This is in accordance with the "fail fast" principle so that
 * errors can be found much faster than if invalid parameters and states were
 * ignored.
 * 
 * @author Trent Hoeppner
 */
public final class Param {

	/**
	 * Constructor for <code>Param</code>. Private to prevent instantiation.
	 */
	private Param() {
		// do nothing
	}

	/**
	 * Checks that the <code>param</code> is not null. If so, a
	 * NullPointerException is thrown.
	 *
	 * @param param
	 *            The parameter value to check. Cannot be null.
	 * @param name
	 *            The name of the parameter in the code. This is used to
	 *            construct the message for the exception. If null or empty, the
	 *            message will be malformed.
	 *
	 * @throws NullPointerException
	 *             If <code>param</code> is null.
	 */
	public static void notNull(Object param, String name) throws NullPointerException {
		if (param == null) {
			throw new NullPointerException(name + " cannot be null.");
		}
	}

	/**
	 * Checks that the <code>param</code> is not null or empty.
	 *
	 * @param param
	 *            The parameter value to check. Cannot be null or empty.
	 * @param name
	 *            The name of the parameter in the code. This is used to
	 *            construct the message for the exception. If null or empty, the
	 *            message will be malformed.
	 *
	 * @throws NullPointerException
	 *             If <code>param</code> is null.
	 * @throws IllegalArgumentException
	 *             If <code>param</code> is empty.
	 */
	public static void notNullOrEmpty(String param, String name) throws NullPointerException, IllegalArgumentException {
		notNull(param, name);

		if (param.isEmpty()) {
			throw new IllegalArgumentException(name + " cannot be empty.");
		}
	}

	/**
	 * Checks that the <code>param</code> is not null or empty.
	 *
	 * @param param
	 *            The parameter value to check. Cannot be null or empty.
	 * @param name
	 *            The name of the parameter in the code. This is used to
	 *            construct the message for the exception. If null or empty, the
	 *            message will be malformed.
	 *
	 * @throws NullPointerException
	 *             If <code>param</code> is null.
	 * @throws IllegalArgumentException
	 *             If <code>param</code> is empty.
	 */
	public static void notNullOrEmpty(Collection param, String name)
			throws NullPointerException, IllegalArgumentException {
		notNull(param, name);

		if (param.isEmpty()) {
			throw new IllegalArgumentException(name + " cannot be empty.");
		}
	}

	public static void gtE0(long param, String name) {
		gtEOther(param, name, 0, "zero");
	}

	public static void gtEOther(long param, String name, long other, String otherName) {
		if (param < other) {
			throw new IndexOutOfBoundsException(
					name + " (" + param + ") must be greater than or equal to " + otherName + " (" + other + ").");
		}
	}

	public static void gt0(long param, String name) {
		gtOther(param, name, 0, "zero");
	}

	public static void gtOther(long param, String name, long other, String otherName) {
		if (param <= other) {
			throw new IndexOutOfBoundsException(
					name + " (" + param + ") must be greater than " + otherName + " (" + other + ").");
		}
	}

	public static void lt0(long param, String name) {
		ltOther(param, name, 0, "zero");
	}

	public static void ltOther(long param, String name, long other, String otherName) {
		if (param >= other) {
			throw new IndexOutOfBoundsException(
					name + " (" + param + ") must be less than " + otherName + " (" + other + ").");
		}
	}

	public static void ltE0(long param, String name) {
		ltEOther(param, name, 0, "zero");
	}

	public static void ltEOther(long param, String name, long other, String otherName) {
		if (param > other) {
			throw new IndexOutOfBoundsException(
					name + " (" + param + ") must be less than or equal to " + otherName + " (" + other + ").");
		}
	}

	public static void boundsValid(int startIndex, String startName, int endIndex, String endName, String string,
			String stringName) {
		Param.gtE0(startIndex, startName);
		Param.gtEOther(endIndex, endName, startIndex, startName);
		String stringLengthName = stringName + ".length()";
		Param.ltOther(startIndex, startName, string.length(), stringLengthName);
		Param.ltEOther(endIndex, endName, string.length(), stringLengthName);
	}

	/**
	 * Returns whether the given parameter is what is expected. This can be used
	 * to ensure consistency between objects that must have a common attribute.
	 *
	 * @param <T>
	 *            The type of the input parameter. This cannot be a Double,
	 *            Float, or array.
	 * @param param
	 *            The value to check. May be null.
	 * @param expected
	 *            The expected value. May be null.
	 * @param name
	 *            The name of the parameter in the code. This is used to
	 *            construct the message for the exception. If null or empty, the
	 *            message will be malformed.
	 *
	 * @throws IllegalArgumentException
	 *             If <code>param</code> is not the same as
	 *             <code>expected</code>, or the type is invalid.
	 */
	public static <T> void equals(T param, T expected, String name) throws IllegalArgumentException {
		if (param != null) {
			Class<? extends Object> type = param.getClass();
			if (type.equals(Double.class) || param.getClass().equals(Float.class)) {
				throw new IllegalArgumentException(name + " cannot be of class " + type);
			}
		}

		boolean equal = false;
		if (param == expected) {
			equal = true;
		} else if (param == null || expected == null) {
			equal = false;
		} else {
			equal = param.equals(expected);
		}

		if (!equal) {
			throw new IllegalArgumentException(name + " should be equal to " + expected + ", but was " + param);
		}
	}
}
