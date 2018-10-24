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
 * This class tests the {@link SpellingValidator} class.
 * 
 * @author Trent Hoeppner
 */
public class SpellingValidatorTest extends TestCase {

	/**
	 * The resource that represents SpellingTestClass.java. This value will not
	 * be null during test execution.
	 */
	private ValidatorResource resource;

	/**
	 * The validator that will be run. This should be initialized in an
	 * overridden version of the {@link #setUp()} method. This value will not be
	 * null during test execution.
	 */
	private Validator validator;

	/**
	 * Constructor for this.
	 */
	public SpellingValidatorTest() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUp() throws Exception {
		super.setUp();
		resource = new ExternalValidatorResource(new File("testData\\SpellingTestClass.java"));
		validator = new SpellingValidator();
	}

	/**
	 * {@inheritDoc}
	 */
	public void tearDown() {
		validator = null;
		resource = null;
	}

	/**
	 * Tests all the errors reported by the SpellingValidator in the
	 * SpellingTestClass.java file.
	 *
	 * @throws Exception
	 *             If any unexpected error occurs.
	 */
	public void testSpellingErrors() throws Exception {
		ArrayList<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(resource);
		List<ValidationResult> errors = validator.analyze(resources, new FakeProblemActionFactory(),
				new NullProgressMonitor());

		Collections.sort(errors, new ValidationResultComparator());
		ValidationResult error;
		Iterator<ValidationResult> iterator = errors.iterator();

		// first spelling error: 'fr'
		error = iterator.next();
		assertEquals(19, error.getLine());
		assertEquals(29, error.getColumn());
		assertEquals(2, error.getLength());

		// second spelling error: 'cs'
		error = iterator.next();
		assertEquals(19, error.getLine());
		assertEquals(68, error.getColumn());
		assertEquals(2, error.getLength());

		// third spelling error: 'os'
		error = iterator.next();
		assertEquals(21, error.getLine());
		assertEquals(4, error.getColumn());
		assertEquals(2, error.getLength());

		// fourth spelling error: 'heer'
		error = iterator.next();
		assertEquals(26, error.getLine());
		assertEquals(8, error.getColumn());
		assertEquals(4, error.getLength());

		// fifth spelling error: 'heer'
		error = iterator.next();
		assertEquals(26, error.getLine());
		assertEquals(22, error.getColumn());
		assertEquals(4, error.getLength());

		// www,ibm,com are not errors because they are in the user dictionary

		// sixth spelling error: 'usin'
		error = iterator.next();
		assertEquals(31, error.getLine());
		assertEquals(25, error.getColumn());
		assertEquals(4, error.getLength());

		// seventh spelling error: 'obect'
		error = iterator.next();
		assertEquals(39, error.getLine());
		assertEquals(18, error.getColumn());
		assertEquals(5, error.getLength());

		// eighth spelling error: 'Dfd'
		error = iterator.next();
		assertEquals(46, error.getLine());
		assertEquals(32, error.getColumn());
		assertEquals(3, error.getLength());

		// ninth spelling error: 'fil'
		error = iterator.next();
		assertEquals(56, error.getLine());
		assertEquals(38, error.getColumn());
		assertEquals(3, error.getLength());

		// tenth spelling error: 'thre'
		error = iterator.next();
		assertEquals(58, error.getLine());
		assertEquals(61, error.getColumn());
		assertEquals(4, error.getLength());

		// 11th spelling error: 'exemptexemptnot'
		error = iterator.next();
		assertEquals(78, error.getLine());
		assertEquals(13, error.getColumn());
		assertEquals(28, error.getLength());

		// there are no more errors
		assertEquals(false, iterator.hasNext());
	}
}
