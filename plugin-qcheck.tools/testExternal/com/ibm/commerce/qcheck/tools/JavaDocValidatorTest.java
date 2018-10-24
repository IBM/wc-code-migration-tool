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
import com.ibm.commerce.qcheck.tools.config.Level;
import com.ibm.commerce.qcheck.tools.config.LevelEnum;

/**
 * JavaDocValidatorTest tests that the JavaDocValidator class work correctly.
 * 
 * @author Trent Hoeppner
 */
public class JavaDocValidatorTest extends ValidatorTestBase {

	/**
	 * Constructor for this.
	 */
	public JavaDocValidatorTest() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUp() throws Exception {
		super.setUp();
		Level javaDocLevel = new Level();
		javaDocLevel.setValue(LevelEnum.STRICT);
		validator = new JavaDocValidator(javaDocLevel);
	}

	/**
	 * {@inheritDoc}
	 */
	public void tearDown() {
		validator = null;
		super.tearDown();
	}

	/**
	 * Tests all the errors reported by the JavaDocValidator in the
	 * TestClass.java file.
	 *
	 * @throws Exception
	 *             If any unexpected error occurs.
	 */
	public void testJavaDocErrors() throws Exception {
		ArrayList<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(resource);
		List<ValidationResult> errors = validator.analyze(resources, new FakeProblemActionFactory(),
				new NullProgressMonitor());

		Collections.sort(errors, new ValidationResultComparator());
		ValidationResult error;
		Iterator<ValidationResult> iterator = errors.iterator();

		// the class comment should have too much space
		error = iterator.next();
		assertEquals(23, error.getLine());
		assertEquals(18, error.getColumn());
		assertEquals(19, error.getLength());

		// the class comment should have an @throws instead of @exception
		error = iterator.next();
		assertEquals(29, error.getLine());
		assertEquals(3, error.getColumn());
		assertEquals(10, error.getLength());

		// the class comment @exception should start with "If"
		error = iterator.next();
		assertEquals(29, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(6, error.getLength());

		// the first method comment should have an @throws instead of @exception
		error = iterator.next();
		assertEquals(35, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(10, error.getLength());

		// the first method comment, first @exception tag should start with If
		error = iterator.next();
		assertEquals(37, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(6, error.getLength());

		// the first method comment, first @exception tag should not refer to
		// the exception name
		error = iterator.next();
		assertEquals(37, error.getLine());
		assertEquals(27, error.getColumn());
		assertEquals(28, error.getLength());

		// the first method comment, second @exception tag should not refer to
		// the exception name
		error = iterator.next();
		assertEquals(39, error.getLine());
		assertEquals(25, error.getColumn());
		assertEquals(14, error.getLength());

		// the first method comment, third @exception tag should not refer to
		// the exception name
		error = iterator.next();
		assertEquals(41, error.getLine());
		assertEquals(25, error.getColumn());
		assertEquals(28, error.getLength());

		// the first method comment, fourth @exception tag should not refer to
		// the exception name
		error = iterator.next();
		assertEquals(43, error.getLine());
		assertEquals(25, error.getColumn());
		assertEquals(14, error.getLength());

		// the first method comment, fifth @exception tag should not refer to
		// the exception name
		error = iterator.next();
		assertEquals(45, error.getLine());
		assertEquals(25, error.getColumn());
		assertEquals(28, error.getLength());

		// the first method comment, sixth @exception tag should not refer to
		// the exception name
		error = iterator.next();
		assertEquals(47, error.getLine());
		assertEquals(25, error.getColumn());
		assertEquals(33, error.getLength());

		// the checkHTMLUsageMethod comment, parameter should not reference its
		// type
		error = iterator.next();
		assertEquals(57, error.getLine());
		assertEquals(30, error.getColumn());
		assertEquals(6, error.getLength());

		// the checkHTMLUsageMethod comment, return should not reference its
		// type
		error = iterator.next();
		assertEquals(58, error.getLine());
		assertEquals(25, error.getColumn());
		assertEquals(6, error.getLength());

		// the checkPrimitiveReturnReferenceMethod comment, return should not
		// reference its type
		error = iterator.next();
		assertEquals(65, error.getLine());
		assertEquals(19, error.getColumn());
		assertEquals(3, error.getLength());

		// the checkObjectReturnReferenceMethod comment, return handle the null
		// case
		error = iterator.next();
		assertEquals(72, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(28, error.getLength());

		// the checkObjectReturnReferenceMethod comment, return should not
		// reference its type
		error = iterator.next();
		assertEquals(72, error.getLine());
		assertEquals(19, error.getColumn());
		assertEquals(7, error.getLength());

		// the checkObjectNullReturnMethod comment, return handle the null case
		error = iterator.next();
		assertEquals(86, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkArrayNullReturnMethod comment, return handle the null case
		error = iterator.next();
		assertEquals(93, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkArrayNullReturnMethod comment, return handle the empty case
		error = iterator.next();
		assertEquals(93, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkStringNullReturnMethod comment, return handle the null case
		error = iterator.next();
		assertEquals(100, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkStringNullReturnMethod comment, return handle the empty case
		error = iterator.next();
		assertEquals(100, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkStringArrayNullReturnMethod comment, return handle the null
		// case
		error = iterator.next();
		assertEquals(107, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkStringArrayNullReturnMethod comment, return handle the empty
		// array case
		error = iterator.next();
		assertEquals(107, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkStringArrayNullReturnMethod comment, return handle the empty
		// String case
		error = iterator.next();
		assertEquals(107, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkPrimitiveParamReferenceMethod comment, param references
		// primitive type
		error = iterator.next();
		assertEquals(114, error.getLine());
		assertEquals(24, error.getColumn());
		assertEquals(3, error.getLength());

		// the checkObjectParamReferenceMethod comment, param handle null object
		error = iterator.next();
		assertEquals(121, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(28, error.getLength());

		// the checkObjectParamReferenceMethod comment, param references object
		// type
		error = iterator.next();
		assertEquals(121, error.getLine());
		assertEquals(24, error.getColumn());
		assertEquals(7, error.getLength());

		// the checkObjectNullParamMethod comment, param handle null object
		error = iterator.next();
		assertEquals(135, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkArrayNullParamMethod comment, param handle null array
		error = iterator.next();
		assertEquals(142, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkArrayNullParamMethod comment, param handle empty array
		error = iterator.next();
		assertEquals(142, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkArrayNullParamMethod comment, param handle null String
		error = iterator.next();
		assertEquals(149, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkArrayNullParamMethod comment, param handle empty String
		error = iterator.next();
		assertEquals(149, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkStringArrayNullParamMethod comment, param handle null array
		error = iterator.next();
		assertEquals(156, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkStringArrayNullParamMethod comment, param handle empty array
		error = iterator.next();
		assertEquals(156, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkStringArrayNullParamMethod comment, param handle empty
		// String
		error = iterator.next();
		assertEquals(156, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(20, error.getLength());

		// the checkStringNullButNoEmptyParamMethod comment, param handle empty
		// String
		error = iterator.next();
		assertEquals(187, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(31, error.getLength());

		// the checkPrimitiveArrayParamEmpty comment, param handle null
		// primitive array
		error = iterator.next();
		assertEquals(215, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(21, error.getLength());

		// the checkPrimitiveArrayParamEmpty comment, param handle empty
		// primitive array
		error = iterator.next();
		assertEquals(215, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(21, error.getLength());

		// check attribute 1 - private field must point to protected set
		error = iterator.next();
		assertEquals(241, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(10, error.getLength());

		// check attribute 1 - protected set must describe null situation
		error = iterator.next();
		assertEquals(249, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(17, error.getLength());

		// check attribute 1 - protected set must describe empty situation
		error = iterator.next();
		assertEquals(249, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(17, error.getLength());

		// check attribute 1 - protected get must point to protected set
		error = iterator.next();
		assertEquals(258, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(10, error.getLength());

		// check attribute 3 - private field must point to public get
		error = iterator.next();
		assertEquals(291, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(10, error.getLength());

		// check attribute 3 - protected set must point to public get
		error = iterator.next();
		assertEquals(299, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(17, error.getLength());

		// check attribute 3 - public get must describe null situation
		error = iterator.next();
		assertEquals(308, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(10, error.getLength());

		// check attribute 3 - public get must describe empty situation
		error = iterator.next();
		assertEquals(308, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(10, error.getLength());

		// check attribute 5 - public field must describe null situation
		error = iterator.next();
		assertEquals(341, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(10, error.getLength());

		// check attribute 5 - public field must describe empty situation
		error = iterator.next();
		assertEquals(341, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(10, error.getLength());

		// check attribute 5 - protected set must point to public field
		error = iterator.next();
		assertEquals(349, error.getLine());
		assertEquals(20, error.getColumn());
		assertEquals(17, error.getLength());

		// check attribute 5 - protected get must point to public field
		error = iterator.next();
		assertEquals(358, error.getLine());
		assertEquals(15, error.getColumn());
		assertEquals(10, error.getLength());

		// check attribute 7 - protected set must not mention parameter class
		// name
		error = iterator.next();
		assertEquals(399, error.getLine());
		assertEquals(80, error.getColumn());
		assertEquals(6, error.getLength());

		// check attribute 7 - protected get must not mention return class name
		error = iterator.next();
		assertEquals(408, error.getLine());
		assertEquals(69, error.getColumn());
		assertEquals(6, error.getLength());

		// check that static non-final fields are counted as an attribute and
		// checked for null
		error = iterator.next();
		assertEquals(431, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(17, error.getLength());

		// check that static non-final fields are counted as an attribute and
		// checked for empty
		error = iterator.next();
		assertEquals(431, error.getLine());
		assertEquals(7, error.getColumn());
		assertEquals(17, error.getLength());

		// getTag3 - class name in the description of a @link is an error
		error = iterator.next();
		assertEquals(509, error.getLine());
		assertEquals(30, error.getColumn());
		assertEquals(3, error.getLength());

		// getTag4 - class name as the entire second name of a @link is an error
		error = iterator.next();
		assertEquals(520, error.getLine());
		assertEquals(22, error.getColumn());
		assertEquals(3, error.getLength());

		// there are no more errors
		assertEquals(false, iterator.hasNext());
	}

}
