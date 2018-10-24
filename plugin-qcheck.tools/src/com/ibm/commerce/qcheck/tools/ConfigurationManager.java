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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ModelRegistry;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.WatchedFile;
import com.ibm.commerce.qcheck.tools.config.Config;
import com.ibm.commerce.qcheck.tools.config.Event;
import com.ibm.commerce.qcheck.tools.config.Level;
import com.ibm.commerce.qcheck.tools.config.LevelEnum;
import com.ibm.commerce.qcheck.tools.config.ObjectFactory;
import com.ibm.commerce.qcheck.tools.config.ScopeEnum;
import com.ibm.commerce.qcheck.tools.config.Setup;
import com.ibm.commerce.qcheck.tools.config.TimeEnum;
import com.ibm.commerce.qcheck.tools.config.ValidatorDef;
import com.ibm.commerce.qcheck.tools.config.ValidatorInst;

/**
 * ConfigurationManager loads configuration data, and based on that
 * configuration, creates {@link Validator validators} for resources as needed.
 * The entry point is the {@link #getValidators(List, TimeEnum)} method.
 * <p>
 * To maximize performance, caching is used to avoid recreating validators
 * unnecessarily. Moreover, since many files will have common configurations,
 * the validators for these configurations are stored together. Each file is
 * associated with one such configuration to make validation faster next time
 * the file is validated (although many files can map to the same
 * configuration).
 * <p>
 * The configuration data is loaded from an XML file using JAXB. When that file
 * is updated, the configuration and cache will be reset the next time the
 * {@link #getValidators(List, TimeEnum)} method is called.
 * 
 * @author Trent Hoeppner
 */
public class ConfigurationManager {

	/**
	 * The name of the extension point which is used to specify new validators.
	 */
	private static final String EXTENSION_POINT_NAME = "com.ibm.commerce.qcheck.tools.validators";

	/**
	 * A mapping from validator names to the ValidatorFactory objects that those
	 * names represent. Names are arbitrary and defined in the <code>name</code>
	 * attribute of the <code>validator</code> element (child of the
	 * <code>setup</code> element).
	 * <p>
	 * If two configuration files have the same name for a factory, the
	 * definition in the later list will be ignored. This value will never be
	 * null.
	 */
	private Map<String, ValidatorFactory> validatorNameToValidatorFactoryMap = new HashMap<String, ValidatorFactory>();

	/**
	 * A mapping from ValidatorFactory objects to the levels that are configured
	 * for those factories. This value will never be null.
	 */
	private Map<ValidatorFactory, List<Level>> validatorFactoryToLevelsMap = new HashMap<ValidatorFactory, List<Level>>();

	/**
	 * A mapping from the resource paths to ConfigGroups that can return
	 * Validators for those resources. The resource path is obtained by
	 * combining the {@link ValidatorResource#getBaseDir() base directory} and
	 * the {@link ValidatorResource#getPathFilename() relative filename}.
	 * Resources that require exactly the same Validators and configurations for
	 * those Validators will point to the same ConfigGroups. In this way, this
	 * map serves as a cache of validators for each resource. This value will
	 * never be null.
	 */
	private Map<String, ConfigGroup> resourceToConfigGroupMap = new HashMap<String, ConfigGroup>();

	/**
	 * A mapping from regular expressions in String form to Pattern objects
	 * which represent those expressions. This is used to avoid recreating
	 * Pattern objects from regular expressions in the configuration files. This
	 * value will never be null.
	 */
	private Map<String, Pattern> globToPatternMap = new HashMap<String, Pattern>();

	/**
	 * The name of the user on whose behalf this configuration is working. In
	 * the toolkit, the user should be the developer's CMVC ID. In the build
	 * environment, the user should be the CMVC ID of the developer that last
	 * checked in the file. This value will never be null or empty.
	 */
	private String user;

	/**
	 * The XML files that contain the configuration data. This value will never
	 * be null.
	 */
	private List<ConfigFile> configFiles;

	/**
	 * Constructor for ConfigurationManager. The order of the files given is
	 * significant. If a validator is defined in an earlier configuration file
	 * with a name, that name cannot be used in a later configuration file.
	 * Also, <code>config</code> elements defined in later files will override
	 * settings in earlier files, but only with increasing strictness (similar
	 * to overriding behavior within a configuration file).
	 * <p>
	 * If any file passed into the constructor changes on disk, or if it
	 * originally does not exist and is later created, the configuration will
	 * automatically updated to reflect the changes the next time the
	 * {@link #getValidators(List, TimeEnum)} method is called.
	 *
	 * @param newUser
	 *            The CMVC ID of the user who needs files validated. In the
	 *            toolkit, this is the developer's ID. In the build environment,
	 *            this is the developer who last checked in the file. Cannot be
	 *            null or empty.
	 * @param newConfigFiles
	 *            The files that contain the configurations to be used. Cannot
	 *            be null, and there must be at least one file, and no elements
	 *            can be null.
	 */
	public ConfigurationManager(String newUser, File... newConfigFiles) {
		this.user = newUser;
		this.configFiles = new ArrayList<ConfigFile>();
		for (File file : newConfigFiles) {
			ConfigFile configFile = new ConfigFile(file);
			configFiles.add(configFile);
		}
	}

