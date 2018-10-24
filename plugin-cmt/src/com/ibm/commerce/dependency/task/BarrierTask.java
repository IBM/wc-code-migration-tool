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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.commerce.cmt.Check;

/**
 * This class is a task that can be used to act as a synchronization point for a
 * number of other tasks or task groups. It works by making this depend on other
 * tasks through {@link Task#addOtherContextDependency(TaskContext, String)}.
 * Then make other tasks depend on this through the same method (where the
 * constraint is the one given to the constructor of this class). This task will
 * only be scheduled when all the tasks it depends on complete. Then the tasks
 * that this depends on will be available for scheduling when this task
 * completes. Thus, this task acts as a "barrier" between tasks or task groups.
 * <p>
 * When executed the implementation simply adds the constraint from the
 * constructor to the context with a value of true. This same functionality can
 * be done with other custom tasks, so this is really just a convenience.
 * 
 * @param <C>
 *            The type of the {@link TaskContext} for this task.
 * 
 * @author Trent Hoeppner
 */
public class BarrierTask<C extends TaskContext> extends Task<C> {

	/**
	 * The constraints that will be added to the context output. This actually
	 * only contains a single constraint from the constructor.
	 */
	private Set<String> finishConstraints;

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context that this uses to read and write data during
	 *            execution. This value cannot be null.
	 * @param finishedConstraint
	 *            The constraint that is used to signal that this task is done,
	 *            so that other tasks may add a dependency to this task. This
	 *            value cannot be null or empty.
	 */
	public BarrierTask(String name, C context, String finishedConstraint) {
		super(name, context);

		Check.notNullOrEmpty(finishedConstraint, "constraint");

		finishConstraints = new HashSet<>();
		finishConstraints.add(finishedConstraint);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getInputConstraints() {
		return Collections.emptySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getOutputConstraints() {
		return finishConstraints;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(C context) throws Exception {
		// this task only signifies that it is finished because all its
		// prerequisites finished
		// this is used to indicate that other tasks that depend on this can be
		// executed
		for (String constraint : finishConstraints) {
			context.put(constraint, true);
		}
	}

}
