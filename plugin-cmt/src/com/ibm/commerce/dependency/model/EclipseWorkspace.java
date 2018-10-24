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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.DebugUtil;
import com.ibm.commerce.dependency.model.eclipse.IJavaProjectWrapper;
import com.ibm.commerce.dependency.model.eclipse.IPackageFragmentWrapper;
import com.ibm.commerce.dependency.model.eclipse.IWorkspaceWrapper;

/**
 * EclipseWorkspace is a specific workspace implementation for Eclipse.
 * 
 * @author Trent Hoeppner
 */
public class EclipseWorkspace implements Workspace {

	private static final Pattern PACKAGE_CLASS_PATTERN = Pattern.compile("(\\w*(\\.\\w*)*)\\.(\\w+)");

	private ProjectLoader loader;

	private Set<JavaItem> packagesWithClassesLoaded = new LinkedHashSet<JavaItem>();

	private Set<JavaItem> classesWithDependenciesLoaded = new LinkedHashSet<JavaItem>();

	private IWorkspaceWrapper workspace;

	private JavaItemFactory factory;

	private boolean privateVisible;

	/**
	 * Constructor for this.
	 *
	 * @param workspace
	 *            The wrapper that is used to find projects and items within
	 *            them. Cannot be null.
	 * @param loader
	 *            The object used to load project dependencies. Cannot be null.
	 */
	public EclipseWorkspace(IWorkspaceWrapper workspace, ProjectLoader loader, JavaItemFactory factory,
			boolean privateVisible) {
		if (workspace == null) {
			throw new NullPointerException("workspace cannot be null.");
		}

		if (loader == null) {
			throw new NullPointerException("loader cannot be null.");
		}

		Check.notNull(factory, "factory");

		this.workspace = workspace;
		this.loader = loader;
		this.factory = factory;
		this.privateVisible = privateVisible;
	}

