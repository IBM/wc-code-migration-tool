package com.ibm.commerce.cmt.search;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.MigrationTestCase;
import com.ibm.commerce.cmt.plan.IDGenerator;

/**
 * This class tests the {@link NameParam} class.
 * 
 * @author Trent Hoeppner
 */
public class NameParamTest extends MigrationTestCase {

	protected void setUp() throws Exception {
		super.setUp();
		parentDir = new File("testData\\classRefCmd");
	}

	/**
	 * Tests that if the data is null, an exception will be thrown.
	 */
	public void testConstructorIfDataNullExpectException() {
		try {
			new NameParam("name", null, Collections.emptyList());
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the data is empty, an exception will be thrown.
	 */
	public void testConstructorIfDataEmptyExpectException() {
		try {
			new NameParam("name", "", Collections.emptyList());
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests that if a full class name is given, it can be retrieved.
	 */
	public void testConstructorIfFullNameExpectCanBeRetrieved() {
		NameParam r = new NameParam("name", "javax.ejb.FinderException", Collections.emptyList());
		assertEquals("name is wrong.", "name", r.getName());
		assertEquals("data is wrong.", "javax.ejb.FinderException", r.getData());
		assertEquals("sub parameters list is wrong.", Collections.emptyList(), r.getSubParams());
	}

	public void testAcceptIfNoRegexAndMatchExpectTrue() {
		Context context = new Context(new IDGenerator(1));
		NameParam r = new NameParam("name", "javax.ejb.FinderException", Collections.emptyList());
		boolean result = r.accept(context, "javax.ejb.FinderException");
		assertEquals("Not matched.", true, result);
	}

	public void testAcceptIfNoRegexAndNotMatchExpectFalse() {
		Context context = new Context(new IDGenerator(1));
		NameParam r = new NameParam("name", "javax.ejb.FinderException", Collections.emptyList());
		boolean result = r.accept(context, "javax.ejb.FinderExceptio");
		assertEquals("Matched.", false, result);
	}

	public void testAcceptIfRegexAndMatchExpectTrue() {
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.ALL_MATCHERS, new ArrayList<>());
		NameParam r = new NameParam("name", null, Arrays.asList(new RegexSearchParam(".*FinderException")));
		boolean result = r.accept(context, "javax.ejb.FinderException");
		assertEquals("Not matched.", true, result);
	}

	public void testAcceptIfRegexAndNotMatchExpectFalse() {
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.ALL_MATCHERS, new ArrayList<>());
		NameParam r = new NameParam("name", null, Arrays.asList(new RegexSearchParam(".*FinderException")));
		boolean result = r.accept(context, "javax.ejb.FinderExceptio");
		assertEquals("Matched.", false, result);
	}

}
