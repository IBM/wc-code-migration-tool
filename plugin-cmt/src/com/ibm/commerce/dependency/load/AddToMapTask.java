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
 * This class adds a key/value pair in the context to a map in the context.
 * 
 * @author Trent Hoeppner
 */
public class AddToMapTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(
			Arrays.asList(Name.MAP_KEY_NAME, Name.MAP_VALUE_NAME, Name.MAP));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.ADDED_TO_MAP));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public AddToMapTask(String name, LoadingContext context) {
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void execute(LoadingContext context) throws Exception {
		String mapKeyName = context.get(Name.MAP_KEY_NAME);
		Object mapKey = context.get(mapKeyName);
		String mapValueName = context.get(Name.MAP_VALUE_NAME);
		Object mapValue = context.get(mapValueName);
		Map map = context.get(Name.MAP);

		map.put(mapKey, mapValue);

		context.put(Name.ADDED_TO_MAP, true);
	}

}