	/**
	 * Constructor for ConfigurationManager. The order of the files given is
	 * significant. If a validator is defined in an earlier configuration file
	 * with a name, that name cannot be used in a later configuration file.
	 * Also, <code>config</code> elements defined in later files will override
	 * settings in earlier files, but only with increasing strictness (similar
	 * to overriding behavior within a configuration file).
	 * <p>
	 * This constructor differs from the
	 * {@link #ConfigurationManager(String, File...)} constructor in that files
	 * opened as URLs will not be automatically updated if the file changes.
	 * There is an exception to this: if the URL has a <code>file</code>
	 * protocol, it will be converted to a File and will behave as if it was
	 * created in the other constructor.
	 *
	 * @param newUser
	 *            The CMVC ID of the user who needs files validated. In the
	 *            toolkit, this is the developer's ID. In the build environment,
	 *            this is the developer who last checked in the file. Cannot be
	 *            null or empty.
	 * @param newConfigURLs
	 *            The URLs that point to resources that contain the
	 *            configurations to be used. Cannot be null, and there must be
	 *            at least one URL, and no elements can be null.
	 */
	public ConfigurationManager(String newUser, URL... newConfigURLs) {
		this.user = newUser;
		this.configFiles = new ArrayList<ConfigFile>();
		for (URL url : newConfigURLs) {
			ConfigFile configFile = new ConfigFile(url);
			configFiles.add(configFile);
		}
	}

	/**
	 * Returns the Validators that can be applied to the given list of
	 * resources. For each Validator, a list of resources is returned in the map
	 * that should be validated by that Validator. Each Validator will have at
	 * least one resource, and each resource might exist in the list for more
	 * than one Validator.
	 * <p>
	 * A configuration for a validator in the configuration file must specify a
	 * {@link LevelEnum level}. If the configured level for a validator is
	 * {@link LevelEnum#NONE}, the validator will not be returned in this list.
	 * <p>
	 * Configured levels within each scope can be overridden. {@link ScopeEnum}
	 * values with a lower ordinal value will override those with a higher
	 * ordinal value. Also, levels can only increase or stay the same when
	 * overridden, not decrease. The exception is when <code>time</code> is
	 * either {@link TimeEnum#TOOLKITBUILD} or {@link TimeEnum#ASYOUTYPE} - if
	 * the level is specified as {@link LevelEnum#NONE}, the configured
	 * validator will not be returned. This allows users to tailor their
	 * workstations to improve performance when a build occurs or when typing.
	 * <p>
	 * For example, consider this snippet of a configuration file:
	 *
	 * <pre>
	 *     &lt;config scope=&quot;global&quot; name=&quot;.*&quot;&gt;
	 *         &lt;event time=&quot;wizard&quot;&gt;
	 *             &lt;validator name=&quot;JavaDoc&quot; level=&quot;loose&quot; /&gt;
	 *             &lt;validator name=&quot;RSAR&quot; level=&quot;loose&quot; /&gt;
	 *             &lt;validator name=&quot;JGC&quot; level=&quot;loose&quot; /&gt;
	 *         &lt;/event&gt;
	 *         &lt;event time=&quot;asyoutype&quot;&gt;
	 *             &lt;validator name=&quot;JavaDoc&quot; level=&quot;loose&quot; /&gt;
	 *             &lt;validator name=&quot;RSAR&quot; level=&quot;loose&quot; /&gt;
	 *             &lt;validator name=&quot;JGC&quot; level=&quot;loose&quot; /&gt;
	 *         &lt;/event&gt;
	 *     &lt;/config&gt;
	 *     &lt;config scope=&quot;class&quot; name=&quot;TestClass01&quot;&gt;
	 *         &lt;event time=&quot;wizard&quot;&gt;
	 *             &lt;validator name=&quot;JavaDoc&quot; level=&quot;normal&quot; /&gt;
	 *             &lt;validator name=&quot;RSAR&quot; level=&quot;normal&quot; /&gt;
	 *             &lt;validator name=&quot;JGC&quot; level=&quot;normal&quot; /&gt;
	 *         &lt;/event&gt;
	 *         &lt;event time=&quot;asyoutype&quot;&gt;
	 *             &lt;validator name=&quot;JavaDoc&quot; level=&quot;strict&quot; /&gt;
	 *             &lt;validator name=&quot;RSAR&quot; level=&quot;normal&quot; /&gt;
	 *             &lt;validator name=&quot;JGC&quot; level=&quot;loose&quot; /&gt;
	 *         &lt;/event&gt;
	 *     &lt;/config&gt;
	 * </pre>
	 *
	 * Here, the <code>class</code> scope and <code>wizard</code> event will
	 * override the same event in the <code>global</code> scope, so that all
	 * validators will be normal. For the <code>asyoutype</code> event, the
	 * <code>class</code> scope will again override the <code>global</code>
	 * scope, but the <code>JGC</code> validator will remain the same because
	 * the level specified is the same.
	 * <p>
	 * Furthermore, if more than one configuration file is used, later
	 * configurations may override earlier configurations. For each scope, there
	 * may be a matching configuration in each configuration file. If there are
	 * two or more configurations for a scope, the later configurations will
	 * override the earlier ones. After all configurations are evaluated for a
	 * scope, the process repeats with the next scope, until all scopes are
	 * evaluated.
	 *
	 * @param resources
	 *            The resources that need to be validated. Cannot be null.
	 * @param time
	 *            The circumstances under which the validation occurs. Cannot be
	 *            null.
	 *
	 * @return A mapping from Validators to lists of resources for each
	 *         Validator. Will not be null, and there will be no null keys, and
	 *         the lists of resources will not be null or empty.
	 */
	public Map<Validator, List<ValidatorResource>> getValidators(List<ValidatorResource> resources, TimeEnum time) {

		if (Debug.CONFIG.isActive()) {
			Debug.CONFIG.log("resourceToConfigGroupMap: ", resourceToConfigGroupMap.size(), ", globToPatternMap: ",
					globToPatternMap.size(), ", configFiles: ", configFiles.size(), ", validatorFactoryToLevelsMap: ",
					validatorFactoryToLevelsMap.size(), ", validatorNameToValidatorFactoryMap: ",
					validatorNameToValidatorFactoryMap.size());
		}

		ensureLatestConfigLoaded();

		Map<Validator, List<ValidatorResource>> validatorToResourcesMap = new HashMap<Validator, List<ValidatorResource>>();
		for (ValidatorResource resource : resources) {
			// ConfigGroup configGroup = findConfigGroup(resource);
			// addResourceToValidators(validatorToResourcesMap, resource,
			// configGroup, time);
			addResourceToValidators(validatorToResourcesMap, resource);

			// TODO a bit hacky to have to clean here
			ModelRegistry.getDefault().clearValidator(resource);
		}

		if (Debug.CONFIG.isActive()) {
			for (Validator validator : validatorToResourcesMap.keySet()) {
				Debug.CONFIG.log("added validator ", validator.getClass().getName());
			}
		}

		return validatorToResourcesMap;
	}

