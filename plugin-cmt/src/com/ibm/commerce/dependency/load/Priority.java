package com.ibm.commerce.dependency.load;

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
 * This interface defines constants that are priorities for various phases of
 * the loading process.
 * 
 * @author Trent Hoeppner
 */
public interface Priority {

	/**
	 * The priority for the tasks that are first given to the task list to start
	 * the loading process.
	 */
	int TOP_LEVEL = 100;

	/**
	 * The priority for loading projects.
	 */
	int LOADING_PROJECTS = 90;

	/**
	 * The priority for loading project dependencies.
	 */
	int LOADING_PROJECT_DEPENDENCIES = 80;

	/**
	 * The priority for loading packages from JARs.
	 */
	int LOADING_PACKAGES = 70;

	/**
	 * The priority for creating tasks to load classes, class dependencies,
	 * methods, and method dependencies.
	 */
	int CREATING_TASKS_FOR_LOADING_CLASSES = 60;

	/**
	 * The priority for loading classes.
	 */
	int LOADING_CLASSES = 50;

	/**
	 * The priority for preparing a binary JAR to be loaded by ClassLoader.
	 */
	int PREPARE_FOR_CLASSLOADING = 45;

	/**
	 * The priority for loading Java compilation units, to be used when loading
	 * methods and method dependencies.
	 */
	int LOADING_COMP_UNITS = 43;

	/**
	 * The priority for loading class dependencies.
	 */
	int LOADING_CLASS_DEPENDENCIES = 40;

	/**
	 * The priority for loading methods.
	 */
	int LOADING_METHODS = 30;

	/**
	 * The priority for loading pseudo methods - methods that are invoked but
	 * the class file for them is not found so they would not be loaded by the
	 * LOADING_METHODS group of tasks.
	 */
	int LOADING_PSEUDO_METHODS = 25;

	/**
	 * The priority for loading method dependencies.
	 */
	int LOADING_METHOD_DEPENDENCIES = 20;

}
