package com.ibm.commerce.dependency.task;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class treats a set of tasks as a single task from the task scheduler's
 * point of view. Tasks in the chain are run in the order that they are added to
 * the chain, and the tasks in the chain will be scheduled as a group, not
 * separately.
 * <p>
 * This is useful to ensure that two tasks always occur together to avoid
 * holding up resources. For example, if one task loads a file into memory and
 * another task processes it, it could use up a lot of memory if many tasks are
 * loading files and processing the contents of those files is delayed for some
 * reason.
 * 
 * @param <C>
 *            The type of the {@link TaskContext} for this task.
 * 
 * @author Trent Hoeppner
 */
public class ChainTask<C extends TaskContext> extends Task<C> {

	/**
	 * The chain of tasks to run.
	 */
	private List<Task<C>> chain = new ArrayList<>();

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context that this uses to read and write data during
	 *            execution. This value cannot be null.
	 */
	public ChainTask(String name, C context) {
		super(name, context);
	}

	/**
	 * Adds a task to the chain.
	 * 
	 * @param task
	 *            The task to add. This value cannot be null.
	 */
	public void addTask(Task<C> task) {
		chain.add(task);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getInputConstraints() {
		return chain.get(0).getInputConstraints();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getOutputConstraints() {
		return chain.get(chain.size() - 1).getOutputConstraints();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(C context) throws Exception {
		for (Task<C> task : chain) {
			task.run();
		}
	}

}
