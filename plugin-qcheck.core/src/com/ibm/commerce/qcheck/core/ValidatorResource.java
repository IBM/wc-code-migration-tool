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
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * ValidatorResource represents a file that needs to be validated.
 * 
 * @author Trent Hoeppner
 */
public interface ValidatorResource {

	/**
	 * Returns a list of nodes which match the given type. Cannot be null.
	 *
	 * @param type
	 *            The type of node as defined by constants in {@link ASTNode}.
	 *            Must be a valid constant value.
	 *
	 * @return A new list with the nodes that match the given type. Will not be
	 *         null, but may be empty if there are no results.
	 */
	public List<ASTNode> getTypedNodeList(int type);

	/**
	 * Returns the base name of the file for this resource, not including path
	 * information.
	 *
	 * @return The name of the file without path information. Will not be null.
	 */
	public String getFilename();

	/**
	 * Returns the base directory of the file. For Java files, this includes the
	 * path information before the package directories, if any.
	 *
	 * @return The base directory of this. Will not be null or empty.
	 */
	public String getBaseDir();

	/**
	 * Returns the file path, relative to the {@link #getBaseDir() base
	 * directory}.
	 *
	 * @return The file path relative to the base directory. Will not be null or
	 *         empty.
	 */
	public String getPathFilename();

	/**
	 * Returns the resource wrapped by this as an Eclipse <code>IResource</code>
	 * with an associated project in the workspace.
	 *
	 * @return The resource wrapped by this as an <code>IResource</code>. Will
	 *         not be null.
	 */
	public IResource getFileAsResource();

	/**
	 * Returns the file that the resource represents.
	 *
	 * @return The file that the resource represents. Will not be null.
	 */
	public File getFileAsFile();

	/**
	 * Returns the package name of this resource. This is intended for Java
	 * files, and for other types of files will return nothing.
	 *
	 * @return The package name if this is a Java file, or an empty string if
	 *         this is not a Java file or the Java file has no package. Will not
	 *         be null.
	 */
	public String getPackageName();

	/**
	 * Returns the fully-qualified class name of this resource. This is intended
	 * for Java files, and for other types of files will return nothing.
	 *
	 * @return The class name if this is a Java file including the package, or
	 *         an empty string if this is not a Java file. Will not be null.
	 */
	public String getClassName();

	public ModelRegistry getModelRegistry();

	public List<ModelEnum> getSupportedModels();
}
