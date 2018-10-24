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
 * This interface declares the constants used to define constraints/variables in
 * Tasks for this package.
 * 
 * @author Trent Hoeppner
 */
public interface Name {

	/**
	 * Variable for a Java source file, of type File.
	 */
	String JAVA_SOURCE_FILE = "JavaSourceFile";

	/**
	 * Variable for a Java compilation unit, of type CompilationUnit.
	 */
	String JAVA_COMPILATION_UNIT = "JavaCompilationUnit";

	/**
	 * Variable to indicate that the methods of a class have been loaded, of
	 * type Boolean.
	 */
	String METHODS_LOADED = "MethodsLoaded";

	/**
	 * Variable for a class item, of type JavaItem.
	 */
	String CLASS_JAVAITEM = "ClassJavaItem";

	/**
	 * Variable for an Eclipse workspace directory, of type File.
	 */
	String WORKSPACE_DIR = "WorkspaceDir";

	/**
	 * Variable for a filename filter, of type FileFilter.
	 */
	String FILTER = "Filter";

	/**
	 * Variable for a list of Eclipse project directories, of type
	 * List&lt;File&gt;.
	 */
	String ECLIPSE_PROJECT_DIRS = "EclipseProjectDirs";

	/**
	 * Variable to indicate that the tasks to load Eclipse projects and their
	 * manifest files have been added to the TaskList, of type Boolean.
	 */
	String ECLIPSE_PROJECT_LOADER_TASKS_ADDED = "EclipseProjectLoaderTasksAdded";

	/**
	 * Variable for the name of a project, of type String.
	 */
	String PROJECT_NAME = "ProjectName";

	/**
	 * Variable for the manifest file of a project, of type File.
	 */
	String PROJECT_MANIFEST_FILE = "ProjectManifestFile";

	/**
	 * Variable to indicate that a project has been loaded, of type Boolean.
	 */
	String PROJECT_LOADED = "ProjectLoaded";

	/**
	 * Variable to indicate whether the purpose is to extract an API for a set
	 * of projects, of type Boolean.
	 */
	String IS_EXTRACTING_API = "IsExtractingAPI";

	/**
	 * Variable to indicate that the dependencies for a project have been
	 * loaded, of type Boolean.
	 */
	String PROJECT_DEPENDENCIES_LOADED = "ProjectDependenciesLoaded";

	/**
	 * Variable for the directories to search for JARs in, of type
	 * Set&lt;File&gt;.
	 */
	String JAR_DIRECTORIES = "JARDirectories";

	/**
	 * Variable for the JARs that can be made into Projects, of type
	 * Set&lt;File&gt;.
	 */
	String PROJECT_JARS = "ProjectJARs";

	/**
	 * Variable to indicate that the tasks to load JAR projects and their
	 * manifest files have been added to the TaskList, of type Boolean.
	 */
	String JAR_PROJECT_LOADER_TASKS_ADDED = "JARProjectLoaderTasksAdded";

	/**
	 * Variable for a JAR that will be made into a project item, of type File.
	 */
	String PROJECT_JAR_FILE = "ProjectJARFile";

	/**
	 * Variable for an input stream, of type InputStream.
	 */
	String INPUT_STREAM = "InputStream";

	/**
	 * Variable for text content of a file loaded from somewhere, of type
	 * String.
	 */
	String TEXT_CONTENT = "TextContent";

	/**
	 * Variable for a file, of type File.
	 */
	String FILE = "File";

	/**
	 * Variable for a ZIP entry in a ZIP file, of type ZipEntry.
	 */
	String ZIP_ENTRY = "ZipEntry";

	/**
	 * Variable for a ZIP file, of type ZipFile.
	 */
	String ZIP_FILE = "ZipFile";

	/**
	 * Variable to indicate that a ZIP file has been closed, of type Boolean.
	 */
	String ZIP_CLOSED = "ZipClosed";

	/**
	 * Variable to indicate the name of a ZIP entry to find, of type String.
	 */
	String ZIP_ENTRY_NAME = "ZipEntryName";

	/**
	 * Variable to indicate whether a project is binary (most JAR files have
	 * only class files in them), of type Boolean.
	 */
	String IS_BINARY = "IsBinary";

	/**
	 * Variable for an Eclipse project directory, of type File.
	 */
	String PROJECT_DIR = "ProjectDir";

	/**
	 * Variable to indicate that the tasks for loading the packages and classes
	 * of an Eclipse project have been added to the unscheduled list, of type
	 * Boolean.
	 */
	String ECLIPSE_PACKAGE_CLASS_LOADER_TASKS_ADDED = "EclipsePackageClassLoaderTasksAdded";

	/**
	 * Variable for a package item, of type JavaItem.
	 */
	String PACKAGE_ITEM = "PackageItem";

	/**
	 * Variable to indicate that a class has been loaded, of type Boolean.
	 */
	String CLASS_LOADED = "ClassLoaded";

	/**
	 * Variable for the name of a class (without package), of type String.
	 */
	String CLASS_NAME = "ClassName";

	/**
	 * Variable to indicate that the class dependencies for a source file have
	 * been loaded, of type Boolean.
	 */
	String CLASS_DEPENDENCIES_LOADED = "ClassDependenciesLoaded";

	/**
	 * Variable for a class which can be used to process ZIP entries from a ZIP
	 * file, of type ZipEntryRunnable.
	 */
	String ZIP_ENTRY_RUNNABLE = "ZipEntryRunnable";

