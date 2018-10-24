package com.ibm.commerce.qcheck.tools;

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

import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.Level;

/**
 * ValidatorFactory is capable of creating a specific kind of Validator. This is
 * used in conjunction with the {@link ConfigurationManager} to create
 * validators as needed. The ConfigurationManager will dynamically create
 * factories as specified in the configuration file that the
 * ConfigurationManager reads.
 * <p>
 * The implementing class should cache Validator instances that have the same
 * level if possible, to avoid creating too many Validators. This will
 * significantly improve memory usage, as there are potentially tens of
 * thousands of files to validate for a clean build.
 * 
 * @author Trent Hoeppner
 */
public interface ValidatorFactory {

	/**
	 * Returns whether Validators produced by this factory can validate the
	 * given resource. At a minimum, the type of the file should be checked.
	 * This method should perform fast so that if validation is not possible,
	 * the validation framework can skip it.
	 * 
	 * @param resource
	 *            The resource that may need validation. Cannot be null.
	 * @param level
	 *            The level of the data at which the file would be validated.
	 *            This value cannot be null.
	 * 
	 * @return True if the file can be validated, false otherwise.
	 */
	public boolean canValidate(ValidatorResource resource, Level level);

	/**
	 * Returns a Validator instance that can be used to validate the given
	 * resource at the given level.
	 * 
	 * @param resource
	 *            The resource that requires validation. Cannot be null.
	 * @param level
	 *            The detail at which to examine given resource. Cannot be null.
	 * 
	 * @return The object that can be used to validate the given resource at the
	 *         given detail level. Will be null if the resource cannot be
	 *         validated by this factory.
	 */
	public Validator getValidatorInstance(ValidatorResource resource, Level level);

	/**
	 * Returns whether Validators produced by
	 * {@link #getValidatorInstance(ValidatorResource, Level)} implement the
	 * {@link ConfigurableValidator} interface.
	 * 
	 * @return True if Validator instances implement {@link Configurable}, false
	 *         otherwise.
	 */
	public boolean isConfigurable();

	/**
	 * Initializes this factory. This will be called by the
	 * {@link ConfigurationManager} before any other methods.
	 */
	public void init();

	/**
	 * Returns whether this factory was the one that created the given validator
	 * instance.
	 * 
	 * @param validator
	 *            The validator to check. Cannot be null.
	 * 
	 * @return True if this factory created the given validator, false
	 *         otherwise.
	 */
	public boolean hasCreated(Validator validator);

	/**
	 * Releases resources used by this factory. This will be called before
	 * {@link ConfigurationManager} releases this factory to the garbage
	 * collector when reloading a configuration file.
	 */
	public void cleanup();
}
