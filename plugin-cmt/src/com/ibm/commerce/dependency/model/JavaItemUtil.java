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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.DebugUtil;

/**
 * This class contains functions to help deal with JavaItems. Although it is
 * named as a utility class, it contains contextual information for performing
 * many of the functions, and so behaves more like a singleton.
 * 
 * @author Trent Hoeppner
 */
public class JavaItemUtil {

	/**
	 * Maps names of packages to package JavaItems. This value will never be
	 * null.
	 */
	private static Map<String, JavaItem> nameToPackageMap = new LinkedHashMap<String, JavaItem>();

	/**
	 * Maps names of primitive types (like boolean) to special JavaItems that
	 * represent those types. This value is null until
	 * {@link #initialize(JavaItemFactory)} is called.
	 */
	private static Map<String, JavaItem> primitiveNameToTypeMap;

	/**
	 * Contains the primitive types in the order from narrowest to widest type
	 * cast. This is used to approximate what the resulting type of an
	 * expression is when there are two expressions of different types. This
	 * value is null until {@link #initialize(JavaItemFactory)} is called.
	 */
	private static List<JavaItem> primitiveTypeOrder;

	/**
	 * The factory that is used to create new JavaItems. This value is null
	 * until {@link #initialize(JavaItemFactory)} is called.
	 */
	private static JavaItemFactory factory;

	/**
	 * Represents any type. This is used as a placeholder for an expression type
	 * or method argument type when the type cannot be determined. This value is
	 * null until {@link #initialize(JavaItemFactory)} is called.
	 */
	private static JavaItem wildcardType;

	/**
	 * URLs to be used to create classloaders for loading classes without source
	 * code, so that their methods and dependencies can be discovered.
	 */
	private static List<URL> urlsToLoad = new ArrayList<>();

	/**
	 * The classloader to load classes without source code, so that their
	 * methods and dependencies can be discovered.
	 */
	private static URLClassLoader jarLoader;

	/**
	 * The index used to find JavaItems.
	 */
	private static JavaItemIndex index;

	/**
	 * Sets the given factory and loads the primitive types and wildcard type.
	 * This must be called before any other method.
	 * 
	 * @param factory
	 *            The factory used to create the primitive types. This value
	 *            cannot be null.
	 */
	public static void initialize(JavaItemFactory factory) {
		JavaItemUtil.factory = factory;
		index = factory.getIndex();

		primitiveTypeOrder = new ArrayList<>();
		primitiveTypeOrder.add(factory.createClass(null, "boolean"));
		primitiveTypeOrder.add(factory.createClass(null, "char"));
		primitiveTypeOrder.add(factory.createClass(null, "byte"));
		primitiveTypeOrder.add(factory.createClass(null, "short"));
		primitiveTypeOrder.add(factory.createClass(null, "int"));
		primitiveTypeOrder.add(factory.createClass(null, "long"));
		primitiveTypeOrder.add(factory.createClass(null, "float"));
		primitiveTypeOrder.add(factory.createClass(null, "double"));

		primitiveNameToTypeMap = new HashMap<>();
		for (JavaItem p : primitiveTypeOrder) {
			primitiveNameToTypeMap.put(p.getName(), p);
		}

		wildcardType = factory.createClass(null, "*");
	}

	/**
	 * Creates a URL for the given JAR to be used to load classes without source
	 * code, to discover their methods and dependencies. After
	 * {@link #classForName(String)} has been called once, calls to this method
	 * will be ignored.
	 * 
	 * @param jar
	 *            The name of the JAR file to load. This value cannot be null.
	 */
	public static void addClassLoader(File jar) {
		try {
			URL url = jar.toURI().toURL();
			urlsToLoad.add(url);
		} catch (MalformedURLException e) {
			// should never happen
		}
	}

	/**
	 * Finds the class with the given name by looking through existing code and
	 * JAR files added with {@link #addClassLoader(File)}.
	 * 
	 * @param fullyQualifiedName
	 *            The package and class name of the class to load. This value
	 *            cannot be null or empty.
	 * 
	 * @return The Class that was loaded. This value will not be null.
	 * 
	 * @throws ClassNotFoundException
	 *             If the class could not be found.
	 */
	public static Class<?> classForName(String fullyQualifiedName) throws ClassNotFoundException {
		if (jarLoader == null) {
			URL[] urls = urlsToLoad.toArray(new URL[urlsToLoad.size()]);
			jarLoader = new URLClassLoader(urls, JavaItemUtil.class.getClassLoader());
		}

		Class<?> type = Class.forName(fullyQualifiedName, false, jarLoader);
		return type;
	}

	/**
	 * Finds the closest class that contains the given node.
	 * 
	 * @param context
	 *            The context which is used to get the compilation unit. This
	 *            value cannot be null.
	 * @param startingPoint
	 *            The node that is contained by a class. If this node is itself
	 *            a class declaration, the JavaItem for the node will be
	 *            returned. This value cannot be null.
	 * 
	 * @return The class object that contains the given node, or null if there
	 *         is no object to represent that class or there is no type which
	 *         contains the given node.
	 */
	public static JavaItem findJavaClass(Context context, ASTNode startingPoint) {
		TypeDeclaration declaringType = getContainingTypeDeclaration(startingPoint);
		if (declaringType == null) {
			// no class declaration, weird
			return null;
		}

		String classBaseName = declaringType.getName().getIdentifier();

		String packageName;
		CompilationUnit compUnit = context.get(Context.Prop.COMP_UNIT);
		PackageDeclaration p = compUnit.getPackage();
		if (p != null) {
			packageName = p.getName().getFullyQualifiedName();
		} else {
			packageName = "";
		}

		JavaItemIndex index = context.get(Context.Prop.JAVA_ITEM_INDEX);
		JavaItem javaClass = index.findClass(packageName, classBaseName);

		return javaClass;
	}

