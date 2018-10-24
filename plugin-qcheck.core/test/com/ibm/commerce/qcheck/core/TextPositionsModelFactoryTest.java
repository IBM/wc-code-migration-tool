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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.jmock.Mockery;

/**
 * TextPositionsModelFactoryTest tests the {@link TextPositionsModelFactory}
 * class.
 * 
 * @author Trent Hoeppner
 */
public class TextPositionsModelFactoryTest extends FileCreatingTestCase {

	/**
	 * Constructor for this.
	 */
	public TextPositionsModelFactoryTest() {
		// do nothing.
	}

	/**
	 * Tests that if null is given, an exception will be thrown.
	 */
	public void testCreateModelIfNullExpectException() {
		TextPositionsModelFactory factory = new TextPositionsModelFactory();
		try {
			factory.createModel(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if a valid resource is given, the model can be retrieved.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testCreateModelIfNormalExpectModelReturned() throws Exception {
		Mockery mockery = new Mockery();
		File file = createTempFile(true, false, "junk.txt");

		final ValidatorResource resource = new FakeResource(file);
		final String value = "Hello";
		final InputStream stream = new ByteArrayInputStream(value.getBytes());

		TextPositionsModelFactory factory = new TextPositionsModelFactory() {

			InputStream openStream(ValidatorResource resource) throws IOException {
				return stream;
			}
		};

		TextPositionsModel model = factory.createModel(resource);
		assertNotNull("TextPositions was null.", model.getModel());
	}

}
