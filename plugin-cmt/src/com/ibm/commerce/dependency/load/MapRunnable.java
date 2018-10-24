package com.ibm.commerce.dependency.load;

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
 * This interface represents code that can be run on the key/value pairs of a
 * Map.
 * 
 * @author Trent Hoeppner
 */
public interface MapRunnable<K, V> {

	/**
	 * Processes the given key and value.
	 * 
	 * @param key
	 *            The key to process.
	 * @param value
	 *            The value to process.
	 * 
	 * @return True to stop execution on other key/value pairs, false to
	 *         continue processing other key/value pairs.
	 */
	boolean run(K key, V value);

	/**
	 * Adds the properties set by this to the given context.
	 * 
	 * @param context
	 *            The context to add the properties to. This value will not be
	 *            null.
	 */
	void addProperties(LoadingContext context);
}