	/**
	 * Returns the list of validators that will validate the files according to
	 * the configuration file(s). The behavior is the same as
	 * {@link #getValidators(List, TimeEnum)}, except that only the validators
	 * with the given names will be returned. In effect, the validators are
	 * filtered by the given list.
	 *
	 * @param resources
	 *            The resources that need to be validated. Cannot be null.
	 * @param time
	 *            The circumstances under which the validation occurs. Cannot be
	 *            null.
	 * @param configValidatorNames
	 *            The list of validators to use. The values must be specified in
	 *            the configuration file as <code>name</code> attributes in
	 *            <code>validator</code> elements. A null value indicates that
	 *            all validators will be used. An empty list indicates that no
	 *            validators will be used.
	 *
	 * @return A mapping from Validators to lists of resources for each
	 *         Validator. Will not be null, and there will be no null keys, and
	 *         the lists of resources will not be null or empty.
	 */
	public Map<Validator, List<ValidatorResource>> getValidators(List<ValidatorResource> resources, TimeEnum time,
			List<String> configValidatorNames) {
		Map<Validator, List<ValidatorResource>> validatorToResourcesMap = getValidators(resources, time);

		// filter out the validators that are not in the given list
		Iterator<Validator> validatorIterator = validatorToResourcesMap.keySet().iterator();
		while (validatorIterator.hasNext()) {
			Validator validator = validatorIterator.next();
			String validatorName = findValidatorName(validator);
			if (!configValidatorNames.contains(validatorName)) {
				validatorIterator.remove();
			}
		}

		return validatorToResourcesMap;
	}

	/**
	 * Returns the name of the given validator as it exists in the configuration
	 * file.
	 *
	 * @param validator
	 *            The validator to get the name for. Cannot be null.
	 *
	 * @return The name of the validator as it appears in the <code>namek</code>
	 *         attribute of the <code>validatord</code> element in the
	 *         configuration file. This value will not be null or empty.
	 */
	private String findValidatorName(Validator validator) {
		String validatorName = null;
		for (String potentialName : validatorNameToValidatorFactoryMap.keySet()) {
			ValidatorFactory factory = validatorNameToValidatorFactoryMap.get(potentialName);
			if (factory.hasCreated(validator)) {
				validatorName = potentialName;
				break;
			}
		}
		return validatorName;
	}

