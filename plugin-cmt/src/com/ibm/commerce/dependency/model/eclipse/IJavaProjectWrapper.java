package com.ibm.commerce.dependency.model.eclipse;

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
import java.util.List;

/**
 * This interface represents a Java project much like a corresponding Eclipse
 * one, but allows non-Eclipse implementations to be used in its place.
 * 
 * @author Trent Hoeppner
 */
public interface IJavaProjectWrapper {

	/**
	 * Returns the name of this project.
	 *
	 * @return The name of this. Will not be null or empty.
	 */
	String getName();

	/**
	 * Returns the manifest file for this project.
	 *
	 * @return The manifest file. May be null if no manifest file exists.
	 */
	File getManifestFile();

	List<IPackageFragmentWrapper> getFragments();

	boolean isBinary();

}