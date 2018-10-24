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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.dependency.model.JavaItem;

/**
 * This class finds the classes and nested classes in a compilation unit and
 * adds them to the JavaItemIndex. The superclass and super interfaces are not
 * added here, they are added when loading the dependencies.
 * 
 * @author Trent Hoeppner
 */
public class LoadJavaSourceClassesTask extends AbstractJavaSourceTask {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(
			Arrays.asList(Name.PACKAGE_ITEM, Name.JAVA_COMPILATION_UNIT));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.CLASS_LOADED));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadJavaSourceClassesTask(String name, LoadingContext context) {
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

		context.put(Name.CLASS_LOADED, true);
	}

	/**
	 * This class goes through the class AST tree looking for classes and nested
	 * classes and creating JavaItems for them.
	 */
	private class ClassInternalVisitor extends ASTVisitor {

		/**
		 * The package to add classes to.
		 */
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
		public boolean visit(TypeDeclaration node) {
			if (!allow(node.getModifiers(), packageItem)) {
				// it's private, ignore it
				return super.visit(node);
			}

			if (!(node.getParent() instanceof AnonymousClassDeclaration)) {
				String fullyQualifiedName = node.getName().getFullyQualifiedName();

				JavaItem lastNonMemberClass = lastNonMemberClassStack.isEmpty() ? null : lastNonMemberClassStack.peek();
				if (!node.isPackageMemberTypeDeclaration() && lastNonMemberClass != null) {
					fullyQualifiedName = lastNonMemberClass.getName() + "$" + fullyQualifiedName;
				}

				JavaItem javaClass = getContext().getFactory().createClass(packageItem, fullyQualifiedName);
				javaClass.setAttribute(JavaItem.ATTR_BINARY, false);

				if (lastNonMemberClass != null) {
					// it is nested inside another class
					Set<Integer> innerClassIDs = lastNonMemberClass.getAttribute(JavaItem.ATTR_INNER_CLASSES);
					if (innerClassIDs == null) {
						innerClassIDs = new LinkedHashSet<>();
						lastNonMemberClass.setAttribute(JavaItem.ATTR_INNER_CLASSES, innerClassIDs);
					}
					innerClassIDs.add(javaClass.getID());

					javaClass.setAttribute(JavaItem.ATTR_OUTER_CLASS, lastNonMemberClass.getID());
				}

				lastNonMemberClassStack.push(javaClass);
			}

			return super.visit(node);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void endVisit(TypeDeclaration node) {
			lastNonMemberClassStack.pop();
			super.endVisit(node);
		}
	}
}