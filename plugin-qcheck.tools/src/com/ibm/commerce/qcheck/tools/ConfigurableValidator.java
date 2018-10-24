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
import com.ibm.commerce.qcheck.tools.config.Level;

/**
 * ConfigurableValidator is a Validator that can be configured to operate at
 * different {@link com.ibm.commerce.qcheck.tools.config.LevelEnum levels of
 * detail}. Which levels are supported is up to the Validator, but
 * {@link com.ibm.commerce.qcheck.tools.config.LevelEnum#NONE NONE} always means
 * that the validator will not be instantiated, so no validation needs to be
 * supported in this case.
 * 
 * @author Trent Hoeppner
 */
public interface ConfigurableValidator extends Validator {

	/**
	 * Returns the detail level of this Validator, which includes any data to
	 * configure which checks are performed.
	 * 
	 * @return The detail level of this Validator. Will not be null.
	 */
	public Level getLevel();
}
