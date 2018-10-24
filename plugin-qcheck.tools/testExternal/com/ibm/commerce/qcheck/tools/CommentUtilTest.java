package com.ibm.commerce.qcheck.tools;

import com.ibm.commerce.qcheck.core.comment.ColLine;
import com.ibm.commerce.qcheck.core.comment.CommentUtil;

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
 * This class tests the {@link CommentUtil} class.
 * 
 * @author Trent Hoeppner
 */
public class CommentUtilTest extends TestCase {

	public void testGetLineColumn() {
		String string = "\r\n\r\nHello\r\n.";

		testStartPos(string, 0, 0, 0);
		testStartPos(string, 1, 0, 0);
		testStartPos(string, 2, 0, 1);
		testStartPos(string, 3, 0, 1);
		testStartPos(string, 4, 0, 2);
		testStartPos(string, 5, 1, 2);
		testStartPos(string, 6, 2, 2);
		testStartPos(string, 7, 3, 2);
		testStartPos(string, 8, 4, 2);
		testStartPos(string, 9, 5, 2);
		testStartPos(string, 10, 5, 2);
		testStartPos(string, 11, 0, 3);
	}

	private void testStartPos(String string, int commentStartPos, int expectedCol, int expectedLine) {
		ColLine colLine = CommentUtil.calcColumnAndLine(string, commentStartPos);
		assertEquals(expectedCol, colLine.getColumnNum());
		assertEquals(expectedLine, colLine.getLineNum());
	}

}
