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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.commerce.cmt.Check;

/**
 * This class represents a unit of work to be run in a {@link TaskList}. Every
 * task has a context, which can be used to get inputs and add outputs for the
 * task while it is being executed.
 * <p>
 * To support parallelization, tasks have constraints associated with them.
 * Every task has input constraints and output constraints. The input
 * constraints define what variables must be available in the context before the
 * task can be executed. The output constraints define what variables will be
 * written to the context after execution completes.
 * <p>
 * These constraints, together with the {@link TaskContext} allow tasks to be
 * part of a "group". A task that writes one variable to the context, allows all
 * other tasks that need that variable to be scheduled and executed. However,
 * the tasks must share the same context to access that variable, so the context
 * is what defines which tasks are in the same group. To put several tasks in
 * the same group, simply give all the tasks the same context when they are
 * created. This also allows different groups to have the same variable name
 * without triggering tasks to be executed in another group. For example:
 * 
 * <pre>
 * TaskContext contextA = new TaskContext();
 * 
 * Task task1 = new Task(contextA);
 * 
 * Task task2 = new Task(contextA);
 * 
 * TaskContext contextB = new TaskContext();
 * 
 * Task task3 = new Task(contextB);
 * 
 * Task task4 = new Task(contextB);
 * </pre>
 * 
 * In the above example, task1 and task2 are in the same group, and task3 and
 * task4 are in the same group. However each group can be scheduled
 * independently of each other, so they can be run in parallel.
 * <p>
 * A task can also {@link #addOtherContextDependency(TaskContext, String) depend
 * on} another task group. This ensures that the task in one group will not
 * execute until the variable in the other group has been written. Continuing
 * the above example:
 * 
 * <pre>
 * task3.addOtherContextDependency(contextA, "task1.finished");
 * </pre>
 *
 * If we assume that "task1.finished" is an output variable of task1, then task3
 * will not be scheduled until task1 completes successfully.
 * <p>
 * Using these mechanisms, it is possible to define which parts of the
 * application run in parallel and which parts are run in serial, to maximize
 * CPU usage while maintaining thread safety.
 *
 * @param <C>
 *            The type of the {@link TaskContext} for this task.
 * 
 * @author Trent Hoeppner
 */
public abstract class Task<C extends TaskContext> implements Runnable {

	private String name;

	/**
	 * The context that this task uses to read and write data during execution.
	 */
	private C context;

	/**
	 * A mapping from constraint/variable names to list of contexts that this
	 * depends on for that name.
	 */
	private Map<String, List<C>> constraintNameToContextsMap;

	/**
	 * A lock to control read and write access to the external dependencies map.
	 */
	private ReentrantReadWriteLock mapLock = new ReentrantReadWriteLock();

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context that this uses to read and write data during
	 *            execution. This value cannot be null.
	 */
	public Task(String name, C context) {
		Check.notNullOrEmpty(name, "name");
		Check.notNull(context, "context");
		this.name = name;
		this.context = context;
	}

	/**
	 * Returns the context that this uses to read and write data during
	 * execution.
	 * 
	 * @return The context that this uses to read and write data during
	 *         execution. This value will not be null.
	 */
	public C getContext() {
		return context;
	}

	/**
	 * Makes this dependent on the given constraint from the other context. When
	 * the other context contains the variable, this task will become available
	 * for execution.
	 * 
	 * @param dependentContext
	 *            The context that will contain the constraint/variable name.
	 *            This value cannot be null.
	 * @param constraint
	 *            The constraint/variable name that will be added to the other
	 *            context.
	 */
	public void addOtherContextDependency(C dependentContext, String constraint) {
		try {
			mapLock.writeLock().lock();
			if (constraintNameToContextsMap == null) {
				constraintNameToContextsMap = new HashMap<>();
			}

			List<C> contextsForConstraint = constraintNameToContextsMap.get(constraint);
			if (contextsForConstraint == null) {
				contextsForConstraint = new ArrayList<>();
				constraintNameToContextsMap.put(constraint, contextsForConstraint);
			}

			contextsForConstraint.add(dependentContext);
		} finally {
			mapLock.writeLock().unlock();
		}
	}

