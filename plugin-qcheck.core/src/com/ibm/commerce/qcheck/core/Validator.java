package com.ibm.commerce.qcheck.core;

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

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Validator represents an object that can examine code, Java doc, or other
 * files and report errors.
 * <p>
 * Validators must not assume that an instance is created each time
 * {@link #analyze(List, ProblemActionFactory, IProgressMonitor)} is called.
 * Instances may be reused and so validators must not create or depend on state
 * for a particular use.
 * 
 * @author Trent Hoeppner
 */
public interface Validator {

	/**
	 * Examines each of the given resources and returns a list of errors found
	 * in all resources.
	 *
	 * @param resources
	 *            The list of resources to check. Cannot be null.
	 * @param actionFactory
	 *            The factory to generate actions to resolve the problems found.
	 *            This value cannot be null.
	 * @param monitor
	 *            The monitor used to report the progress of the validator and
	 *            detect cancellation. May be null.
	 *
	 * @return The list of exceptions that occurred in those resources. Will not
	 *         be null.
	 *
	 * @throws ValidationException
	 *             If an error occurred during validation, such as parsing.
	 * @throws IOException
	 *             If an error occurred reading the input file.
	 * @throws OperationCanceledException
	 *             If the validation was canceled through the given monitor.
	 */
	List<ValidationResult> analyze(List<ValidatorResource> resources, ProblemActionFactory actionFactory,
			IProgressMonitor monitor) throws ValidationException, IOException, OperationCanceledException;

	List<ModelEnum> getRequiredModels();
}
