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

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemType;
import com.ibm.commerce.dependency.task.ChainTask;
import com.ibm.commerce.dependency.task.Task;

/**
 * This class creates tasks to create packages and classes and their
 * dependencies that exist in a Eclipse project directory.
 * 
 * @author Trent Hoeppner
 */
public class LoadEclipseProjectPackagesAndClassesTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(Arrays.asList(Name.PROJECT_NAME, Name.PROJECT_DIR));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(
			Arrays.asList(Name.ECLIPSE_PACKAGE_CLASS_LOADER_TASKS_ADDED));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadEclipseProjectPackagesAndClassesTask(String name, LoadingContext context) {
		super(name, context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getInputConstraints() {
		return INPUTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getOutputConstraints() {
		return OUTPUTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(LoadingContext context) throws Exception {
		String projectName = context.get(Name.PROJECT_NAME);
		File projectDir = context.get(Name.PROJECT_DIR);
		// optional parameter
		Set<File> javaFiles = context.get(Name.JAVA_FILES);

		Set<String> javaFilePaths = null;
		if (javaFiles != null) {
			javaFilePaths = new LinkedHashSet<>();
			for (File javaFile : javaFiles) {
				javaFilePaths.add(javaFile.getCanonicalPath());
			}
		}

		JavaItem project = context.getIndex().findItem(null, projectName, JavaItemType.PROJECT);

		Map<String, File> packageNameToDirMap = getPackageFragments(projectDir);
		for (String packageName : packageNameToDirMap.keySet()) {
			JavaItem packageItem = context.getFactory().createPackage(project, packageName);
			packageItem.setAttribute(JavaItem.ATTR_BINARY, false);
		}

		for (String packageName : packageNameToDirMap.keySet()) {
			File packageDir = packageNameToDirMap.get(packageName);

			JavaItem packageItem = context.getIndex().findItem(project, packageName, JavaItemType.PACKAGE);

			File[] subFiles = packageDir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					String lower = pathname.getName().toLowerCase();
					return pathname.isFile() && lower.endsWith(".java");
				}

			});

			if (subFiles == null) {
				continue;
			}

			for (File javaFile : subFiles) {
				if (javaFilePaths != null) {
					// we are filtering the files to those specified
					String path = javaFile.getCanonicalPath();
					if (!javaFilePaths.contains(path)) {
						continue;
					}
				}

				// create the classes
				LoadingContext loadClassContext = context.forNewTaskGroup();
				loadClassContext.put(Name.PACKAGE_ITEM, packageItem);
				loadClassContext.put(Name.FILE, javaFile);
				ChainTask<LoadingContext> loadClassesChain = new ChainTask<>("LoadClasses", loadClassContext);
				loadClassesChain.addTask(new FileToInputStreamTask("FileToInputStream", loadClassContext));
				loadClassesChain.addTask(new LoadFromInputStreamTask("LoadFromInputStream", loadClassContext));
				loadClassesChain.addTask(new LoadJavaCompilationUnitTask("LoadJavaCompilationUnit", loadClassContext));
				loadClassesChain.addTask(new LoadJavaSourceClassesTask("LoadJavaSourceClasses", loadClassContext));
				loadClassContext.addTask(loadClassesChain, Priority.LOADING_CLASSES);

				// create the class dependencies
				LoadingContext loadClassDependenciesContext = context.forNewTaskGroup();
				loadClassDependenciesContext.put(Name.PACKAGE_ITEM, packageItem);
				loadClassDependenciesContext.put(Name.FILE, javaFile);
				ChainTask<LoadingContext> loadClassDependenciesChain = new ChainTask<>("LoadClassDependencies",
						loadClassDependenciesContext);
				loadClassDependenciesChain
						.addTask(new FileToInputStreamTask("FileToInputStream", loadClassDependenciesContext));
				loadClassDependenciesChain
						.addTask(new LoadFromInputStreamTask("LoadFromInputStream", loadClassDependenciesContext));
				loadClassDependenciesChain.addTask(
						new LoadJavaCompilationUnitTask("LoadJavaCompilationUnit", loadClassDependenciesContext));
				loadClassDependenciesChain.addTask(new LoadJavaSourceClassDependenciesTask(
						"LoadJavaSourceClassDependencies", loadClassDependenciesContext));
				context.addTask(loadClassDependenciesChain, Priority.LOADING_CLASS_DEPENDENCIES);

				// create the methods
				LoadingContext loadMethodsContext = context.forNewTaskGroup();
				loadMethodsContext.put(Name.PACKAGE_ITEM, packageItem);
				loadMethodsContext.put(Name.FILE, javaFile);
				ChainTask<LoadingContext> loadMethodsChain = new ChainTask<>("LoadMethods", loadMethodsContext);
				loadMethodsChain.addTask(new FileToInputStreamTask("FileToInputStream", loadMethodsContext));
				loadMethodsChain.addTask(new LoadFromInputStreamTask("LoadFromInputStream", loadMethodsContext));
				loadMethodsChain
						.addTask(new LoadJavaCompilationUnitTask("LoadJavaCompilationUnit", loadMethodsContext));
				loadMethodsChain.addTask(new LoadJavaSourceMethodsTask("LoadJavaSourceMethods", loadMethodsContext));
				context.addTask(loadMethodsChain, Priority.LOADING_METHODS);

				// create the pseudo methods - can only be found by looking at
				// methods invoked
				LoadingContext loadPseudoMethodsContext = context.forNewTaskGroup();
				loadPseudoMethodsContext.put(Name.PACKAGE_ITEM, packageItem);
				loadPseudoMethodsContext.put(Name.FILE, javaFile);
				loadPseudoMethodsContext.put(Name.CREATE_DEPENDENT_METHOD_ITEMS, true);
				ChainTask<LoadingContext> loadPseudoMethodsChain = new ChainTask<>("LoadPseudoMethods",
						loadPseudoMethodsContext);
				loadPseudoMethodsChain
						.addTask(new FileToInputStreamTask("FileToInputStream", loadPseudoMethodsContext));
				loadPseudoMethodsChain
						.addTask(new LoadFromInputStreamTask("LoadFromInputStream", loadPseudoMethodsContext));
				loadPseudoMethodsChain
						.addTask(new LoadJavaCompilationUnitTask("LoadJavaCompilationUnit", loadPseudoMethodsContext));
				loadPseudoMethodsChain.addTask(new LoadJavaSourceMethodDependenciesTask(
						"LoadJavaSourceMethodDependencies", loadPseudoMethodsContext));
				context.addTask(loadPseudoMethodsChain, Priority.LOADING_PSEUDO_METHODS);

				// create the method dependencies
				LoadingContext loadMethodDependenciesContext = context.forNewTaskGroup();
				loadMethodDependenciesContext.put(Name.PACKAGE_ITEM, packageItem);
				loadMethodDependenciesContext.put(Name.FILE, javaFile);
				loadMethodDependenciesContext.put(Name.CREATE_DEPENDENT_METHOD_ITEMS, false);
				ChainTask<LoadingContext> loadMethodDependenciesChain = new ChainTask<>("LoadMethodDependencies",
						loadMethodDependenciesContext);
				loadMethodDependenciesChain
						.addTask(new FileToInputStreamTask("FileToInputStream", loadMethodDependenciesContext));
				loadMethodDependenciesChain
						.addTask(new LoadFromInputStreamTask("LoadFromInputStream", loadMethodDependenciesContext));
				loadMethodDependenciesChain.addTask(
						new LoadJavaCompilationUnitTask("LoadJavaCompilationUnit", loadMethodDependenciesContext));
				loadMethodDependenciesChain.addTask(new LoadJavaSourceMethodDependenciesTask(
						"LoadJavaSourceMethodDependencies", loadMethodDependenciesContext));
				context.addTask(loadMethodDependenciesChain, Priority.LOADING_METHOD_DEPENDENCIES);
			}
		}

		context.put(Name.ECLIPSE_PACKAGE_CLASS_LOADER_TASKS_ADDED, true);
	}

	/**
	 * Returns a mapping from package names to each directory. Only directories
	 * that have Java source files will be considered packages.
	 * 
	 * @param dir
	 *            The base directory of the Eclipse project, which contains
	 *            sub-directories with source code. This value cannot be null.
	 * 
	 * @return A mapping from package names to package directories. This value
	 *         will not be null, but may be empty if the project is empty.
	 */
	private Map<String, File> getPackageFragments(File dir) {
		// lazy-load the fragments
		Map<String, File> fragments = new LinkedHashMap<>();
		File[] subDirs = dir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory() && !pathname.getName().startsWith(".")
						&& !pathname.getName().startsWith("META-INF");
			}

		});

		for (File subDir : subDirs) {
			addPackages(subDir, fragments);
		}

		return fragments;
	}

	/**
	 * Adds the packages in the given package base directory to the given map.
	 * Only directories that have Java source files will be considered packages.
	 * 
	 * @param baseDir
	 *            The directory of a default package, which may contain other
	 *            directories that represent packages. This value cannot be
	 *            null.
	 * @param packageFragments
	 *            The mapping from package names to package directories to add
	 *            to. This value cannot be null.
	 */
	private void addPackages(File baseDir, Map<String, File> packageFragments) {
		String baseString = baseDir.getAbsolutePath();
		File[] subFiles = baseDir.listFiles();
		boolean defaultPackageAdded = false;
		for (File subFile : subFiles) {
			if (subFile.isDirectory()) {
				addPackages(baseString, subFile, packageFragments);
			} else if (!defaultPackageAdded && subFile.isFile() && isFileForPackage(subFile)) {
				// only add it if there is a relevant file in it
				packageFragments.put("", baseDir);
				defaultPackageAdded = true;
			}
		}
	}

	/**
	 * Adds the packages in the given package directory to the given map. Only
	 * directories that have Java source files will be considered packages.
	 * 
	 * @param baseString
	 *            The absolute path of the base default package, which is
	 *            removed from the given directory name and converted to a
	 *            package name. This value cannot be null or empty.
	 * @param dir
	 *            The directory of a package which is not the default package,
	 *            which may contain other directories that represent packages.
	 *            This value cannot be null.
	 * @param packageFragments
	 *            The mapping from package names to package directories to add
	 *            to. This value cannot be null.
	 */
	private void addPackages(String baseString, File dir, Map<String, File> fragments) {
		File[] subFiles = dir.listFiles();
		boolean packageAdded = false;
		for (File subFile : subFiles) {
			if (subFile.isDirectory()) {
				addPackages(baseString, subFile, fragments);
			} else if (!packageAdded && subFile.isFile() && isFileForPackage(subFile)) {
				// only add it if there is a relevant file in it
				String packageName = getPackageName(baseString, dir);
				fragments.put(packageName, dir);
				packageAdded = true;
			}
		}
	}

	/**
	 * Returns whether the given file represents a Java file that indicates the
	 * parent directory is considered a package directory.
	 * 
	 * @param file
	 *            The file to identify. This value cannot be null.
	 * 
	 * @return True if the file is a Java file, false otherwise.
	 */
	private boolean isFileForPackage(File file) {
		// in an eclipse project it's not binary
		boolean binary = false;
		String lowerCase = file.getName().toLowerCase();
		return !binary && lowerCase.endsWith(".java") || binary && lowerCase.endsWith(".class");
	}

	/**
	 * Creates a dot-separated package name from the given directory, removing
	 * the given absolute path as a prefix.
	 * 
	 * @param baseString
	 *            The absolute path of the base default package, which is
	 *            removed from the given directory name and converted to a
	 *            package name. This value cannot be null or empty.
	 * @param dir
	 *            The directory of a package which is not the default package,
	 *            which may contain other directories that represent packages.
	 *            This value cannot be null.
	 * 
	 * @return The package name. This value will not be null or empty.
	 */
	private String getPackageName(String baseDirString, File dir) {
		String dirString = dir.getAbsolutePath();
		String pathName;
		if (!dirString.equals(baseDirString)) {
			pathName = dirString.substring(baseDirString.length() + 1);
		} else {
			pathName = "";
		}

		String name = pathName.replaceAll("\\\\|/", ".");
		return name;
	}
}
