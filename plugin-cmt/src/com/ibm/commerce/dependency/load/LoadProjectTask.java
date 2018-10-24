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
import java.util.Set;

import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.task.Task;

/**
 * This class creates a JavaItem for an Eclipse project and sets its attributes.
 * 
 * @author Trent Hoeppner
 */
public class LoadProjectTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(
			Arrays.asList(Name.PROJECT_NAME, Name.IS_WORKSPACE, Name.IS_BINARY, Name.IS_THIRD_PARTY));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.PROJECT_LOADED));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadProjectTask(String name, LoadingContext context) {
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
	@Override
	public void execute(LoadingContext context) throws Exception {
		String name = context.get(Name.PROJECT_NAME);
		boolean isWorkspace = context.get(Name.IS_WORKSPACE);
		boolean isBinary = context.get(Name.IS_BINARY);
		boolean isThirdParty = context.get(Name.IS_THIRD_PARTY);

		JavaItem loadedProject = context.getFactory().createProject(name);

		loadedProject.setAttribute(JavaItem.ATTR_PROJECT_PRIVATE_VISIBLE, isWorkspace);
		loadedProject.setAttribute(JavaItem.ATTR_BINARY, isBinary);
		loadedProject.setAttribute(JavaItem.ATTR_THIRD_PARTY, isThirdParty);

		context.put(Name.PROJECT_LOADED, true);

	}

}