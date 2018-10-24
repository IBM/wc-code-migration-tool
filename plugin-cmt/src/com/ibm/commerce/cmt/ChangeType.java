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

/**
 * This represents the different types of changes that can occur in a list or a
 * map.
 * 
 * @author Trent Hoeppner
 */
public enum ChangeType {
	/**
	 * An item was added to a list or map.
	 */
	ADD,

	/**
	 * An item was updated in a list or map.
	 */
	UPDATE,

	/**
	 * An item was removed in a list or map.
	 */
	REMOVE;
}
