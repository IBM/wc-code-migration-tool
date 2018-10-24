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
 * TextPositionsTest tests the {@link TextPositionsTest} class.
 * 
 * @author Trent Hoeppner
 */
public class TextPositionsTest extends TestCase {

	/**
	 * Constructor for this.
	 */
	public TextPositionsTest() {
		// do nothing.
	}

	/**
	 * Tests that if null is given, an exception will be thrown.
	 */
	public void testConstructorIfNullExpectException() {
		try {
			new TextPositions(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if an empty string is given, the correct information can be
	 * retrieved.
	 */
	public void testConstructorIfEmptyExpect1Line() {
		TextPositions positions = new TextPositions("");

		assertEquals("Number of lines is wrong.", 1, positions.getLineCount());
		checkLine(positions, 0, 0, 0, 0, "", null);
	}

	/**
	 * Tests that if a string with 1 char is given, the correct information can
	 * be retrieved.
	 */
	public void testConstructorIfOneCharExpect1Line() {
		TextPositions positions = new TextPositions("c");

		assertEquals("Number of lines is wrong.", 1, positions.getLineCount());
		checkLine(positions, 0, 0, 1, 1, "c", null);
	}

	/**
	 * Tests that if a string with 2 lines with 1 char each is given, the
	 * correct information can be retrieved.
	 */
	public void testConstructorIf2LinesWith1CharExpect1Line() {
		TextPositions positions = new TextPositions("c\nd");

		assertEquals("Number of lines is wrong.", 2, positions.getLineCount());
		checkLine(positions, 0, 0, 1, 1, "c", "\n");
		checkLine(positions, 1, 2, 3, 1, "d", null);
	}

	private void checkLine(TextPositions positions, int lineIndex, int expectedStartPos, int expectedEndPos,
			int expectedLength, String expectedText, String expectedLineSeparator) {
		assertEquals("Start position of line " + lineIndex + " is wrong.", expectedStartPos,
				positions.getStartPosition(lineIndex));
		assertEquals("End position of line " + lineIndex + " is wrong.", expectedEndPos,
				positions.getEndPosition(lineIndex));
		assertEquals("Length of line " + lineIndex + " is wrong.", expectedLength, positions.getLength(lineIndex));
		assertEquals("Text for line " + lineIndex + " is wrong.", expectedText, positions.getText(lineIndex));
		assertEquals("Line separator for line " + lineIndex + " is wrong.", expectedLineSeparator,
				positions.getLineSeparator(lineIndex));
	}
}
