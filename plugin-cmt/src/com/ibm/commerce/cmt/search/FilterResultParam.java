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

import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;

/**
 * This interface represents a kind of parameter that can be used to further
 * refine a search result. A base {@link SearchParam} may find an instance, and
 * implementations of this class will be used to refine whether that instance is
 * valid for the final result.
 *
 * @param <T>
 *            The type of the result to filter.
 * 
 * @author Trent Hoeppner
 */
public interface FilterResultParam<T> extends Param {

	boolean accept(Context context, T result);
}
