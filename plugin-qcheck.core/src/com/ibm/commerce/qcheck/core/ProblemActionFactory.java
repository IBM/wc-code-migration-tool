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

import java.net.URL;

/**
 * This class generates {@link ProblemAction ProblemActions} based on some
 * standard action types. The reason for making this interface is so that the
 * actions generated can vary, depending on the type of environment. For
 * example, a complicated action in a GUI might have a confirmation dialog. The
 * same action in a command-line environment might just log the details, and
 * generate warning or error messages as appropriate.
 * 
 * @author Trent Hoeppner
 */
public interface ProblemActionFactory {

	/**
	 * Returns an action that can replace text in the document with another set
	 * of text.
	 *
	 * @param resource
	 *            The resource which has the problem. This value cannot be null.
	 * @param startPosition
	 *            The 0-based index into the file which marks the start of the
	 *            section to be replaced. This value must be &gt;= 0.
	 * @param endPosition
	 *            The 0-based index into the file which marks the end
	 *            (exclusive) of the section to be replaced. This value must be
	 *            &gt;= 0.
	 * @param replacement
	 *            The value to replace the marked section with. This value
	 *            cannot be null, but may be empty.
	 *
	 * @return The action to perform the replacement. This value will not be
	 *         null.
	 */
	ProblemAction buildReplace(ValidatorResource resource, int startPosition, int endPosition, String replacement);

	/**
	 * Returns an action that can open or display the given link for more
	 * information.
	 *
	 * @param url
	 *            The link which is used by the action. This value cannot be
	 *            null.
	 *
	 * @return The action to use the link. This value will not be null.
	 */
	ProblemAction buildLink(URL url);
}
