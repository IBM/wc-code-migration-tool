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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.commerce.qcheck.core.Param;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.Level;
import com.ibm.commerce.qcheck.tools.config.LevelEnum;

/**
 * CheckPIIValidatorFactory generates instances of {@link CheckPIIValidator}.
 * Files that have _en_US or _en in the filename, or which are in a parent
 * directory called _en_US or _en are analyzed. Only one instance is cached,
 * which is not configurable.
 * 
 * @author Trent Hoeppner
 */
public class CheckPIIValidatorFactory extends BaseValidatorFactory {

	private static final Pattern FILE_PATTERN = Pattern.compile(".*_en(_US)?\\..+");

	private static final Pattern PARENT_PATTERN = Pattern.compile("en(_US)?");

	/**
	 * A mapping from levels to Validators that can validate at those levels.
	 * Each Validator instance has its own configuration file, described in the
	 * data of the {@link Level} given in
	 * {@link #getValidatorInstance(ValidatorResource, Level)}. This value will
	 * not be null.
	 */
	private Map<LevelEnum, CheckPIIValidator> levelToValidatorMap = new HashMap<LevelEnum, CheckPIIValidator>();

	/**
	 * Constructor for this.
	 */
	public CheckPIIValidatorFactory() {
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
			validator = getValidatorForLevel(level);
		} else {
			Matcher parentMatcher = PARENT_PATTERN.matcher(file.getParentFile().getName());
			if (parentMatcher.matches()) {
				validator = getValidatorForLevel(level);
			} else {
				validator = null;
			}
		}

		return validator;
	}

	private Validator getValidatorForLevel(Level level) {
		CheckPIIValidator validator = levelToValidatorMap.get(level.getValue());
		if (validator == null) {
			validator = new CheckPIIValidator(level);
			levelToValidatorMap.put(level.getValue(), validator);
		}

		return validator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasCreated(Validator validator) {
		Param.notNull(validator, "validator");

		return levelToValidatorMap.containsValue(validator);
	}

	/**
	 * Returns true, indicating that CheckPIIValidators are configurable.
	 *
	 * @return True.
	 */
	@Override
	public boolean isConfigurable() {
		return true;
	}
}
