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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ExternalClasses represents the classes that another class depends on. This
 * includes imported classes, fully-qualified classes used in the code but not
 * explicitly imported, and classes that are implicitly imported such as
 * <code>java.lang</code> and classes in the current package.
 * 
 * @author Trent Hoeppner
 */
public class ExternalClasses {

	/**
	 * A portion of a package name, excluding dots. Package names are assumed to
	 * have start with a lower-case letter.
	 */
	private static final String PACKAGE_IDENTIFIER_PART = "(([a-z_\\$][\\w\\$]*)|CORBA|CORBA_2_3)";

	/**
	 * A portion of a class name, excluding dots for the nested classes. Class
	 * names are assumed to start with an upper-case letter.
	 */
	private static final String CLASS_IDENTIFIER_PART = "[A-Z_\\$][\\w\\$]*";

	/**
	 * The pattern to parse an import or other name into a package and class
	 * name. Here are the interesting capturing groups:
	 * <ul>
	 * <li>2 - the package name
	 * <li>4 - the class name (including nested classes)
	 * <li>5 - the main class (excluding nested classes)
	 * <li>6 - the dot-separated (.) list of nested classes, starting with an
	 * initial dot
	 * <li>8 - the final nested class in the chain
	 * </ul>
	 * <p>
	 * This pattern assumes that [A-Z_\$][\w\$]* is always a class name and
	 * [a-z_\\$][\\w\\$]* is always an identifier in a package string.
	 */
	private static final Pattern PACKAGE_CLASS_PATTERN = Pattern.compile("((" + PACKAGE_IDENTIFIER_PART + "(\\."
			+ PACKAGE_IDENTIFIER_PART + ")*)\\.)?" + "((" + CLASS_IDENTIFIER_PART + ")((\\." + CLASS_IDENTIFIER_PART
			+ ")*\\.(" + CLASS_IDENTIFIER_PART + "))?)");

	/**
	 * List of classes in <code>java.lang</code> that are always available.
	 */
	private static final String[] JAVA_LANG_CLASSES = new String[] { "AbstractMethodError", "Appendable",
			"ApplicationShutdownHooks", "ArithmeticException", "ArrayIndexOutOfBoundsException", "ArrayStoreException",
			"AssertionError", "AssertionStatusDirectives", "Boolean", "Byte", "Character", "CharacterData00",
			"CharacterData01", "CharacterData02", "CharacterData0E", "CharacterDataLatin1", "CharacterDataPrivateUse",
			"CharacterDataUndefined", "CharSequence", "Class", "ClassCastException", "ClassCircularityError",
			"ClassFormatError", "ClassLoader", "ClassNotFoundException", "Cloneable", "CloneNotSupportedException",
			"Comparable", "Compiler", "ConditionalSpecialCasing", "Deprecated", "Double", "Enum",
			"EnumConstantNotPresentException", "Error", "Exception", "ExceptionInInitializerError", "Float",
			"IllegalAccessError", "IllegalAccessException", "IllegalArgumentException", "IllegalMonitorStateException",
			"IllegalStateException", "IllegalThreadStateException", "IncompatibleClassChangeError",
			"IndexOutOfBoundsException", "InheritableThreadLocal", "InstantiationError", "InstantiationException",
			"Integer", "InternalError", "InterruptedException", "Iterable", "J9VMInternals", "LinkageError", "Long",
			"Math", "NegativeArraySizeException", "NoClassDefFoundError", "NoSuchFieldError", "NoSuchFieldException",
			"NoSuchMethodError", "NoSuchMethodException", "NullPointerException", "Number", "NumberFormatException",
			"Object", "OutOfMemoryError", "Override", "Package", "Process", "ProcessBuilder", "ProcessEnvironment",
			"ProcessImpl", "Readable", "Runnable", "Runtime", "RuntimeException", "RuntimePermission",
			"SecurityException", "SecurityManager", "Short", "Shutdown", "StackOverflowError", "StackTraceElement",
			"StrictMath", "String", "StringBuffer", "StringBuilder", "StringCoding", "StringIndexOutOfBoundsException",
			"SuppressWarnings", "System", "Terminator", "Thread", "ThreadDeath", "ThreadGroup", "ThreadLocal",
			"Throwable", "TypeNotPresentException", "UnknownError", "UnsatisfiedLinkError",
			"UnsupportedClassVersionError", "UnsupportedOperationException", "VerifyError", "VirtualMachineError",
			"Void" };

	/**
	 * The classes that have been resolved through imports, fully qualified
	 * names, or are in the same package. This value will never be null.
	 */
	private List<String> imports = new ArrayList<String>();

	/**
	 * The classes that are used but the package has not been determined. This
	 * value will never be null.
	 */
	private Set<String> unresolvedClasses = new LinkedHashSet<String>();

