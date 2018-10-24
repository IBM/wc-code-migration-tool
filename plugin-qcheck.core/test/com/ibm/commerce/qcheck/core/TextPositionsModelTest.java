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

import junit.framework.TestCase;

/**
 * TextPositionsModelTest tests the {@link TextPositionsModelFactory} class.
 * 
 * @author Trent Hoeppner
 */
public class TextPositionsModelTest extends TestCase {

	/**
	 * Constructor for this.
	 */
	public TextPositionsModelTest() {
		// do nothing
	}

	/**
	 * Tests that if null is given, an exception will be thrown.
	 */
	public void testCreateIfNullExpectException() {
		try {
			new TextPositionsModel(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if a normal string is given, it can be retrieved.
	 */
	public void testCreateIfNormalExpectCanBeRetrieved() {
		TextPositions positions = new TextPositions("Hello");
		TextPositionsModel model = new TextPositionsModel(positions);

		assertEquals(positions, model.getModel());
	}
}
