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
 * StringModel represents a resource as a String.
 * 
 * @author Trent Hoeppner
 */
public class StringModel implements Model<String> {

	private String data;

	/**
	 * Constructor for this.
	 *
	 * @param data
	 *            The string data for this model. Cannot be null, but may be
	 *            empty.
	 */
	public StringModel(String data) {
		if (data == null) {
			throw new NullPointerException("data cannot be null.");
		}

		this.data = data;
	}

	/**
	 * Returns the string data for this model.
	 *
	 * @return The string data for this model. Will not be null, but may be
	 *         empty.
	 */
	@Override
	public String getModel() {
		return data;
	}

}
