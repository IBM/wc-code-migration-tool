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
import java.util.regex.Matcher;

import com.ibm.commerce.cmt.plan.Range;

/**
 * This class represents a single search result that matches the required
 * criteria for a pattern.
 * 
 * @param <T>
 *            The type of the result object.
 * 
 * @author Trent Hoeppner
 */
public interface SearchResult<T> {

	Range getRange();

	T getDataObject();

	List<Matcher> getMatchers();
}
