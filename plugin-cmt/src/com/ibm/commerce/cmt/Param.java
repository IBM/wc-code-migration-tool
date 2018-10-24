package com.ibm.commerce.cmt;

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
 * This class represents a parameter used by commands to specify search criteria
 * or some action to be taken.
 * <p>
 * Every instance can be described by a string, and is in fact created by a
 * string description. The general format of the string is as follows:
 * 
 * <pre>
 * \name{<data>}
 * </pre>
 * 
 * or
 * 
 * <pre>
 * \name{\subparam1{...}\subparam2{...}...}
 * </pre>
 * 
 * @author Trent Hoeppner
 */
public interface Param extends XMLConvertable {

	/**
	 * Returns the name of this parameter.
	 * 
	 * @return The name of this. Will not be null or empty.
	 */
	String getName();

	/**
	 * Returns the data for this parameter that provides some details. The
	 * format of this data depends on the specific implementation.
	 * 
	 * @return The data for this parameter that provides some details. If this
	 *         contains sub parameters, the data will be null. Will not be
	 *         empty.
	 */
	String getData();

	/**
	 * Returns the sub parameters that provide more dynamic details. The allowed
	 * types of Parameters depend on the specific implementation.
	 * 
	 * @return The sub parameters that provide more dynamic details. This value
	 *         will be empty if there are no sub parameters. This value will not
	 *         be null.
	 */
	List<? extends Param> getSubParams();

}
