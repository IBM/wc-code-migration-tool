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
import java.util.Locale;
import java.util.Map;

import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.Level;
import com.ibm.commerce.qcheck.tools.config.LevelEnum;

/**
 * CMTValidatorFactory can create instances of {@link CMTValidator}. Instances
 * are cached based on the detail level given in
 * {@link #getValidatorInstance(ValidatorResource, Level)}.
 * <p>
 * CMTValidator is {@link ConfigurableValidator configurable}. However, in the
 * configuration file for the validation tools, the <code>data</code> field of
 * the <code>level</code> element will be ignored so it may be empty. For
 * example:
 *
 * <pre>
 * &lt;setup xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot;
 *      xsi:schemaLocation=&quot;http://www.validator.commerce.ibm.com/setup validatorconfig.xsd&quot;
 *      xmlns=&quot;http://www.validator.commerce.ibm.com/setup&quot;&gt;
 *  ...
 *  &lt;validator name=&quot;CMT&quot; class=&quot;com.ibm.commerce.qcheck.tools.CMTValidatorFactory&quot;&gt;
 *      ...
 *      &lt;level value=&quot;strict&quot; data=&quot;&quot;/&gt;
 *      &lt;level value=&quot;normal&quot; data=&quot;&quot;/&gt;
 *      &lt;level value=&quot;loose&quot; data=&quot;&quot;/&gt;
 *      &lt;level value=&quot;none&quot; data=&quot;&quot;/&gt;
 *  &lt;/validator&gt;
 *  ...
 * </pre>
 * 
 * @author Trent Hoeppner
 */
public class CMTValidatorFactory extends BaseValidatorFactory {

	/**
	 * A mapping from levels to Validators that can validate at those levels.
	 * This value will not be null.
	 */
	private Map<LevelEnum, CMTValidator> levelToValidatorMap = new HashMap<LevelEnum, CMTValidator>();

	/**
	 * Constructor for this.
	 */
	public CMTValidatorFactory() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Validator getValidatorInstance(ValidatorResource resource, Level level) {
		CMTValidator validatorToUse;
		File file = resource.getFileAsFile();
		if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".java")) {
			validatorToUse = levelToValidatorMap.get(level.getValue());
			if (validatorToUse == null) {
				validatorToUse = new CMTValidator(level);
				levelToValidatorMap.put(level.getValue(), validatorToUse);
			}

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
		boolean fromThis = false;
		for (CMTValidator cmtValidator : levelToValidatorMap.values()) {
			if (validator.equals(cmtValidator)) {
				fromThis = true;
				break;
			}
		}

		return fromThis;
	}

	/**
	 * Returns true, indicating that CMTValidators are configurable.
	 *
	 * @return True.
	 */
	@Override
	public boolean isConfigurable() {
		return true;
	}

}
