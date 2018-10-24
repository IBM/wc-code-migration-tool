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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemType;
import com.ibm.commerce.dependency.task.ChainTask;
import com.ibm.commerce.dependency.task.Task;

/**
 * This class creates tasks to create packages and classes and their
 * dependencies that exist in a JAR file.
 * 
 * @author Trent Hoeppner
 */
public class LoadJARProjectPackagesAndClassesTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(
			Arrays.asList(Name.PROJECT_NAME, Name.FILE, Name.IS_EXTRACTING_API, Name.PROJECT_DEPENDENCIES_LOADED));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(
			Arrays.asList(Name.ECLIPSE_PACKAGE_CLASS_LOADER_TASKS_ADDED));

	/**
	 * A pattern which identifies class names for types declared inside methods.
	 */
	private static final Pattern METHOD_DECL_TYPE_PATTERN = Pattern.compile("$\\d");

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadJARProjectPackagesAndClassesTask(String name, LoadingContext context) {
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
		File jarFile = context.get(Name.FILE);
		boolean isExtractingAPI = context.get(Name.IS_EXTRACTING_API);
		Boolean projectDependenciesLoaded = context.get(Name.PROJECT_DEPENDENCIES_LOADED);

		if (projectDependenciesLoaded == null || !projectDependenciesLoaded.booleanValue()) {
			System.out.println("Skipping JAR: Did not find MANIFEST.MF in zip file " + jarFile.getAbsolutePath());
			context.put(Name.JAR_PACKAGE_CLASS_LOADER_TASKS_ADDED, false);
			return;
		}

		JavaItem project = context.getIndex().findItem(null, projectName, JavaItemType.PROJECT);

		// create all packages
		LoadingContext loadPackagesContext = context.forNewTaskGroup();
		loadPackagesContext.put(Name.FILE, jarFile);
		loadPackagesContext.put(Name.ZIP_ENTRY_RUNNABLE, new LoadPackages(project));
		loadPackagesContext.addTask(new RunCodeForZipEntriesTask("RunCodeForZipEntries", loadPackagesContext),
				Priority.LOADING_PACKAGES);

		// find all class names and add the JAR to the class loader
		LoadingContext loadBinaryClassesContext = context.forNewTaskGroup();
		loadBinaryClassesContext.put(Name.FILE, jarFile);
		loadBinaryClassesContext.put(Name.ZIP_ENTRY_RUNNABLE, new FindClassNamesAndCreateClasses(project));
		loadBinaryClassesContext.addTask(new RunCodeForZipEntriesTask("RunCodeForZipEntries", loadBinaryClassesContext),
				Priority.LOADING_CLASSES);

		Boolean binary = (Boolean) project.getAttribute(JavaItem.ATTR_BINARY);
		if (binary) {
			LoadingContext addToClassLoaderContext = context.forNewTaskGroup();
			addToClassLoaderContext.put(Name.FILE, jarFile);
			AddJARToClassLoaderTask addToClassLoaderTask = new AddJARToClassLoaderTask("AddJARToClassLoader",
					addToClassLoaderContext);
			addToClassLoaderContext.addTask(addToClassLoaderTask, Priority.PREPARE_FOR_CLASSLOADING);

			// skip class dependencies for binary files

			// load methods
			LoadingContext loadMethodsContext = context.forNewTaskGroup();
			loadMethodsContext.put(Name.FILE, jarFile);
			loadMethodsContext.put(Name.ZIP_ENTRY_RUNNABLE, new LoadBinaryMethods(project));
			loadMethodsContext.addTask(new RunCodeForZipEntriesTask("RunCodeForZipEntries", loadMethodsContext),
					Priority.LOADING_METHODS);

		} else {
			// load compilation units for methods and dependencies
			LoadingContext loadCompUnitsContext = context.forNewTaskGroup();
			loadCompUnitsContext.put(Name.FILE, jarFile);
			loadCompUnitsContext.put(Name.ZIP_ENTRY_RUNNABLE, new LoadCompilationUnits(project));
			loadCompUnitsContext.addTask(new RunCodeForZipEntriesTask("RunCodeForZipEntries", loadCompUnitsContext),
					Priority.LOADING_COMP_UNITS);

			// load class dependencies
			LoadingContext loadClassDependenciesContext = context.forNewTaskGroup();
			loadClassDependenciesContext.put(Name.OTHER_CONTEXT, loadCompUnitsContext);
			loadClassDependenciesContext.put(Name.OTHER_CONTEXT_NAME, Name.MAP);
			loadClassDependenciesContext.put(Name.MAP_RUNNABLE, new LoadJARSourceClassDependencies());
			ChainTask<LoadingContext> loadClassDependenciesChain = new ChainTask<>("loadClassDependencies",
					loadClassDependenciesContext);
			loadClassDependenciesChain
					.addTask(new AddFromOtherContextTask("AddFromOtherContext", loadClassDependenciesContext));
			loadClassDependenciesChain.addTask(new RunCodeForMapTask("RunCodeForMap", loadClassDependenciesContext));
			loadClassDependenciesContext.addTask(loadClassDependenciesChain, Priority.LOADING_CLASS_DEPENDENCIES);

			// load methods
			LoadingContext loadMethodsContext = context.forNewTaskGroup();
			loadMethodsContext.put(Name.OTHER_CONTEXT, loadCompUnitsContext);
			loadMethodsContext.put(Name.OTHER_CONTEXT_NAME, Name.MAP);
			loadMethodsContext.put(Name.MAP_RUNNABLE, new LoadSourceMethods());
			ChainTask<LoadingContext> loadMethodsChain = new ChainTask<>("LoadMethods", loadMethodsContext);
			loadMethodsChain.addTask(new AddFromOtherContextTask("AddFromOtherContext", loadMethodsContext));
			loadMethodsChain.addTask(new RunCodeForMapTask("RunCodeForMap", loadMethodsContext));
			loadMethodsContext.addTask(loadMethodsChain, Priority.LOADING_METHODS);

			if (!isExtractingAPI) {
				// load pseudo methods
				LoadingContext loadPseudoMethodsContext = context.forNewTaskGroup();
				loadPseudoMethodsContext.put(Name.OTHER_CONTEXT, loadCompUnitsContext);
				loadPseudoMethodsContext.put(Name.OTHER_CONTEXT_NAME, Name.MAP);
				loadPseudoMethodsContext.put(Name.MAP_RUNNABLE, new LoadPseudoMethods());
				ChainTask<LoadingContext> loadseudoMethodsChain = new ChainTask<>("LoadPseudoMethods",
						loadPseudoMethodsContext);
				loadseudoMethodsChain
						.addTask(new AddFromOtherContextTask("AddFromOtherContext", loadPseudoMethodsContext));
				loadseudoMethodsChain.addTask(new RunCodeForMapTask("RunCodeForMap", loadPseudoMethodsContext));
				loadPseudoMethodsContext.addTask(loadseudoMethodsChain, Priority.LOADING_PSEUDO_METHODS);

				// load method dependencies
				LoadingContext loadMethodDependenciesContext = context.forNewTaskGroup();
				loadMethodDependenciesContext.put(Name.OTHER_CONTEXT, loadCompUnitsContext);
				loadMethodDependenciesContext.put(Name.OTHER_CONTEXT_NAME, Name.MAP);
				loadMethodDependenciesContext.put(Name.MAP_RUNNABLE, new LoadSourceMethodDependencies());
				ChainTask<LoadingContext> loadMethodDependenciesChain = new ChainTask<>("LoadMethodDependencies",
						loadMethodDependenciesContext);
				loadMethodDependenciesChain
						.addTask(new AddFromOtherContextTask("AddFromOtherContext", loadMethodDependenciesContext));
				loadMethodDependenciesChain
						.addTask(new RunCodeForMapTask("RunCodeForMap", loadMethodDependenciesContext));
				loadMethodDependenciesContext.addTask(loadMethodDependenciesChain,
						Priority.LOADING_METHOD_DEPENDENCIES);
			}
		}

		context.put(Name.JAR_PACKAGE_CLASS_LOADER_TASKS_ADDED, true);
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
	private boolean isFileForPackage(String filename, boolean binary) {
		String lowerCase = filename.toLowerCase();
		return !binary && lowerCase.endsWith(".java")
				|| binary && lowerCase.endsWith(".class") && !METHOD_DECL_TYPE_PATTERN.matcher(lowerCase).find();
	}

	/**
	 * Creates a dot-separated package name from the given ZipEntry name.
	 * 
	 * @param zipEntryName
	 *            The name from a ZipEntry which the package name will be
	 *            extracted from. This value cannot be null.
	 * 
	 * @return The package name. This value will not be null or empty.
	 */
	private String getPackageName(String zipEntryName) {
		// remove the filename from the path
		zipEntryName = zipEntryName.replace('\\', '/');

		String packageName;
		int lastSlashIndex = zipEntryName.lastIndexOf('/');
		if (lastSlashIndex >= 0) {
			packageName = zipEntryName.substring(0, lastSlashIndex);
		} else {
			packageName = zipEntryName;
		}

		packageName = packageName.replace('/', '.');
		return packageName;
	}

	/**
	 * Creates a class name from the given ZipEntry name.
	 * 
	 * @param zipEntryName
	 *            The name from a ZipEntry which the class name will be
	 *            extracted from. This value cannot be null.
	 * @param binary
	 *            True indicates that this is for a binary (.class) file, false
	 *            indicates that it is for a source (.java) file.
	 * 
	 * @return The class name. This value will not be null or empty.
	 */
	private String getClassName(String zipEntryName, boolean binary) {
		// remove the filename from the path
		zipEntryName = zipEntryName.replace('\\', '/');

		String className;
		int lastSlashIndex = zipEntryName.lastIndexOf('/');
		if (lastSlashIndex >= 0) {
			className = zipEntryName.substring(lastSlashIndex + 1);
		} else {
			className = zipEntryName;
		}

		int trimSize;
		if (binary) {
			trimSize = ".class".length();
		} else {
			trimSize = ".java".length();
		}
		className = className.substring(0, className.length() - trimSize);

		return className;
	}

	/**
	 * Adds a method in the given class with the given name and parameters.
	 * 
	 * @param javaClass
	 *            The class item which will contain the method. This value
	 *            cannot be null.
	 * @param name
	 *            The name of the method. This value cannot be null or empty.
	 * @param methodParameters
	 *            The parameters of the method, which will be converted to
	 *            JavaItems. This value cannot be null, but may be empty.
	 * @param returnType
	 *            The type of the return value, null if it is void.
	 * @param modifiers
	 *            The modifiers from the Eclipse method node. If the modifiers
	 *            indicate a private or default method, the method will not be
	 *            created.
	 */
	private void addMethod(JavaItem javaClass, String name, Parameter[] methodParameters, Class<?> returnType,
			int modifiers) {
		if ((modifiers & Modifier.PUBLIC) > 0 || (modifiers & Modifier.PROTECTED) > 0) {
			List<Integer> parameterIDs = new ArrayList<>();
			for (Parameter parameter : methodParameters) {
				Class<?> parameterType = parameter.getType();
				JavaItem parameterTypeItem = findClassForParameter(javaClass, parameterType);
				if (parameterTypeItem == null) {
					parameterTypeItem = getContext().getUtil().getWildcardType();
				}
				Check.notNull(parameterTypeItem, "paramTypeItem");
				parameterIDs.add(parameterTypeItem.getID());
			}

			JavaItem returnTypeItem = null;
			if (returnType != null) {
				returnTypeItem = findClassForParameter(javaClass, returnType);
				if (returnTypeItem == null) {
					returnTypeItem = getContext().getUtil().getWildcardType();
				}
			}

			Integer returnTypeID = null;
			if (returnTypeItem != null) {
				returnTypeID = returnTypeItem.getID();
			}

			JavaItem javaMethod = getContext().getFactory().createMethod(javaClass, name, parameterIDs);
			javaMethod.setAttribute(JavaItem.ATTR_RETURN_TYPE, returnTypeID);
			javaClass.getChildrenIDs().add(javaMethod.getID());
		}
	}

	/**
	 * Finds a class item for the given Class object.
	 * 
	 * @param javaClass
	 *            The class which has dependencies to search. This value cannot
	 *            be null.
	 * @param parameterType
	 *            The Class object to find the matching class item for. This
	 *            value cannot be null.
	 * 
	 * @return The matching class item. This value will be null if a class item
	 *         could not be found.
	 */
	private JavaItem findClassForParameter(JavaItem javaClass, Class<?> parameterType) {
		String fullName = parameterType.getCanonicalName();

		JavaItem parameterTypeItem;
		if (fullName != null) {
			parameterTypeItem = getContext().getUtil().findClassForName(fullName, javaClass);
		} else {
			parameterTypeItem = null;
		}
		return parameterTypeItem;
	}

	/**
	 * Loads from the given stream and returns its contents as a string. The
	 * input stream will be closed after this method exits.
	 *
	 * @param in
	 *            The stream with the file content to load. Cannot be null.
	 *
	 * @return The contents of the stream. Will not be null, but may be empty.
	 *
	 * @throws IOException
	 *             If there was an error reading the stream.
	 */
	private String loadContent(InputStream in) throws IOException {
		StringBuffer buf = new StringBuffer();

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in));
			int nextChar = reader.read();
			while (nextChar > -1) {
				buf.append((char) nextChar);
				nextChar = reader.read();
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// we don't want to interrupt the flow if it fails to close
					e.printStackTrace();
				}
			}
		}

		return buf.toString();
	}

	/**
	 * This class loads a package for each ZipEntry in a ZipFile.
	 */
	public class LoadPackages implements ZipEntryRunnable {

		/**
		 * The project to which the packages belong.
		 */
		private JavaItem project;

		/**
		 * The set of packages that was created.
		 */
		private Set<JavaItem> packagesCreated = new LinkedHashSet<>();

		/**
		 * Constructor for this.
		 * 
		 * @param project
		 *            The project to which the packages belong. This value
		 *            cannot be null.
		 */
		public LoadPackages(JavaItem project) {
			this.project = project;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean run(ZipFile zipFile, ZipEntry entry) {
			String entryName = entry.getName();
			Boolean binary = project.getAttribute(JavaItem.ATTR_BINARY);
			if (isFileForPackage(entryName, binary)) {
				String packageName = getPackageName(entryName);
				JavaItem packageItem = getContext().getFactory().createPackage(project, packageName);
				packagesCreated.add(packageItem);
			}

			// don't stop
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addProperties(LoadingContext context) {
			context.put(Name.PACKAGES, packagesCreated);
		}

	}

	/**
	 * This class creates class names from the .class ZipEntries in a ZipFile.
	 */
	public class FindClassNamesAndCreateClasses implements ZipEntryRunnable {

		/**
		 * The project to which the classes belong.
		 */
		private JavaItem project;

		/**
		 * The class names that are found.
		 */
		private Set<String> classNames = new LinkedHashSet<>();

		/**
		 * Constructor for this.
		 * 
		 * @param project
		 *            The project to which the classes belong. This value cannot
		 *            be null.
		 */
		public FindClassNamesAndCreateClasses(JavaItem project) {
			this.project = project;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean run(ZipFile zipFile, ZipEntry entry) {
			String entryName = entry.getName();

			// always get the binary class names, so we can create the nested
			// classes without creating a compilation unit if the JAR has source
			// code
			if (isFileForPackage(entryName, true)) {
				String packageName = getPackageName(entryName);
				JavaItem packageItem = getContext().getIndex().findItem(project, packageName, JavaItemType.PACKAGE);

				String className = getClassName(entryName, true);
				getContext().getFactory().createClass(packageItem, className);

				String fullClassName = packageName + "." + className;
				classNames.add(fullClassName);
			}

			// don't stop
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addProperties(LoadingContext context) {
			context.put(Name.FULL_CLASS_NAMES, classNames);
		}

	}

	/**
	 * This class loads methods from .class ZipEntries in a ZipFile.
	 */
	public class LoadBinaryMethods implements ZipEntryRunnable {

		/**
		 * The project to which the methods belong.
		 */
		private JavaItem project;

		/**
		 * Constructor for this.
		 * 
		 * @param project
		 *            The project to which the methods belong. This value cannot
		 *            be null.
		 */
		public LoadBinaryMethods(JavaItem project) {
			this.project = project;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean run(ZipFile zipFile, ZipEntry entry) {
			String entryName = entry.getName();
			if (isFileForPackage(entryName, true)) {
				String packageName = getPackageName(entryName);
				JavaItem packageItem = getContext().getIndex().findItem(project, packageName, JavaItemType.PACKAGE);

				String className = getClassName(entryName, true);
				JavaItem classItem = getContext().getIndex().findItem(packageItem, className, JavaItemType.CLASS);

				String fullClassName = packageName + "." + className;
				try {
					Class<?> type = getContext().getUtil().classForName(fullClassName);
					addBinaryMethods(classItem, type);
					for (Class<?> nested : type.getDeclaredClasses()) {
						if (!nested.isAnonymousClass()) {
							String nestedClassName = className + "$" + nested.getSimpleName();
							JavaItem nestedClassItem = getContext().getIndex().findItem(packageItem, nestedClassName,
									JavaItemType.CLASS);
							if (nestedClassItem != null) {
								addBinaryMethods(nestedClassItem, nested);
							}
						}
					}
				} catch (ClassNotFoundException e) {
					System.out.println("Could not classload " + fullClassName + " to get methods");
				} catch (NoClassDefFoundError e) {
					// System.out.println("Could not classload methods for " +
					// fullClassName + " due to missing library");
				} catch (IncompatibleClassChangeError e) {
					System.out.println(
							"Could not classload methods for " + fullClassName + " due to class incompatibilty");
				} catch (VerifyError e) {
					System.out.println(
							"Could not classload methods for " + fullClassName + " due to class verified incorrectly");
				} catch (IllegalArgumentException e) {
					// was for an inner class, ignore because we load the inner
					// classes when we load the main class
				}

			}

			// don't stop
			return false;
		}

		/**
		 * Adds the methods and constructors from the given class to the given
		 * class item.
		 * 
		 * @param classItem
		 *            The class item to add the methods to. This value cannot be
		 *            null.
		 * @param type
		 *            The class to get the methods from. This value cannot be
		 *            null.
		 */
		private void addBinaryMethods(JavaItem classItem, Class<?> type) {
			for (Method method : type.getDeclaredMethods()) {
				String name = method.getName();
				Parameter[] parameters = method.getParameters();
				Class<?> returnType = method.getReturnType();
				if (returnType.equals(Void.TYPE)) {
					returnType = null;
				}
				int modifiers = method.getModifiers();
				addMethod(classItem, name, parameters, returnType, modifiers);
			}

			for (Constructor<?> constructor : type.getConstructors()) {
				String name = classItem.getName();
				Parameter[] parameters = constructor.getParameters();
				int modifiers = constructor.getModifiers();
				addMethod(classItem, name, parameters, null, modifiers);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addProperties(LoadingContext context) {
			// do nothing
		}

	}

	/**
	 * This class loads the class dependencies using the source .java ZipEntries
	 * in a ZipFile.
	 */
	public class LoadJARSourceClassDependencies implements MapRunnable<CompilationUnit, JavaItem> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean run(CompilationUnit compUnit, JavaItem packageItem) {
			// create the class dependencies
			LoadingContext loadClassDependenciesContext = getContext().forNewTaskGroup();
			loadClassDependenciesContext.put(Name.PACKAGE_ITEM, packageItem);
			loadClassDependenciesContext.put(Name.JAVA_COMPILATION_UNIT, compUnit);
			getContext().addTask(new LoadJavaSourceClassDependenciesTask("LoadJavaSourceClassDependencies",
					loadClassDependenciesContext), Priority.LOADING_CLASS_DEPENDENCIES);

			// don't stop
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addProperties(LoadingContext context) {
			// do nothing
		}

	}

	/**
	 * This class loads the compilation units from .java ZipEntries in a
	 * ZipFile.
	 */
	public class LoadCompilationUnits implements ZipEntryRunnable {

		/**
		 * The project to which the methods belong.
		 */
		private JavaItem project;

		/**
		 * A mapping from compilation units to package items.
		 */
		private Map<CompilationUnit, JavaItem> compilationUnits = new ConcurrentHashMap<>();

		/**
		 * Constructor for this.
		 * 
		 * @param project
		 *            The project to which the methods belong. This value cannot
		 *            be null.
		 */
		public LoadCompilationUnits(JavaItem project) {
			this.project = project;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean run(ZipFile zipFile, ZipEntry entry) throws IOException {
			String entryName = entry.getName();
			if (isFileForPackage(entryName, false)) {
				String packageName = getPackageName(entryName);
				JavaItem packageItem = getContext().getIndex().findItem(project, packageName, JavaItemType.PACKAGE);

				if (packageItem == null) {
					return false;
				}

				InputStream in = zipFile.getInputStream(entry);
				String textContent = loadContent(in);

				// create the methods
				LoadingContext loadCompUnitContext = getContext().forNewTaskGroup();
				loadCompUnitContext.put(Name.PACKAGE_ITEM, packageItem);
				loadCompUnitContext.put(Name.TEXT_CONTENT, textContent);
				loadCompUnitContext.put(Name.MAP_KEY_NAME, Name.JAVA_COMPILATION_UNIT);
				loadCompUnitContext.put(Name.MAP_VALUE_NAME, Name.PACKAGE_ITEM);
				loadCompUnitContext.put(Name.MAP, compilationUnits);
				loadCompUnitContext.put(Name.ZIP_ENTRY_NAME, entryName);
				ChainTask<LoadingContext> loadCompUnitChain = new ChainTask<>("LoadCompUnit", loadCompUnitContext);
				loadCompUnitChain
						.addTask(new LoadJavaCompilationUnitTask("LoadJavaCompilationUnit", loadCompUnitContext));
				loadCompUnitChain.addTask(new AddToMapTask("AddToMap", loadCompUnitContext));
				loadCompUnitContext.addTask(loadCompUnitChain, Priority.LOADING_COMP_UNITS);
			}

			// don't stop
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addProperties(LoadingContext context) {
			context.put(Name.MAP, compilationUnits);
		}

	}

	/**
	 * This class loads methods from compilation units in a collection.
	 */
	public class LoadSourceMethods implements MapRunnable<CompilationUnit, JavaItem> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean run(CompilationUnit compUnit, JavaItem packageItem) {
			// create the methods
			LoadingContext loadMethodsContext = getContext().forNewTaskGroup();
			loadMethodsContext.put(Name.PACKAGE_ITEM, packageItem);
			loadMethodsContext.put(Name.JAVA_COMPILATION_UNIT, compUnit);
			getContext().addTask(new LoadJavaSourceMethodsTask("LoadJavaSourceMethods", loadMethodsContext),
					Priority.LOADING_METHODS);

			// don't stop
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addProperties(LoadingContext context) {
			// do nothing
		}

	}

	/**
	 * This class loads pseudo methods (invoked methods that are not in a found
	 * class) from compilation units in a collection.
	 */
	public class LoadPseudoMethods implements MapRunnable<CompilationUnit, JavaItem> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean run(CompilationUnit compUnit, JavaItem packageItem) {
			// create the methods
			LoadingContext loadPseudoMethodsContext = getContext().forNewTaskGroup();
			loadPseudoMethodsContext.put(Name.PACKAGE_ITEM, packageItem);
			loadPseudoMethodsContext.put(Name.JAVA_COMPILATION_UNIT, compUnit);
			loadPseudoMethodsContext.put(Name.CREATE_DEPENDENT_METHOD_ITEMS, true);
			getContext().addTask(
					new LoadJavaSourceMethodDependenciesTask("LoadJavaSourcePseudoMethods", loadPseudoMethodsContext),
					Priority.LOADING_PSEUDO_METHODS);

			// don't stop
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addProperties(LoadingContext context) {
			// do nothing
		}

	}

	/**
	 * This class loads method dependencies from from compilation units in a
	 * collection.
	 */
	public class LoadSourceMethodDependencies implements MapRunnable<CompilationUnit, JavaItem> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean run(CompilationUnit compUnit, JavaItem packageItem) {
			// create the methods
			LoadingContext loadMethodsContext = getContext().forNewTaskGroup();
			loadMethodsContext.put(Name.PACKAGE_ITEM, packageItem);
			loadMethodsContext.put(Name.JAVA_COMPILATION_UNIT, compUnit);
			loadMethodsContext.put(Name.CREATE_DEPENDENT_METHOD_ITEMS, false);
			getContext().addTask(
					new LoadJavaSourceMethodDependenciesTask("LoadJavaSourceMethodDependencies", loadMethodsContext),
					Priority.LOADING_METHOD_DEPENDENCIES);

			// don't stop
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addProperties(LoadingContext context) {
			// do nothing
		}

	}

}
