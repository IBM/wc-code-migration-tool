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

import java.util.Comparator;

/**
 * ValidationResultComparator is used to order validation results by line number
 * (lowest to highest), then by column number (lowest to highest), then by
 * message length (lowest to highest).
 * 
 * @author Trent Hoeppner
 */
public class ValidationResultComparator implements Comparator<ValidationResult> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compare(ValidationResult result1, ValidationResult result2) {
		int comparison = 0;

		comparison = result1.getLine() - result2.getLine();
		if (comparison != 0) {
			return comparison;
		}

		comparison = result1.getColumn() - result2.getColumn();
		if (comparison != 0) {
			return comparison;
		}

		comparison = result1.getLength() - result2.getLength();
		if (comparison != 0) {
			return comparison;
		}

		return comparison;
	}

}
