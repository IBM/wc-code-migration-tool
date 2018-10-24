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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.commerce.cmt.Check;

/**
 * This class represents a place where data for {@link Task Tasks} can be read
 * from and written to. Data is exchanged between dependent tasks through a
 * context object.
 * <p>
 * A task may create and add other tasks to be executed by calling
 * {@link #addTask(Task)}.
 * <p>
 * When tasks share the same context, they become part of the same "group". Many
 * contexts can be created to group tasks that need to share data together, and
 * to separate tasks that do not need to share data by putting them in different
 * groups.
 * <p>
 * Tasks can also depend on contexts from other groups, to create dependencies
 * between groups. For example, this can be used to ensure that a task in one
 * group does not execute until multiple other groups are all finished
 * execution.
 * <p>
 * For details on task dependencies, see the {@link Task} class.
 * 
 * @author Trent Hoeppner
 */
public class TaskContext {

	/**
	 * The TaskList that tasks can be added to.
	 */
	private TaskList taskList;

	/**
	 * This is a global variable for all tasks, so they can check if it is safe
	 * to proceed.
	 */
	private ConcurrentSkipListSet<String> constraintsFinished;

	/**
	 * This is a global variable for all tasks, so they can check if a
	 * requirement cannot be fulfilled
	 */
	private ConcurrentSkipListSet<String> constraintsBroken;

	/**
	 * The variables that are available in this context.
	 */
	private Map<String, Object> variables = new HashMap<>();

	/**
	 * A read-write lock for the variables map.
	 */
	final private ReentrantReadWriteLock variableLock = new ReentrantReadWriteLock();

	/**
	 * Constructor for this.
	 * 
	 * @param taskList
	 *            The task list that this is used within, which can be used to
	 *            add additional tasks. This value cannot be null.
	 */
	public TaskContext(TaskList taskList) {
		Check.notNull(taskList, "taskList");

		this.taskList = taskList;

		this.constraintsFinished = new ConcurrentSkipListSet<>();
		this.constraintsBroken = new ConcurrentSkipListSet<>();
	}

	/**
	 * Returns the task list that this is used within.
	 * 
	 * @return The task list that this is used within. This value will not be
	 *         null.
	 */
	protected TaskList getTaskList() {
		return taskList;
	}

	/**
	 * Returns the constraint/variable names that have been finished in this.
	 * 
	 * @return The constraints that have been satisfied. This value will not be
	 *         null, but may be empty.
	 */
	public ConcurrentSkipListSet<String> getConstraintsFinished() {
		return constraintsFinished;
	}

	/**
	 * Returns the constraint/variable names that have been broken in this due
	 * to failed tasks. Any broken constraints will soon result in termination
	 * of the owning {@link TaskList}.
	 * 
	 * @return The constraints that have been broken. This value will not be
	 *         null, but may be empty.
	 */
	public ConcurrentSkipListSet<String> getConstraintsBroken() {
		return constraintsBroken;
	}

	/**
	 * Adds the given task to the task list for eventual execution. Tasks can
	 * use this to create more tasks. The new task does not necessarily have to
	 * use this as a context.
	 * 
	 * @param task
	 *            The task to add. This value cannot be null.
	 * @param priority
	 *            The priority for this task. The scheduler thread will schedule
	 *            all tasks with a higher priority value before any tasks of a
	 *            lower priority value. Must be >= 0.
	 */
	public void addTask(Task<?> task, int priority) {
		taskList.addTask(task, priority);
	}

	/**
	 * Returns the value of a variable with the given name.
	 * 
	 * @param name
	 *            The name of the variable. This value cannot be null or empty.
	 * 
	 * @return The value for the variable. This value may be null if null is
	 *         stored with the variable name or if the variable does not exist.
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String name) {
		Check.notNullOrEmpty(name, "name");
		try {
			variableLock.readLock().lock();
			return (T) variables.get(name);
		} finally {
			variableLock.readLock().unlock();
		}
	}

	/**
	 * Puts a variable name and value in this. The variable name is also
	 * considered a constraint and added to the list of
	 * {@link #getConstraintsFinished() satisfied constraints}.
	 * 
	 * @param name
	 *            The name of the variable. This value cannot be null or empty.
	 * @param value
	 *            The value of the variable. This value may be null.
	 */
	public void put(String name, Object value) {
		Check.notNullOrEmpty(name, "name");
		try {
			variableLock.writeLock().lock();
			variables.put(name, value);
		} finally {
			variableLock.writeLock().unlock();
		}
		constraintsFinished.add(name);
	}

	/**
	 * Returns whether this context contains all the given variables.
	 * 
	 * @param variableNames
	 *            The variable names to check. This value cannot be null, but
	 *            may be empty.
	 * 
	 * @return True if this context contains all the variables, false otherwise.
	 */
	public boolean containsAllVariables(Collection<String> variableNames) {
		try {
			variableLock.readLock().lock();
			return variables.keySet().containsAll(variableNames);
		} finally {
			variableLock.readLock().unlock();
		}
	}

	/**
	 * Returns whether this context contains the given variable.
	 * 
	 * @param variableName
	 *            The variable name to check. This value cannot be null or
	 *            empty.
	 * 
	 * @return True if this context contains the given variable, false
	 *         otherwise.
	 */
	public boolean containsVariable(String variableName) {
		try {
			variableLock.readLock().lock();
			return variables.containsKey(variableName);
		} finally {
			variableLock.readLock().unlock();
		}
	}

	/**
	 * Returns the variables in this as an immutable map.
	 * 
	 * @return The variables in this as an immutable map. This value will not be
	 *         null, but may be empty.
	 */
	public Map<String, Object> getVariables() {
		try {
			variableLock.readLock().lock();
			return Collections.unmodifiableMap(new HashMap<>(variables));
		} finally {
			variableLock.readLock().unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		try {
			variableLock.readLock().lock();
			StringBuilder b = new StringBuilder();
			b.append("[");
			for (String name : variables.keySet()) {
				Object value = variables.get(name);
				String stringValue = String.valueOf(value);
				if (stringValue.length() <= 150) {
					String withoutLineEndings = stringValue.replace('\n', ' ').replace('\r', ' ');
					b.append(name).append("=").append(withoutLineEndings).append(", ");
				}
			}
			b.append("]");

			return b.toString();
		} finally {
			variableLock.readLock().unlock();
		}
	}
}
