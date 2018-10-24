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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.commerce.qcheck.core.Param;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.Level;

/**
 * SegmentationCheckValidatorFactory generates instances of
 * {@link SegmentationCheckValidator}. Only one instance is cached, which is not
 * configurable.
 * 
 * @author Trent Hoeppner
 */
public class SegmentationCheckValidatorFactory extends BaseValidatorFactory {

	private static final Pattern FILE_PATTERN = Pattern.compile(".*_en(_US)?\\..+");

	private static final Pattern PARENT_PATTERN = Pattern.compile("en(_US)?");

	private SegmentationCheckValidator validator = new SegmentationCheckValidator();

	/**
	 * Constructor for this.
	 */
	public SegmentationCheckValidatorFactory() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Validator getValidatorInstance(ValidatorResource resource, Level level) {
		Param.notNull(resource, "resource");
		Param.notNull(level, "level");

		Validator validator;
		File file = resource.getFileAsFile();
		Matcher fileMatcher = FILE_PATTERN.matcher(file.getName());
		if (fileMatcher.matches()) {
			validator = this.validator;
		} else {
			Matcher parentMatcher = PARENT_PATTERN.matcher(file.getParentFile().getName());
			if (parentMatcher.matches()) {
				validator = this.validator;
			} else {
				validator = null;
			}
		}

		return validator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasCreated(Validator validator) {
		Param.notNull(validator, "validator");

		return validator.equals(this.validator);
	}

}
