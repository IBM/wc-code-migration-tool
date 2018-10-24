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
 * Model is a representation of some resource that can be validated. Some
 * resources may have many models available to be used, which help validators
 * analyze the resource in different ways.
 *
 * @param <T>
 *            The type of data returned by this model.
 * 
 * @author Trent Hoeppner
 */
public interface Model<T> {

	/**
	 * Returns the data for this which can be used to get information about the
	 * resource.
	 *
	 * @return The data object for this model. Will not be null.
	 */
	T getModel();
}