	/**
	 * Adds the given resource to all Validators in the given map which
	 * correspond to the given configGroup. The configGroup is queried to find
	 * out which Validators can be used for this resource, and then for each
	 * Validator, the resource is added to that Validator's list in the given
	 * map. If a Validator does not exist in the map, it will be added. If no
	 * Validators were found for the resource, the map will not be updated.
	 *
	 * @param validatorToResourcesMap
	 *            The mapping from Validators to lists of resources that should
	 *            be validated by those Validators. Cannot be null.
	 * @param resource
	 *            The resource to find Validators for. Cannot be null.
	 * @param configGroup
	 *            The configuration that specifically applies to the given
	 *            resource. Cannot be null.
	 * @param time
	 *            The circumstances under which the validation occurs. Cannot be
	 *            null.
	 */
	private void addResourceToValidators(Map<Validator, List<ValidatorResource>> validatorToResourcesMap,
			ValidatorResource resource, ConfigGroup configGroup, TimeEnum time) {
		List<Validator> validators = configGroup.getValidators(resource, time);
		for (Validator validator : validators) {
			if (Debug.CONFIG.isActive()) {
				Debug.CONFIG.log("found that validator ", validator.getClass().getName(), " applies for resource ",
						resource.getFilename());
			}

			List<ValidatorResource> resourcesForValidator = validatorToResourcesMap.get(validator);
			if (resourcesForValidator == null) {
				resourcesForValidator = new ArrayList<ValidatorResource>();
				validatorToResourcesMap.put(validator, resourcesForValidator);
			}
			resourcesForValidator.add(resource);
		}
	}

	/**
	 * Adds the given resource to all valid Validators in the given map. This
	 * ignores the config for time and scope and instead assumes a default level
	 * of {@link LevelEnum#NORMAL}. This is done for performance reasons. See
	 * {@link #getValidators(List, TimeEnum)} for details.
	 *
	 * @param validatorToResourcesMap
	 *            The mapping from Validators to lists of resources that should
	 *            be validated by those Validators. Cannot be null.
	 * @param resource
	 *            The resource to find Validators for. Cannot be null.
	 */
	private void addResourceToValidators(Map<Validator, List<ValidatorResource>> validatorToResourcesMap,
			ValidatorResource resource) {
		List<Validator> validators = getValidators(resource);
		for (Validator validator : validators) {
			if (Debug.CONFIG.isActive()) {
				Debug.CONFIG.log("found that validator ", validator.getClass().getName(), " applies for resource ",
						resource.getFilename());
			}

			List<ValidatorResource> resourcesForValidator = validatorToResourcesMap.get(validator);
			if (resourcesForValidator == null) {
				resourcesForValidator = new ArrayList<ValidatorResource>();
				validatorToResourcesMap.put(validator, resourcesForValidator);
			}
			resourcesForValidator.add(resource);
		}
	}

	/**
	 * Returns the validators for the given <code>resource</code>. This method
	 * ignores the configured and scope for resources, and just assumes a
	 * {@link LevelEnum#NORMAL} level. This is done for performance reasons to
	 * reduce the overhead of finding configs on a full build (finding configs
	 * causes the whole Commerce toolkit workspace to take 2-3 hours to
	 * validate).
	 *
	 * @param resource
	 *            The resource to validate. Cannot be null.
	 *
	 * @return The validators that can be used for the given
	 *         <code>resource</code> at the given <code>time</code>. Will not be
	 *         null, but may be empty if no validators are configured.
	 */
	public List<Validator> getValidators(ValidatorResource resource) {
		List<Validator> validators = new ArrayList<Validator>();
		for (ValidatorFactory factory : validatorFactoryToLevelsMap.keySet()) {
			if (Debug.CONFIG.isActive()) {
				Debug.CONFIG.log("checking if factory ", factory.getClass().getName(), " can validate ",
						resource.getFilename());
			}

			Level levelData = getLevel(factory, LevelEnum.NORMAL);
			if (factory.canValidate(resource, levelData)) {
				Validator validator = factory.getValidatorInstance(resource, levelData);
				validators.add(validator);
				if (Debug.CONFIG.isActive()) {
					Debug.CONFIG.log("  it can");
				}
			} else {
				if (Debug.CONFIG.isActive()) {
					Debug.CONFIG.log("  it can't");
				}
			}
		}

		return validators;
	}

	/**
	 * Returns the configuration that applies specifically to the given
	 * resource. This configuration will be cached and associated with the
	 * resource so that future searches are faster. If a configuration already
	 * exists which can be applied to the resource, it will be used instead of
	 * creating a new one.
	 *
	 * @param resource
	 *            The resource to find the configuration of. Cannot be null.
	 *
	 * @return The configuration that can be used. Will not be null.
	 */
	private ConfigGroup findConfigGroup(ValidatorResource resource) {
		String resourcePath = resource.getBaseDir() + resource.getPathFilename();
		ConfigGroup configGroup = resourceToConfigGroupMap.get(resourcePath);
		if (configGroup == null) {
			configGroup = new ConfigGroup(resource);

			// try to find an existing instance so we can reuse the validators
			for (ConfigGroup possibleMatch : resourceToConfigGroupMap.values()) {
				if (configGroup.equals(possibleMatch)) {
					configGroup = possibleMatch;
					break;
				}
			}

			resourceToConfigGroupMap.put(resourcePath, configGroup);
		}

		return configGroup;
	}

