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

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.dependency.model.JavaItemFactory;
import com.ibm.commerce.dependency.model.JavaItemIndex;
import com.ibm.commerce.dependency.model.JavaItemUtil2;
import com.ibm.commerce.dependency.task.TaskContext;
import com.ibm.commerce.dependency.task.TaskList;

/**
 * This class represents the information that is needed to perform tasks in this
 * package.
 * 
 * @author Trent Hoeppner
 */
public class LoadingContext extends TaskContext {

	/**
	 * The factory used to create new items.
	 */
	private JavaItemFactory factory;

	/**
	 * The JavaItem utility methods, configured for the current index.
	 */
	private JavaItemUtil2 util;

	/**
	 * Constructor for this.
	 * 
	 * @param taskList
	 *            The task list that may be accessed to add new tasks. This
	 *            value cannot be null.
	 * @param factory
	 *            The factory used to create new items. This value cannot be
	 *            null.
	 * @param util
	 *            The JavaItem utility methods, configured for the current
	 *            index. This value cannot be null.
	 */
	public LoadingContext(TaskList taskList, JavaItemFactory factory, JavaItemUtil2 util) {
		super(taskList);

		Check.notNull(factory, "factory");
		Check.notNull(util, "util");

		this.factory = factory;
		this.util = util;
	}

	/**
	 * Returns the factory used to create new items.
	 * 
	 * @return The factory used to create new items. This value will not be
	 *         null.
	 */
	public JavaItemFactory getFactory() {
		return factory;
	}

	/**
	 * Returns the index that stores all items and can be used to find existing
	 * items.
	 * 
	 * @return The index for all items. This value will not be null.
	 */
	public JavaItemIndex getIndex() {
		return factory.getIndex();
	}

	/**
	 * Returns the JavaItem utility methods, configured for the current index.
	 * 
	 * @return The JavaItem utility methods. This value will not be null.
	 */
	public JavaItemUtil2 getUtil() {
		return util;
	}

	/**
	 * Copies this context but without any of the variables or constraint
	 * information, and returns it. This can be used by a task to create an
	 * empty context when spawning other tasks into task groups.
	 * 
	 * @return A new context like this one, but with none of the variables or
	 *         constraint information.
	 */
	public LoadingContext forNewTaskGroup() {
		LoadingContext context = new LoadingContext(getTaskList(), factory, util);
		return context;
	}
}