	/**
	 * Returns the root compilation unit that contains the given node.
	 * 
	 * @param node
	 *            The node that contains the compilation unit. This value cannot
	 *            be null.
	 * 
	 * @return The compilation unit that contains the node. This value not be
	 *         null.
	 */
	private static CompilationUnit getCompilationUnit(ASTNode node) {
		ASTNode current = node;
		while (current != null && !(current instanceof CompilationUnit)) {
			current = current.getParent();
		}

		return (CompilationUnit) current;
	}

	/**
	 * Returns the closest type declaration that contains the given node. If the
	 * node is not inside a type declaration, the first type declaration in the
	 * root compilation unit will be returned.
	 * 
	 * @param node
	 *            The node to find the type declaration of. This value cannot be
	 *            null.
	 * 
	 * 
	 * @return The containing type declaration. This value will not be null.
	 */
	public static TypeDeclaration getContainingTypeDeclaration(ASTNode node) {
		// TODO should we include the given node or start with the parent?
		ASTNode current = node;
		while (current != null && !(current instanceof TypeDeclaration)) {
			current = current.getParent();
		}

		if (current == null) {
			// no class declaration, just get the first one from the comp unit
			// instead
			CompilationUnit compUnit = getCompilationUnit(node);
			if (!compUnit.types().isEmpty()) {
				current = (ASTNode) compUnit.types().get(0);
			}
		}

		TypeDeclaration declaringType;
		if (current instanceof TypeDeclaration) {
			declaringType = (TypeDeclaration) current;
		} else {
			// TODO should handle EnumDeclaration as well
			declaringType = null;
		}

		return declaringType;
	}

