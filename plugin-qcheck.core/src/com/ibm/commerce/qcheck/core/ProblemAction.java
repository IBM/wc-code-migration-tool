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

import java.io.IOException;

/**
 * This interface represents some action that can be taken to resolve a problem
 * detected by QCheck. For example, different actions are to replace some text,
 * or open a link in a browser for more information.
 * 
 * @author Trent Hoeppner
 */
public interface ProblemAction {

	/**
	 * The text to display to the user which describes the action that will be
	 * taken.
	 *
	 * @return The description of this action. Will not be null or empty.
	 */
	String getDescription();

	/**
	 * Executes the action.
	 *
	 * @throws IOException
	 *             If an input or output error occurs when attempting to execute
	 *             the action.
	 */
	void execute() throws IOException;
}
