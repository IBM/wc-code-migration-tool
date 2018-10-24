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

/**
 * ModelFactory implementations are capable of generating models of a specific
 * type.
 *
 * @param <T>
 *            The type of model produced.
 * 
 * @author Trent Hoeppner
 */
public interface ModelFactory<T extends Model> {

	/**
	 * Creates a model of a specific type for the given resource. This will
	 * always create a new instance. To avoid two instances of the same model
	 * for the same resource, use
	 * {@link ModelRegistry#getModel(String, ValidatorResource)} to get
	 * instances instead.
	 *
	 * @param resource
	 *            The resource to create the model for. Cannot be null.
	 *
	 * @return The created model. Will not be null.
	 */
	public T createModel(ValidatorResource resource);
}
