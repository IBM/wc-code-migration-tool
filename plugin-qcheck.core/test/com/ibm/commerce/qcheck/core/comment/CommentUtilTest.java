package com.ibm.commerce.qcheck.core.comment;

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

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.commerce.qcheck.core.Model;
import com.ibm.commerce.qcheck.core.ModelRegistry;
import com.ibm.commerce.qcheck.core.StringModel;
import com.ibm.commerce.qcheck.core.ValidatorResource;

import junit.framework.TestCase;

/**
 * This class tests the {@link CommentUtil} class.
 * 
 * @author Trent Hoeppner
 */
public class CommentUtilTest extends TestCase {

	private Mockery mockery;

	private ValidatorResource resource;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mockery = new Mockery();
		resource = mockery.mock(ValidatorResource.class);
		final StringModel model = new StringModel("Hello");
		final ModelRegistry registry = new ModelRegistry() {

			@Override
			public <T extends Model> T getModel(String modelName, ValidatorResource resource) {
				return (T) model;
			}

		};

		mockery.checking(new Expectations() {

			{
				allowing(resource).getModelRegistry();
				will(returnValue(registry));
			}
		});
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetOriginalTextNull() {
		try {
			CommentUtil.getOriginalText((ValidatorResource) null, 0, 1);
			fail();
		} catch (NullPointerException e) {
			// success
		}
	}

	public void testGetOriginalTextHighStartIndex() {
		try {
			CommentUtil.getOriginalText(resource, 10, 1);
			fail();
		} catch (IndexOutOfBoundsException e) {
			// success
		}
	}

	public void testGetOriginalTextNegativeStartIndex() {
		try {
			CommentUtil.getOriginalText(resource, -5, 1);
			fail();
		} catch (IndexOutOfBoundsException e) {
			// success
		}
	}

	public void testGetOriginalTextHighEndIndex() {
		try {
			CommentUtil.getOriginalText(resource, 2, 10);
			fail();
		} catch (IndexOutOfBoundsException e) {
			// success
		}
	}

	public void testGetOriginalTextLowEndIndex() {
		try {
			CommentUtil.getOriginalText(resource, 2, 1);
			fail();
		} catch (IndexOutOfBoundsException e) {
			// success
		}
	}

	public void testGetOriginalTextNegativeEndIndex() {
		try {
			CommentUtil.getOriginalText(resource, 2, -19);
			fail();
		} catch (IndexOutOfBoundsException e) {
			// success
		}
	}

	public void testGetOriginalTextNormal() {
		String subString = CommentUtil.getOriginalText(resource, 1, 3);
		assertEquals("el", subString);
	}

	public void testGetOriginalText0Start() {
		String subString = CommentUtil.getOriginalText(resource, 0, 3);
		assertEquals("Hel", subString);
	}

	public void testGetOriginalTextToTheEnd() {
		String subString = CommentUtil.getOriginalText(resource, 3, 5);
		assertEquals("lo", subString);
	}

	public void testGetOriginalTextFull() {
		String subString = CommentUtil.getOriginalText(resource, 0, 5);
		assertEquals("Hello", subString);
	}
}