	/**
	 * Returns whether the given class is an array wrapper. An array may wrap
	 * another array, but ultimately the last array will wrap a base class.
	 * 
	 * @param typeClass
	 *            The class that may be an array wrapper. The value cannot be
	 *            null.
	 * 
	 * @return True if the given object is an array that wraps another class,
	 *         false otherwise.
	 */
	public static boolean isArray(JavaItem typeClass) {
		return typeClass.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS) != null;
	}

	/**
	 * Returns the full class name of the given class, including package and
	 * class name. If the class is an array wrapper, the base class will be used
	 * to determine the name instead, and the resulting name will not have any
	 * array indicators.
	 * 
	 * @param typeClass
	 *            The class to get the full class name for. This value cannot be
	 *            null.
	 * 
	 * @return The full class name. This value will not be null or empty.
	 */
	public static String getFullClassNameForType(JavaItem typeClass) {
		Check.notNull(typeClass, "typeClass");

		JavaItem packageClass = typeClass;
		while (isArray(packageClass)) {
			Integer packageClassID = packageClass.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS);
			packageClass = index.getItem(packageClassID);
			Check.notNull(packageClass, "packageClass");
		}

		String packageName;
		if (packageClass.getParent() == null) {
			packageName = "";
		} else {
			packageName = packageClass.getParent().getName();
		}

		if (!packageName.isEmpty()) {
			packageName = packageName + ".";
		}

		String name = packageName + typeClass.getName();

		return name;
	}

	/**
	 * Finds all super classes and super interfaces of the given class,
	 * recursively, not including the class itself. Supers will be added
	 * breadth-first, so that a class's super class will be added, then its
	 * super interfaces will be added in the order they are declared, then the
	 * super class's supers, then the super interfaces' supers, and so on.
	 * 
	 * @param baseClass
	 *            The class to find the supers of. This value cannot be null.
	 * @param allSupers
	 *            The set to add the supers to. This value cannot be null.
	 * @param includeInterfaces
	 *            True indices that super interfaces will be added, false
	 *            indicates that super interfaces will not be added.
	 */
	public static void findAllSupers(JavaItem baseClass, Set<JavaItem> allSupers, boolean includeInterfaces) {
		Check.notNull(baseClass, "baseClass");
		Integer superClassID = baseClass.getAttribute(JavaItem.ATTR_SUPERCLASS);
		JavaItem superClass = index.getItem(superClassID);

		if (superClass != null) {
			allSupers.add(superClass);
		}

		Set<Integer> superInterfaceIDs = baseClass.getAttribute(JavaItem.ATTR_SUPERINTERFACES);
		if (includeInterfaces) {
			if (superInterfaceIDs != null) {
				for (Integer superInterfaceID : superInterfaceIDs) {
					JavaItem superInterface = index.getItem(superInterfaceID);
					allSupers.add(superInterface);
				}
			}
		}

		if (superClass != null) {
			findAllSupers(superClass, allSupers, includeInterfaces);
		}

		if (includeInterfaces) {
			if (superInterfaceIDs != null) {
				for (Integer superInterfaceID : superInterfaceIDs) {
					JavaItem superInterface = index.getItem(superInterfaceID);
					findAllSupers(superInterface, allSupers, includeInterfaces);
				}
			}
		}
	}

	/**
	 * Finds the class for the given Type, using the dependencies of the given
	 * class to perform the search.
	 * 
	 * @param iType
	 *            The type to find the class of. If this value is null, null
	 *            will be returned.
	 * @param javaClass
	 *            The class which contains the dependencies which restrict the
	 *            scope of the search. This value cannot be null.
	 * 
	 * 
	 * @return The class that is the best match for the given Type. This value
	 *         will be null if no class could be found.
	 */
	public static JavaItem findClassForType(Type iType, JavaItem javaClass) {
		if (iType == null) {
			return null;
		}

		String iName = getNameForType(iType, javaClass);
		JavaItem interfaceType = findClassForName(iName, javaClass);
		return interfaceType;
	}

	/**
	 * Returns a JavaItem that represents the type of the given expression. This
	 * is not the runtime type, but the static type. If the type cannot be
	 * clearly determined, the wildcard type will be returned.
	 * 
	 * @param expression
	 *            The expression to get the type of. If null, null will be
	 *            returned.
	 * @param javaClass
	 *            The class in which the expression was found. This value cannot
	 *            be null.
	 * @param variablesInScope
	 *            The variables and types of variables that are visible where
	 *            the expression is. This value cannot be null.
	 * 
	 * @return The type of the expression, or null if the expression is null.
	 */
	public static JavaItem getTypeForExpression(Expression expression, JavaItem javaClass, Scope variablesInScope) {
		if (expression == null) {
			return null;
		}

		ExpressionResolverVisitor resolver = new ExpressionResolverVisitor(javaClass, variablesInScope);
		expression.accept(resolver);

		JavaItem expressionType = resolver.outputType;

		return expressionType;
	}

	/**
	 * Gets the package and class name of the given type. If the type is an
	 * array type, array delimiters are added for each of the dimensions of the
	 * array.
	 * 
	 * @param type
	 *            The type to create the string for. This value cannot be null.
	 * @param javaClass
	 *            The class in which the type was found. This value cannot be
	 *            null.
	 * 
	 * @return The package and class name of the given type, with array
	 *         delimiters, if any. This value will not be null.
	 */
	public static String getNameForType(Type type, JavaItem javaClass) {
		Check.notNull(type, "type");
		Check.notNull(javaClass, "javaClass");

		String name;
		if (type instanceof PrimitiveType) {
			PrimitiveType t = (PrimitiveType) type;
			name = t.getPrimitiveTypeCode().toString();
		} else if (type instanceof ArrayType) {
			ArrayType t = (ArrayType) type;
			name = getNameForType(t.getElementType(), javaClass) + getDimensionString(t.getDimensions());
		} else if (type instanceof ParameterizedType) {
			ParameterizedType t = (ParameterizedType) type;
			name = getNameForType(t.getType(), javaClass);
		} else if (type instanceof SimpleType) {
			SimpleType t = (SimpleType) type;
			JavaItem typeClass = findClassForName(t.getName().getFullyQualifiedName(), javaClass);
			if (typeClass == null) {
				// TODO why is this null? unresolved class name?
				name = t.getName().getFullyQualifiedName();
			} else {
				name = JavaItemUtil.getFullClassNameForType(typeClass);
			}
		} else {
			// TODO handle the other types
			name = type.toString();
		}

		return name;
	}

	/**
	 * Finds the class for the given name by searching the dependencies of the
	 * given class.
	 * 
	 * @param fullyQualifiedName
	 *            The full (including package name) or partial name of the class
	 *            to find. If the package is not included the class may be a
	 *            best guess match. This value cannot be null or empty.
	 * @param javaClass
	 *            The class which has dependencies to search. This value cannot
	 *            be null.
	 * 
	 * @return The class that was found, or null if no matching class could be
	 *         found.
	 */
	public static JavaItem findClassForName(String fullyQualifiedName, JavaItem javaClass) {
		Check.notNullOrEmpty(fullyQualifiedName, "fullyQualifiedName");
		Check.notNull(javaClass, "javaClass");

		String fullNameWithoutArray = fullyQualifiedName;
		int bracketIndex = fullyQualifiedName.indexOf('[');
		if (bracketIndex >= 0) {
			fullNameWithoutArray = fullNameWithoutArray.substring(0, bracketIndex);
		}

		String partialName = fullNameWithoutArray;
		int lastDot = fullNameWithoutArray.lastIndexOf('.');
		if (lastDot >= 0) {
			partialName = fullNameWithoutArray.substring(lastDot + 1);
		}

		JavaItem primitiveType = primitiveNameToTypeMap.get(partialName);
		if (primitiveType != null) {
			return primitiveType;
		}

		JavaItem targetClass = null;
		Boolean binaryAttribute = javaClass.getAttribute(JavaItem.ATTR_BINARY);
		boolean binary;
		if (binaryAttribute != null) {
			binary = binaryAttribute;
		} else {
			binary = false;
		}
		if (!binary) {
			for (JavaItem dependency : javaClass.getDependencies()) {
				String dependencyFullName = JavaItemUtil.getFullClassNameForType(dependency);
				if (dependencyFullName.equals(fullNameWithoutArray)) {
					targetClass = dependency;
					break;
				} else if (dependency.getName().equals(partialName)) {
					// the fullyQualifiedName did not have a package, we can try
					// to
					// match a partial name
					targetClass = dependency;
					break;
				}
			}

			// if that fails check if the current class is the desired one
			if (targetClass == null) {
				String sourceClassFullName = JavaItemUtil.getFullClassNameForType(javaClass);
				if (sourceClassFullName.equals(fullNameWithoutArray)) {
					targetClass = javaClass;
				} else if (javaClass.getName().equals(partialName)) {
					// the fullyQualifiedName did not have a package, we can try
					// to
					// match a partial name
					targetClass = javaClass;
				}
			}
		} else {
			Check.notEmpty(fullNameWithoutArray, "fullNameWithoutArray");
			String packageName;
			if (lastDot < 0) {
				// weird
				packageName = "";
			} else {
				packageName = fullNameWithoutArray.substring(0, lastDot);
			}
			targetClass = findClassInAnyProject(packageName, partialName);
		}

		int numBrackets = 0;
		while (bracketIndex >= 0) {
			numBrackets++;
			bracketIndex = fullyQualifiedName.indexOf('[', bracketIndex + 1);
		}

		if (numBrackets > 0 && targetClass != null) {
			targetClass = createArrayWrapper(targetClass, numBrackets);
		}

		return targetClass;
	}

	/**
	 * Finds a class that has the given package name and class name without
	 * restricting the search to a particular project. If more than one match
	 * exists, the first match will be returned.
	 * 
	 * @param packageName
	 *            The package name of the class to find. An empty string denotes
	 *            the default package. This value cannot be null.
	 * @param className
	 *            The name of the class to find. This value cannot be null or
	 *            empty.
	 * 
	 * @return The class that was found, or null if no matching class could be
	 *         found.
	 */
	private static JavaItem findClassInAnyProject(String packageName, String className) {
		JavaItem javaClass = index.findClass(packageName, className);
		return javaClass;
	}

	/**
	 * Creates an array type to wrap the given type. Array types are represented
	 * this way so that the underlying type can be reused. The given type may
	 * itself be an array type which wraps a base type.
	 * <p>
	 * The returned array type has an attribute
	 * {@link JavaItem#ATTR_ARRAY_BASE_CLASS} whose value is the base type. The
	 * base class is java.lang.Object, and the superinterfaces are Cloneable and
	 * Serializable.
	 * 
	 * @param targetClass
	 *            The class to wrap. This value cannot be null.
	 * @param numBrackets
	 *            The number of dimensions of the array. If < 1, the targetClass
	 *            will be returned.
	 * 
	 * @return The array type. This value will not be null.
	 */
	private static JavaItem createArrayWrapper(JavaItem targetClass, int numBrackets) {
		Check.notNull(targetClass, "targetClass");

		if (numBrackets == 0) {
			return targetClass;
		}

		JavaItem javalangPackage = getCachedPackageWithNoProject("java.lang");
		JavaItem javaioPackage = getCachedPackageWithNoProject("java.io");

		JavaItem arrayWrapper = factory.createClass(null, targetClass.getName() + "[]");
		arrayWrapper.setType(JavaItemType.CLASS);
		arrayWrapper.setAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS, targetClass.getID());
		JavaItem objectType = getClassValue(javalangPackage, "Object");
		arrayWrapper.setAttribute(JavaItem.ATTR_SUPERCLASS, objectType.getID());

		JavaItem clonableType = getClassValue(javalangPackage, "Cloneable");
		JavaItem serializableType = getClassValue(javaioPackage, "Serializable");
		Set<Integer> superInterfaceIDs = new LinkedHashSet<>();
		superInterfaceIDs.add(clonableType.getID());
		superInterfaceIDs.add(serializableType.getID());
		arrayWrapper.setAttribute(JavaItem.ATTR_SUPERINTERFACES, superInterfaceIDs);

		JavaItem lengthField = factory.createField(null, "length");
		lengthField.setType(JavaItemType.FIELD);
		arrayWrapper.getChildrenIDs().add(lengthField.getID());

		arrayWrapper = createArrayWrapper(arrayWrapper, numBrackets - 1);

		return arrayWrapper;
	}

	/**
	 * Creates a string which repeats the array delimiters "[]" the given number
	 * of times.
	 * 
	 * @param dimensions
	 *            The number of times to repeat the array delimiters. If < 1,
	 *            the empty string will be returned.
	 * 
	 * @return The string with the number of dimensions given. This value will
	 *         not be null, but may be empty.
	 */
	private static String getDimensionString(int dimensions) {
		String d = "";
		for (int i = 0; i < dimensions; i++) {
			d += "[]";
		}

		return d;
	}

	/**
	 * Looks for the class with the given name in the given package. If the
	 * class does not exist, it will be created and added to the package.
	 * 
	 * @param packageValue
	 *            The package to search in. This value cannot be null.
	 * @param className
	 *            The class name to search for. This value cannot be null or
	 *            empty.
	 * 
	 * @return The found class object, or a new one if one was created. This
	 *         value will not be null.
	 */
	public static JavaItem getClassValue(JavaItem packageValue, String className) {
		JavaItem found = null;
		for (JavaItem javaClass : packageValue.getChildren()) {
			if (javaClass.getName().equals(className)) {
				found = javaClass;
				break;
			}
		}

		if (found == null) {
			found = factory.createClass(packageValue, className);
			found.setType(JavaItemType.CLASS);
			packageValue.getChildrenIDs().add(found.getID());
		}

		return found;
	}

	/**
	 * Returns the first package that matches the given name and which contains
	 * the given class. The order of search is determined by the given project.
	 * The project's dependent projects will be searched in the order that they
	 * appear in the manifest file. If no project can be found a new package
	 * will be created and returned.
	 * <p>
	 * Identical calls to this method will return the same object.
	 *
	 * @param project
	 *            The project which determines the order of the search. Cannot
	 *            be null.
	 * @param packageName
	 *            The name of the package which contains the class. Cannot be
	 *            null or empty.
	 * @param className
	 *            The name of the class which should be in the package. Cannot
	 *            be null or empty.
	 *
	 * @return The package which contains the given class. Will not be null.
	 */
	public static JavaItem getPackage(JavaItem project, String packageName, String className) {

		// ensure the named project is the first to look for the package in
		List<JavaItem> projectsToSearch = new ArrayList<JavaItem>(project.getDependencies());
		projectsToSearch.add(0, project);

		JavaItem found = null;

		DebugUtil.stopAtPackageAndClassAndProject(packageName, className, project.getName(),
				"before searching for the package/class combo");

		for (JavaItem currentProject : projectsToSearch) {
			found = findClassInPackage(currentProject.getChildren(), packageName, className, true);
		}

		if (found == null) {
			found = getBinaryPackageForClass(packageName, className);
		}

		if (found == null) {
			found = getPackageForClassNotInManifest(packageName, className);
		}

		if (found == null) {
			found = getCachedPackageWithNoProject(packageName);
		}

		return found;
	}

	/**
	 * Finds the class item using the given package and class name to search
	 * within the given package items.
	 * 
	 * @param packages
	 *            The items which represent packages, which have classes as
	 *            children. This value cannot be null, but may be empty.
	 * @param packageName
	 *            The package name for the class to find. This value cannot be
	 *            null, but may be empty to indicate the default package.
	 * @param className
	 *            The name of the class to find, without the package name. This
	 *            value cannot be null or empty.
	 * @param packagesAreFromProject
	 *            True indicates the packages in the list are from a single
	 *            project, false indicates that they are from multiple projects.
	 * 
	 * @return The first class item that was found, or null if no class item
	 *         could be found.
	 */
	private static JavaItem findClassInPackage(List<JavaItem> packages, String packageName, String className,
			boolean packagesAreFromProject) {
		JavaItem found = null;
		outer: for (JavaItem currentPackage : packages) {
			if (currentPackage.getName().equals(packageName)) {
				for (JavaItem currentClass : currentPackage.getChildren()) {
					if (currentClass.getName().equals(className)) {
						found = currentPackage;
						break outer;
					}
				}

				if (packagesAreFromProject) {
					// there won't be two packages in this project with the same
					// name
					break;
				}
			}
		}

		return found;
	}

	/**
	 * Finds the class item that has the given package and class name when the
	 * manifest file for the containing project does not contain a project that
	 * contains the given class.
	 * 
	 * @param packageName
	 *            The package name for the class to find. This value cannot be
	 *            null, but may be empty to indicate the default package.
	 * @param className
	 *            The name of the class to find, without the package name. This
	 *            value cannot be null or empty.
	 * 
	 * @return The first class item that was found, or null if no class item
	 *         could be found.
	 */
	private static JavaItem getPackageForClassNotInManifest(String packageName, String className) {
		List<JavaItem> allPackages = index.findPackages(packageName);
		JavaItem found = findClassInPackage(allPackages, packageName, className, false);

		return found;
	}

	/**
	 * Finds the class item that has the given package and class name, but was
	 * discovered as a binary class (a class without source code).
	 * 
	 * @param packageName
	 *            The package name for the class to find. This value cannot be
	 *            null, but may be empty to indicate the default package.
	 * @param className
	 *            The name of the class to find, without the package name. This
	 *            value cannot be null or empty.
	 * 
	 * @return The first binary class item that was found, or null if no class
	 *         item could be found.
	 */
	private static JavaItem getBinaryPackageForClass(String packageName, String className) {
		JavaItem found = null;
		List<JavaItem> packages = index.findPackages(packageName);
		for (JavaItem current : packages) {
			Boolean binary = current.getAttribute(JavaItem.ATTR_BINARY);
			if (binary != null && binary) {
				for (JavaItem javaClass : current.getChildren()) {
					if (javaClass.getName().equals(className)) {
						found = current;
						break;
					}
				}
			}
		}

		return found;
	}

	/**
	 * Returns a package by the given name that is not associated with any
	 * project, meaning that the package that is referred to in a class could be
	 * found in the system. If the same packageName is used in two calls, the
	 * same project will be returned both times.
	 * 
	 * @param packageName
	 *            The package name for the class to find. This value cannot be
	 *            null, but may be empty to indicate the default package.
	 * 
	 * @return The package item found that does not have a parent project. This
	 *         value will not be null.
	 */
	private static JavaItem getCachedPackageWithNoProject(String packageName) {
		JavaItem javaPackage = nameToPackageMap.get(packageName);
		if (javaPackage == null) {
			javaPackage = factory.createPackage(null, packageName);
			javaPackage.setType(JavaItemType.PACKAGE);
			nameToPackageMap.put(packageName, javaPackage);
		}

		return javaPackage;
	}

	/**
	 * Creates a method item for a method call, by resolving the given name and
	 * method argument expressions using the variables in the given scope. This
	 * method can only be used to create a fake (temporary) method that is used
	 * for comparison purposes when looking for a real method.
	 * 
	 * @param nodeName
	 *            The name of the method as an Eclipse name. This value cannot
	 *            be null.
	 * @param nodeArguments
	 *            The argument Expression objects. If any variable names are
	 *            used in the expression, the scope will be used to resolve them
	 *            to argument types. This value cannot be null, but may be
	 *            empty.
	 * @param javaClass
	 *            The class item in which the method call occurs, and is also
	 *            used to resolve dependencies. This value cannot be null.
	 * @param variablesInScope
	 *            The variable names and types that are available in scope at
	 *            the time when the method is called. This value cannot be null.
	 * 
	 * @return The method that was created. This value will not be null.
	 */
	public static JavaItem createFakeMethodUsingScope(SimpleName nodeName, List<?> nodeArguments, JavaItem javaClass,
			Scope variablesInScope) {
		String name = nodeName.getFullyQualifiedName();
		return createFakeMethodUsingScope(name, nodeArguments, javaClass, variablesInScope);
	}

	/**
	 * Creates a method item for a method call, by resolving the given name and
	 * method argument expressions using the variables in the given scope. This
	 * method can only be used to create a fake (temporary) method that is used
	 * for comparison purposes when looking for a real method.
	 * 
	 * @param nodeName
	 *            The name of the method as a string. This value cannot be null
	 *            or empty.
	 * @param nodeArguments
	 *            The argument Expression objects. If any variable names are
	 *            used in the expression, the scope will be used to resolve them
	 *            to argument types. This value cannot be null, but may be
	 *            empty.
	 * @param javaClass
	 *            The class item in which the method call occurs, and is also
	 *            used to resolve dependencies. This value cannot be null.
	 * @param variablesInScope
	 *            The variable names and types that are available in scope at
	 *            the time when the method is called. This value cannot be null.
	 * 
	 * @return The method that was created. This value will not be null.
	 */
	public static JavaItem createFakeMethodUsingScope(String name, List<?> nodeArguments, JavaItem javaClass,
			Scope variablesInScope) {
		List<Integer> parameterTypeIDs = new ArrayList<>();
		for (Object o : nodeArguments) {
			Expression argument = (Expression) o;
			JavaItem argType = JavaItemUtil.getTypeForExpression(argument, javaClass, variablesInScope);
			if (argType == null) {
				argType = wildcardType;
			}
			parameterTypeIDs.add(argType.getID());
		}

		JavaItem method = factory.createUntracked(name, JavaItemType.METHOD);
		method.setType(JavaItemType.METHOD);
		method.setAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES, parameterTypeIDs);

		return method;
	}

	/**
	 * Returns true if the given methods are a match, which is used to find an
	 * existing method item (i.e. in the JavaItemIndex) by using a temporary
	 * method item created from a method call statement.
	 * 
	 * @param method
	 *            The first method (usually the method from the JavaItemIndex).
	 *            This value cannot be null.
	 * @param fakeMethod
	 *            The second method (usually a temporary method item generated
	 *            from a method call statement). This value cannot be null.
	 * 
	 * @return True if the methods have the same method name and parameter
	 *         types, false otherwise. Note that if at least one argument in the
	 *         same position in both methods is represented as a wildcard type,
	 *         the arguments will be considered a match.
	 */
	public static boolean isMethodsEqual(JavaItem method, JavaItem fakeMethod) {
		if (!method.getName().equals(fakeMethod.getName())) {
			return false;
		}

		List<Integer> paramIDs1 = method.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
		List<Integer> paramIDs2 = fakeMethod.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
		if (paramIDs1 == null && paramIDs2 == null) {
			// they both have no parameters
			return true;
		} else if (paramIDs1 == null) {
			if (paramIDs2.isEmpty()) {
				// params1 is null and params2 is empty, we can consider these
				// as both having no parameters
				return true;
			} else {
				// params1 is null and params2 is non-empty, they have different
				// numbers of parameters
				return false;
			}
		} else if (paramIDs2 == null) {
			if (paramIDs1.isEmpty()) {
				// params2 is null and params1 is empty, we can consider these
				// as both having no parameters
				return true;
			} else {
				// params2 is null and params1 is non-empty, they have different
				// numbers of parameters
				return false;
			}
		}

		// both parameter lists are non-null
		if (paramIDs1.size() != paramIDs2.size()) {
			return false;
		}

		for (int i = 0; i < paramIDs1.size(); i++) {
			Integer paramID1 = paramIDs1.get(i);
			Integer paramID2 = paramIDs2.get(i);
			if (paramID1.equals(wildcardType.getID()) || paramID2.equals(wildcardType.getID())) {
				continue;
			}

			if (!paramID1.equals(paramID2)) {
				// maybe because they are arrays
				JavaItem param1 = index.getItem(paramID1);
				JavaItem param2 = index.getItem(paramID2);
				if (JavaItemUtil.isArray(param1) && JavaItemUtil.isArray(param2)) {
					if (!isArraysEqual(param1, param2)) {
						return false;
					}
					// else the arrays are equal, this param passed
				} else {
					// not arrays, they are just not equal
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Returns true if the given array items are equal.
	 * 
	 * @param array1
	 *            The first array item to compare. This value cannot be null.
	 * @param array2
	 *            The second array item to compare. This value cannot be null.
	 * 
	 * @return True if the array items have the same dimensionality and the same
	 *         base type, false otherwise.
	 */
	private static boolean isArraysEqual(JavaItem array1, JavaItem array2) {
		Integer baseClassID1 = array1.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS);
		Integer baseClassID2 = array2.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS);
		JavaItem baseClass1 = index.getItem(baseClassID1);
		JavaItem baseClass2 = index.getItem(baseClassID2);

		boolean equal;
		if (JavaItemUtil.isArray(baseClass1) && JavaItemUtil.isArray(baseClass2)) {
			equal = isArraysEqual(baseClass1, baseClass2);
		} else {
			equal = baseClass1 == baseClass2;
		}

		return equal;
	}

	/**
	 * Finds all variables and their types in scope at the location of the given
	 * node.
	 * 
	 * @param javaClass
	 *            The class item in which the node occurs. This value cannot be
	 *            null.
	 * @param node
	 *            The node where the variables are declared. This value cannot
	 *            be null.
	 * 
	 * @return The scope object which contains the variables and their types.
	 *         This value will not be null.
	 */
	public static Scope findScopeAtNode(JavaItem javaClass, ASTNode node) {
		TypeDeclaration typeDeclaration = getContainingTypeDeclaration(node);
		VariableVisitor visitor = new VariableVisitor(null, javaClass, typeDeclaration, node);
		typeDeclaration.accept(visitor);

		Scope scope = visitor.getScope();
		return scope;
	}

	/**
	 * Finds a method similar to the given method in the given class or a
	 * superclass of the given class. If the given method has a wildcard type as
	 * a parameter, that parameter will match any parameter when searching for
	 * similar methods. Super interfaces will not be searched.
	 * 
	 * @param fakeMethod
	 *            An object that has a method name and the parameter types
	 *            defined as an attribute with key
	 *            {@link JavaItem#ATTR_METHOD_PARAM_TYPES}. Parents and children
	 *            will be ignored. This value cannot be null.
	 * @param sourceClass
	 *            The class to find the method in, or which is a subclass of the
	 *            desired method. This value cannot be null.
	 * 
	 * @return The first method that is a match, or null if no matching method
	 *         could be found.
	 */
	public static JavaItem findMethodInClassOrSupers(JavaItem fakeMethod, JavaItem sourceClass) {
		Set<JavaItem> allSupers = new LinkedHashSet<>();
		allSupers.add(sourceClass);
		JavaItemUtil.findAllSupers(sourceClass, allSupers, false);

		JavaItem calledMethod = null;
		outer: for (JavaItem superClass : allSupers) {
			for (JavaItem method : superClass.getChildren(JavaItemType.METHOD)) {
				if (JavaItemUtil.isMethodsEqual(method, fakeMethod)) {
					calledMethod = method;
					break outer;
				}
			}
		}
		return calledMethod;
	}

	/**
	 * Creates a string for the method that includes the method name and the
	 * full class name of each method parameter.
	 * 
	 * @param method
	 *            The object that represents the method. This value cannot be
	 *            null.
	 * 
	 * @return A string representation for the method. This value will not be
	 *         null.
	 */
	public static String getStringForMethod(JavaItem method) {
		StringBuilder b = new StringBuilder();
		b.append(method.getName());
		b.append("(");
		boolean first = true;
		List<Integer> parameterTypeIDs = method.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
		for (Integer parameterTypeID : parameterTypeIDs) {
			if (first) {
				first = false;
			} else {
				b.append(",");
			}

			JavaItem parameterType = method.getIndex().getItem(parameterTypeID);
			String typeName = JavaItemUtil.getFullClassNameForType(parameterType);
			b.append(typeName);
		}
		b.append(")");

		String methodName = b.toString();
		return methodName;
	}

	/**
	 * Returns the type that represents any type. This is used when the type of
	 * a variable or method parameter cannot be resolved accurately, so that
	 * this unknown type can be matched in searches.
	 * <p>
	 * There is only one wildcard object in the system.
	 * 
	 * @return The wildcard type object. This value will not be null.
	 */
	public static JavaItem getWildcardType() {
		return wildcardType;
	}

	/**
	 * Returns the primitive type for the given name.
	 * <p>
	 * There is only one primitive type object in the system for each of the
	 * Java primitive types.
	 * 
	 * @param name
	 *            The name of the primitive type, such as "int" or "boolean". If
	 *            null or empty, null will be returned.
	 * 
	 * @return The primitive type object. This value will be null if the name
	 *         given was not of a primitive type.
	 */
	public static JavaItem getPrimitiveType(String name) {
		return primitiveNameToTypeMap.get(name);
	}

	/**
	 * This class is used to resolve the type of an Eclipse Expression object.
	 */
	private static class ExpressionResolverVisitor extends ASTVisitor {

		/**
		 * The class where the expression exists.
		 */
		private JavaItem javaClass;

		/**
		 * The result that is found, i.e. the class item that is the result of
		 * the expression.
		 */
		private JavaItem outputType;

		/**
		 * The variables that are available in each scope.
		 */
		private Scope scope;

		/**
		 * Constructor for this.
		 * 
		 * @param javaClass
		 *            The class where the expression exists. This value cannot
		 *            be null.
		 * @param variablesInScope
		 *            The variables that are available in the current scope
		 *            where the expression exists. This value cannot be null.
		 */
		public ExpressionResolverVisitor(JavaItem javaClass, Scope variablesInScope) {
			this.javaClass = javaClass;
			this.scope = variablesInScope;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(ArrayAccess node) {
			Expression array = node.getArray();
			JavaItem arrayType = getTypeForExpression(array, javaClass, scope);
			if (arrayType != null) {
				Integer outputTypeID = arrayType.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS);
				outputType = arrayType.getIndex().getItem(outputTypeID);
			}
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(ArrayCreation node) {
			Type type = node.getType().getElementType();
			outputType = findClassForType(type, javaClass);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(ArrayInitializer node) {
			// ignore this one for now
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(Assignment node) {
			Expression assignee = node.getLeftHandSide();
			outputType = getTypeForExpression(assignee, javaClass, scope);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(BooleanLiteral node) {
			outputType = findClassForName("boolean", javaClass);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(CastExpression node) {
			if (node.getType().toString().contains("Parameter")) {
				System.out.println("about to resolve a cast to Parameter");
			}
			outputType = findClassForType(node.getType(), javaClass);
			if (outputType == null) {
				// TODO this happens for
				// Enablement-BaseComponentsLogic:com.ibm.commerce.condition:OpenCondition
				// which contains a nested Parameter type, need to handle nested
				// types
				System.out.println("Could not resolve type for " + node.getType().toString());
			}
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(CharacterLiteral node) {
			outputType = findClassForName("char", javaClass);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(ClassInstanceCreation node) {
			outputType = findClassForType(node.getType(), javaClass);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(ConditionalExpression node) {
			JavaItem thenEx = getTypeForExpression(node.getThenExpression(), javaClass, scope);
			JavaItem elseEx = getTypeForExpression(node.getElseExpression(), javaClass, scope);

			// not sure which one... should choose the first non-null
			if (thenEx != null) {
				outputType = thenEx;
			} else {
				outputType = elseEx;
			}

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(CreationReference node) {
			outputType = findClassForType(node.getType(), javaClass);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(ExpressionMethodReference node) {
			outputType = getTypeForExpression(node.getExpression(), javaClass, scope);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(FieldAccess node) {
			JavaItem fieldType = getTypeForExpression(node.getExpression(), javaClass, scope);
			if (fieldType != null) {
				String fieldName = node.getName().getFullyQualifiedName();
				JavaItem found = findTypeForField(fieldType, fieldName);

				if (found != null) {
					Integer outputTypeID = found.getAttribute(JavaItem.ATTR_FIELD_TYPE);
					outputType = index.getItem(outputTypeID);
				}
			} else {
				outputType = wildcardType;
			}

			return false;
		}

		/**
		 * Finds the class item that represents the type of a field.
		 * 
		 * @param classThatContainsField
		 *            The class that contains the field. This value cannot be
		 *            null.
		 * @param fieldName
		 *            The name of the field in the class. This value cannot be
		 *            null or empty.
		 * 
		 * @return The class item that represents the type of the field, or null
		 *         if the field was not found.
		 */
		private JavaItem findTypeForField(JavaItem classThatContainsField, String fieldName) {
			List<JavaItem> fields = classThatContainsField.getChildren(JavaItemType.FIELD);
			JavaItem found = null;
			for (JavaItem field : fields) {
				if (field.getName().equals(fieldName)) {
					found = field;
					break;
				}
			}
			return found;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(InfixExpression node) {
			if (isBoolean(node.getOperator())) {
				outputType = findClassForName("boolean", javaClass);
			} else {
				JavaItem left = getTypeForExpression(node.getLeftOperand(), javaClass, scope);
				JavaItem right = getTypeForExpression(node.getRightOperand(), javaClass, scope);

				int leftIndex = primitiveTypeOrder.indexOf(left);
				int rightIndex = primitiveTypeOrder.indexOf(right);

				// higher one wins
				if (leftIndex >= rightIndex) {
					outputType = left;
				} else {
					outputType = right;
				}
			}

			return false;
		}

		/**
		 * Returns whether the given operator has an operand before and after
		 * the operator.
		 * 
		 * @param operator
		 *            The operator to check. This value cannot be null.
		 * 
		 * @return True if the operator has 2 operands, false otherwise.
		 */
		private boolean isBoolean(Operator operator) {
			return operator == Operator.AND || operator == Operator.CONDITIONAL_AND
					|| operator == Operator.CONDITIONAL_OR || operator == Operator.EQUALS
					|| operator == Operator.GREATER || operator == Operator.GREATER_EQUALS || operator == Operator.LESS
					|| operator == Operator.LESS_EQUALS || operator == Operator.LESS_EQUALS
					|| operator == Operator.LESS_EQUALS || operator == Operator.NOT_EQUALS || operator == Operator.OR
					|| operator == Operator.XOR;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(InstanceofExpression node) {
			outputType = findClassForName("boolean", javaClass);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(LambdaExpression node) {
			// cannot determine from the lambda expression, must come from the
			// parent
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(MethodInvocation node) {
			SimpleName nodeName = node.getName();
			List<?> nodeArguments = node.arguments();
			JavaItem fakeMethod = createFakeMethodUsingScope(nodeName, nodeArguments, javaClass, scope);

			JavaItem foundMethod = null;
			JavaItem classForMethod = getTypeForExpression(node.getExpression(), javaClass, scope);
			if (classForMethod == null) {
				// no prefix, it means the method is in this class or a
				// superclass
				Set<JavaItem> allSupers = new LinkedHashSet<>();
				allSupers.add(javaClass);
				JavaItemUtil.findAllSupers(javaClass, allSupers, false);

				foundMethod = null;
				for (JavaItem superClass : allSupers) {
					foundMethod = findRealMethod(fakeMethod, superClass);
					if (foundMethod != null) {
						break;
					}
				}

			} else {
				// the method is in the class we found
				foundMethod = findRealMethod(fakeMethod, classForMethod);
			}

			if (foundMethod != null) {
				Integer outputTypeID = foundMethod.getAttribute(JavaItem.ATTR_RETURN_TYPE);
				outputType = foundMethod.getIndex().getItem(outputTypeID);
			}

			return false;
		}

		/**
		 * Finds the method item that matches the signature of the given fake
		 * method item. If a wildcard is present for a parameter in either
		 * method, it will match.
		 * 
		 * @see JavaItemUtil#isMethodsEqual(JavaItem, JavaItem)
		 * 
		 * @param fakeMethod
		 *            The method item that contains the name and parameters to
		 *            match. This value cannoot be null.
		 * @param superClass
		 *            The class item that contains the target method. This value
		 *            cannot be null.
		 * 
		 * @return The first matching method item that was found, or null if no
		 *         match was found.
		 */
		private JavaItem findRealMethod(JavaItem fakeMethod, JavaItem superClass) {
			JavaItem foundMethod = null;
			for (JavaItem method : superClass.getChildren(JavaItemType.METHOD)) {
				if (isMethodsEqual(method, fakeMethod)) {
					foundMethod = method;
					break;
				}
			}

			return foundMethod;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(MethodRef node) {
			// ignore this for now, it should be covered by other types
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(NullLiteral node) {
			// TODO need to think about this more, null can represent any class
			outputType = null;
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(NumberLiteral node) {
			String number = node.getToken();
			String typeName = "int";
			if (number.endsWith("l") || number.endsWith("L")) {
				typeName = "long";
			} else if (number.endsWith("d") || number.endsWith("D") || number.contains("e") || number.contains("E")) {
				typeName = "double";
			} else if (number.endsWith("f") || number.endsWith("f")) {
				typeName = "float";
			}

			outputType = findClassForName(typeName, javaClass);

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(ParenthesizedExpression node) {
			outputType = getTypeForExpression(node.getExpression(), javaClass, scope);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(PostfixExpression node) {
			outputType = findClassForName("int", javaClass);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(PrefixExpression node) {
			outputType = findClassForName("int", javaClass);
			return false;
		}

		@Override
		public boolean visit(StringLiteral node) {
			outputType = findClassForName("java.lang.String", javaClass);
			return false;
		}

		@Override
		public boolean visit(SuperFieldAccess node) {
			JavaItem baseClass;
			if (node.getQualifier() != null) {
				String baseClassName = node.getQualifier().getFullyQualifiedName();
				baseClass = findClassForName(baseClassName, javaClass);
			} else {
				baseClass = javaClass;
			}
			String fieldName = node.getName().getFullyQualifiedName();

			// create a list of superclasses and superinterfaces in a certain
			// order, then go through the list to find the field
			// need to do this in the superclasses of baseClass, not
			// baseClass itself
			Set<JavaItem> allSupers = new LinkedHashSet<>();
			JavaItemUtil.findAllSupers(baseClass, allSupers, true);

			for (JavaItem superThing : allSupers) {
				outputType = findTypeForField(superThing, fieldName);
				if (outputType != null) {
					break;
				}
			}

			return false;
		}

		@Override
		public boolean visit(SuperMethodInvocation node) {
			JavaItem baseClass;
			if (node.getQualifier() != null) {
				String baseClassName = node.getQualifier().getFullyQualifiedName();
				baseClass = findClassForName(baseClassName, javaClass);
			} else {
				baseClass = javaClass;
			}

			SimpleName nodeName = node.getName();
			List<?> nodeArguments = node.arguments();
			JavaItem fakeMethod = createFakeMethodUsingScope(nodeName, nodeArguments, javaClass, scope);

			Set<JavaItem> allSupers = new LinkedHashSet<>();
			JavaItemUtil.findAllSupers(baseClass, allSupers, false);

			for (JavaItem superClass : allSupers) {
				JavaItem method = findRealMethod(fakeMethod, superClass);
				if (method != null) {
					Integer outputTypeID = method.getAttribute(JavaItem.ATTR_RETURN_TYPE);
					outputType = method.getIndex().getItem(outputTypeID);
				}

				if (outputType != null) {
					break;
				}
			}

			return false;
		}

		@Override
		public boolean visit(SuperMethodReference node) {
			// TODO not sure how to handle this now

			return false;
		}

		@Override
		public boolean visit(ThisExpression node) {
			JavaItem baseClass;
			if (node.getQualifier() != null) {
				String baseClassName = node.getQualifier().getFullyQualifiedName();
				baseClass = findClassForName(baseClassName, javaClass);
			} else {
				baseClass = javaClass;
			}

			outputType = baseClass;

			return false;
		}

		@Override
		public boolean visit(TypeLiteral node) {
			outputType = findClassForName("java.lang.Class", javaClass);
			return false;
		}

		@Override
		public boolean visit(TypeMethodReference node) {
			// TODO not sure how to handle this right now
			return false;
		}

		@Override
		public boolean visit(VariableDeclarationExpression node) {
			outputType = findClassForType(node.getType(), javaClass);
			return false;
		}

		@Override
		public boolean visit(QualifiedName node) {
			// TODO this could be a field or a local variable
			return false;
		}

		@Override
		public boolean visit(SimpleName node) {
			String varName = node.getFullyQualifiedName();
			outputType = scope.getVariable(varName);
			return false;
		}

	}

}
