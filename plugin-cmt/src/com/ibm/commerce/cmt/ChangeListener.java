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
 * This interface is for listening to changes in a {@link NotifierList} or
 * {@link NotifierSet}. When an item is added, removed or set in a NotifierList,
 * the listener will be notified.
 * 
 * @author Trent Hoeppner
 */
public interface ChangeListener {

	/**
	 * Notifies this that the list or set being listened to has changed in some
	 * way.
	 */
	void changed();
}