	/**
	 * The list of star imports (*), which might later be used to help resolve
	 * the {@link #getUnresolved() unresolved classes}. This value will never be
	 * null.
	 */
	private List<String> onDemandImports = new ArrayList<String>();

	/**
	 * A mapping from packages to resolved class names within each package. This
	 * value will never be null.
	 */
	private Map<String, List<String>> packageToClassesMap = new HashMap<String, List<String>>();

	/**
	 * A mapping from resolved classes to the package for each class. This value
	 * will never be null.
	 */
	private Map<String, String> classToPackageMap = new HashMap<String, String>();

	/**
	 * The package that the analyzed class is in. This value will never be null
	 * or empty.
	 */
	private String packageName;

	/**
	 * The name of the analyzed class. This value will never be null or empty.
	 */
	private String className;

	/**
	 * The name of a declared class. This value will never be null, but may be
	 * empty if the file erroneously contains no type declarations.
	 */
	private List<String> classes = new ArrayList<String>();

	/**
	 * A mapping from nested class names to their main class names.
	 */
	private Map<String, String> nestedToMainClassMap = new HashMap<String, String>();

	/**
	 * Constructor for this.
	 *
	 * @param packageName
	 *            The package which contains the class that is being analyzed by
	 *            this. Cannot be null or empty.
	 * @param className
	 *            The class which is being analyzed by this. Cannot be null or
	 *            empty.
	 */
	public ExternalClasses(String packageName, String className) {
		this.packageName = packageName;
		this.className = className;

		for (String javaLangClass : JAVA_LANG_CLASSES) {
			trackResolved("java.lang", javaLangClass);
		}
	}

	/**
	 * Adds a star import (*) to this.
	 *
	 * @param type
	 *            The package name, without a trailing dot (.) or star (*).
	 *            Cannot be null or empty.
	 */
	public void addOnDemandImports(String type) {
		onDemandImports.add(type);
	}

	/**
	 * Adds an explicitly imported class to this.
	 *
	 * @param type
	 *            The fully-qualified class name. Cannot be null or empty.
	 */
	public void addImport(String type) {
		Matcher matcher = PACKAGE_CLASS_PATTERN.matcher(type);
		boolean matched = matcher.matches();

		if (!matched) {
			return;
		}

		String packageValue = matcher.group(2);
		String classValue = matcher.group(9);
		String nestedValue = matcher.group(10);

		String trackedClass = classValue;
		if (nestedValue != null) {
			trackedClass = matcher.group(8);
			nestedToMainClassMap.put(trackedClass, classValue);
		}

		trackResolved(packageValue, trackedClass);

		addImport(packageValue, classValue);
	}

	/**
	 * Records the given class as belonging to the given package so that if the
	 * class name appears in the file without the package, the package can be
	 * determined easily.
	 *
	 * @param packageName
	 *            The name of the package. Cannot be null, but may be empty.
	 * @param className
	 *            The name of the class. Cannot be null or empty.
	 */
	private void trackResolved(String packageName, String className) {
		List<String> classesForPackage = packageToClassesMap.get(packageName);
		if (classesForPackage == null) {
			classesForPackage = new ArrayList<String>();
			packageToClassesMap.put(packageName, classesForPackage);
		}

		classesForPackage.add(className);

		classToPackageMap.put(className, packageName);
	}

	/**
	 * Adds the use of a class to this. If the given type is a fully-qualified
	 * class name, then it will be added to the resolved list of classes. If
	 * just the class name is given then this method will attempt to resolve the
	 * package name and add it to the list of resolved classes. If the package
	 * name cannot be resolved, then the class will be added to the list of
	 * unresolved classes.
	 *
	 * @param type
	 *            The class used, with or without a package name. Cannot be null
	 *            or empty.
	 */
	public void addUsage(String type) {
		Matcher matcher = PACKAGE_CLASS_PATTERN.matcher(type);
		boolean matches = matcher.matches();

		if (!matches) {
			return;
		}

		String packageValue = matcher.group(2);
		String classValue = matcher.group(9);
		String nestedValue = matcher.group(10);
		String finalNestedClass = matcher.group(12);

		if (packageValue == null) {
			// Class or Class.Nested
			if (!isResolved(classValue)) {
				if (classes.contains(classValue)) {
					// the class doesn't import itself or nested classes
					return;
				}

				// this class was not imported explicitly and was not previously
				// resolved to the current package
				packageValue = resolve(classValue);
				if (packageValue != null) {
					// we know the package so we can import it
					addImport(packageValue, classValue);
				} else {
					// there are star imports so we don't have enough
					// information to determine the package
					unresolvedClasses.add(classValue);
				}
			} else {
				// lookup the package
				packageValue = classToPackageMap.get(classValue);

				// we are giving Class or Nested - not sure which
				// if Nested then the Class will be found
				// if Class then that's what's used
				addImportFromUsage(packageValue, classValue, classValue);
			}
		} else {
			// com.ibm.Class or com.ibm.Class.Nested
			if (nestedValue == null) {
				if (classes.contains(classValue) && packageValue.equals(packageName)) {
					// the class doesn't import itself
					return;
				}

				// com.ibm.Class
				imports.add(type);
			} else {
				// com.ibm.Class.Nested
				if (packageValue.equals(packageName) && classes.contains(finalNestedClass)
						&& classes.contains(classValue)) {
					// the nested class is declared in this file, so it does not
					// import itself
					return;
				}

				addImportFromUsage(packageValue, finalNestedClass, classValue);
			}
		}
	}