	/**
	 * Checks that the configuration files are all up-to-date with the
	 * configurations loaded in memory. If there are no configurations in memory
	 * or the configurations in the files have changed, the configurations in
	 * memory will be released to the garbage collector (if they exist) and the
	 * new configurations will be loaded.
	 */
	private void ensureLatestConfigLoaded() {
		boolean changed = false;
		for (ConfigFile configFile : configFiles) {
			if (configFile.changed()) {
				cleanCaches();
				changed = true;
				break;
			}
		}

		if (changed) {
			for (ConfigFile configFile : configFiles) {
				configFile.ensureLatestLoaded();
			}
		}
	}

	/**
	 * Returns the level details for the given factory and level.
	 *
	 * @param validatorFactory
	 *            The factory that has levels configured for it. Cannot be null.
	 * @param level
	 *            The level of the level detail to get. Cannot be null.
	 *
	 * @return The level details, which include the level and any data
	 *         associated with that level for the given factory. Will be null if
	 *         no level details exist.
	 */
	private Level getLevel(ValidatorFactory validatorFactory, LevelEnum level) {
		List<Level> levels = validatorFactoryToLevelsMap.get(validatorFactory);
		Level levelData = null;
		for (Level possibleLevelData : levels) {
			if (possibleLevelData.getValue() == level) {
				levelData = possibleLevelData;
				break;
			}
		}

		return levelData;
	}

	/**
	 * Finds the first <code>config</code> element in each configuration file
	 * that matches the given scope and name. Since scope names are specified
	 * using regular expressions, there may be more than one match for the given
	 * name, but only the first match will be returned. This means that
	 * <code>config</code> elements that have the same scope that occur first in
	 * a file have a higher precedence than <code>config</code> elements that
	 * occur later in the file.
	 *
	 * @param scope
	 *            The indicator of which object is used for the name. Cannot be
	 *            null.
	 * @param name
	 *            The name that will be matched against the scope to determine a
	 *            match. Cannot be null, but may be empty.
	 *
	 * @return The first configuration object in each configuration file that
	 *         match the given scope and name. The order of the objects will
	 *         follow the order of the configuration files. However, if a
	 *         configuration file had no matching objects, these will be skipped
	 *         in the list. Will not be null, but may be empty if no matching
	 *         configurations exist in any configuration file.
	 */
	private List<Config> findFirstConfigs(ScopeEnum scope, String name) {
		List<Config> configs = new ArrayList<Config>();
		for (ConfigFile file : configFiles) {

			Config realConfig = file.findFirstConfig(scope, name);
			if (realConfig != null) {
				configs.add(realConfig);
			}
		}

		return configs;
	}

	/**
	 * Returns the object which represents the given regular expression. If the
	 * object is not cached, a new one will be created and cached.
	 *
	 * @param regex
	 *            The regular expression to get the pattern object for. Cannot
	 *            be null or empty.
	 *
	 * @return An object which represents the given regular expression. Will not
	 *         be null.
	 */
	private Pattern getPattern(String regex) {
		Pattern pattern = globToPatternMap.get(regex);
		if (pattern == null) {
			pattern = Pattern.compile(regex);
			globToPatternMap.put(regex, pattern);
		}

		return pattern;
	}

	/**
	 * Returns all factories for the given time within the given configuration
	 * object. The map also indicates the configured level for each factory. If
	 * a validator is configured but its factory cannot be found, the validator
	 * will not be included in the returned map, and a warning message will be
	 * logged.
	 *
	 * @param config
	 *            The configuration to find factories in. Cannot be null.
	 * @param time
	 *            The validation circumstances within the configuration object.
	 *            Cannot be null.
	 *
	 * @return A mapping from factories for the given time to configured levels
	 *         for those factories. Will not be null, but may be empty if the
	 *         configuration has no events configured.
	 */
	Map<ValidatorFactory, LevelEnum> findValidators(Config config, TimeEnum time,
			Map<String, ValidatorFactory> validatorNameToValidatorFactoryMap) {
		Map<ValidatorFactory, LevelEnum> globalValidatorFactories = new HashMap<ValidatorFactory, LevelEnum>();
		for (Event event : config.getEvent()) {
			if (event.getTime() == time) {
				for (ValidatorInst validatorInst : event.getValidator()) {
					String validatorName = validatorInst.getName();
					ValidatorFactory factory = validatorNameToValidatorFactoryMap.get(validatorName);
					if (factory != null) {
						globalValidatorFactories.put(factory, validatorInst.getLevel());
					} else {
						Debug.CONFIG.log("Warning: Could not find factory for validator \"" + validatorName
								+ "\", this validator will not be run.");
					}
				}
				break;
			}
		}

		return globalValidatorFactories;
	}

	/**
	 * Removes data from class caches so that new configurations may be loaded.
	 */
	private void cleanCaches() {
		if (Debug.CONFIG.isActive()) {
			Debug.CONFIG.log("Cleaning ConfigurationManager caches.");
		}

		for (ValidatorFactory factory : validatorNameToValidatorFactoryMap.values()) {
			factory.cleanup();
		}

		validatorNameToValidatorFactoryMap.clear();
		validatorFactoryToLevelsMap.clear();
		resourceToConfigGroupMap.clear();
		globToPatternMap.clear();

		for (ConfigFile configFile : configFiles) {
			configFile.setLoadedObject(null);
		}
	}

