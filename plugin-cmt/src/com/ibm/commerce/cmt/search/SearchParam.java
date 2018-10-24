package com.ibm.commerce.cmt.search;

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

import com.ibm.commerce.cmt.Context;

/**
 * This interface represents a search parameter used by commands to find the
 * next piece of code to change. This class uses a {@link Context} as a source
 * of data, but it tracks its own progress through the context to ensure
 * duplicates are not found.
 * 
 * @author Trent Hoeppner
 */
public interface SearchParam extends FileFilterParam {

	/**
	 * Finds the next code in the context to replace or remove.
	 * 
	 * @param context
	 *            The data which defines the search space. This value cannot be
	 *            null.
	 * 
	 * @return The ASTNodes which define the next replacement point. A value of
	 *         null will be returned when no more replacement points are found.
	 *         This value will not be empty.
	 */
	List<? extends SearchResult<?>> findAll(Context context);

}