	/**
	 * First determines the main class to import, then decides imports. The
	 * given nested class will be used to lookup a main class first (this
	 * corresponds to an explicit import of the nested class). If that lookup
	 * fails, the given main class will be used.
	 *
	 * @param packageValue
	 *            The package of the main class to import. Cannot be null, but
	 *            may be empty.
	 * @param possibleNestedClass
	 *            The class which might be an explicitly imported nested class.
	 *            Cannot be null or empty.
	 * @param mainClassIfNoNesting
	 *            The class which is the main class if the potential nested
	 *            class is not actually nested. Cannot be null or empty.
	 */
	private void addImportFromUsage(String packageValue, String possibleNestedClass, String mainClassIfNoNesting) {
		String mainClass = nestedToMainClassMap.get(possibleNestedClass);
		if (mainClass == null) {
			mainClass = mainClassIfNoNesting;
		}

		addImport(packageValue, mainClass);
	}

	/**
	 * Adds the given import. Nested classes should not be imported, only their
	 * containing classes.
	 *
	 * @param packageValue
	 *            The package name of the class. Cannot be null, but may be
	 *            empty.
	 * @param mainClass
	 *            The class to import. Cannot be null or empty.
	 */
	private void addImport(String packageValue, String mainClass) {
		String importValue = packageValue + "." + mainClass;
		if (!imports.contains(importValue)) {
			imports.add(importValue);
		}
	}

	/**
	 * Returns the package name for the given class value by checking if there
	 * are star imports (*). If there are no star imports we can assume that the
	 * class is from the same package.
	 *
	 * @param classValue
	 *            The class to resolve. Cannot be null or empty.
	 *
	 * @return The package name, or null if the package name could not be
	 *         determined from the star imports. This value will be empty if the
	 *         package is the default package.
	 */
	private String resolve(String classValue) {
		String packageValue = null;

		if (onDemandImports.size() == 0) {
			packageValue = packageName;
			trackResolved(packageValue, classValue);
		}

		return packageValue;
	}

	/**
	 * Returns whether the package for the given class name has been previously
	 * determined.
	 *
	 * @param classValue
	 *            The class to check. Cannot be null or empty.
	 *
	 * @return True if the package for the class has been previously determined,
	 *         or false if it has not.
	 */
	private boolean isResolved(String classValue) {
		return classToPackageMap.containsKey(classValue);
	}

	/**
	 * Returns the list of fully-qualified classes found.
	 *
	 * @return The list of fully-qualified classes used. Will not be null, but
	 *         may be empty.
	 */
	public List<String> getResolved() {
		return imports;
	}

	/**
	 * Returns the list of classes whose package could not be determined.
	 *
	 * @return The list of classes whose package could not be determined. Will
	 *         not be null, but may be empty.
	 */
	public List<String> getUnresolved() {
		return new ArrayList<String>(unresolvedClasses);
	}

	/**
	 * Returns the star imports (*) as a list of package names. For example,
	 * <code>import java.util.*;</code> would appear in the list as
	 * <code>"java.util"</code>.
	 *
	 * @return Returns the list of star imports. Will not be null, but may be
	 *         empty.
	 */
	public List<String> getStarImports() {
		return onDemandImports;
	}

	/**
	 * Adds the given class name to the list of classes declared in the file.
	 *
	 * @param name
	 *            The name of the class to add. Cannot be null or empty.
	 */
	public void addClassDeclaration(String name) {
		classes.add(name);
	}

	/**
	 * Creates a copy of this.
	 * 
	 * @return A copy of this. This value will not be null.
	 */
	public ExternalClasses copy() {
		ExternalClasses copy = new ExternalClasses(packageName, className);
		copy.imports.addAll(imports);
		copy.unresolvedClasses.addAll(unresolvedClasses);
		copy.onDemandImports.addAll(onDemandImports);
		copy.packageToClassesMap.putAll(packageToClassesMap);
		copy.classToPackageMap.putAll(classToPackageMap);

		return copy;
	}
}
