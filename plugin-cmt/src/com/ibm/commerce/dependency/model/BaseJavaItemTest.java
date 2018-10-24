package com.ibm.commerce.dependency.model;

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
 * JavaItemTest tests the {@link JavaItem} class.
 * 
 * @author Trent Hoeppner
 */
public class BaseJavaItemTest extends TestCase {

	private JavaItemIndex index;

	/**
	 * Constructor for this.
	 *
	 * @param testName
	 *            The name of the test used by JUnit. Cannot be null or empty.
	 */
	public BaseJavaItemTest(String testName) {
		super(testName);
	}

	@Override
	protected void setUp() throws Exception {
		index = new JavaItemIndex("v8");
	}

	public void testGetParent() {
		JavaItem item = new BaseJavaItem("hi", index);
		JavaItem parent = new BaseJavaItem("hi", index);
		parent.setID(15);

		assertEquals("Parent is wrong.", null, item.getParentID());

		item.setParentID(parent.getID());

		assertSame("Parent is wrong.", parent.getID(), item.getParentID());

		item.setParentID(null);

		assertEquals("Parent is wrong.", null, item.getParentID());
	}

	public void testConstructorIfNameNullExpectException() {
		try {
			new BaseJavaItem(null, index);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	public void testConstructorIfNameIsEmptyExpectReturnedByGetName() {
		JavaItem item = new BaseJavaItem("", index);
		assertEquals("Name is wrong.", "", item.getName());
	}

	public void testConstructorIfNameNormalExpectReturnedByGetName() {
		JavaItem item = new BaseJavaItem("hi", index);
		assertEquals("Name is wrong.", "hi", item.getName());
	}

	public void testSetGetType() {
		JavaItem item = new BaseJavaItem("item", index);
		assertEquals("getType is wrong.", JavaItemType.PROJECT, item.getType());

		item.setType(JavaItemType.PACKAGE);
		assertEquals("getType is wrong.", JavaItemType.PACKAGE, item.getType());

		try {
			item.setType(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}

	}

}