	/**
	 * Returns the required constraints of this task. All these variables must
	 * exist in the context before this task can be executed.
	 * 
	 * @return The required constraints for this. This value will not be null,
	 *         but may be empty.
	 */
	abstract public Set<String> getInputConstraints();

	/**
	 * Returns the outputs that this task will write to the context during
	 * execution. All these variables are expected to be written. Failure to
	 * write an output variable during execution will cause the TaskList to
	 * fail, which is done to prevent deadlocks with other tasks that depend on
	 * this task.
	 * 
	 * @return The outputs that this task will write to the context. This value
	 *         will not be null, but may be empty.
	 */
	abstract public Set<String> getOutputConstraints();

	/**
	 * Informs the {@link TaskList} that this task is started, then calls
	 * {@link #execute(TaskContext)}. If execute() throws an exception or fails
	 * to write all of its output variables, the output constraints of this will
	 * be considered "broken" and will cause the {@link TaskList} to stop
	 * execution.
	 */
	final public void run() {
		long startTime = System.currentTimeMillis();
		try {
			context.getTaskList().startTask();

			boolean successful = false;
			try {
				execute(context);
				if (context.containsAllVariables(getOutputConstraints())) {
					successful = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// assume that none of the constraints were fulfilled
			if (!successful) {
				context.getConstraintsBroken().addAll(getOutputConstraints());
			}
		} finally {
			context.getTaskList().doneTask();
			long endTime = System.currentTimeMillis();
			long diff = endTime - startTime;
			if (diff > 1200) {
				System.out.println("Executed task " + this + " in " + diff + " ms, variables = " + getContext());
			}
		}
	}

	/**
	 * Executes this task. Data may be read from, and written to, the given
	 * context. When this method starts, the given context is guaranteed to have
	 * all input constraints/variables. After this method finishes, the context
	 * is expected to have all output constraints/variables.
	 * 
	 * @param context
	 *            The context to read to and write from. This value cannot be
	 *            null.
	 * 
	 * @throws Exception
	 *             If any error occurs during execution.
	 */
	abstract public void execute(C context) throws Exception;

	/**
	 * Returns whether any required constraints/variables are still missing.
	 * This includes the input constraints, and also includes any constraints
	 * that come from a {@link #addOtherContextDependency(TaskContext, String)
	 * dependent context}. While this method returns true, this task will not be
	 * scheduled for execution.
	 * 
	 * @return True if any constraints are missing, false otherwise.
	 * 
	 * @throws ConstraintBrokenException
	 *             If any required constraints will never be satisfied due to
	 *             the failure of another task.
	 */
	public boolean isContraintMissing() throws ConstraintBrokenException {
		Set<String> required = getInputConstraints();
		boolean requiredAreFinishedBasic = context.getConstraintsFinished().containsAll(required);
		boolean requiredAreFinished;
		if (requiredAreFinishedBasic) {
			try {
				mapLock.readLock().lock();
				if (constraintNameToContextsMap != null) {
					// that all the dependent contexts have the required
					// constraints
					requiredAreFinished = true;
					outer: for (String constraint : constraintNameToContextsMap.keySet()) {
						List<C> contextsForConstraint = constraintNameToContextsMap.get(constraint);
						for (C dependentContext : contextsForConstraint) {
							if (!dependentContext.containsVariable(constraint)) {
								requiredAreFinished = false;
								break outer;
							}
						}
					}
				} else {
					requiredAreFinished = true;
				}
			} finally {
				mapLock.readLock().unlock();
			}
		} else {
			requiredAreFinished = false;
		}

		if (!requiredAreFinished) {
			for (String r : required) {
				if (context.getConstraintsBroken().contains(r)) {
					throw new ConstraintBrokenException("Constraint " + r + " is broken, the task cannot complete.");
				}
			}
		}

		return !requiredAreFinished;
	}

	/**
	 * Returns the external constraints as a string for debugging purposes.
	 * 
	 * @return The external constraints. This value will not be null or empty.
	 */
	public String getExternalConstraintsAsString() {
		try {
			mapLock.readLock().lock();
			return String.valueOf(constraintNameToContextsMap);
		} finally {
			mapLock.readLock().unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return name;
	}
}
