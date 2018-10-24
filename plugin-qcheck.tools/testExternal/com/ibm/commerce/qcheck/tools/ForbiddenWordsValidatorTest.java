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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.ValidationResultComparator;
import com.ibm.commerce.qcheck.core.ValidatorResource;

/**
 * ForbiddenWordsValidatorTest tests that the ForbiddenWordsValidator class work
 * correctly.
 * 
 * @author Trent Hoeppner
 */
public class ForbiddenWordsValidatorTest extends ValidatorTestBase {

	/**
	 * Constructor for this.
	 */
	public ForbiddenWordsValidatorTest() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUp() throws Exception {
		super.setUp();
		validator = new ForbiddenWordsValidator();
	}

	/**
	 * {@inheritDoc}
	 */
	public void tearDown() {
		validator = null;
		super.tearDown();
	}

	/**
	 * Tests all the errors reported by the ForbiddenWordsValidator in the
	 * TestClass.java file.
	 *
	 * @throws Exception
	 *             If any unexpected error occurs.
	 */
	public void testJGCErrors() throws Exception {
		ArrayList<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(resource);
		List<ValidationResult> errors = validator.analyze(resources, new FakeProblemActionFactory(),
				new NullProgressMonitor());

		Collections.sort(errors, new ValidationResultComparator());
		ValidationResult error;
		Iterator<ValidationResult> iterator = errors.iterator();

		// the class comment, fourth line: JavaDoc
		error = iterator.next();
		assertEquals(20, error.getLine());
		assertEquals(29, error.getColumn());
		assertEquals(7, error.getLength());

		// the checkForbiddenWordsMethod comment, first line: please
		error = iterator.next();
		assertEquals(195, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(6, error.getLength());

		// the checkForbiddenWordsMethod comment, second line: following
		error = iterator.next();
		assertEquals(196, error.getLine());
		assertEquals(11, error.getColumn());
		assertEquals(9, error.getLength());

		// the checkForbiddenWordsMethod comment, third line: invalid
		error = iterator.next();
		assertEquals(197, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(7, error.getLength());

		// the checkForbiddenWordsMethod comment, fourth line: due to
		error = iterator.next();
		assertEquals(198, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(6, error.getLength());

		// the checkForbiddenWordsMethod comment, fourth line: plugin
		error = iterator.next();
		assertEquals(199, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(6, error.getLength());

		// there are no more errors
		assertEquals(false, iterator.hasNext());
	}
}
