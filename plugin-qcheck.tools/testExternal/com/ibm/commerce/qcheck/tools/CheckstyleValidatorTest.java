package com.ibm.commerce.qcheck.tools;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.commerce.qcheck.core.ExternalValidatorResource;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.ValidationResultComparator;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;

import junit.framework.TestCase;

/**
 * CheckstyleValidatorTest tests that the CheckstyleValidator class work
 * correctly.
 * 
 * @author Trent Hoeppner
 */
public class CheckstyleValidatorTest extends TestCase {

	/**
	 * The resource that represents SpellingTestClass.java. This value is not
	 * null after {@link #setUp()} is called.
	 */
	private ValidatorResource resource;

	/**
	 * The validator that will be run. This value is not null after
	 * {@link #setUp()} is called.
	 */
	private Validator validator;

	/**
	 * Constructor for this.
	 */
	public CheckstyleValidatorTest() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUp() throws Exception {
		super.setUp();
		resource = new ExternalValidatorResource(new File("testData\\CheckstyleTestClass.java"));
		validator = new CheckstyleValidator();
	}

	/**
	 * {@inheritDoc}
	 */
	public void tearDown() {
		validator = null;
		resource = null;
	}

	/**
	 * Tests all the errors reported by the CheckstyleValidator in the
	 * CheckstyleTestClass.java file.
	 *
	 * @throws Exception
	 *             If any unexpected error occurs.
	 */
	public void testCheckstyleErrors() throws Exception {
		ArrayList<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(resource);
		List<ValidationResult> errors = validator.analyze(resources, new FakeProblemActionFactory(),
				new NullProgressMonitor());

		Collections.sort(errors, new ValidationResultComparator());
		ValidationResult error;
		Iterator<ValidationResult> iterator = errors.iterator();

		error = iterator.next();
		assertEquals(0, error.getLine());
		assertEquals(0, error.getColumn());
		assertEquals("Missing package-info.java file.", error.getMessage());

		error = iterator.next();
		assertEquals(17, error.getLine());
		assertEquals(0, error.getColumn());
		assertEquals("Missing a Javadoc comment.", error.getMessage());

		error = iterator.next();
		assertEquals(17, error.getLine());
		assertEquals(0, error.getColumn());
		assertEquals("Missing package declaration.", error.getMessage());

		error = iterator.next();
		assertEquals(17, error.getLine());
		assertEquals(1, error.getColumn());
		assertEquals("Class should define a constructor.", error.getMessage());

		error = iterator.next();
		assertEquals(17, error.getLine());
		assertEquals(1, error.getColumn());
		assertEquals("Utility classes should not have a public or default constructor.", error.getMessage());

		error = iterator.next();
		assertEquals(18, error.getLine());
		assertEquals(0, error.getColumn());
		assertEquals("Line has trailing spaces.", error.getMessage());

		error = iterator.next();
		assertEquals(18, error.getLine());
		assertEquals(1, error.getColumn());
		assertEquals("File contains tab characters (this is the first instance).", error.getMessage());

		error = iterator.next();
		assertEquals(19, error.getLine());
		assertEquals(0, error.getColumn());
		assertEquals("method def modifier at indentation level 8 not at correct indentation, 4", error.getMessage());

		error = iterator.next();
		assertEquals(19, error.getLine());
		assertEquals(9, error.getColumn());
		assertEquals("Missing a Javadoc comment.", error.getMessage());

		error = iterator.next();
		assertEquals(19, error.getLine());
		assertEquals(33, error.getColumn());
		assertEquals("'(' is preceded with whitespace.", error.getMessage());

		error = iterator.next();
		assertEquals(19, error.getLine());
		assertEquals(48, error.getColumn());
		assertEquals("'{' is not preceded with whitespace.", error.getMessage());

		error = iterator.next();
		assertEquals(20, error.getLine());
		assertEquals(0, error.getColumn());
		assertEquals("Line has trailing spaces.", error.getMessage());

		error = iterator.next();
		assertEquals(21, error.getLine());
		assertEquals(0, error.getColumn());
		assertEquals("method def rcurly at indentation level 8 not at correct indentation, 4", error.getMessage());

		// there are no more errors
		assertEquals(false, iterator.hasNext());
	}
}