	/**
	 * Returns the factory that corresponds to the given definition from the
	 * configuration file. If the configuration specifies a factory class, it
	 * will simply be created through reflection. If not, the class will be
	 * created by searching through the Validators extension points.
	 *
	 * @param validatorDef
	 *            The element from the configuration file that specifies the
	 *            validators. Cannot be null.
	 *
	 * @return The factory that was created, or null if the factory class could
	 *         not be found.
	 */
	ValidatorFactory getFactoryForValidatorName(ValidatorDef validatorDef) {
		ValidatorFactory factory = null;

		String className = validatorDef.getClazz();
		if (className == null || className.trim().equals("")) {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_POINT_NAME);
			factory = findFactoryInExtensions(validatorDef.getName(), extensionPoint);
		} else {
			try {
				Class clazz = Class.forName(className);
				Constructor constructor = clazz.getConstructor(new Class[0]);
				factory = (ValidatorFactory) constructor.newInstance(new Object[0]);
			} catch (ClassNotFoundException e) {
				Debug.CONFIG.log(e);
			} catch (SecurityException e) {
				Debug.CONFIG.log(e);
			} catch (NoSuchMethodException e) {
				Debug.CONFIG.log(e);
			} catch (IllegalArgumentException e) {
				Debug.CONFIG.log(e);
			} catch (InstantiationException e) {
				Debug.CONFIG.log(e);
			} catch (IllegalAccessException e) {
				Debug.CONFIG.log(e);
			} catch (InvocationTargetException e) {
				Debug.CONFIG.log(e);
			}
		}

