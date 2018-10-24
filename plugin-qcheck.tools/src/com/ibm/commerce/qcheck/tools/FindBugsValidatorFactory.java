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

import java.io.File;
import java.util.Locale;

import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.Level;

/**
 * FindBugsValidatorFactory can create instances of {@link FindBugsValidator}.
 * Only one instance is cached, which is not configurable.
 * 
 * @author Trent Hoeppner
 */
public class FindBugsValidatorFactory extends BaseValidatorFactory {

	/**
	 * The singleton instance of the validator. This value will not be null.
	 */
	private FindBugsValidator validator = new FindBugsValidator();

	/**
	 * Constructor for this.
	 */
	public FindBugsValidatorFactory() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Validator getValidatorInstance(ValidatorResource resource, Level level) {
		Validator validatorToUse;
		File file = resource.getFileAsFile();
		if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".java")) {
			validatorToUse = this.validator;
		} else {
			validatorToUse = null;
		}

		return validatorToUse;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasCreated(Validator validator) {
		return this.validator.equals(validator);
	}
}
