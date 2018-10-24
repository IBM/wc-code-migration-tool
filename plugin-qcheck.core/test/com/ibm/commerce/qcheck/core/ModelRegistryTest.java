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

import java.io.File;

import org.jmock.Expectations;
import org.jmock.Mockery;

/**
 * ModelRegistryTest tests the {@link ModelRegistry} class.
 * 
 * @author Trent Hoeppner
 */
public class ModelRegistryTest extends FileCreatingTestCase {

	/**
	 * Constructor for this.
	 */
	public ModelRegistryTest() {
		// do nothing
	}

	/**
	 * Tests that if the name and resource are both null, an exception will be
	 * thrown.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testGetModelIfNoNameAndNoResourceExpectException() throws Exception {
		ModelRegistry registry = new ModelRegistry();
		try {
			registry.getModel(null, null);
			fail();
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the resource is null, an exception will be thrown.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testGetModelIfNoResourceExpectException() throws Exception {
		ModelRegistry registry = new ModelRegistry();
		try {
			registry.getModel("Hello", null);
			fail();
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the name is empty and the resource is null, an exception
	 * will be thrown.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testGetModelIfEmptyNameAndNoResourceExpectException() throws Exception {
		ModelRegistry registry = new ModelRegistry();
		try {
			registry.getModel("", null);
			fail();
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests that if the name and factory are both null, an exception will be
	 * thrown.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testRegisterIfBothNullExpectException() {
		ModelRegistry registry = new ModelRegistry();
		try {
			registry.register(null, null);
			fail();
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the factory is null, an exception will be thrown.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testRegisterIfHaveNameButNoFactoryExpectException() {
		ModelRegistry registry = new ModelRegistry();
		try {
			registry.register("Hello", null);
			fail();
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the name is empty and the factory is null, an exception
	 * will be thrown.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testRegisterIfEmptyNameAndNoFactoryExpectException() {
		ModelRegistry registry = new ModelRegistry();
		try {
			registry.register("", null);
			fail();
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests that if the name and factory are ok, the factory can be retrieved.
	 */
	public void testRegisterIfNormalExpectCanBeRetrieved() {
		ModelRegistry registry = new ModelRegistry();
		Mockery context = new Mockery();
		final ModelFactory factory = context.mock(ModelFactory.class);

		registry.register("Hello", factory);
		ModelFactory output = registry.getFactory("Hello");

		assertEquals(factory, output);
	}

	/**
	 * Tests that if a model is requested twice, the same model will be
	 * returned.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testGetModelIfCalledTwiceExpectSameOneReturned() throws Exception {
		File file = createTempFile(true, false, "junk.txt");

		ModelRegistry registry = new ModelRegistry();
		Mockery context = new Mockery();
		final ModelFactory factory = context.mock(ModelFactory.class);
		final Model model1 = context.mock(Model.class, "1");
		final Model model2 = context.mock(Model.class, "2");
		final ValidatorResource resource = new FakeResource(file);
		context.checking(new Expectations() {

			{
				atLeast(1).of(factory).createModel(resource);
				will(onConsecutiveCalls(returnValue(model1), returnValue(model2)));
			}
		});

		registry.register("Hello", factory);
		Model output1 = registry.getModel("Hello", resource);
		Model output2 = registry.getModel("Hello", resource);

		assertEquals(model1, output1);
		assertEquals(model1, output2);
		assertEquals(output1, output2);
	}

	/**
	 * Tests that if null is given, an exception will be returned.
	 */
	public void testClearValidatorIfNullExpectException() {
		ModelRegistry registry = new ModelRegistry();
		try {
			registry.clearValidator(null);
			fail();
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if a model is requested, then the resource is cleared, then
	 * the same model is requested, it will be a different model.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testClearValidatorIfNormalExpectSecondModelNotSameAsFirst() throws Exception {
		File file = createTempFile(true, false, "junk.txt");

		ModelRegistry registry = new ModelRegistry();
		Mockery context = new Mockery();
		final ModelFactory factory = context.mock(ModelFactory.class);
		final Model model1 = context.mock(Model.class, "1");
		final Model model2 = context.mock(Model.class, "2");
		final ValidatorResource resource = new FakeResource(file);
		context.checking(new Expectations() {

			{
				atLeast(1).of(factory).createModel(resource);
				will(onConsecutiveCalls(returnValue(model1), returnValue(model2)));
			}
		});

		registry.register("Hello", factory);
		Model output1 = registry.getModel("Hello", resource);

		assertEquals(model1, output1);

		registry.clearValidator(resource);
		Model output2 = registry.getModel("Hello", resource);
		assertEquals(model2, output2);
	}
}
