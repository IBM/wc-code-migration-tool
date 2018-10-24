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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class manages {@link Task Tasks} and scheduling them for execution. Two
 * lists are maintained. The first list is the list of unscheduled tasks, and
 * the second list is the list of scheduled tasks. A scheduler thread is always
 * running that simply moves tasks from the unscheduled to the scheduled list.
 * <p>
 * When a task is first {@link #addTask(Task) added}, it will be put in the
 * unscheduled list. When the scheduler thread is looking for tasks to move, it
 * calls {@link Task#isContraintMissing()}. If it returns true, the task will be
 * passed over. If it returns false, the task will be moved to the scheduled
 * list.
 * <p>
 * The scheduler thread moves a maximum number of tasks each time. When it
 * reaches the maximum or goes through the whole unscheduled list, it will sleep
 * for a short time to avoid wasting processing time.
 * <p>
 * Tasks are taken from the scheduled list and executed by a number of worker
 * threads. When a task starts running, it will call {@link #startTask()} and
 * when it finishes it will call {@link #doneTask()}. This is used to track that
 * the task is running, even though it is in neither the unscheduled list nor
 * the schedule list.
 * <p>
 * When both the unscheduled and scheduled lists are empty, and no tasks are
 * running, the scheduler thread will automatically stop the task list. Note
 * that if the last remaining running task {@link TaskContext#addTask(Task) adds
 * a new task} through the context, it will be added to the unscheduled list and
 * scheduled normally, so that the scheduler thread will not stop. If any task
 * fails, the task list will be stopped.
 * <p>
 * A typical code sample is:
 * 
 * <pre>
 * TaskList taskList = new TaskList();
 * 
 * TaskContext context = new TaskContext();
 * MyTask task1 = new MyTask(context);
 * taskList.add(task1, 10);
 * // ...add more tasks...
 * 
 * taskList.start();
 * taskList.waitForCompletion();
 * </pre>
 * 
 * Calling {@link #waitForCompletion()} is not necessary, but allows the calling
 * thread to wait for all tasks to finish, or for one task to fail.
 * <p>
 * When a task is added to this, it is assigned a priority value. The
 * unscheduled list is divided by priorities, so that tasks which have a higher
 * priority value will be moved to the scheduled list before any tasks of lower
 * priority will be scheduled. If tasks at some priority level are currently
 * being scheduled and a higher priority task is added, that one will
 * immediately take precedence.
 * <p>
 * The implementation of priorities is strict - before switching to any lower
 * priority tasks, there must be no more higher priority tasks in the scheduled
 * or unscheduled list, and there must be no more running tasks. This allows
 * groups of tasks to be executed together, ensuring that no lower priority
 * groups will be executed until all in the current group are complete. This can
 * be used as an alternative to the {@link BarrierTask}.
 * 
 * @author Trent Hoeppner
 */
public class TaskList {

	/**
	 * The minimum number of worker threads.
	 */
	private static final int MIN_THREADS = 4;

	/**
	 * The maximum number of worker threads.
	 */
	private static final int MAX_THREADS = 8;

	/**
	 * The timeout time, in minutes, that {@link #waitForCompletion()} will wait
	 * before failing.
	 */
	private static final int MAX_MINUTES_TO_WAIT_TO_STOP = 120;

	/**
	 * The maximum number of tasks in the scheduled list.
	 */
	private static final int MAX_TASKS_IN_SCHEDULED_LIST = 1000;

	/**
	 * The maximum number of tasks to move from the unscheduled list to the
	 * scheduled list each time.
	 */
	private static final int NUM_TASKS_TO_ADD_TO_SCHEDULED_LIST = 1000;

	/**
	 * The number of milliseconds for the scheduler thread to sleep before again
	 * moving tasks to the scheduled list.
	 */
	private static final int MILLIS_BETWEEN_ADDING_TO_EXECUTE_QUEUE = 50;

	/**
	 * The percent to change the nextDelay value by when an adjustment is needed
	 * up or down.
	 */
	private static final int DELAY_CHANGE_PERCENT = 20;

	/**
	 * The minimum number of milliseconds that the nextDelay value can have.
	 */
	private static final int DELAY_MIN = 10;

	/**
	 * The maximum number of milliseconds that the nextDelay value can have.
	 */
	private static final int DELAY_MAX = 10000;

	/**
	 * The minimum number of tasks that must be moved in a loop to maintain the
	 * current nextDelay value. If the number of tasks moved is below this
	 * value, the nextDelay will be increased to slow down scheduling.
	 */
	private static final int ADDING_TASKS_SWEETSPOT_MIN = 50;

	/**
	 * The maximum number of tasks that must be moved in a loop to maintain the
	 * current nextDelay value. If the number of tasks moved is above this
	 * value, the nextDelay will be decreased to speed up scheduling.
	 */
	private static final int ADDING_TASKS_SWEETSPOT_MAX = 100;

	/**
	 * The executor that is used to manage the worker threads and the scheduled
	 * list of tasks.
	 */
	private ThreadPoolExecutor executor;

	/**
	 * The list of unscheduled tasks at the current priority.
	 */
	private BlockingQueue<Task<?>> unscheduledTasks = new LinkedBlockingQueue<>();

	/**
	 * The current priority level of the tasks to execute.
	 */
	private int currentPriority;

	/**
	 * The list of unscheduled lists for each priority level, where the position
	 * in the list corresponds to the priority level of that unscheduled list.
	 * This may contain null values where there is no unscheduled list at the
	 * given priority level.
	 */
	private List<BlockingQueue<Task<?>>> prioritizedUnscheduledTasks = new ArrayList<>();

	/**
	 * A lock to control access to the unscheduled lists.
	 */
	private ReentrantReadWriteLock unscheduledLock = new ReentrantReadWriteLock();

	/**
	 * The scheduler thread which moves tasks from the unscheduled list to the
	 * scheduled list.
	 */
	private Thread schedulerThread;

	/**
	 * A semaphore which tracks the number of tasks still running.
	 */
	private Semaphore numRunningTasks = new Semaphore(0);

	/**
	 * Constructor for this.
	 */
	public TaskList() {
		currentPriority = 0;
		prioritizedUnscheduledTasks.add(unscheduledTasks);
		executor = new ThreadPoolExecutor(MIN_THREADS, MAX_THREADS, 5, TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(MAX_TASKS_IN_SCHEDULED_LIST), Executors.defaultThreadFactory(),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}

	/**
	 * Starts the scheduler thread. If no tasks have been {@link #addTask(Task)
	 * added} to the unscheduled list at the time this method is called, the
	 * scheduler thread may detect that there is nothing to do and stop the task
	 * list immediately.
	 */
	public void start() {
		schedulerThread = new Thread(new Scheduler());
		schedulerThread.start();
	}

	/**
	 * Waits until all tasks are completed, and any tasks that those tasks
	 * create are also completed, or until a lengthy period of time passes.
	 */
	public void waitForCompletion() {
		try {
			boolean terminated = executor.awaitTermination(MAX_MINUTES_TO_WAIT_TO_STOP, TimeUnit.MINUTES);
			if (!terminated) {
				throw new IllegalStateException("Could not finish all tasks in the requested time.");
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("Was interrupted while waiting for tasks to complete.", e);
		}
	}

	/**
	 * Adds the given task to the list of unscheduled tasks.
	 * 
	 * @param task
	 *            The task to add. This value cannot be null.
	 * @param priority
	 *            The priority for this task. The scheduler thread will schedule
	 *            all tasks with a higher priority value before any tasks of a
	 *            lower priority value. Must be >= 0.
	 */
	public void addTask(Task<?> task, int priority) {
		try {
			unscheduledLock.writeLock().lock();
			if (priority == currentPriority) {
				unscheduledTasks.put(task);
			} else if (priority < currentPriority) {
				// no need to switch priority
				BlockingQueue<Task<?>> lowerPriorityUnscheduledTasks = prioritizedUnscheduledTasks.get(priority);
				if (lowerPriorityUnscheduledTasks == null) {
					lowerPriorityUnscheduledTasks = new LinkedBlockingQueue<>();
					prioritizedUnscheduledTasks.set(priority, lowerPriorityUnscheduledTasks);
				}
				lowerPriorityUnscheduledTasks.put(task);
			} else {
				// this task is higher priority than we are working on now, we
				// need to switch priority now
				while (prioritizedUnscheduledTasks.size() <= priority) {
					prioritizedUnscheduledTasks.add(null);
				}

				BlockingQueue<Task<?>> higherPriorityUnscheduledTasks = prioritizedUnscheduledTasks.get(priority);
				if (higherPriorityUnscheduledTasks == null) {
					higherPriorityUnscheduledTasks = new LinkedBlockingQueue<>();
					prioritizedUnscheduledTasks.set(priority, higherPriorityUnscheduledTasks);
				}

				higherPriorityUnscheduledTasks.put(task);

				// now switch
				currentPriority = priority;
				unscheduledTasks = higherPriorityUnscheduledTasks;
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("Was interrupted while waiting to add a new task.", e);
		} finally {
			unscheduledLock.writeLock().unlock();
		}
	}

	/**
	 * Notifies this that a task has started running. This is used to track the
	 * number of currently running tasks.
	 */
	public void startTask() {
		numRunningTasks.release();
	}

	/**
	 * Notifies this that a task has finished running. This is used to track the
	 * number of currently running tasks.
	 */
	public void doneTask() {
		try {
			numRunningTasks.acquire();
		} catch (InterruptedException e) {
			// should never reach here, because we always release before we
			// acquire
			e.printStackTrace();
		}
	}

	/**
	 * This class moves tasks from the unscheduled to the scheduled list, and
	 * detects termination conditions.
	 */
	public class Scheduler implements Runnable {

		/**
		 * The number of loop iterations that have been executed without moving
		 * any tasks from the unscheduled to the scheduled list. This is used to
		 * detect deadlock where tasks are waiting for a constraint that will
		 * never be fulfilled.
		 */
		private int loopsWithNoMoves = 0;

		/**
		 * The delay between loops, which is increased or decreased in order to
		 * minimize scheduling overhead.
		 */
		private int nextDelay = MILLIS_BETWEEN_ADDING_TO_EXECUTE_QUEUE;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			while (true) {
				long startTime = System.currentTimeMillis();
				// detect stopping conditions
				try {
					unscheduledLock.readLock().lock();
					if (numRunningTasks.availablePermits() == 0 && executor.getQueue().size() == 0) {
						if (unscheduledTasks.size() == 0) {
							// no tasks waiting and no tasks running
							// check for lower priority
							boolean switched = false;
							for (int i = currentPriority - 1; i >= 0; i--) {
								BlockingQueue<Task<?>> lowerPriorityUnscheduledTasks = prioritizedUnscheduledTasks
										.get(i);
								if (lowerPriorityUnscheduledTasks != null && lowerPriorityUnscheduledTasks.size() > 0) {
									currentPriority = i;
									unscheduledTasks = lowerPriorityUnscheduledTasks;

									// reset the delay because the new priority
									// list may go faster or slower than the
									// last one
									nextDelay = MILLIS_BETWEEN_ADDING_TO_EXECUTE_QUEUE;
									loopsWithNoMoves = 0;

									switched = true;
									break;
								}
							}

							long endTime = System.currentTimeMillis();
							System.out.println("Finished detecting priority done in " + (endTime - startTime)
									+ " ms, switched priority = " + switched + ", priority = " + currentPriority);
							startTime = endTime;

							if (!switched) {
								// there really are no more tasks
								// just exit normally
								executor.shutdown();
								break;
							}
						} else if (loopsWithNoMoves >= 10) {
							// still have tasks unscheduled, but since none are
							// running or scheduled, the unscheduled ones will
							// never have their constraints satisfied
							// so we are in deadlock
							executor.shutdown();

							// list the tasks that are in deadlock
							for (Task<?> task : unscheduledTasks) {
								System.out.println("task " + task + " deadlocked: needs " + task.getInputConstraints()
										+ " but only has " + task.getContext()
										+ ", and also waiting for these external constraints: "
										+ task.getExternalConstraintsAsString());
							}
							break;
						}
					}
				} finally {
					unscheduledLock.readLock().unlock();
				}

				// find tasks with no constraints
				int remainingTasks;
				List<Task<?>> tasks = new ArrayList<>(NUM_TASKS_TO_ADD_TO_SCHEDULED_LIST);
				try {
					unscheduledLock.writeLock().lock();
					int sizeToAdd = NUM_TASKS_TO_ADD_TO_SCHEDULED_LIST - executor.getQueue().size();
					int i = 0;
					Iterator<Task<?>> iterator = unscheduledTasks.iterator();
					while (iterator.hasNext()) {
						Task<?> task = iterator.next();
						if (i >= sizeToAdd) {
							// reached our limit
							break;
						}

						try {
							if (!task.isContraintMissing()) {
								iterator.remove();
								tasks.add(task);
							}
						} catch (ConstraintBrokenException e) {
							executor.shutdown();
							break;
						}

						i++;
					}
					remainingTasks = unscheduledTasks.size();
				} finally {
					unscheduledLock.writeLock().unlock();
				}

				for (Task<?> task : tasks) {
					executor.execute(task);
				}

				if (tasks.size() > 0) {
					loopsWithNoMoves = 0;

					if (tasks.size() < ADDING_TASKS_SWEETSPOT_MIN) {
						// we are adding too few tasks each time, increase
						// the delay
						nextDelay = Math.min((int) (nextDelay * (1 + DELAY_CHANGE_PERCENT / 100.0)), DELAY_MAX);
					} else if (tasks.size() > ADDING_TASKS_SWEETSPOT_MAX) {
						// we are adding too many tasks each time, reduce
						// the delay
						int changePercent = DELAY_CHANGE_PERCENT;
						if (nextDelay > 1000) {
							// reduce the delay a lot more when the delay is
							// very high
							changePercent *= 2;
						}

						nextDelay = Math.max((int) (nextDelay / (1 + changePercent / 100.0)), DELAY_MIN);
					}

					System.out.println("moved " + tasks.size() + " tasks to the scheduled list, " + remainingTasks
							+ " at this priority, nextDelay = " + nextDelay);

					if (tasks.size() == 1) {
						System.out.println("Executing " + tasks.get(0) + " variables = "
								+ tasks.get(0).getExternalConstraintsAsString());
					}
				} else {
					loopsWithNoMoves++;
				}

				try {
					Thread.sleep(nextDelay);
				} catch (InterruptedException e) {
					System.out.println("Interrupted while sleeping");
				}
			}

			System.out.println("Scheduler thread stopped.");
		}

	}

}