	/**
	 * Returns the workspace wrapper that allows projects to be retrieved.
	 * 
	 * @return The workspace wrapper. This value will not be null.
	 */
	public IWorkspaceWrapper getWrapper() {
		return workspace;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<JavaItem> getProjects() throws IOException {
		List<JavaItem> projects = new ArrayList<JavaItem>();

		for (IJavaProjectWrapper javaProject : workspace.getProjects()) {
			String name = javaProject.getName();
			File manifestFile = javaProject.getManifestFile();

			JavaItem project;
			if (manifestFile != null) {
				project = loader.load(name, manifestFile);
			} else {
				project = loader.load(name, "");
			}

			project.setAttribute(JavaItem.ATTR_PROJECT_PRIVATE_VISIBLE, privateVisible);
			project.setAttribute(JavaItem.ATTR_BINARY, javaProject.isBinary());

			projects.add(project);
		}

		return projects;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void findPackages(JavaItem project) {
		if (project == null) {
			throw new NullPointerException("project cannot be null.");
		}

		for (IJavaProjectWrapper javaProject : workspace.getProjects()) {
			String eclipseProjectName = javaProject.getName();
			if (project.getName().equals(eclipseProjectName)) {
				addSourcePackages(project, javaProject);
				break;
			}
		}
	}

	/**
	 * Adds the package fragments from the given project wrapper to the given
	 * project.
	 * 
	 * @param project
	 *            The project to add packages to. This value cannot be null.
	 * @param javaProject
	 *            The project to get package fragments from. This value cannot
	 *            be null.
	 */
	private void addSourcePackages(JavaItem project, IJavaProjectWrapper javaProject) {
		for (IPackageFragmentWrapper fragment : javaProject.getFragments()) {
			String packageName = fragment.getName();
			JavaItem javaPackage = factory.createPackage(project, packageName);
			javaPackage.setType(JavaItemType.PACKAGE);
			javaPackage.setAttribute(JavaItem.ATTR_BINARY, fragment.isBinary());
			project.getChildrenIDs().add(javaPackage.getID());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void findClasses(JavaItem javaPackage) {
		if (javaPackage == null) {
			throw new NullPointerException("javaPackage cannot be null.");
		}

		IPackageFragmentWrapper fragment = getPackageFragment(javaPackage);

		for (File sourceFile : fragment.getFiles()) {
			String className;
			if (!fragment.isBinary()) {
				className = sourceFile.getName().replace(".java", "");
			} else {
				className = sourceFile.getName().replace(".class", "");
			}
			DebugUtil.stopAtClassName(className, "before creating the class value");
			JavaItem javaClass = JavaItemUtil.getClassValue(javaPackage, className);
			javaClass.setAttribute(JavaItem.ATTR_BINARY, fragment.isBinary());
			// javaPackage.getChildren().add(javaClass);
		}
		packagesWithClassesLoaded.add(javaPackage);
	}

	/**
	 * Must be called after all the class dependencies are resolved
	 */
	public void findMethods(JavaItem javaClass) {
		if (javaClass == null) {
			throw new NullPointerException("javaClass cannot be null.");
		}

		boolean binary = javaClass.getAttribute(JavaItem.ATTR_BINARY);
		if (!binary) {
			IPackageFragmentWrapper fragment = getPackageFragment(javaClass.getParent());
			CompilationUnit compUnit = getCompilationUnit(javaClass, fragment);
			ClassInternalVisitor visitor = new ClassInternalVisitor(javaClass);
			compUnit.accept(visitor);
			visitor.addDefaultConstructor();
		} else {
			String className = JavaItemUtil.getFullClassNameForType(javaClass);
			try {
				Class<?> type = JavaItemUtil.classForName(className);
				for (Method method : type.getDeclaredMethods()) {
					String name = method.getName();
					Parameter[] parameters = method.getParameters();
					int modifiers = method.getModifiers();
					addMethod(javaClass, name, parameters, modifiers);
				}

				for (Constructor<?> constructor : type.getConstructors()) {
					String name = javaClass.getName();
					Parameter[] parameters = constructor.getParameters();
					int modifiers = constructor.getModifiers();
					addMethod(javaClass, name, parameters, modifiers);
				}
			} catch (ClassNotFoundException e) {
				System.out.println("Could not classload " + className + " to get methods");
			} catch (NoClassDefFoundError e) {
				System.out.println("Could not classload methods for " + className + " due to missing library");
			} catch (IncompatibleClassChangeError e) {
				System.out.println("Could not classload methods for " + className + " due to class incompatibilty");
			} catch (VerifyError e) {
				System.out
						.println("Could not classload methods for " + className + " due to class verified incorrectly");
			}
		}
	}

	public void addMethod(JavaItem javaClass, String name, Parameter[] methodParameters, int modifiers) {
		if ((modifiers & Modifier.PUBLIC) > 0 || (modifiers & Modifier.PROTECTED) > 0) {
			List<Integer> parameterIDs = new ArrayList<>();
			for (Parameter parameter : methodParameters) {
				Class<?> parameterType = parameter.getType();
				JavaItem parameterTypeItem = findClassForClass(javaClass, parameterType);
				if (parameterTypeItem == null) {
					parameterTypeItem = JavaItemUtil.getWildcardType();
				}
				Check.notNull(parameterTypeItem, "paramTypeItem");
				parameterIDs.add(parameterTypeItem.getID());
			}

			JavaItem javaMethod = factory.createMethod(javaClass, name, parameterIDs);
			javaClass.getChildrenIDs().add(javaMethod.getID());
		}
	}

	private JavaItem findClassForClass(JavaItem javaClass, Class<?> parameterType) {
		String fullName = parameterType.getCanonicalName();

		JavaItem parameterTypeItem;
		if (fullName != null) {
			parameterTypeItem = JavaItemUtil.findClassForName(fullName, javaClass);
		} else {
			parameterTypeItem = null;
		}
		return parameterTypeItem;
	}

	public void findMethodDependencies(JavaItem javaClass) {
		boolean binary = javaClass.getAttribute(JavaItem.ATTR_BINARY);
		if (binary) {
			// we don't look for method dependencies in binary java classes
			return;
		}

		IPackageFragmentWrapper fragment = getPackageFragment(javaClass.getParent());
		CompilationUnit compUnit = getCompilationUnit(javaClass, fragment);
		MethodDepVisitor visitor = new MethodDepVisitor(javaClass);
		compUnit.accept(visitor);
	}

	public List<JavaItem> findNodeDependencies(JavaItem javaClass, ASTNode node) {
		JavaItem fakeMethod = null;
		ASTNode current = node;
		while (current != null) {
			if (current instanceof MethodDeclaration) {
				MethodDeclaration m = (MethodDeclaration) current;
				fakeMethod = createFakeMethod(m, javaClass);
				break;
			} else if (current instanceof Initializer) {
				// TODO usually it should be zero but this could be wrong
				fakeMethod = createFakeMethod(0);
				break;
			}
			current = current.getParent();
		}

		JavaItem caller = fakeMethod;

		List<JavaItem> dependentMethods = Collections.emptyList();
		if (current != null) {
			MethodDepVisitor visitor = new MethodDepVisitor(javaClass, caller, node);
			node.accept(visitor);

			dependentMethods = caller.getDependencies();
		}

		return dependentMethods;
	}

	// private or default
	private boolean isPrivateVisible(JavaItem item) {
		boolean visible = false;
		JavaItem current = item;
		while (current != null && current.getType() != JavaItemType.PROJECT) {
			current = current.getParent();
		}

		if (current.getType() == JavaItemType.PROJECT) {
			visible = current.getAttribute(JavaItem.ATTR_PROJECT_PRIVATE_VISIBLE);
		}

		return visible;
	}

	private IPackageFragmentWrapper getPackageFragment(JavaItem javaPackage) {
		JavaItem javaProject = javaPackage.getParent();

		IJavaProjectWrapper project = getProject(javaProject);

		IPackageFragmentWrapper fragment = null;

		for (IPackageFragmentWrapper fragment2 : project.getFragments()) {
			if (fragment2.getName().equals(javaPackage.getName())) {
				fragment = fragment2;
				break;
			}
		}

		return fragment;
	}

	private IJavaProjectWrapper getProject(JavaItem javaProject) {
		IJavaProjectWrapper project = null;

		String projectName = javaProject.getName();

		for (IJavaProjectWrapper internalProject : workspace.getProjects()) {

			String eclipseProjectName = internalProject.getName();
			if (projectName.equals(eclipseProjectName)) {
				project = internalProject;
				break;
			}

		}
		return project;
	}

	@Override
	public void findClassDependencies(JavaItem javaClass) {
		if (javaClass == null) {
			throw new NullPointerException("javaClass cannot be null.");
		}

		boolean binary = javaClass.getAttribute(JavaItem.ATTR_BINARY);
		if (binary) {
			// ignore class dependencies in binary files
			return;
		}

		IPackageFragmentWrapper fragment = getPackageFragment(javaClass.getParent());
		CompilationUnit compUnit = getCompilationUnit(javaClass, fragment);

		try {
			ExternalClasses classes = findImports(javaClass.getParent().getName(), javaClass.getName(), compUnit);

			JavaItem project = javaClass.getParent().getParent();

			addResolvedClasses(javaClass, classes, project);
			addUnresolvedClasses(javaClass, classes, project);
		} catch (JavaModelException e) {
			e.printStackTrace();

		}

		classesWithDependenciesLoaded.add(javaClass);
	}

	@SuppressWarnings("unused")
	private static ExternalClasses findImports(String packageName, String className, CompilationUnit compUnit)
			throws JavaModelException {
		TypeVisitor typeVisitor = new TypeVisitor(packageName, className);
		compUnit.accept(typeVisitor);
		ImportVisitor visitor = new ImportVisitor(typeVisitor.getImports());
		compUnit.accept(visitor);

		return visitor.getImports();
	}

	private void addUnresolvedClasses(JavaItem javaClass, ExternalClasses classes, JavaItem project) {
		for (String className : classes.getUnresolved()) {
			if (className.equals("AbstractAccessBean")) {
				System.out.println("found AbstractAccessBean");
			}

			JavaItem nullProjectFound = null;
			JavaItem found = null;
			for (String packageName : classes.getStarImports()) {
				JavaItem javaPackage = JavaItemUtil.getPackage(project, packageName, className);
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

				JavaItem dependentClass = JavaItemUtil.getClassValue(packageUsed, className);
				if (!javaClass.getDependenciesIDs().contains(dependentClass.getID())) {
					javaClass.getDependenciesIDs().add(dependentClass.getID());
					dependentClass.getIncomingIDs().add(javaClass.getID());
				}
			}

		}
	}

	private void addResolvedClasses(JavaItem javaClass, ExternalClasses classes, JavaItem project) {
		for (String fullName : classes.getResolved()) {
			Matcher matcher = PACKAGE_CLASS_PATTERN.matcher(fullName);
			if (!matcher.matches()) {
				throw new IllegalStateException("Could not parse " + fullName + " into a package and class name.");
			}

			String packageName = matcher.group(1);
			String className = matcher.group(3);

			if (packageName.equals("javax.persistence") && className.equals("EntityExistsException")) {
				System.out.println("found javax.persistence.EntityExistsException");
			}

			JavaItem javaPackage = JavaItemUtil.getPackage(project, packageName, className);

			JavaItem dependentClass = JavaItemUtil.getClassValue(javaPackage, className);
			if (!javaClass.getDependenciesIDs().contains(dependentClass.getID())) {
				javaClass.getDependenciesIDs().add(dependentClass.getID());
				dependentClass.getIncomingIDs().add(javaClass.getID());
			}
		}
	}

	private CompilationUnit getCompilationUnit(JavaItem javaClass, IPackageFragmentWrapper fragment) {
		File sourceFile = new File(fragment.getDir(), javaClass.getName() + ".java");
		return getCompilationUnit(sourceFile);
	}

	private CompilationUnit getCompilationUnit(File sourceFile) {
		String stringDoc = getFileContents(sourceFile);

		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(stringDoc.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		CompilationUnit compUnit = (CompilationUnit) parser.createAST(null);
		return compUnit;
	}

	private String getFileContents(File file) {
		StringBuffer buffer = new StringBuffer();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = reader.readLine();
			while (line != null) {
				buffer.append(line).append("\n");
				line = reader.readLine();
			}
		} catch (IOException e) {
			// warning but not stop the execution
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// swallow to allow th emain exception to escape
				}
			}
		}

		String stringDoc = buffer.toString();
		return stringDoc;
	}

	@Override
	public void findPackageDependencies(JavaItem javaPackage) {
		if (javaPackage == null) {
			throw new NullPointerException("javaPackage cannot be null.");
		}

		if (!packagesWithClassesLoaded.contains(javaPackage)) {
			throw new IllegalStateException("Classes have not been loaded for package " + javaPackage.toString());
		}

		Set<Integer> dependencies = new LinkedHashSet<>();
		for (JavaItem currentClass : javaPackage.getChildren()) {
			if (!classesWithDependenciesLoaded.contains(currentClass)) {
				throw new IllegalStateException(
						"Dependencies have not been loaded for class " + currentClass.toString());
			}

			for (JavaItem currentDependency : currentClass.getDependencies()) {
				JavaItem otherPackage = currentDependency.getParent();
				if (otherPackage != javaPackage) {
					dependencies.add(otherPackage.getID());
					otherPackage.getIncomingIDs().add(javaPackage.getID());
				}
			}
		}

		javaPackage.getDependenciesIDs().addAll(dependencies);
	}

	private JavaItem createFakeMethod(MethodDeclaration node, JavaItem javaClass) {
		List<Integer> parameterTypeIDs = getMethodParameters(node, javaClass);

		String name = node.getName().getFullyQualifiedName();
		JavaItem method = factory.createUntracked(name, JavaItemType.METHOD);
		method.setAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES, parameterTypeIDs);

		return method;
	}

	private JavaItem createMethod(JavaItem parent, MethodDeclaration node, JavaItem javaClass) {
		List<Integer> parameterTypeIDs = getMethodParameters(node, javaClass);

		String name = node.getName().getFullyQualifiedName();
		JavaItem method = factory.createMethod(parent, name, parameterTypeIDs);

		return method;
	}

	private List<Integer> getMethodParameters(MethodDeclaration node, JavaItem javaClass) {
		List<Integer> parameterTypeIDs = new ArrayList<>();
		for (Object parameter : node.parameters()) {
			SingleVariableDeclaration p = (SingleVariableDeclaration) parameter;
			JavaItem parameterType = JavaItemUtil.findClassForType(p.getType(), javaClass);
			if (parameterType == null) {
				parameterType = JavaItemUtil.getWildcardType();
			}
			parameterTypeIDs.add(parameterType.getID());
		}
		return parameterTypeIDs;
	}

	private JavaItem createFakeMethod(int initializerIndex) {
		String name = "#initializer" + initializerIndex;
		JavaItem method = factory.createUntracked(name, JavaItemType.METHOD);
		method.setAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES, Collections.emptyList());

		return method;
	}

