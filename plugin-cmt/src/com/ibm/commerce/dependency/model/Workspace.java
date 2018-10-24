package com.ibm.commerce.dependency.model;

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

import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Workspace is used to wrap the project management system. Projects can be
 * retrieved, created, deleted, and files can be moved between them.
 * 
 * @author Trent Hoeppner
 */
public interface Workspace {

	/**
	 * Finds and returns the projects in the current workspace.
	 *
	 * @return The projects that were found and loaded. Will not be null, but
	 *         may be empty.
	 *
	 * @throws IOException
	 *             If there was an error loading the project dependencies.
	 */
	List<JavaItem> getProjects() throws IOException;

	/**
	 * Finds the packages under the given project and adds them to the project.
	 *
	 * @param project
	 *            The project to find packages for. Cannot be null.
	 */
	void findPackages(JavaItem project);

	/**
	 * Finds classes for the given package and adds them.
	 * 
	 * @param javaPackage
	 *            The package to find classes for. Cannot be null.
	 */
	void findClasses(JavaItem javaPackage);

	/**
	 * Finds dependencies from the given class to other classes.
	 * 
	 * @param javaClass
	 *            The class to find dependencies for. Cannot be null.
	 */
	void findClassDependencies(JavaItem javaClass);

	/**
	 * Finds dependencies from the given package to other packages.
	 * 
	 * @param javaPackage
	 *            The package to find dependencies for. Cannot be null.
	 */
	void findPackageDependencies(JavaItem javaPackage);

	/**
	 * Finds methods in the given class.
	 * 
	 * @param javaClass
	 *            The class to find the methods for. Cannot be null.
	 */
	void findMethods(JavaItem javaClass);

	/**
	 * Finds methods that are called by methods within the given class and adds
	 * them as dependencies to the calling methods.
	 * 
	 * @param javaClass
	 *            The class that contains methods to find dependencies for.
	 *            Cannot be null.
	 */
	void findMethodDependencies(JavaItem javaClass);

	/**
	 * Finds the nearest node that is a method or initializer that contains the
	 * given node, creates a fake method for that node, and finds the
	 * dependences of that method on other methods. The search for dependent
	 * classes and methods is done using the given class as the base.
	 * 
	 * @param javaClass
	 *            The class to use to find related methods (through class and
	 *            method dependencies in that class). Cannot be null.
	 * @param node
	 *            The node which is contained by a method. Cannot be null.
	 * 
	 * @return The list of methods that are found. This value will not be null,
	 *         but may be empty.
	 */
	List<JavaItem> findNodeDependencies(JavaItem javaClass, ASTNode node);
}
