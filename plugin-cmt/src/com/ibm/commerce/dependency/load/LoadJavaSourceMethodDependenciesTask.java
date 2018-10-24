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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemType;
import com.ibm.commerce.dependency.model.Scope;
import com.ibm.commerce.dependency.model.VariableVisitor;

/**
 * This class finds the method dependencies in a compilation unit and adds them
 * to the JavaItemIndex. Must be called after all the methods are loaded.
 * 
 * @author Trent Hoeppner
 */
public class LoadJavaSourceMethodDependenciesTask extends AbstractJavaSourceTask {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(
			Arrays.asList(Name.PACKAGE_ITEM, Name.JAVA_COMPILATION_UNIT, Name.CREATE_DEPENDENT_METHOD_ITEMS));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.METHOD_DEPENDENCIES_LOADED));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadJavaSourceMethodDependenciesTask(String name, LoadingContext context) {
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
		boolean createDependentMethodItems = context.get(Name.CREATE_DEPENDENT_METHOD_ITEMS);

		TypeVisitor typeVisitor = new TypeVisitor(packageItem);
		compUnit.accept(typeVisitor);
		Map<String, TypeDeclaration> fullClassNames = typeVisitor.getClasses();

		Scope scopeWithImports = null;
		if (!fullClassNames.isEmpty()) {
			String firstClassName = fullClassNames.keySet().iterator().next();
			JavaItem topClass = getContext().getIndex().findItem(packageItem, firstClassName, JavaItemType.CLASS);
			if (topClass != null) {
				VariableVisitor variableVisitor = new VariableVisitor(context.getUtil(), topClass, null, null);
				compUnit.accept(variableVisitor);
				scopeWithImports = variableVisitor.getScope();
			}
		}

		for (String fullyQualifiedName : fullClassNames.keySet()) {
			TypeDeclaration classNode = fullClassNames.get(fullyQualifiedName);
			JavaItem currentClass = getContext().getIndex().findItem(packageItem, fullyQualifiedName,
					JavaItemType.CLASS);

			if (currentClass == null) {
				System.out.println("Could not find class for " + packageItem + ":" + fullyQualifiedName);
				continue;
			}

			MethodDepVisitor visitor = new MethodDepVisitor(this, currentClass, scopeWithImports,
					createDependentMethodItems);
			classNode.accept(visitor);
		}

		context.put(Name.METHOD_DEPENDENCIES_LOADED, true);
	}

	/**
	 * This class just finds all class declarations in a compilation unit.
	 */
	private class TypeVisitor extends ASTVisitor {

		/**
		 * The package in which to search for classes.
		 */
		private JavaItem packageItem;

		/**
		 * The classes that were found.
		 */
		private Map<String, TypeDeclaration> classNodes = new LinkedHashMap<>();

		/**
		 * The stack of TypeDeclarations being visited currently. The top of the
		 * stack contains the last class visited.
		 */
		private Stack<JavaItem> lastNonMemberClassStack = new Stack<>();

		/**
		 * Constructor for this.
		 * 
		 * @param packageItem
		 *            The package in which to search for classes. This value
		 *            cannot be null.
		 */
		public TypeVisitor(JavaItem packageItem) {
			this.packageItem = packageItem;
		}

		/**
		 * Returns the classes that were found.
		 * 
		 * @return The classes that were found. This value will not be null.
		 */
		public Map<String, TypeDeclaration> getClasses() {
			return classNodes;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(TypeDeclaration node) {
			if (!(node.getParent() instanceof AnonymousClassDeclaration)) {
				String fullyQualifiedName = node.getName().getFullyQualifiedName();

				JavaItem lastNonMemberClass = lastNonMemberClassStack.isEmpty() ? null : lastNonMemberClassStack.peek();
				if (!node.isPackageMemberTypeDeclaration() && lastNonMemberClass != null) {
					fullyQualifiedName = lastNonMemberClass.getName() + "$" + fullyQualifiedName;
				}

				JavaItem javaClass = getContext().getIndex().findItem(packageItem, fullyQualifiedName,
						JavaItemType.CLASS);

				classNodes.put(fullyQualifiedName, node);
				lastNonMemberClassStack.push(javaClass);
			}

			return true;
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