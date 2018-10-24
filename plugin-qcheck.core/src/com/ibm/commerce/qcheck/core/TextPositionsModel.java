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

/**
 * TextPositionsModel represents a {@link TextPositions} instance for a
 * resource.
 * 
 * @author Trent Hoeppner
 */
public class TextPositionsModel implements Model<TextPositions> {

	private TextPositions textPositions;

	/**
	 * Constructor for this.
	 *
	 * @param textPositions
	 *            The object which contains all the positions of lines. Cannot
	 *            be null.
	 */
	public TextPositionsModel(TextPositions textPositions) {
		Param.notNull(textPositions, "textPositions");

		this.textPositions = textPositions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TextPositions getModel() {
		return textPositions;
	}
}