	/**
	 * Variable for a set of packages, of type Set&lt;JavaItem&gt;.
	 */
	String PACKAGES = "Packages";

	/**
	 * Variable for a list of fully qualified class names suitable for using
	 * with Class.forName(), of type Set&lt;String&gt;.
	 */
	String FULL_CLASS_NAMES = "FullClassNames";

	/**
	 * Variable to indicate that a JAR has been added to the class loaders in
	 * JavaItemUtil, of type Boolean.
	 */
	String JAR_ADDED_TO_CLASSLOADER = "JARAddedToClassLoader";

	/**
	 * Variable to indicate that a ZipEntryRunnable has finished executing for
	 * all ZIP entries, of type Boolean.
	 */
	String ZIP_ENTRY_RUNNABLE_DONE = "ZipEntryRunnableDone";

	/**
	 * Variable for a Collection object or subclass, of type Collection.
	 */
	String COLLECTION = "Collection";

	/**
	 * Variable for a class which can be used to process elements in a
	 * Collection, of type CollectionRunnable.
	 */
	String COLLECTION_RUNNABLE = "CollectionRunnable";

	/**
	 * Variable to indicate that a CollectionRunnable has finished executing for
	 * all Collection elements, of type Boolean.
	 */
	String COLLECTION_RUNNABLE_DONE = "CollectionRunnableDone";

	/**
	 * Variable to indicate that the tasks for loading the packages and classes
	 * of a JAR file have been added to the unscheduled list, of type Boolean.
	 */
	String JAR_PACKAGE_CLASS_LOADER_TASKS_ADDED = "JARPackageClassLoaderTasksAdded";

	/**
	 * Variable to indicate that the method dependencies for a class file have
	 * been loaded, of type Boolean.
	 */
	String METHOD_DEPENDENCIES_LOADED = "MethodDependenciesLoaded";

	/**
	 * Variable for a map of package items to compilation units in a project, of
	 * type Map&lt;JavaItem, CompilationUnit&gt;.
	 */
	String JAVA_COMPILATION_UNITS = "JavaCompilationUnits";

	/**
	 * Variable for another context, of type TextContext. This is used with
	 * {@link AddFromOtherContextTask} to copy an output variable from one
	 * context to be the input variable of another context.
	 */
	String OTHER_CONTEXT = "OtherContext";

	/**
	 * Variable for a variable name in another context, of type String. This is
	 * used with {@link AddFromOtherContextTask} to copy an output variable from
	 * one context to be the input variable of another context.
	 */
	String OTHER_CONTEXT_NAME = "OtherContextName";

	/**
	 * Variable for a variable name in the current context which will be added
	 * to a map, of type String.
	 */
	String MAP_KEY_NAME = "MapKeyName";

	/**
	 * Variable for a variable name in the current context which is the value to
	 * be added to a map, of type String.
	 */
	String MAP_VALUE_NAME = "MapValueName";

	/**
	 * Variable to indicate that an element was added to a collection, of type
	 * Boolean.
	 */
	String ADDED_TO_COLLECTION = "AddedToCollection";

	/**
	 * Variable to indicate that a variable from one context was added to
	 * another context, of type Boolean.
	 */
	String ADDED_FROM_OTHER_CONTEXT = "AddedFromOtherContext";

	/**
	 * Variable to indicate that a key/value pair was added to a map, of type
	 * Boolean.
	 */
	String ADDED_TO_MAP = "AddedToMap";

	/**
	 * Variable for a map, of type Map.
	 */
	String MAP = "Map";

	/**
	 * Variable for a class which can be used to process key/value pairs in a
	 * Map, of type MapRunnable.
	 */
	String MAP_RUNNABLE = "MapRunnable";

	/**
	 * Variable to indicate that a MapRunnable has finished executing for all
	 * Map key/value pairs, of type Boolean.
	 */
	String MAP_RUNNABLE_DONE = "MapRunnableDone";

	/**
	 * Variable for the directories that contain JARs or something that may be
	 * directly referenced by product code, of type Set&lt;File&gt;. The
	 * JARs/projects that are not directly referenced by Commerce code will not
	 * be kept in the final loading results.
	 */
	String THIRD_PARTY_DIRECTORIES = "ThirdPartyDirectories";

	/**
	 * Variable for the directories that have been identified as third-party,
	 * which means they are only important as references, of type
	 * Set&lt;File&gt;.
	 */
	String THIRD_PARTY_JARS = "ThirdPartyJARs";

	/**
	 * Variable to indicate that a project and all its children are from a third
	 * party, of type Boolean. If a project is third party, it only needs to be
	 * kept if a product JAR depends on it directly.
	 */
	String IS_THIRD_PARTY = "IsThirdParty";

	/**
	 * Variable to indicate that a project is a workspace project, of type
	 * Boolean.
	 */
	String IS_WORKSPACE = "IsWorkspace";

	/**
	 * Variable for a set of java files, which is used as a filter to only parse
	 * certain files, rather than all files in a workspace, of type
	 * Set&lt;File&gt;.
	 */
	String JAVA_FILES = "JavaFiles";

	/**
	 * Variable to indicate that the
	 * {@link LoadJavaSourceMethodDependenciesTask} should create dependent
	 * method items. If this is false, the task will add the dependencies it
	 * finds instead. The type is Boolean.
	 */
	String CREATE_DEPENDENT_METHOD_ITEMS = "CreateDependentMethodItems";

}
