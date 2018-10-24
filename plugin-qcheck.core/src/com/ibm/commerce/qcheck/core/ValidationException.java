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

import java.util.List;

/**
 * An exception to indicate an error that occurred during validation. This error
 * should not be used to indicate an IOException, as that is indicated
 * separately in {@link Validator#analyze(List)}.
 * 
 * @author Trent Hoeppner
 */
public class ValidationException extends Exception {

	/**
	 * Constructor for ValidationException with no message or cause.
	 */
	public ValidationException() {
		this(null, null);
	}

	/**
	 * Constructor for ValidationException with a message but no cause.
	 * 
	 * @param message
	 *            The message that describes the error that occurred. May be
	 *            null.
	 */
	public ValidationException(String message) {
		this(message, null);
	}

	/**
	 * Constructor for ValidationException with a message and cause.
	 * 
	 * @param message
	 *            The message that describes the error that occurred. May be
	 *            null.
	 * @param cause
	 *            The error that caused this error. May be null.
	 */
	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
