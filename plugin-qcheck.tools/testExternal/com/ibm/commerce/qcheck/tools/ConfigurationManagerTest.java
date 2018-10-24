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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

import com.ibm.commerce.qcheck.core.ExternalValidatorResource;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.Config;
import com.ibm.commerce.qcheck.tools.config.Event;
import com.ibm.commerce.qcheck.tools.config.Level;
import com.ibm.commerce.qcheck.tools.config.LevelEnum;
import com.ibm.commerce.qcheck.tools.config.ScopeEnum;
import com.ibm.commerce.qcheck.tools.config.TimeEnum;
import com.ibm.commerce.qcheck.tools.config.ValidatorDef;
import com.ibm.commerce.qcheck.tools.config.ValidatorInst;

import junit.framework.TestCase;

/**
 * ConfigurationManagerTest tests loading of various configuration files, and
 * makes sure that the correct validators can be found and instantiated based on
 * those configuration files.
 * <p>
 * Scopes can override other scopes, but only to make the validators more
 * strict, not less strict. See
 * {@link ConfigurationManager.ConfigGroup#getFactoryToLevelMap} for details.
 * 
 * @author Trent Hoeppner
 */
public class ConfigurationManagerTest extends TestCase {

	/**
	 * The number of enumerated test class files, of the form TestClassXX.java,
	 * where XX is a two-digit number from 01 to 18.
	 */
	private static final int NUM_TEST_FILES = 18;