	private JavaItem createMethod(JavaItem parent, int initializerIndex) {
		String name = "#initializer" + initializerIndex;
		JavaItem method = factory.createMethod(parent, name, Collections.emptyList());

		return method;
	}

	private boolean allow(int modifiers, JavaItem item) {
		int protectedBit = modifiers & Modifier.PROTECTED;
		int publicBit = modifiers & Modifier.PUBLIC;
		boolean isPublic = publicBit > 0 || protectedBit > 0;
		return isPublic || isPrivateVisible(item);
	}

	public JavaItemIndex getJavaItemIndex() {
		return factory.getIndex();
	}

	private class ClassInternalVisitor extends ASTVisitor {

		private JavaItem javaClass;

		private int initializerIndex = 0;

		public ClassInternalVisitor(JavaItem javaClass) {
			this.javaClass = javaClass;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			// TODO this will include methods in nested classes, need to verify
			// that the containing class is the same as javaClass
			if (node.getName().getFullyQualifiedName().equals("getEntityManager")) {
				System.out.println("found getEntityManager");
			}
			if (!allow(node.getModifiers(), javaClass)) {
				// it's private, ignore it
				return super.visit(node);
			}

			JavaItem method = createMethod(javaClass, node, javaClass);
			javaClass.getChildrenIDs().add(method.getID());

			JavaItem returnType = JavaItemUtil.findClassForType(node.getReturnType2(), javaClass);
			Integer returnTypeID;
			if (returnType != null) {
				returnTypeID = returnType.getID();
			} else {
				returnTypeID = null;
			}
			method.setAttribute(JavaItem.ATTR_RETURN_TYPE, returnTypeID);

			List<Integer> exceptionTypeIDs = new ArrayList<>();
			for (Object o : node.thrownExceptionTypes()) {
				Type type = (Type) o;
				JavaItem exceptionType = JavaItemUtil.findClassForType(type, javaClass);
				if (exceptionType == null) {
					System.out.println("exceptionType not found for " + type.toString() + " for method " + method);
					continue;
				}
				exceptionTypeIDs.add(exceptionType.getID());
			}
			method.setAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES, exceptionTypeIDs);

