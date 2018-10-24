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

import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.Level;

/**
 * BaseValidatorFactory is an abstract implementation of ValidatorFactory which
 * implement default behavior for most methods.
 * 
 * @author Trent Hoeppner
 */
public abstract class BaseValidatorFactory implements ValidatorFactory {

	/**
	 * Returns whether this can validate the given resource or not.
	 * 
	 * @param resource
	 *            The resource that may need validation. Cannot be null.
	 * 
	 * @return True if a {@link com.ibm.commerce.qcheck.core.Validator} can be
	 *         created, false otherwise.
	 */
	@Override
	public boolean canValidate(ValidatorResource resource, Level level) {
		return getValidatorInstance(resource, level) != null;
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void cleanup() {
		// do nothing
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void init() {
		// do nothing
	}

	/**
	 * Returns false, indicating that this validator is not configurable.
	 * 
	 * @return False.
	 */
	public boolean isConfigurable() {
		return false;
	}
}
