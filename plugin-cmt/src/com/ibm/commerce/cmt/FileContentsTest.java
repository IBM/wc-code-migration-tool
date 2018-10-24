package com.ibm.commerce.cmt;

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

import com.ibm.commerce.cmt.plan.Position;
import com.ibm.commerce.cmt.plan.Range;

import junit.framework.TestCase;

/**
 * This class tests the {@link FileContents} class.
 * 
 * @author Trent Hoeppner
 */
public class FileContentsTest extends TestCase {

	public void testConstructorIfFileNullExpectException() {
		try {
			new FileContents(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	public void testConstructorIfFileNotExistsExpectException() {
		try {
			new FileContents(new File("notExists.txt"));
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	public void testConstructorIfFileNotAFileExpectException() {
		try {
			new FileContents(new File("testData\\notAFile"));
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	public void testConstructorIfFileOkExpectCanBeRetrieved() throws Exception {
		File file = new File("testData\\empty.txt");
		FileContents fc = new FileContents(file);
		assertEquals("File is wrong.", file, fc.getFile());
		assertEquals("Contents are wrong.", null, fc.getContents());
	}

	public void testLoadIfFileEmptyExpectContentsEmpty() throws Exception {
		FileContents fc = new FileContents(new File("testData\\empty.txt"));
		fc.load();
		assertEquals("Contents are wrong.", "", fc.getContents());
	}

	public void testLoadIfFileNotEmptyExpectContentsCorrect() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();
		assertEquals("Contents are wrong.",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSep() + "<patterns>" + lineSep() + "</patterns>",
				fc.getContents());
	}

	public void testFormatIfRangeNullExpectException() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		try {
			fc.format(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	public void testFormatIfRange0ExpectValidString() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Range range = new Range();
		String rangeString = fc.format(range);
		assertEquals("string is wrong.", "1:1-1:1", rangeString);
	}

	public void testToPositionIfContentIndex0ExpectPosition1And1() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Position position = fc.toPosition(0);
		assertEquals("Line is wrong.", 1, position.getLine());
		assertEquals("Column is wrong.", 1, position.getColumn());
	}

	public void testToPositionIfContentIndexBeforeCarriageReturnExpectPosition1AndThirdLastColumn() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Position position = fc.toPosition(37);
		assertEquals("char is wrong.", '>', fc.getContents().charAt(37));
		assertEquals("Line is wrong.", 1, position.getLine());
		assertEquals("Column is wrong.", 38, position.getColumn());
	}

	public void testToPositionIfContentIndexBeforeNewlineExpectPosition1AndSecondLastColumn() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Position position = fc.toPosition(38);
		assertEquals("char is wrong.", '\r', fc.getContents().charAt(38));
		assertEquals("Line is wrong.", 1, position.getLine());
		assertEquals("Column is wrong.", 39, position.getColumn());
	}

	public void testToPositionIfContentIndexAtNewlineExpectPosition1AndLastColumn() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Position position = fc.toPosition(39);
		assertEquals("char is wrong.", '\n', fc.getContents().charAt(39));
		assertEquals("Line is wrong.", 1, position.getLine());
		assertEquals("Column is wrong.", 40, position.getColumn());
	}

	public void testToPositionIfContentIndexAfterNewlineExpectPosition2And1() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Position position = fc.toPosition(40);
		assertEquals("char is wrong.", '<', fc.getContents().charAt(40));
		assertEquals("Line is wrong.", 2, position.getLine());
		assertEquals("Column is wrong.", 1, position.getColumn());
	}

	public void testToContentIndexIfPosition2AndThirdLastColumnExpectCorrectIndex() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Position position = new Position(2, 10);
		int contentIndex = fc.toContentIndex(position);
		assertEquals("contentIndex is wrong.", 49, contentIndex);
		assertEquals("char is wrong.", '>', fc.getContents().charAt(contentIndex));
	}

	public void testToContentIndexIfPosition2AndSecondLastColumnExpectCorrectIndex() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Position position = new Position(2, 11);
		int contentIndex = fc.toContentIndex(position);
		assertEquals("contentIndex is wrong.", 50, contentIndex);
		assertEquals("char is wrong.", '\r', fc.getContents().charAt(contentIndex));
	}

	public void testToContentIndexIfPosition2AndLastColumnExpectCorrectIndex() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Position position = new Position(2, 12);
		int contentIndex = fc.toContentIndex(position);
		assertEquals("contentIndex is wrong.", 51, contentIndex);
		assertEquals("char is wrong.", '\n', fc.getContents().charAt(contentIndex));
	}

	public void testToContentIndexIfPosition3And1ExpectCorrectIndex() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Position position = new Position(3, 1);
		int contentIndex = fc.toContentIndex(position);
		assertEquals("contentIndex is wrong.", 52, contentIndex);
		assertEquals("char is wrong.", '<', fc.getContents().charAt(contentIndex));
	}

	public void testGetSubstringIfRangeNullExpectException() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		try {
			fc.getSubstring(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}

	}

	public void testGetSubstringIfRangeLengthEmptyExpectEmptyString() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		String substring = fc.getSubstring(new Range());
		assertEquals("substring is wrong.", "", substring);
	}

	public void testGetSubstringIfRangeLargerExpectCorrectSubstring() throws Exception {
		FileContents fc = new FileContents(new File("testData\\input.txt"));
		fc.load();

		Range range = new Range();
		range.setStart(40);
		range.setEnd(50);
		String substring = fc.getSubstring(range);
		assertEquals("substring is wrong.", "<patterns>", substring);
	}

	private String lineSep() {
		return System.getProperty("line.separator");
	}
}
