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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemType;

/**
 * This class finds the methods in a compilation unit and adds them to the
 * JavaItemIndex. Must be called after all the class dependencies are resolved.
 * 
 * @author Trent Hoeppner
 */
public class LoadJavaSourceMethodsTask extends AbstractJavaSourceTask {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(
			Arrays.asList(Name.PACKAGE_ITEM, Name.JAVA_COMPILATION_UNIT));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.METHODS_LOADED));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadJavaSourceMethodsTask(String name, LoadingContext context) {
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

		ClassInternalVisitor visitor = new ClassInternalVisitor(packageItem);
		compUnit.accept(visitor);
		visitor.addDefaultConstructor();

		context.put(Name.METHODS_LOADED, true);
	}

	/**
	 * Returns the IDs of the class {@link JavaItem JavaItems} that correspond
	 * to the parameters of the given method. If a parameter's type could not be
	 * found, the wildcard class item will be substituted.
	 * 
	 * @param node
	 *            The method to find parameters for. This value cannot be null.
	 * @param javaClass
	 *            The class item that contains the method. This value cannot be
	 *            null.
	 * 
	 * @return The IDs of the class items representing the parameters of the
	 *         method. This value will not be null, but may be empty.
	 */
	private List<Integer> getMethodParameters(MethodDeclaration node, JavaItem javaClass) {
		List<Integer> parameterTypeIDs = new ArrayList<>();
		for (Object parameter : node.parameters()) {
			SingleVariableDeclaration p = (SingleVariableDeclaration) parameter;
			JavaItem parameterType = getContext().getUtil().findClassForType(p.getType(), javaClass);
			if (parameterType == null) {
				parameterType = getContext().getUtil().getWildcardType();
			}
			parameterTypeIDs.add(parameterType.getID());
		}
		return parameterTypeIDs;
	}

	/**
	 * Creates a method item from the given node.
	 * 
	 * @param parent
	 *            The parent class item of the given method. This value cannot
	 *            be null.
	 * @param node
	 *            The method node to be parsed. This value cannot be null.
	 * @param javaClass
	 *            The class item corresponding to the source file. This value
	 *            cannot be null.
	 * 
	 * @return The method item that was created. This value will not be null.
	 */
	private JavaItem createMethod(JavaItem parent, MethodDeclaration node, JavaItem javaClass) {
		List<Integer> parameterTypeIDs = getMethodParameters(node, javaClass);

		String name = node.getName().getFullyQualifiedName();
		JavaItem method = getContext().getFactory().createMethod(parent, name, parameterTypeIDs);

		return method;
	}

	/**
	 * Creates a method item for a initializer.
	 * 
	 * @param parent
	 *            The parent class item of the initializer. This value cannot be
	 *            null.
	 * @param initializerIndex
	 *            The index of the initializer, which is used to make the method
	 *            item name.
	 * 
	 * @return The method item that was created. This value will not be null.
	 */
	private JavaItem createMethod(JavaItem parent, int initializerIndex) {
		String name = "#initializer" + initializerIndex;
		JavaItem method = getContext().getFactory().createMethod(parent, name, Collections.emptyList());

		return method;
	}

	/**
	 * This class goes through the class AST tree looking for methods,
	 * initializers, and constructors, and creating method items for them.
	 */
	private class ClassInternalVisitor extends ASTVisitor {

		/**
		 * The package that contains the classes in this.
		 */
		private JavaItem packageItem;

		/**
		 * The top-level class that methods will be added to.
		 */
		private JavaItem currentClass;

		/**
		 * The index of the next initializer.
		 */
		private int initializerIndex = 0;

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
		 */
		public ClassInternalVisitor(JavaItem packageItem) {
			this.packageItem = packageItem;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(MethodDeclaration node) {
			// TODO this will include methods in nested classes, need to verify
			// that the containing class is the same as javaClass
			if (!allow(node.getModifiers(), currentClass)) {
				// it's private, ignore it
				return super.visit(node);
			}

			JavaItem method = createMethod(currentClass, node, currentClass);

			JavaItem returnType = getContext().getUtil().findClassForType(node.getReturnType2(), currentClass);
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
				JavaItem exceptionType = getContext().getUtil().findClassForType(type, currentClass);
				if (exceptionType == null) {
					System.out.println("exceptionType not found for " + type.toString() + " for method " + method);
					continue;
				}
				exceptionTypeIDs.add(exceptionType.getID());
			}
			method.setAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES, exceptionTypeIDs);

			return super.visit(node);
		}

		/**
		 * Adds a default constructor to the java class, but only if it does not
		 * declare one explicitly.
		 */
		public void addDefaultConstructor() {
			// check if the default constructor already exists
			if (currentClass == null) {
				return;
			}

			String constructorName = currentClass.getName();
			for (JavaItem method : currentClass.getChildren()) {
				if (method.getName().equals(constructorName) && method.getChildren().isEmpty()) {
					// it already has one
					return;
				}
			}

			// it doesn't exist, add it
			JavaItem method = getContext().getFactory().createMethod(currentClass, constructorName,
					Collections.emptyList());
			currentClass.getChildrenIDs().add(method.getID());

			method.setAttribute(JavaItem.ATTR_RETURN_TYPE, null);
			method.setAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES, Collections.EMPTY_LIST);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(Initializer node) {
			if (!allow(node.getModifiers(), currentClass)) {
				// it's private, ignore it
				return super.visit(node);
			}

			// this is like a method, it can call other methods
			JavaItem method = createMethod(currentClass, initializerIndex++);
			currentClass.getChildrenIDs().add(method.getID());

			return super.visit(node);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(FieldDeclaration node) {
			if (!allow(node.getModifiers(), currentClass)) {
				// it's private, ignore it
				return super.visit(node);
			}

			JavaItem type = getContext().getUtil().findClassForType(node.getType(), currentClass);
			for (Object o : node.fragments()) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
				String fieldName = fragment.getName().getFullyQualifiedName();
				JavaItem field = getContext().getFactory().createField(currentClass, fieldName);
				Integer typeID;
				if (type != null) {
					typeID = type.getID();
				} else {
					typeID = null;
				}

				field.setAttribute(JavaItem.ATTR_FIELD_TYPE, typeID);
			}

			return super.visit(node);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(TypeDeclaration node) {
			if (!allow(node.getModifiers(), packageItem)) {
				// it's private, ignore it
				return false;
			}

			if (isUseful(node)) {
				String fullyQualifiedName = node.getName().getFullyQualifiedName();

				JavaItem lastNonMemberClass = lastNonMemberClassStack.isEmpty() ? null : lastNonMemberClassStack.peek();
				if (!node.isPackageMemberTypeDeclaration() && lastNonMemberClass != null) {
					fullyQualifiedName = lastNonMemberClass.getName() + "$" + fullyQualifiedName;
				}

				currentClass = getContext().getIndex().findItem(packageItem, fullyQualifiedName, JavaItemType.CLASS);
				if (currentClass == null) {
					System.out.println("Could not find class item for " + packageItem + ":" + fullyQualifiedName);

					// push something so that it can be popped by
					// endVisit(TypeDeclaration)
					lastNonMemberClassStack.push(null);
					return false;
				}

				lastNonMemberClassStack.push(currentClass);
				return true;
			}

			return false;
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
			}

			super.endVisit(node);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(EnumDeclaration node) {
			// skip enums for now
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(AnnotationTypeDeclaration node) {
			// we don't care about annotations
			return false;
		}
	}
}