package com.ibm.commerce.validation.tools;

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

import com.ibm.commerce.qcheck.core.FakeResource;
import com.ibm.commerce.qcheck.core.FileCreatingTestCase;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.tools.CheckPIIValidator;
import com.ibm.commerce.qcheck.tools.CheckPIIValidatorFactory;
import com.ibm.commerce.qcheck.tools.config.Level;

/**
 * CheckPIIValidatorFactoryTest tests the {@link CheckPIIValidatorFactory}
 * class.
 * 
 * @author Trent Hoeppner
 */
public class CheckPIIValidatorFactoryTest extends FileCreatingTestCase {

	private File testInstallDir;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		testInstallDir = createTempFile(true, true, "fakeEclipseDir");

		System.setProperty("osgi.install.area", testInstallDir.getAbsolutePath());
	}

	/**
	 * Tests that if the resource is null, an exception will be thrown.
	 */
	public void testGetValidatorInstanceIfResourceNullExpectException() {
		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		try {
			factory.getValidatorInstance(null, new Level());
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the level is null, an exception will be thrown.
	 */
	public void testGetValidatorInstanceIfLevelNullExpectException() {
		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		try {
			factory.getValidatorInstance(new FakeResource(), null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if a Java resource is given, null will be returned.
	 */
	public void testGetValidatorInstanceIfJavaResourceGivenExpectNull() {
		File file = new File(testInstallDir, "Hello.java");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertNull("Validator is not null.", validator);
	}

	/**
	 * Tests that if a properties file is given but with no locale information,
	 * null will be returned.
	 */
	public void testGetValidatorInstanceIfPropertiesResourceWithoutLocaleGivenExpectNull() {
		File file = new File(testInstallDir, "Hello.properties");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertNull("Validator is not null.", validator);
	}

	/**
	 * Tests that if a properties file is given with an _en in the name, a
	 * validator will be returned.
	 */
	public void testGetValidatorInstanceIfPropertiesResourceWithENInNameGivenExpectValidator() {
		File file = new File(testInstallDir, "Hello_en.properties");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertEquals("Class is wrong.", CheckPIIValidator.class, validator.getClass());
	}

	/**
	 * Tests that if a properties file is given with an _en_US in the name, a
	 * validator will be returned.
	 */
	public void testGetValidatorInstanceIfPropertiesResourceWithEN_USInNameGivenExpectValidator() {
		File file = new File(testInstallDir, "Hello_en_US.properties");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertEquals("Class is wrong.", CheckPIIValidator.class, validator.getClass());
	}

	/**
	 * Tests that if a properties file is given with an en in the parent
	 * directory, a validator will be returned.
	 */
	public void testGetValidatorInstanceIfPropertiesResourceWithENInParentDirGivenExpectValidator() {
		File nlDir = new File(testInstallDir, "en");
		File file = new File(nlDir, "Hello.properties");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertEquals("Class is wrong.", CheckPIIValidator.class, validator.getClass());
	}

	/**
	 * Tests that if a properties file is given with an en_US in the parent
	 * directory, a validator will be returned.
	 */
	public void testGetValidatorInstanceIfPropertiesResourceWithEN_USInParentDirGivenExpectValidator() {
		File nlDir = new File(testInstallDir, "en_US");
		File file = new File(nlDir, "Hello.properties");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertEquals("Class is wrong.", CheckPIIValidator.class, validator.getClass());
	}

	/**
	 * Tests that if a properties file is given but with a non-english locale,
	 * null will be returned.
	 */
	public void testGetValidatorInstanceIfPropertiesResourceWithNonENLocaleGivenExpectNull() {
		File file = new File(testInstallDir, "Hello_ja.properties");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertNull("Validator is not null.", validator);
	}

	/**
	 * Tests that if a properties file is given but with a non-english locale,
	 * null will be returned.
	 */
	public void testGetValidatorInstanceIfPropertiesResourceWithNonENInParentDirGivenExpectNull() {
		File nlDir = new File(testInstallDir, "ja");
		File file = new File(nlDir, "Hello.properties");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertNull("Validator is not null.", validator);
	}

	/**
	 * Tests that if a properties file is given with an _en in the name, a
	 * validator will be returned.
	 */
	public void testGetValidatorInstanceIfXMLResourceWithENInNameGivenExpectValidator() {
		File file = new File(testInstallDir, "Hello_en.xml");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertEquals("Class is wrong.", CheckPIIValidator.class, validator.getClass());
	}

	/**
	 * Tests that if a properties file is given with an _en_US in the name, a
	 * validator will be returned.
	 */
	public void testGetValidatorInstanceIfXMLResourceWithEN_USInNameGivenExpectValidator() {
		File file = new File(testInstallDir, "Hello_en_US.xml");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertEquals("Class is wrong.", CheckPIIValidator.class, validator.getClass());
	}

	/**
	 * Tests that if a properties file is given with an en in the parent
	 * directory, a validator will be returned.
	 */
	public void testGetValidatorInstanceIfXMLResourceWithENInParentDirGivenExpectValidator() {
		File nlDir = new File(testInstallDir, "en");
		File file = new File(nlDir, "Hello.xml");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertEquals("Class is wrong.", CheckPIIValidator.class, validator.getClass());
	}

	/**
	 * Tests that if a properties file is given with an en_US in the parent
	 * directory, a validator will be returned.
	 */
	public void testGetValidatorInstanceIfXMLResourceWithEN_USInParentDirGivenExpectValidator() {
		File nlDir = new File(testInstallDir, "en_US");
		File file = new File(nlDir, "Hello.xml");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		assertEquals("Class is wrong.", CheckPIIValidator.class, validator.getClass());
	}

	/**
	 * Tests that if a properties file is given with an en_US in the parent
	 * directory, a validator will be returned.
	 */
	public void testGetValidatorInstanceIfLevelGivenExpectLevelInValidator() {
		File nlDir = new File(testInstallDir, "en_US");
		File file = new File(nlDir, "Hello.xml");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Level level = new Level();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), level);

		assertEquals("Class is wrong.", CheckPIIValidator.class, validator.getClass());
		CheckPIIValidator chkpiiValidator = (CheckPIIValidator) validator;
		assertEquals("Level is wrong.", level, chkpiiValidator.getLevel());
	}

	/**
	 * Tests that if the validator is null, an exception will be thrown.
	 */
	public void testHasCreatedIfValidatorNullExpectException() {
		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		try {
			factory.hasCreated(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the validator was created by the factory previously, true
	 * will be returned.
	 */
	public void testHasCreatedIfValidatorCreatedByFactoryExpectTrue() {
		File file = new File(testInstallDir, "Hello_en.properties");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = factory.getValidatorInstance(new FakeResource(file), new Level());

		boolean created = factory.hasCreated(validator);

		assertEquals("Has created is wrong.", true, created);
	}

	/**
	 * Tests that if the validator was not created by the factory previously,
	 * false will be returned.
	 */
	public void testHasCreatedIfNotCreatedByFactoryExpectFalse() {
		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		Validator validator = new CheckPIIValidator(new Level());

		boolean created = factory.hasCreated(validator);

		assertEquals("Has created is wrong.", false, created);
	}

	/**
	 * Tests that if the validator was created by a different factory
	 * previously, false will be returned.
	 */
	public void testHasCreatedIfNotCreatedBySameFactoryExpectFalse() {
		File file = new File(testInstallDir, "Hello_en.properties");

		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();
		CheckPIIValidatorFactory wrongFactory = new CheckPIIValidatorFactory();
		Validator validator = wrongFactory.getValidatorInstance(new FakeResource(file), new Level());

		boolean created = factory.hasCreated(validator);

		assertEquals("Has created is wrong.", false, created);
	}

	/**
	 * Tests that if the validator was created by a different factory
	 * previously, false will be returned.
	 */
	public void testIsConfigurableIfNothingExpectTrue() {
		CheckPIIValidatorFactory factory = new CheckPIIValidatorFactory();

		assertEquals("isConfigurable is wrong.", true, factory.isConfigurable());
	}

}
