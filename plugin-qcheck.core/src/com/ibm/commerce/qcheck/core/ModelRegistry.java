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

import java.util.HashMap;
import java.util.Map;

/**
 * ModelRegistry stores the models and resources that use those models in a
 * central location. This allows the models for resources to be retrieved and
 * reused between validators.
 * <p>
 * This class allows for multiple registries and is not a singleton. However, a
 * default registry is provided.
 * 
 * @author Trent Hoeppner
 */
public class ModelRegistry {

	/**
	 * The default instance of this.
	 */
	private static final ModelRegistry DEFAULT = new ModelRegistry();

	/**
	 * A mapping from model names to the factory objects which can generate new
	 * model instances. Will never be null.
	 */
	private Map<String, ModelFactory> modelNameToFactoryMap = new HashMap<String, ModelFactory>();

	/**
	 * A mapping from resource paths to the models for each resource. Will never
	 * be null.
	 */
	private Map<String, Map<String, Model>> resourceToModelsMap = new HashMap<String, Map<String, Model>>();

	/**
	 * Constructor for ModelRegistry.
	 */
	public ModelRegistry() {
		// do nothing
	}

	/**
	 * Returns the default registry.
	 *
	 * @return The default registry. Will not be null.
	 */
	public static ModelRegistry getDefault() {
		return DEFAULT;
	}

	/**
	 * Registers a new factory.
	 *
	 * @param modelName
	 *            The name of the factory. Cannot be null or empty.
	 * @param factory
	 *            The factory to register. Cannot be null.
	 */
	public void register(String modelName, ModelFactory factory) {
		Param.notNullOrEmpty(modelName, "modelName");
		Param.notNull(factory, "factory");

		modelNameToFactoryMap.put(modelName, factory);
	}

	/**
	 * Returns a model for the resource using the given model name. The model
	 * factory for the name must already exist.
	 * <p>
	 * If the model has already been created, it will be returned. If not, it
	 * will be created using the model factory. The model factory may also use a
	 * registry to get required models before returning the desired model.
	 *
	 * @param <T>
	 *            The model type. The model will be cast to this type so the
	 *            user must be aware of the expected type for the given model
	 *            name.
	 * @param modelName
	 *            The name of the model type, which was used to register the
	 *            factory for that model. Cannot be null or empty, and must
	 *            already be registered.
	 * @param resource
	 *            The resource to get a model for. Cannot be null.
	 *
	 * @return The model for the given resource. Will not be null.
	 */
	public <T extends Model> T getModel(String modelName, ValidatorResource resource) {
		Param.notNullOrEmpty(modelName, "modelName");
		Param.notNull(resource, "resource");

		String resourcePath = resource.getFileAsFile().getAbsolutePath();

		Map<String, Model> nameToModelForResourceMap = resourceToModelsMap.get(resourcePath);
		if (nameToModelForResourceMap == null) {
			nameToModelForResourceMap = new HashMap<String, Model>();
			resourceToModelsMap.put(resourcePath, nameToModelForResourceMap);
		}

		Model model = nameToModelForResourceMap.get(modelName);
		if (model == null) {
			model = getFactory(modelName).createModel(resource);
			nameToModelForResourceMap.put(modelName, model);
		}

		return (T) model;
	}

	/**
	 * Returns the factory for the given model name.
	 *
	 * @param modelName
	 *            The name of the model which is used to retrieve the factory.
	 *            Cannot be null, and it must be {@link #isRegistered(String)
	 *            registered}. Cannot be empty.
	 *
	 * @return The factory that can be used to generate instances of the model.
	 *         Will not be null.
	 */
	public ModelFactory getFactory(String modelName) {
		Param.notNullOrEmpty(modelName, "modelName");
		// TODO the return value could be null if the model name is not
		// registered
		ModelFactory factory = modelNameToFactoryMap.get(modelName);
		return factory;
	}

	/**
	 * Removes all models for the given resource.
	 *
	 * @param resource
	 *            The resource that may have models registered. Cannot be null.
	 */
	public void clearValidator(ValidatorResource resource) {
		if (resource == null) {
			throw new NullPointerException("resource cannot be null.");
		}

		String resourcePath = resource.getFileAsFile().getAbsolutePath();
		resourceToModelsMap.remove(resourcePath);
	}

	/**
	 * Returns whether the given model name has a registered factory.
	 *
	 * @param name
	 *            The name of the model type to check for registration. Cannot
	 *            be null or empty.
	 *
	 * @return True if the model name is registered with a factory, false
	 *         otherwise.
	 */
	public boolean isRegistered(String name) {
		return modelNameToFactoryMap.containsKey(name);
	}

}
