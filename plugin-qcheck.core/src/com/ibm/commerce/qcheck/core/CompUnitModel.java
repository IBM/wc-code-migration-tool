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

import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * CompUnitModel represents a Java resource as an Eclipse CompilationUnit.
 * 
 * @author Trent Hoeppner
 */
public class CompUnitModel implements Model<CompilationUnit> {

	private CompilationUnit data;

	/**
	 * Constructor for this.
	 *
	 * @param data
	 *            The compilation unit for this model. Cannot be null.
	 */
	public CompUnitModel(CompilationUnit data) {
		if (data == null) {
			throw new NullPointerException("data cannot be null.");
		}

		this.data = data;
	}

	/**
	 * Returns the compilation unit for this.
	 *
	 * @return The compilation unit for this model. Will not be null.
	 */
	@Override
	public CompilationUnit getModel() {
		return data;
	}

}
