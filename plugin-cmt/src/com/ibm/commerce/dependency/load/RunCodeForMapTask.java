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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.commerce.dependency.task.Task;

/**
 * This class runs a {@link MapRunnable} for each key/value pair in a map.
 * 
 * @author Trent Hoeppner
 */
public class RunCodeForMapTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(Arrays.asList(Name.MAP, Name.MAP_RUNNABLE));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.MAP_RUNNABLE_DONE));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public RunCodeForMapTask(String name, LoadingContext context) {
		super(name, context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getInputConstraints() {
		return INPUTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getOutputConstraints() {
		return OUTPUTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void execute(LoadingContext context) throws Exception {
		Map map = context.get(Name.MAP);
		MapRunnable runnable = context.get(Name.MAP_RUNNABLE);

		for (Object o : map.entrySet()) {
			Map.Entry e = (Map.Entry) o;
			if (runnable.run(e.getKey(), e.getValue())) {
				break;
			}
		}

		runnable.addProperties(context);

		context.put(Name.MAP_RUNNABLE_DONE, true);
	}

}