			return super.visit(node);
		}

		public void addDefaultConstructor() {
			// check if the default constructor already exists
			String constructorName = javaClass.getName();
			for (JavaItem method : javaClass.getChildren()) {
				if (method.getName().equals(constructorName) && method.getChildren().isEmpty()) {
					// it already has one
					return;
				}
			}

			// it doesn't exist, add it
			JavaItem method = factory.createMethod(javaClass, constructorName, Collections.emptyList());
			javaClass.getChildrenIDs().add(method.getID());

			method.setAttribute(JavaItem.ATTR_RETURN_TYPE, null);
		}

		@Override
		public boolean visit(Initializer node) {
			if (!allow(node.getModifiers(), javaClass)) {
				// it's private, ignore it
				return super.visit(node);
			}

			// this is like a method, it can call other methods
			JavaItem method = createMethod(javaClass, initializerIndex++);
			javaClass.getChildrenIDs().add(method.getID());

			return super.visit(node);
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			if (!allow(node.getModifiers(), javaClass)) {
				// it's private, ignore it
				return super.visit(node);
			}

			JavaItem type = JavaItemUtil.findClassForType(node.getType(), javaClass);
			for (Object o : node.fragments()) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
				String fieldName = fragment.getName().getFullyQualifiedName();
				JavaItem field = factory.createField(javaClass, fieldName);
				field.setType(JavaItemType.FIELD);
				Integer typeID;
				if (type != null) {
					typeID = type.getID();
				} else {
					typeID = null;
				}
				field.setAttribute(JavaItem.ATTR_FIELD_TYPE, typeID);
				javaClass.getChildrenIDs().add(field.getID());
			}