	/**
	 * Tests that validators can be found for a single validator configuration,
	 * and a single {@link LevelEnum level}.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testSingleResourceAndValidator() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\singleConfigAndValidatorConfig.xml"));

		// test that the ASYOUTYPE validator is NORMAL
		assertSimpleConfigLevelEquals(manager, LevelEnum.NORMAL);
	}

	private void assertSimpleConfigLevelEquals(ConfigurationManager manager, LevelEnum level) throws IOException {
		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass.java")));
		Map<Validator, List<ValidatorResource>> validatorToResources;
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE);
		Iterator<Validator> iterator = validatorToResources.keySet().iterator();
		Validator validator = iterator.next();
		assertEquals(true, validator instanceof JavaDocValidator);
		JavaDocValidator javaDocValidator = (JavaDocValidator) validator;
		assertEquals(level, javaDocValidator.getLevel().getValue());
		List<ValidatorResource> outputResources = validatorToResources.get(validator);
		assertEquals(resources, outputResources);
		assertEquals(false, iterator.hasNext());

		// test that other scopes have no validators
		validatorToResources = manager.getValidators(resources, TimeEnum.CODEREVIEW);
		assertEquals(true, validatorToResources.isEmpty());
		validatorToResources = manager.getValidators(resources, TimeEnum.INTEGRATIONSERVERBUILD);
		assertEquals(true, validatorToResources.isEmpty());
		validatorToResources = manager.getValidators(resources, TimeEnum.MANUAL);
		assertEquals(true, validatorToResources.isEmpty());
		validatorToResources = manager.getValidators(resources, TimeEnum.FULLTOOLKITBUILD);
		assertEquals(true, validatorToResources.isEmpty());
		validatorToResources = manager.getValidators(resources, TimeEnum.INCREMENTALTOOLKITBUILD);
		assertEquals(true, validatorToResources.isEmpty());
		validatorToResources = manager.getValidators(resources, TimeEnum.WIZARD);
		assertEquals(true, validatorToResources.isEmpty());

		// test a larger number of resources
		// the JavaDocValidatorFactory should return the same instance for a
		// specific time and level
		resources.clear();
		StringBuffer filenameBuffer = new StringBuffer();
		for (int i = 1; i <= NUM_TEST_FILES; i++) {
			filenameBuffer.setLength(0);
			filenameBuffer.append("testData\\TestClass");
			if (i < 10) {
				filenameBuffer.append("0");
			}
			filenameBuffer.append(i);
			filenameBuffer.append(".java");
			resources.add(new ExternalValidatorResource(new File(filenameBuffer.toString())));
		}

		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE);
		iterator = validatorToResources.keySet().iterator();
		validator = iterator.next();
		assertEquals(true, validator instanceof JavaDocValidator);
		javaDocValidator = (JavaDocValidator) validator;
		assertEquals(level, javaDocValidator.getLevel().getValue());
		outputResources = validatorToResources.get(validator);
		assertEquals(resources, outputResources);
		assertEquals(false, iterator.hasNext());
	}

	/**
	 * Tests that multiple validators can be returned when requesting validation
	 * for a single file.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testOneFileHasMultipleValidators() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass.java")));

		Map<Validator, List<ValidatorResource>> validatorToResources;

		// test that the ASYOUTYPE validators are all LOOSE
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.LOOSE, LevelEnum.LOOSE);
	}

	/**
	 * Tests that class scope overrides global scope.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testClassOverride() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass01.java")));

		Map<Validator, List<ValidatorResource>> validatorToResources;

		// test the ASYOUTYPE validators
		// JavaDoc is STRICT
		// RSAR is NORMAL
		// JGC exists (not configurable)
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.STRICT, LevelEnum.NORMAL);

		// test the WIZARD validators
		// JavaDoc is NORMAL
		// RSAR is NORMAL
		// JGC exists (not configurable)
		validatorToResources = manager.getValidators(resources, TimeEnum.WIZARD);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.NORMAL, LevelEnum.NORMAL);
	}

	/**
	 * Tests that file scope overrides global scope.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testFileOverride() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass02.java")));

		Map<Validator, List<ValidatorResource>> validatorToResources;

		// test the ASYOUTYPE validators
		// JavaDoc is STRICT
		// RSAR is STRICT
		// JGC exists (not configurable)
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.STRICT, LevelEnum.STRICT);

		// test the WIZARD validators
		// JavaDoc is STRICT
		// RSAR is STRICT
		// JGC exists (not configurable)
		validatorToResources = manager.getValidators(resources, TimeEnum.WIZARD);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.STRICT, LevelEnum.STRICT);
	}

	/**
	 * Tests that user scope and as-you-type level overrides the global scope.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testUserAsYouTypeOverride() throws Exception {
		// test jack
		ConfigurationManager manager = new ConfigurationManager("jack",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass03.java")));

		Map<Validator, List<ValidatorResource>> validatorToResources;

		// test the ASYOUTYPE validators
		// JavaDoc is STRICT
		// RSAR is NORMAL
		// JGC does not exist (not configurable, but no validator)
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.STRICT, LevelEnum.NORMAL);

		// test jill
		manager = new ConfigurationManager("jill", new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass03.java")));

		// test the ASYOUTYPE validators
		// JavaDoc does not exist
		// RSAR is NORMAL
		// JGC exists (not configurable)
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE);
		assertValidatorMapCorrect(resources, validatorToResources, null, LevelEnum.NORMAL);
	}

	/**
	 * Tests that file scope overrides class scope.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testFileOverridesClass() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass01.java")));

		Map<Validator, List<ValidatorResource>> validatorToResources;

		// test the MANUAL validators
		// JavaDoc is STRICT
		// RSAR is STRICT
		// JGC does not exist (not configurable, but no validator)
		validatorToResources = manager.getValidators(resources, TimeEnum.MANUAL);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.STRICT, LevelEnum.STRICT);
	}

	/**
	 * Tests that user scope overrides file scope.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testUserOverridesFile() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("jack",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass01.java")));

		Map<Validator, List<ValidatorResource>> validatorToResources;

		// test the INTEGRATIONSERVERBUILD validators
		// JavaDoc is STRICT
		// RSAR is STRICT
		// JGC does not exist (not configurable, but no validator)
		validatorToResources = manager.getValidators(resources, TimeEnum.INTEGRATIONSERVERBUILD);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.STRICT, LevelEnum.STRICT);
	}

	/**
	 * Tests that if the user configures as-you-type and toolkit build scopes to
	 * not run, this will override all other scopes.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testUserWithNoAsYouTypeAndToolkitBuildOverridesOthers() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("joe",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass01.java")));

		Map<Validator, List<ValidatorResource>> validatorToResources;

		// test the ASYOUTYPE validators
		// JavaDoc is not there
		// RSAR is not there
		// JGC does not exist (not configurable, but no validator)
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE);
		assertValidatorMapCorrect(resources, validatorToResources, null, null);

		// test the TOOLKITBUILD validators
		// JavaDoc is not there
		// RSAR is not there
		// JGC does not exist (not configurable, but no validator)
		validatorToResources = manager.getValidators(resources, TimeEnum.INCREMENTALTOOLKITBUILD);
		assertValidatorMapCorrect(resources, validatorToResources, null, null);
		validatorToResources = manager.getValidators(resources, TimeEnum.FULLTOOLKITBUILD);
		assertValidatorMapCorrect(resources, validatorToResources, null, null);
	}

	/**
	 * Tests that a second file can override settings in the first file.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testSimpleSecondFileCanOverride() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\singleConfigAndValidatorConfig.xml"),
				new File("testData\\singleConfigAndValidatorConfig2.xml"));

		// test that the ASYOUTYPE validator is STRICT
		assertSimpleConfigLevelEquals(manager, LevelEnum.STRICT);
	}

	/**
	 * Tests that if the user configures as-you-type and toolkit build scopes in
	 * a second file, they will override settings in the first file.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testSecondFileAsYouTypeToolkitBuildNoneOverrideFirstFile() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"),
				new File("testData\\multipleConfigsAndValidatorsConfig2.xml"));

		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass01.java")));

		Map<Validator, List<ValidatorResource>> validatorToResources;

		// test the ASYOUTYPE validators
		// JavaDoc is not there
		// RSAR is not there
		// JGC does not exist (not configurable, but no validator)
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE);
		assertValidatorMapCorrect(resources, validatorToResources, null, null);

		// test the TOOLKITBUILD validators
		// JavaDoc is not there
		// RSAR is not there
		// JGC does not exist (not configurable, but no validator)
		validatorToResources = manager.getValidators(resources, TimeEnum.INCREMENTALTOOLKITBUILD);
		assertValidatorMapCorrect(resources, validatorToResources, null, null);
		validatorToResources = manager.getValidators(resources, TimeEnum.INCREMENTALTOOLKITBUILD);
		assertValidatorMapCorrect(resources, validatorToResources, null, null);
	}

	/**
	 * Tests overriding in two dimensions - the first dimension is overriding
	 * within a file, and the second dimension is overriding between files.
	 * First, overriding occurs between files within a single scope. Then
	 * overriding occurs between scopes, and the overriding may be specified in
	 * both files.
	 *
	 * @exception Exception
	 *                If an unexpected error occurs.
	 */
	public void testTwoDimensionalOverride() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\twoDimensionalOverrideConfig.xml"),
				new File("testData\\twoDimensionalOverrideConfig2.xml"));

		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass04.java")));

		Map<Validator, List<ValidatorResource>> validatorToResources;

		// test the ASYOUTYPE validators
		// JavaDoc is STRICT
		// RSAR is STRICT
		// JGC exists (not configurable)
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.STRICT, LevelEnum.STRICT);

		// test the WIZARD validators
		// JavaDoc is NORMAL
		// RSAR is NORMAL
		// JGC exists (not configurable)
		validatorToResources = manager.getValidators(resources, TimeEnum.WIZARD);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.NORMAL, LevelEnum.NORMAL);

		// test the TOOLKITBUILD validators
		// JavaDoc is STRICT
		// RSAR is STRICT
		// JGC exists (not configurable)
		validatorToResources = manager.getValidators(resources, TimeEnum.INCREMENTALTOOLKITBUILD);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.STRICT, LevelEnum.STRICT);
		validatorToResources = manager.getValidators(resources, TimeEnum.FULLTOOLKITBUILD);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.STRICT, LevelEnum.STRICT);

		// test the INTEGRATIONSERVERBUILD validators
		// JavaDoc is NORMAL
		// RSAR is NORMAL
		// JGC exists (not configurable)
		validatorToResources = manager.getValidators(resources, TimeEnum.INTEGRATIONSERVERBUILD);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.NORMAL, LevelEnum.NORMAL);
	}

	/**
	 * Tests that we can remove some of the validators if we specify a set of
	 * validator names that we do want.
	 *
	 * @throws Exception
	 *             If an unexpected exception occurs.
	 */
	public void testCanFilterValidatorsBySpecifyingNames() throws Exception {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));

		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		resources.add(new ExternalValidatorResource(new File("testData\\TestClass01.java")));

		Map<Validator, List<ValidatorResource>> validatorToResources;

		// test the ASYOUTYPE validators
		// JavaDoc is filtered out
		// RSAR is NORMAL
		// JGC is filtered out (not configurable, but no validator)
		List<String> validatorNames = new ArrayList<String>();
		validatorNames.add("RSAR");
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE, validatorNames);
		assertValidatorMapCorrect(resources, validatorToResources, null, LevelEnum.NORMAL);
		validatorNames.clear();

		// test the ASYOUTYPE validators
		// JavaDoc is filtered out
		// RSAR is filtered out
		// JGC exists (not configurable)
		validatorNames = new ArrayList<String>();
		validatorNames.add("JGC");
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE, validatorNames);
		assertValidatorMapCorrect(resources, validatorToResources, null, null);
		validatorNames.clear();

		// test the ASYOUTYPE validators
		// JavaDoc is STRICT
		// RSAR is filtered out
		// JGC exists (not configurable)
		validatorNames = new ArrayList<String>();
		validatorNames.add("JGC");
		validatorNames.add("JavaDoc");
		validatorToResources = manager.getValidators(resources, TimeEnum.ASYOUTYPE, validatorNames);
		assertValidatorMapCorrect(resources, validatorToResources, LevelEnum.STRICT, null);
		validatorNames.clear();
	}

	/**
	 * Tests getFactoryClassForValidatorName() so that if the factory class is
	 * null, the plug-ins will be searched.
	 */
	public void testGetFactoryForValidatorNameIfFactoryClassNullExpectPluginsWillBeSearched() {
		String configFileClassName = null;
		testGetFactoryForValidatorNameIfFactoryClassInvalidExpectPluginsWillBeSearched(configFileClassName);
	}

	/**
	 * Tests getFactoryForValidatorName() so that if the factory class is empty,
	 * the plug-ins will be searched.
	 */
	public void testGetFactoryForValidatorNameIfFactoryClassEmptyExpectPluginsWillBeSearched() {
		String configFileClassName = "";
		testGetFactoryForValidatorNameIfFactoryClassInvalidExpectPluginsWillBeSearched(configFileClassName);
	}

	/**
	 * Tests getFactoryForValidatorName() so that if the factory class is a
	 * string with spaces, the plug-ins will be searched.
	 */
	public void testGetFactoryForValidatorNameIfFactoryClassIsAStringWithSpacesExpectPluginsWillBeSearched() {
		String configFileClassName = "     ";
		testGetFactoryForValidatorNameIfFactoryClassInvalidExpectPluginsWillBeSearched(configFileClassName);
	}

	/**
	 * Tests getFactoryClassForValidatorName() so that if the factory class is
	 * invalid, the plug-ins will be searched.
	 *
	 * @param configFileClassName
	 *            The name of the factory class that will be given to the
	 *            method. May be null or empty.
	 */
	private void testGetFactoryForValidatorNameIfFactoryClassInvalidExpectPluginsWillBeSearched(
			String configFileClassName) {
		ConfigurationManager manager = new FakeFinderConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		ValidatorDef validatorDef = new ValidatorDef();
		validatorDef.setName("RSAR");
		validatorDef.setClazz(configFileClassName);
		ValidatorFactory factory = manager.getFactoryForValidatorName(validatorDef);
		assertEquals("The factory class did not come from the extensions",
				"com.ibm.commerce.qcheck.tools.ConfigurationManagerTest$FakeFactory", factory.getClass().getName());
	}

	/**
	 * Tests findFactoryInExtensions() to ensure that an NPE is thrown if null
	 * is given for the name.
	 */
	public void testFindFactoryInExtensionsIfNameNullExpectThrowsException() {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		try {
			manager.findFactoryInExtensions(null, new FakeExtensionPoint());
			fail();
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests findFactoryInExtensions() to ensure that an exception is thrown if
	 * null is given for the name.
	 */
	public void testFindFactoryInExtensionsIfNameEmptyExpectThrowsException() {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		try {
			manager.findFactoryInExtensions("", new FakeExtensionPoint());
			fail();
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests findFactoryInExtensions() to ensure that an exception is thrown if
	 * null is given for the name.
	 */
	public void testFindFactoryInExtensionsIfNameIsWhiteSpaceExpectThrowsException() {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		try {
			manager.findFactoryInExtensions("  \t", new FakeExtensionPoint());
			fail();
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests findFactoryInExtensions() to ensure that an NPE is thrown if null
	 * is given for the extension point.
	 */
	public void testFindFactoryInExtensionsIfExtensionPointNullExpectThrowsException() {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		try {
			manager.findFactoryInExtensions("Hello", null);
			fail();
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests findFactoryInExtensions() to ensure that a factory is returned if
	 * the name exists if an extension provides it.
	 */
	public void testFindFactoryInExtensionsIfOneExtensionWithValidatorNameExpectReturnFactory() {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		FakeExtensionPoint extensionPoint = new FakeExtensionPoint("Val1");

		ValidatorFactory factory = manager.findFactoryInExtensions("Val1", extensionPoint);
		assertNotNull("The factory is null", factory);
		assertTrue("The factory was not returned", factory instanceof FakeFactory);
		assertEquals("The factory validator name is wrong", "Val1", ((FakeFactory) factory).getValidatorName());
	}

	/**
	 * Tests findFactoryInExtensions() to ensure that a factory is returned if
	 * an extension provides it and there is more than one extension.
	 */
	public void testFindFactoryInExtensionsIfTwoExtensionWithValidatorNameExpectReturnFactory() {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		FakeExtensionPoint extensionPoint = new FakeExtensionPoint("Val1", "Val2");

		ValidatorFactory factory = manager.findFactoryInExtensions("Val2", extensionPoint);
		assertNotNull("The factory is null", factory);
		assertTrue("The factory was not returned", factory instanceof FakeFactory);
		assertEquals("The factory validator name is wrong", "Val2", ((FakeFactory) factory).getValidatorName());
	}

	/**
	 * Tests findFactoryInExtensions() to ensure that null is returned if two
	 * extensions exist and the validator searched for is not in either of them.
	 */
	public void testFindFactoryInExtensionsIfTwoExtensionWithNotExistingValidatorNameExpectReturnNull() {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		FakeExtensionPoint extensionPoint = new FakeExtensionPoint("Val1", "Val2");

		ValidatorFactory factory = manager.findFactoryInExtensions("Val3", extensionPoint);
		assertNull("The factory is not null", factory);
	}

	public void testFindValidatorsIfValidatorNameHasNoFactoryExpectFactoryNotInList() {
		ConfigurationManager manager = new ConfigurationManager("trenth",
				new File("testData\\multipleConfigsAndValidatorsConfig.xml"));
		Config config = new Config();
		config.setName(".*");
		config.setScope(ScopeEnum.GLOBAL);
		Event event = new Event();
		event.setTime(TimeEnum.ASYOUTYPE);
		ValidatorInst validatorInst = new ValidatorInst();
		validatorInst.setName("RSAR");
		validatorInst.setLevel(LevelEnum.LOOSE);
		event.getValidator().add(validatorInst);
		config.getEvent().add(event);

		Map<String, ValidatorFactory> validatorNameToValidatorFactoryMap = new HashMap<String, ValidatorFactory>();

		Map<ValidatorFactory, LevelEnum> factoryToEnumMap = manager.findValidators(config, TimeEnum.ASYOUTYPE,
				validatorNameToValidatorFactoryMap);

		assertEquals("factoryToEnumMap is not empty", 0, factoryToEnumMap.size());
	}

	/**
	 * Checks that the given validator map contains the expected values.
	 *
	 * @param inputResources
	 *            The resources that are first input. Cannot be null.
	 * @param validatorToResources
	 *            The map to check. Cannot be null.
	 * @param javaDocLevel
	 *            The expected level of the Java doc validator. If null, there
	 *            should be no Java doc validator.
	 * @param rsarLevel
	 *            The expected level of the RSAR validator. If null, there
	 *            should be no RSAR validator.
	 */
	private void assertValidatorMapCorrect(List<ValidatorResource> inputResources,
			Map<Validator, List<ValidatorResource>> validatorToResources, LevelEnum javaDocLevel, LevelEnum rsarLevel) {

		int numValidators = 3;
		if (javaDocLevel == null) {
			numValidators--;
		}
		if (rsarLevel == null) {
			numValidators--;
		}

		Iterator<Validator> iterator = validatorToResources.keySet().iterator();
		for (int i = 0; i < numValidators; i++) {
			Validator validator = iterator.next();
			if (validator instanceof ConfigurableValidator) {
				ConfigurableValidator configurableValidator = (ConfigurableValidator) validator;
				if (javaDocLevel != null && validator instanceof JavaDocValidator) {
					assertEquals(javaDocLevel, configurableValidator.getLevel().getValue());
				} else if (rsarLevel != null && validator.getClass().getSimpleName().equals("RSARValidator")) {
					assertEquals(rsarLevel, configurableValidator.getLevel().getValue());
				} else {
					fail("Unexpected configurable validator type: " + validator);
				}
			}

			List<ValidatorResource> outputResources = validatorToResources.get(validator);
			assertEquals(inputResources, outputResources);
		}

		assertEquals(false, iterator.hasNext());
	}

	/**
	 * FakeFinderConfigurationManager overrides the
	 * {@link ConfigurationManager#findFactoryClass(String)} method so that a
	 * fake factory can be returned for testing purposes.
	 */
	private class FakeFinderConfigurationManager extends ConfigurationManager {

		/**
		 * Constructor for FakeFinderConfigurationManager. This simply calls the
		 * <code>super</code> version of the method.
		 *
		 * @param newUser
		 *            The CMVC ID of the user who needs files validated. In the
		 *            toolkit, this is the developer's ID. In the build
		 *            environment, this is the developer who last checked in the
		 *            file. Cannot be null or empty.
		 * @param newConfigFiles
		 *            The files that contain the configurations to be used.
		 *            Cannot be null, and there must be at least one file, and
		 *            no elements can be null.
		 */
		private FakeFinderConfigurationManager(String newUser, File... newConfigFiles) {
			super(newUser, newConfigFiles);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		ValidatorFactory findFactoryInExtensions(String name, IExtensionPoint extensionPoint) {
			return new FakeFactory();
		}
	}

	/**
	 * FakeFactory is a simple factory that is used to confirm that the logic
	 * will look for the factory in the extensions rather than the normal one.
	 */
	private final class FakeFactory implements ValidatorFactory {

		private String validatorName;

		/**
		 * Constructor for factory with no name.
		 */
		private FakeFactory() {
			this(null);
		}

		/**
		 * Constructor for a factory with a name for the validator it creates.
		 *
		 * @param validatorName
		 *            The name of the validator created by this factory. May be
		 *            null or empty.
		 */
		private FakeFactory(String validatorName) {
			this.validatorName = validatorName;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean canValidate(ValidatorResource resource, Level level) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void cleanup() {
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Validator getValidatorInstance(ValidatorResource resource, Level level) {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasCreated(Validator validator) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void init() {
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isConfigurable() {
			return false;
		}

		/**
		 * Returns the validator name that this factory can create.
		 *
		 * @return The name of the validator that this factory can create. May
		 *         be null or empty.
		 */
		public String getValidatorName() {
			return validatorName;
		}
	}

	/**
	 * FakeExtensionPoint is an implementation of the Eclipse interface that
	 * lets us fake data returned by the platform.
	 */
	private class FakeExtensionPoint implements IExtensionPoint {

		/**
		 * The names of validators that this extension point can create.
		 */
		private List<String> validatorNames;

		/**
		 * Constructor for FakeExtensionPoint.
		 *
		 * @param names
		 *            The names of validators that this extension point returns
		 *            through configuration elements. May not be null, but can
		 *            be empty.
		 */
		private FakeExtensionPoint(String... names) {
			List<String> namesToValues = Arrays.asList(names);

			this.validatorNames = namesToValues;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IConfigurationElement[] getConfigurationElements() throws InvalidRegistryObjectException {

			FakeConfigurationElement[] children = new FakeConfigurationElement[validatorNames.size()];
			for (int i = 0; i < validatorNames.size(); i++) {
				String validatorName = validatorNames.get(i);
				children[i] = new FakeConfigurationElement(validatorName, null);
			}

			return new FakeConfigurationElement[] { new FakeConfigurationElement(null, children) };
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IContributor getContributor() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IExtension getExtension(String arg0) throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IExtension[] getExtensions() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getLabel() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getNamespace() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getNamespaceIdentifier() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getSchemaReference() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getSimpleIdentifier() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getUniqueIdentifier() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isValid() {
			return false;
		}

		@Override
		public String getLabel(String arg0) throws InvalidRegistryObjectException {
			return null;
		}

	}

	/**
	 * FakeConfigurationElement is a fake implementation of the Eclipse
	 * interface that lets us return validators for testing purposes.
	 */
	private class FakeConfigurationElement implements IConfigurationElement {

		/**
		 * The name of the validator. This will be non-null for children of the
		 * top-level configuration elements.
		 */
		private String validatorName;

		/**
		 * The child elements of this. This will be non-null for top-level
		 * configuration elements.
		 */
		private FakeConfigurationElement[] children;

		/**
		 * Constructor for FakeConfigurationElement.
		 *
		 * @param validatorName
		 *            The name of the validator, only necessary for child
		 *            elements. May be null or empty.
		 * @param children
		 *            The child elements of this, only necessary for top-level
		 *            elements. May be null.
		 */
		private FakeConfigurationElement(String validatorName, FakeConfigurationElement[] children) {
			this.validatorName = validatorName;
			this.children = children;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object createExecutableExtension(String arg0) throws CoreException {
			return new FakeFactory(validatorName);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getAttribute(String arg0) throws InvalidRegistryObjectException {
			if ("name".equals(arg0)) {
				return validatorName;
			}

			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getAttributeAsIs(String arg0) throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String[] getAttributeNames() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IConfigurationElement[] getChildren() throws InvalidRegistryObjectException {
			return children;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IConfigurationElement[] getChildren(String arg0) throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IContributor getContributor() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IExtension getDeclaringExtension() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getName() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getNamespace() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getNamespaceIdentifier() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object getParent() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getValue() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getValueAsIs() throws InvalidRegistryObjectException {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isValid() {
			return false;
		}

		@Override
		public String getAttribute(String arg0, String arg1) throws InvalidRegistryObjectException {
			return null;
		}

		@Override
		public String getValue(String arg0) throws InvalidRegistryObjectException {
			return null;
		}

	}
}
