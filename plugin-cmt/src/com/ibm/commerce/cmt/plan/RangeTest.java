package com.ibm.commerce.cmt.plan;

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
 * This class tests the {@link Range} class.
 * 
 * @author Trent Hoeppner
 */
public class RangeTest extends TestCase {

	public void testConstructorIfNothingExpectValues0() {
		Range range = new Range();
		assertEquals("Start is wrong.", 0, range.getStart());
		assertEquals("End is wrong.", 0, range.getEnd());
		assertEquals("Length is wrong.", 0, range.getLength());
		assertEquals("toString() is wrong.", "0-0", range.toString());
	}

	public void testGetSetStartPositionIf1ExpectOk() {
		Range range = new Range();
		range.setStart(1);
		assertEquals("Line is wrong.", 1, range.getStart());
		assertEquals("Length is wrong.", -1, range.getLength());
		assertEquals("toString() is wrong.", "1-0", range.toString());
	}

	public void testGetSetEndPositionIf1ExpectOk() {
		Range range = new Range();
		range.setEnd(1);
		assertEquals("Line is wrong.", 1, range.getEnd());
		assertEquals("Length is wrong.", 1, range.getLength());
		assertEquals("toString() is wrong.", "0-1", range.toString());
	}
}