			return super.visit(node);
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			if (node.getName().getFullyQualifiedName().equals("AbstractJpaEntityAccessBean")) {
				System.out.println("found AbstractJpaEntityAccessBean");
			}
			if (!allow(node.getModifiers(), javaClass)) {
				// it's private, ignore it
				return super.visit(node);
			}

			if (node.isPackageMemberTypeDeclaration()) {
				String fullyQualifiedName = node.getName().getFullyQualifiedName();
				if (fullyQualifiedName.equals("AttributeAccessBean")) {
					System.out.println("found AttributeAccessBean");
				}
				JavaItem superClass = JavaItemUtil.findClassForType(node.getSuperclassType(), javaClass);
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
					JavaItem interfaceType = JavaItemUtil.findClassForType((Type) o, javaClass);
					if (interfaceType != null) {
						superInterfaceIDs.add(interfaceType.getID());
					}
				}
			}

			return super.visit(node);
		}

	}

	private class MethodDepVisitor extends VariableVisitor {

		private JavaItem caller;

		private int initializerIndex = 0;

		public MethodDepVisitor(JavaItem javaClass) {
			super(null, javaClass, null, null);
		}

		// used to restrict to calls within the ancestor node
		public MethodDepVisitor(JavaItem javaClass, JavaItem caller, ASTNode ancestor) {
			super(null, javaClass, ancestor, null);
			Check.notNull(caller, "caller");

			this.caller = caller;
		}

		protected boolean canVisit(ASTNode node) {
			return caller != null && super.canVisit(node);
		}

		private boolean isCollectingMode() {
			return ancestor != null;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			if (!allow(node.getModifiers(), javaClass)) {
				// it's private, ignore it
				return super.visit(node);
			}

			JavaItem fakeMethod = createFakeMethod(node, javaClass);
			for (JavaItem m : javaClass.getChildren()) {
				if (m.getType() == JavaItemType.METHOD && JavaItemUtil.isMethodsEqual(m, fakeMethod)) {
					caller = m;
					break;
				}
			}

			return super.visit(node);
		}

		@Override
		public void endVisit(MethodDeclaration node) {
			caller = null;
			super.endVisit(node);
		}

		@Override
		public boolean visit(Initializer node) {
			JavaItem fakeMethod = createFakeMethod(initializerIndex++);
			for (JavaItem m : javaClass.getChildren()) {
				if (m.getType() == JavaItemType.METHOD && JavaItemUtil.isMethodsEqual(m, fakeMethod)) {
					caller = m;
					break;
				}
			}

			return super.visit(node);
		}

		@Override
		public void endVisit(Initializer node) {
			caller = null;
			super.endVisit(node);
		}

		@Override
		public boolean visit(MethodInvocation node) {
			if (!canVisit(node)) {
				return false;
			}

			SimpleName nodeName = node.getName();
			List<?> nodeArguments = node.arguments();
			JavaItem fakeMethod = JavaItemUtil.createFakeMethodUsingScope(nodeName, nodeArguments, javaClass, scope);

			JavaItem calledMethod = null;
			JavaItem classForMethod = JavaItemUtil.getTypeForExpression(node.getExpression(), javaClass, scope);
			if (classForMethod == null) {
				// no prefix, it means the method is in this class or a
				// superclass
				calledMethod = JavaItemUtil.findMethodInClassOrSupers(fakeMethod, javaClass);
			} else {
				calledMethod = JavaItemUtil.findMethodInClassOrSupers(fakeMethod, classForMethod);
			}

			if (calledMethod != null) {
				caller.getDependenciesIDs().add(calledMethod.getID());
				if (!isCollectingMode()) {
					// we are collecting for a fake method, don't want to create
					// incoming dependencies from the fake method to the real
					// method
					calledMethod.getIncomingIDs().add(caller.getID());
				}
			} else {
				System.out.println("Could not resolve method " + JavaItemUtil.getStringForMethod(fakeMethod)
						+ " to determine what is being called");
			}

			return super.visit(node);
		}

	}

	private static class TypeVisitor extends ASTVisitor {

		private ExternalClasses classes;

		public TypeVisitor(String packageName, String className) {
			classes = new ExternalClasses(packageName, className);
		}

		public ExternalClasses getImports() {
			return classes;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			classes.addClassDeclaration(node.getName().toString());
			return true;
		}

	}

	private static class ImportVisitor extends ASTVisitor {

		private ExternalClasses classes;

		public ImportVisitor(ExternalClasses classes) {
			this.classes = classes;
		}

		public ExternalClasses getImports() {
			return classes;
		}

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

		@Override
		public boolean visit(QualifiedType node) {
			System.out.println("QType " + node);
			return false;
		}

		@Override
		public boolean visit(SimpleType node) {
			classes.addUsage(node.toString());
			// System.out.println("SType " + node);
			return false;
		}
	}
}