		return factory;
	}

	/**
	 * Finds and returns the first factory defined for the given validator name
	 * among the extensions for the Validators extension point.
	 *
	 * @param validatorName
	 *            The name of the validator. Cannot be null or empty.
	 *
	 * @return The factory that was found and created through the extension
	 *         mechanism, or null if no factory with the given validator name
	 *         could be found.
	 */
	ValidatorFactory findFactoryInExtensions(String validatorName, IExtensionPoint extensionPoint) {

		if (validatorName == null) {
			throw new NullPointerException("validatorName cannot be null.");
		}

		if (validatorName.trim().isEmpty()) {
			throw new IllegalArgumentException("validatorName cannot be empty or contain only whitespace.");
		}

		if (extensionPoint == null) {
			throw new NullPointerException("extensionPoint cannot be null.");
		}

		ValidatorFactory factory = null;

		IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

		for (int j = 0; j < elements.length; j++) {
			IConfigurationElement element = elements[j];
			IConfigurationElement[] validatorElements = element.getChildren();

			for (IConfigurationElement validatorElement : validatorElements) {
				String extensionValidatorName = validatorElement.getAttribute("name");
				if (extensionValidatorName != null && extensionValidatorName.trim().equals(validatorName)) {
					try {
						factory = (ValidatorFactory) validatorElement.createExecutableExtension("factoryClass");
					} catch (CoreException e) {
						Debug.CONFIG.log(e);
					}

					break;
				}
			}

			if (factory != null) {
				break;
			}
		}

		return factory;
	}

	/**
	 * ConfigGroup represents a configuration that may be used by one or more
	 * files. The purpose is to cache the lookup of {@link ValidationFactory}
	 * objects to improve performance when validating small and large numbers of
	 * files.
	 * <p>
	 * Each file will map to a group of validators, and there will naturally be
	 * several files for the same group of validators. This class is meant to
	 * represent that group.
	 */
	private class ConfigGroup {

		/**
		 * A mapping from scopes to configurations for this scope. When this is
		 * first created, this map will be populated with information for a
		 * single validator resource.
		 * <p>
		 * This map is used as the basis of comparison to other ConfigGroups.
		 * When a new resource is found, a ConfigGroup is first created for it
		 * and compared to other ConfigGroups in
		 * {@link ConfigurationManager#resourceToConfigGroupMap}. For this
		 * reason, performance of creation and comparisons should be good.
		 * <p>
		 * The map is ordered by {@link ScopeEnum} because we process scopes in
		 * order from general to specific. This value will never be null.
		 */
		private Map<ScopeEnum, List<Config>> scopeToConfigMap = new TreeMap<ScopeEnum, List<Config>>(
				new ScopeComparator());

		/**
		 * A mapping from times to factories for those times. Each factory is
		 * also associated with a level. This map is the cache of factories that
		 * can be used for a given file. This value will never be null.
		 */
		private Map<TimeEnum, Map<ValidatorFactory, LevelEnum>> timeToFactoryToLevelMap;

		/**
		 * Constructor for ConfigGroup.
		 *
		 * @param resource
		 *            The resource that is used to construct this configuration
		 *            group. The resource itself is not kept. Cannot be null.
		 */
		private ConfigGroup(ValidatorResource resource) {
			for (ScopeEnum scope : ScopeEnum.values()) {
				String name = getScopeName(resource, scope);
				List<Config> configs = findFirstConfigs(scope, name);
				scopeToConfigMap.put(scope, configs);
			}
		}

		/**
		 * Gets the name for the given scope that is assigned to this resource.
		 * For example, the name for the {@link ScopeEnum#FILE} scope is the
		 * resource's filename.
		 *
		 * @param resource
		 *            The resource to get the name for. Cannot be null.
		 * @param scope
		 *            The scope to get the name for this resource. Cannot be
		 *            null.
		 *
		 * @return The name for this resource in the given scope. Will not be
		 *         null but may be empty.
		 */
		private String getScopeName(ValidatorResource resource, ScopeEnum scope) {
			String name;
			switch (scope) {
			case GLOBAL:
				name = "";
				break;
			case COMPONENT:
				name = ""; // TODO implement mapping of resources to value
				break;
			case CLASS:
				name = resource.getClassName();
				break;
			case FILE:
				name = resource.getBaseDir() + File.separator + resource.getPathFilename();
				break;
			case USER:
				name = user;
				break;
			default:
				name = "";
				break;
			}

			return name;
		}

		/**
		 * Returns the factories in this that are configured for the given time.
		 * Each factory is associated with its configured level in the returned
		 * map. See {@link ConfigurationManager#getValidators(List, TimeEnum)}
		 * for an explanation of how configurations are overridden.
		 *
		 * @param time
		 *            The circumstances under which the validators will be used.
		 *            Cannot be null.
		 *
		 * @return A mapping of validators to configured levels. Will not be
		 *         null.
		 */
		private Map<ValidatorFactory, LevelEnum> getFactoryToLevelMap(TimeEnum time) {

			// lazily create this map because some ConfigGroups are thrown away
			if (timeToFactoryToLevelMap == null) {
				timeToFactoryToLevelMap = new HashMap<TimeEnum, Map<ValidatorFactory, LevelEnum>>();
			}

			Map<ValidatorFactory, LevelEnum> factoryToLevelMap = timeToFactoryToLevelMap.get(time);
			if (factoryToLevelMap == null) {
				factoryToLevelMap = new HashMap<ValidatorFactory, LevelEnum>();
				timeToFactoryToLevelMap.put(time, factoryToLevelMap);
				for (ScopeEnum scope : scopeToConfigMap.keySet()) {
					List<Config> configs = scopeToConfigMap.get(scope);
					for (Config config : configs) {
						Map<ValidatorFactory, LevelEnum> factories = findValidators(config, time,
								validatorNameToValidatorFactoryMap);
						for (ValidatorFactory factory : factories.keySet()) {
							LevelEnum newLevel = factories.get(factory);
							LevelEnum oldLevel = factoryToLevelMap.get(factory);
							if (oldLevel == null && newLevel != LevelEnum.NONE
									|| oldLevel != null && newLevel.ordinal() > oldLevel.ordinal()) {
								factoryToLevelMap.put(factory, newLevel);
							} else if ((time == TimeEnum.ASYOUTYPE || time == TimeEnum.FULLTOOLKITBUILD
									|| time == TimeEnum.INCREMENTALTOOLKITBUILD) && newLevel == LevelEnum.NONE) {
								factoryToLevelMap.remove(factory);
							}
						}
					}
				}
			}

			return factoryToLevelMap;
		}

		/**
		 * Returns the validators for the given <code>resource</code> and
		 * <code>time</code>. The configured {@link ValidatorFactory factories}
		 * will be retrieved, but if a factory's
		 * {@link ValidatorFactory#canValidate(ValidatorResource)
		 * canValidate(ValidatorResource)} method returns false for the given
		 * resource, the factory will not be used to produce a validator.
		 *
		 * @param resource
		 *            The resource to validate. Cannot be null.
		 * @param time
		 *            The circumstances under which the resource will be
		 *            evaluated. Cannot be null.
		 *
		 * @return The validators that can be used for the given
		 *         <code>resource</code> at the given <code>time</code>. Will
		 *         not be null, but may be empty if no validators are
		 *         configured.
		 */
		public synchronized List<Validator> getValidators(ValidatorResource resource, TimeEnum time) {
			List<Validator> validators = new ArrayList<Validator>();
			Map<ValidatorFactory, LevelEnum> factoryToLevelMapLocal = getFactoryToLevelMap(time);
			for (ValidatorFactory factory : factoryToLevelMapLocal.keySet()) {
				if (Debug.CONFIG.isActive()) {
					Debug.CONFIG.log("checking if factory ", factory.getClass().getName(), " can validate ",
							resource.getFilename());
				}

				LevelEnum level = factoryToLevelMapLocal.get(factory);
				Level levelData = getLevel(factory, level);
				if (factory.canValidate(resource, levelData)) {
					Validator validator = factory.getValidatorInstance(resource, levelData);
					validators.add(validator);
					if (Debug.CONFIG.isActive()) {
						Debug.CONFIG.log("  it can");
					}
				} else {
					if (Debug.CONFIG.isActive()) {
						Debug.CONFIG.log("  it can't");
					}
				}
			}

			return validators;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((scopeToConfigMap == null) ? 0 : scopeToConfigMap.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj == null) {
				return false;
			}

			if (getClass() != obj.getClass()) {
				return false;
			}

			ConfigGroup other = (ConfigGroup) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}

			if (scopeToConfigMap == null) {
				if (other.scopeToConfigMap != null) {
					return false;
				}
			} else if (!scopeToConfigMap.equals(other.scopeToConfigMap)) {
				return false;
			}
			return true;
		}

		/**
		 * Returns the outer type of this, so that the parent instance can be
		 * used in {@link #equals(Object)} and {@link #hashCode()}.
		 *
		 * @return The parent instance of this. Will not be null.
		 */
		private ConfigurationManager getOuterType() {
			return ConfigurationManager.this;
		}

	}

	/**
	 * ConfigFile represents a file or URL that can be used to get configuration
	 * data. This class allows the user to treat both as the same for the
	 * purposes of reading.
	 */
	private class ConfigFile extends WatchedFile<Setup> {

		/**
		 * Constructor for ConfigFile.
		 *
		 * @param newFile
		 *            The file that contains the configuration data. Cannot be
		 *            null.
		 */
		private ConfigFile(File newFile) {
			super(newFile);
		}

		/**
		 * Constructor for ConfigFile.
		 *
		 * @param newURL
		 *            The URL that points to a resource that contains the
		 *            configuration data. Cannot be null.
		 */
		private ConfigFile(URL newURL) {
			super(newURL);
		}

		/**
		 * Loads the configuration from the file. When loading definitions for
		 * validators, if a validator with a name has already been loaded, that
		 * valiator's definition will not be overwritten by this method.
		 */
		public void syncWithSystem() {
			if (Debug.CONFIG.isActive()) {
				Debug.CONFIG.log("Reloading ConfigurationManager configuration file.");
			}

			try {
				JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
				Unmarshaller unmarshaller = jc.createUnmarshaller();
				SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				try {
					Bundle bundle = Activator.getDefault().getBundle();
					URL xsdURL = FileLocator.find(bundle, new Path("data\\validatorconfig.xsd"), null);

					Schema schema = sf.newSchema(xsdURL);
					unmarshaller.setSchema(schema);
				} catch (SAXException e1) {
					Debug.CONFIG.log(e1);
				}

				StreamSource source = new StreamSource(openInputStream());
				Class<Setup> setupClass = Setup.class;
				JAXBElement<Setup> unmarshalledRoot = unmarshaller.unmarshal(source, setupClass);
				Setup setup = unmarshalledRoot.getValue();
				setLoadedObject(setup);

				for (ValidatorDef validatorDef : setup.getValidator()) {
					if (validatorNameToValidatorFactoryMap.containsKey(validatorDef.getName())) {
						// do not replace factories that were setup in previous
						// configuration files
						continue;
					}

					ValidatorFactory factory = getFactoryForValidatorName(validatorDef);
					if (factory != null) {
						try {
							validatorNameToValidatorFactoryMap.put(validatorDef.getName(), factory);
							factory.init();
							validatorFactoryToLevelsMap.put(factory, validatorDef.getLevel());
						} catch (IllegalArgumentException e) {
							Debug.CONFIG.log(e);
						}
					}
				}

			} catch (JAXBException e) {
				Debug.CONFIG.log(e);
			} catch (FileNotFoundException e) {
				if (Debug.CONFIG.isActive()) {
					Debug.CONFIG.log(e);
				}

				// localvalidatorconfig.xml wasn't found
				setLoadedObject(new Setup());
			} catch (IOException e) {
				Debug.CONFIG.log(e);
			}
		}

		/**
		 * Finds the first configuration that matches the given scope and name.
		 * Since scope names are specified using regular expressions, there may
		 * be more than one match for the given name, but only the first match
		 * will be returned. This means that configurations that with the same
		 * scope that occur first in the file have a higher precedence than
		 * configurations that occur later in the file.
		 *
		 * @param scope
		 *            The indicator of which object is used for the name. Cannot
		 *            be null.
		 * @param name
		 *            The name that will be matched against the scope to
		 *            determine a match. Cannot be null, but may be empty.
		 *
		 * @return The configuration object for the given scope and name. Will
		 *         be null if no such configuration exists.
		 */
		private Config findFirstConfig(ScopeEnum scope, String name) {
			Config realConfig = null;

			if (getLoadedObject() != null) {
				// the setup was loaded
				for (Config config : getLoadedObject().getConfig()) {
					if (config.getScope() == scope) {
						Pattern pattern = getPattern(config.getName());
						if (pattern.matcher(name).matches()) {
							realConfig = config;
							break;
						}
					}
				}
			}

			return realConfig;
		}
	}

	/**
	 * ScopeComparator orders scopes from highest ordinal value to lowest
	 * ordinal value. This is used for overriding configurations.
	 */
	private class ScopeComparator implements Comparator<ScopeEnum> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(ScopeEnum object1, ScopeEnum object2) {
			return object2.ordinal() - object1.ordinal();
		}

	}

}
