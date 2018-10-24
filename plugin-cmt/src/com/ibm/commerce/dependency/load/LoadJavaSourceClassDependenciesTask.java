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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.dependency.model.ExternalClasses;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemType;

/**
 * This class goes through a compilation unit to find the declared classes, and
 * adds the dependencies on other classes for each class.
 * 
 * @author Trent Hoeppner
 */
public class LoadJavaSourceClassDependenciesTask extends AbstractJavaSourceTask {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(
			Arrays.asList(Name.PACKAGE_ITEM, Name.JAVA_COMPILATION_UNIT));

	/**
	 * The pattern used to identify a package and class name.
	 */
	private static final Pattern PACKAGE_CLASS_PATTERN = Pattern.compile("(\\w*(\\.\\w*)*)\\.(\\w+)");

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.CLASS_DEPENDENCIES_LOADED));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadJavaSourceClassDependenciesTask(String name, LoadingContext context) {
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
		JavaItem packageItem = context.get(Name.PACKAGE_ITEM);
		CompilationUnit compUnit = context.get(Name.JAVA_COMPILATION_UNIT);

		if (compUnit.getProblems() != null && compUnit.getProblems().length > 0) {
			boolean broken = false;
			IProblem reason = null;
			for (IProblem problem : compUnit.getProblems()) {
				if (problem.isError()) {
					broken = true;
					reason = problem;
					break;
				}
			}

			if (broken) {
				System.out.println("Compile errors in " + packageItem + ":"
						+ ((TypeDeclaration) compUnit.types().get(0)).getName() + ": " + reason.getMessage());
				context.put(Name.CLASS_DEPENDENCIES_LOADED, true);
				return;
			}
		}

		try {
			Map<JavaItem, ExternalClasses> classToExternalMap = findImports(packageItem, compUnit);

			JavaItem project = packageItem.getParent();

			for (JavaItem javaClass : classToExternalMap.keySet()) {
				ExternalClasses classes = classToExternalMap.get(javaClass);

				addResolvedClasses(javaClass, classes, project);
				addUnresolvedClasses(javaClass, classes, project);
			}

			// add the supers now that all dependencies are resolved
			SupersVisitor supersVisitor = new SupersVisitor(packageItem);
			compUnit.accept(supersVisitor);
		} catch (JavaModelException e) {
			e.printStackTrace();

		}

		context.put(Name.CLASS_DEPENDENCIES_LOADED, true);
	}

	/**
	 * Finds the imports and other classes used in the given compilation unit.
	 * 
	 * @param packageItem
	 *            The package that contains the file for the given compilation
	 *            unit. This value cannot be null.
	 * @param compUnit
	 *            The compilation unit that was loaded from a Java file. This
	 *            value cannot be null.
	 * 
	 * @return A mapping from class items to the classes that are used by it.
	 *         This value will not be null, but may be empty if the file has no
	 *         classes in it.
	 * 
	 * @throws JavaModelException
	 *             If an error occurs while analyzing the nodes.
	 */
	private Map<JavaItem, ExternalClasses> findImports(JavaItem packageItem, CompilationUnit compUnit)
			throws JavaModelException {
		TypeVisitor typeVisitor = new TypeVisitor(packageItem);
		compUnit.accept(typeVisitor);
		ImportVisitor visitor = new ImportVisitor(packageItem, typeVisitor.getImports());
		compUnit.accept(visitor);

		return visitor.getImports();
	}

	/**
	 * Adds the unresolved class dependencies to the given class item.
	 * Unresolved class dependencies are uses of other classes by name, which
	 * are not explicitly imported, but may be covered by a star (*) import. A
	 * best effort will be made to find the appropriate package and class in the
	 * dependencies of the given project. If that fails, new packages or classes
	 * will be created as needed
	 * 
	 * @param javaClass
	 *            The class item that refers to the other classes. This value
	 *            cannot be null.
	 * @param classes
	 *            The classes that are referred to. This value cannot be null.
	 * @param project
	 *            The project that contains the given class item, which is used
	 *            to search for used packages and classes. This value cannot be
	 *            null.
	 */
	private void addUnresolvedClasses(JavaItem javaClass, ExternalClasses classes, JavaItem project) {
		for (String className : classes.getUnresolved()) {
			JavaItem nullProjectFound = null;
			JavaItem found = null;
			for (String packageName : classes.getStarImports()) {
				JavaItem javaPackage = getPackage(project, packageName, className);
				if (javaPackage.getParent() != null) {
					found = javaPackage;
					break;
				} else if (nullProjectFound == null) {
					nullProjectFound = javaPackage;
				}
			}

			if (found == null && nullProjectFound == null) {
				System.out.println("Could not find package for class " + className + " referenced by class "
						+ javaClass.getName());
			} else {
				JavaItem packageUsed;
				if (found != null) {
					packageUsed = found;
				} else {
					packageUsed = nullProjectFound;
				}

				JavaItem dependentClass = getClassValue(packageUsed, className);
				if (!javaClass.getDependenciesIDs().contains(dependentClass.getID())) {
					javaClass.getDependenciesIDs().add(dependentClass.getID());
					dependentClass.getIncomingIDs().add(javaClass.getID());
				}
			}

		}
	}

	/**
	 * Adds the unresolved class dependencies to the given class item. Resolved
	 * class dependencies are uses of full class names (with package name), or
	 * other classes by name which are explicitly imported. A best effort will
	 * be made to find the appropriate package and class in the dependencies of
	 * the given project. If that fails, new packages or classes will be created
	 * as needed
	 * 
	 * @param javaClass
	 *            The class item that refers to the other classes. This value
	 *            cannot be null.
	 * @param classes
	 *            The classes that are referred to. This value cannot be null.
	 * @param project
	 *            The project that contains the given class item, which is used
	 *            to search for used packages and classes. This value cannot be
	 *            null.
	 */
	private void addResolvedClasses(JavaItem javaClass, ExternalClasses classes, JavaItem project) {
		Check.notNull(javaClass, "javaClass");

		for (String fullName : classes.getResolved()) {
			Matcher matcher = PACKAGE_CLASS_PATTERN.matcher(fullName);
			if (!matcher.matches()) {
				throw new IllegalStateException("Could not parse " + fullName + " into a package and class name.");
			}

			String packageName = matcher.group(1);
			String className = matcher.group(3);

			JavaItem javaPackage = getPackage(project, packageName, className);

			JavaItem dependentClass = getClassValue(javaPackage, className);
			Check.notNull(dependentClass, "dependentClass");
			if (!javaClass.getDependenciesIDs().contains(dependentClass.getID())) {
				javaClass.getDependenciesIDs().add(dependentClass.getID());
				dependentClass.getIncomingIDs().add(javaClass.getID());
			}
		}
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
	public JavaItem getPackage(JavaItem project, String packageName, String className) {
		JavaItem found = null;

		// first look in packages in the projects in the order that they occur
		// in the project manifest
		// ensure the named project is the first to look for the package in
		List<JavaItem> projectsToSearch = new ArrayList<JavaItem>(project.getDependencies());
		projectsToSearch.add(0, project);
		for (JavaItem currentProject : projectsToSearch) {
			JavaItem possiblePackage = getContext().getIndex().findItem(currentProject, packageName,
					JavaItemType.PACKAGE);
			JavaItem foundClass = getContext().getIndex().findItem(possiblePackage, className, JavaItemType.CLASS);
			if (foundClass != null) {
				found = foundClass.getParent();
				break;
			}
		}

		if (found == null) {
			// try to find the package in any project, even though it might not
			// be in the manifest
			List<JavaItem> allPackages = getContext().getIndex().findPackages(packageName);
			found = findClassInPackage(allPackages, packageName, className, false);
		}

		if (found == null) {
			// find or create an orphaned package - a package with no project
			found = getContext().getFactory().createPackage(null, packageName);
		}

		return found;
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
	public JavaItem getClassValue(JavaItem packageValue, String className) {
		JavaItem found = getContext().getIndex().findItem(packageValue, className, JavaItemType.CLASS);

		if (found == null) {
			found = getContext().getFactory().createClass(packageValue, className);
			packageValue.getChildrenIDs().add(found.getID());
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
	private JavaItem findClassInPackage(List<JavaItem> packages, String packageName, String className,
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
	 * This class just finds all class declarations in a compilation unit.
	 */
	private class TypeVisitor extends ASTVisitor {

		private JavaItem packageItem;

		/**
		 * The classes that were found.
		 */
		private ExternalClasses classes;

		/**
		 * The stack of TypeDeclarations being visited currently. The top of the
		 * stack contains the last class visited.
		 */
		private Stack<JavaItem> lastNonMemberClassStack = new Stack<>();

		/**
		 * Constructor for this.
		 * 
		 * @param packageItem
		 *            The package item for the package which contains the
		 *            compilation unit. This value cannot be null, but may be
		 *            empty.
		 */
		public TypeVisitor(JavaItem packageItem) {
			this.packageItem = packageItem;

			classes = new ExternalClasses(packageItem.getName(), null);
		}

		/**
		 * Returns the classes that were found.
		 * 
		 * @return The classes that were found. This value will not be null.
		 */
		public ExternalClasses getImports() {
			return classes;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(TypeDeclaration node) {
			if (isUseful(node)) {
				String fullyQualifiedName = node.getName().getFullyQualifiedName();

				JavaItem lastNonMemberClass = lastNonMemberClassStack.isEmpty() ? null : lastNonMemberClassStack.peek();
				if (!node.isPackageMemberTypeDeclaration() && lastNonMemberClass != null) {
					fullyQualifiedName = lastNonMemberClass.getName() + "$" + fullyQualifiedName;
				}

				JavaItem javaClass = getContext().getIndex().findItem(packageItem, fullyQualifiedName,
						JavaItemType.CLASS);

				classes.addClassDeclaration(fullyQualifiedName);

				lastNonMemberClassStack.push(javaClass);
			}

			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void endVisit(TypeDeclaration node) {
			if (isUseful(node)) {
				lastNonMemberClassStack.pop();
			}

			super.endVisit(node);
		}
	}

	/**
	 * This class loads all imports and finds all uses of other classes in a
	 * compilation unit.
	 */
	private class ImportVisitor extends ASTVisitor {

		/**
		 * The package which contains the classes in the compilation unit.
		 */
		private JavaItem packageItem;

		/**
		 * The base classes and imports that are found.
		 */
		private ExternalClasses classes;

		/**
		 * A mapping from classes in the compilation unit to classes that are
		 * used by each class.
		 */
		private Map<JavaItem, ExternalClasses> classToExternalMap = new HashMap<>();

		/**
		 * The current class whose members are now being visited.
		 */
		private JavaItem currentClass;

		/**
		 * The current accumulation of usages of other classes that are found in
		 * the current class.
		 */
		private ExternalClasses currentExternalClasses;

		/**
		 * The stack of TypeDeclarations being visited currently. The top of the
		 * stack contains the last class visited.
		 */
		private Stack<JavaItem> lastNonMemberClassStack = new Stack<>();

		/**
		 * Constructor for this.
		 * 
		 * @param packageItem
		 *            The package where the compilation unit was found. This
		 *            value cannot be null.
		 * @param classes
		 *            The object with only the class declarations. This value
		 *            cannot be null.
		 */
		public ImportVisitor(JavaItem packageItem, ExternalClasses classes) {
			Check.notNull(packageItem, "packageItem");
			Check.notNull(classes, "classes");

			this.packageItem = packageItem;
			this.classes = classes;
		}

		/**
		 * Returns the classes in this and the uses of other classes in each of
		 * those classes.
		 * 
		 * @return A mapping from classes in the compilation unit to classes
		 *         that are used by each class. This value will not be null.
		 */
		public Map<JavaItem, ExternalClasses> getImports() {
			return classToExternalMap;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(ImportDeclaration node) {
			String type = node.getName().toString();

			if (node.isOnDemand()) {
				classes.addOnDemandImports(type);
			} else {
				classes.addImport(type);
			}

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(TypeDeclaration node) {
			if (!allow(node.getModifiers(), packageItem)) {
				// it's private, ignore it
				return super.visit(node);
			}

			if (isUseful(node)) {
				String fullyQualifiedName = node.getName().getFullyQualifiedName();

				JavaItem lastNonMemberClass = lastNonMemberClassStack.isEmpty() ? null : lastNonMemberClassStack.peek();
				if (!node.isPackageMemberTypeDeclaration() && lastNonMemberClass != null) {
					fullyQualifiedName = lastNonMemberClass.getName() + "$" + fullyQualifiedName;
				}

				currentClass = getContext().getIndex().findItem(packageItem, fullyQualifiedName, JavaItemType.CLASS);
				currentExternalClasses = classes.copy();

				if (currentClass == null) {
					System.out.println("Could not find class " + packageItem + ":" + fullyQualifiedName);

					// push something so that it can be popped by
					// endVisit(TypeDeclaration)
					lastNonMemberClassStack.push(null);
					return false;
				}

				// add the supers to the external classes referenced
				Type superClassType = node.getSuperclassType();
				if (superClassType != null) {
					String superClassName = getContext().getUtil().getNameForType(superClassType, currentClass);
					currentExternalClasses.addUsage(superClassName);
				}

				for (Object o : node.superInterfaceTypes()) {
					Type interfaceType = (Type) o;
					String interfaceName = getContext().getUtil().getNameForType(interfaceType, currentClass);
					currentExternalClasses.addUsage(interfaceName);
				}

				classToExternalMap.put(currentClass, currentExternalClasses);

				lastNonMemberClassStack.push(currentClass);
			}

			return super.visit(node);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void endVisit(TypeDeclaration node) {
			if (!allow(node.getModifiers(), packageItem)) {
				// it's private, ignore it
				return;
			}

			if (isUseful(node)) {
				lastNonMemberClassStack.pop();
				currentClass = lastNonMemberClassStack.isEmpty() ? null : lastNonMemberClassStack.peek();
				currentExternalClasses = classToExternalMap.get(currentClass);
			}

			super.endVisit(node);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(QualifiedType node) {
			System.out.println("QType " + node);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(SimpleType node) {
			if (currentExternalClasses != null) {
				currentExternalClasses.addUsage(node.toString());
			} else {
				// it's an enum
			}

			return false;
		}

	}

	/**
	 * This class adds the superclass and super interfaces to each class after
	 * the dependencies of the classes are figured out.
	 */
	private class SupersVisitor extends ASTVisitor {

		private JavaItem packageItem;

		/**
		 * The stack of TypeDeclarations being visited currently. The top of the
		 * stack contains the last class visited.
		 */
		private Stack<JavaItem> lastNonMemberClassStack = new Stack<>();

		/**
		 * Constructor for this.
		 * 
		 * @param packageItem
		 *            The package item for the package which contains the
		 *            compilation unit. This value cannot be null, but may be
		 *            empty.
		 */
		public SupersVisitor(JavaItem packageItem) {
			this.packageItem = packageItem;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(TypeDeclaration node) {
			if (isUseful(node)) {
				String fullyQualifiedName = node.getName().getFullyQualifiedName();

				JavaItem lastNonMemberClass = lastNonMemberClassStack.isEmpty() ? null : lastNonMemberClassStack.peek();
				if (!node.isPackageMemberTypeDeclaration() && lastNonMemberClass != null) {
					fullyQualifiedName = lastNonMemberClass.getName() + "$" + fullyQualifiedName;
				}

				JavaItem javaClass = getContext().getIndex().findItem(packageItem, fullyQualifiedName,
						JavaItemType.CLASS);

				if (javaClass == null) {
					System.out.println(
							"Source of class was found (likely in a JAR) but the class file is missing, ignoring: "
									+ packageItem + ":" + fullyQualifiedName);

					// we push something here so that the pop() works in
					// endVisit(TypeDeclaration)
					lastNonMemberClassStack.push(null);
					return false;
				}

				// add the superclass and superinterfaces here
				JavaItem superClass = getContext().getUtil().findClassForType(node.getSuperclassType(), javaClass);
				Integer superClassID;
				if (superClass != null) {
					superClassID = superClass.getID();
				} else {
					superClassID = null;
				}

				javaClass.setAttribute(JavaItem.ATTR_SUPERCLASS, superClassID);

				Set<Integer> superInterfaceIDs = javaClass.getAttribute(JavaItem.ATTR_SUPERINTERFACES);
				if (superInterfaceIDs == null) {
					superInterfaceIDs = new LinkedHashSet<>();
					javaClass.setAttribute(JavaItem.ATTR_SUPERINTERFACES, superInterfaceIDs);
				}
				for (Object o : node.superInterfaceTypes()) {
					JavaItem interfaceType = getContext().getUtil().findClassForType((Type) o, javaClass);
					if (interfaceType != null) {
						superInterfaceIDs.add(interfaceType.getID());
					}
				}

				lastNonMemberClassStack.push(javaClass);
			}

			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void endVisit(TypeDeclaration node) {
			if (isUseful(node)) {
				lastNonMemberClassStack.pop();
			}

			super.endVisit(node);
		}
	}

}