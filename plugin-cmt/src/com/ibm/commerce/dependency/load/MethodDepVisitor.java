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
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemFactory;
import com.ibm.commerce.dependency.model.JavaItemIndex;
import com.ibm.commerce.dependency.model.JavaItemType;
import com.ibm.commerce.dependency.model.JavaItemUtil2;
import com.ibm.commerce.dependency.model.Scope;
import com.ibm.commerce.dependency.model.VariableVisitor;

/**
 * This class can find dependencies from a method to other methods in the local
 * compilation unit or in other classes.
 * 
 * @author Trent Hoeppner
 */
class MethodDepVisitor extends VariableVisitor {

	/**
	 * The ID of the wildcard class item.
	 */
	private static final int WILDCARD_ID = 8;

	/**
	 * The task that contains utility methods and context used by this.
	 */
	private final AbstractJavaSourceTask javaSourceTask;

	/**
	 * The caller method item that is being analyzed for dependencies.
	 */
	private JavaItem caller;

	/**
	 * The index of the next initializer.
	 */
	private int initializerIndex = 0;

	/**
	 * True indicates that dependent methods that do not yet exist will be
	 * created without adding any dependencies, false indicates that only method
	 * dependencies will be added.
	 */
	private boolean createMethods;

	/**
	 * Constructor for this.
	 * 
	 * @param javaSourceTask
	 *            The source task which contains utility methods used by this.
	 *            This value cannot be null.
	 * @param javaClass
	 *            The class item where the compilation unit was found. This
	 *            value cannot be null.
	 * @param initialScope
	 *            The initial scope before visiting nodes. This value cannot be
	 *            null.
	 * @param createMethods
	 *            True indicates that dependent methods that do not yet exist
	 *            will be created without adding any dependencies, false
	 *            indicates that only method dependencies will be added.
	 */
	public MethodDepVisitor(AbstractJavaSourceTask javaSourceTask, JavaItem javaClass, Scope initialScope,
			boolean createMethods) {
		super(javaSourceTask.getContext().getUtil(), javaClass, null, null, initialScope);
		this.javaSourceTask = javaSourceTask;
		this.createMethods = createMethods;
	}

	/**
	 * Constructor for this with more restrictions on the scope to search.
	 * 
	 * @param javaSourceTask
	 *            The source task which contains utility methods used by this.
	 *            This value cannot be null.
	 * @param javaClass
	 *            The class which is used to search for dependencies within
	 *            methods. This value cannot be null.
	 * @param caller
	 *            The caller method item that is being searched. This value
	 *            cannot be null.
	 * @param ancestor
	 *            The ancestor node to restrict searching in. This value may be
	 *            null to not restrict nodes to having an ancestor.
	 * @param createMethods
	 *            True indicates that dependent methods that do not yet exist
	 *            will be created without adding any dependencies, false
	 *            indicates that only method dependencies will be added.
	 */
	public MethodDepVisitor(AbstractJavaSourceTask javaSourceTask, JavaItem javaClass, JavaItem caller,
			ASTNode ancestor, boolean createMethods) {
		super(javaSourceTask.getContext().getUtil(), javaClass, ancestor, null);
		this.javaSourceTask = javaSourceTask;
		Check.notNull(caller, "caller");

		this.caller = caller;
		this.createMethods = createMethods;
	}

	/**
	 * Returns the factory used to create items.
	 * 
	 * @return The factory used to create items. This value will not be null.
	 */
	private JavaItemFactory getFactory() {
		return javaSourceTask.getContext().getFactory();
	}

	/**
	 * Returns the utility with common methods needed by this.
	 * 
	 * @return the utility with common methods. This value will not be null.
	 */
	private JavaItemUtil2 getUtil() {
		return javaSourceTask.getContext().getUtil();
	}

	/**
	 * Returns the index which contains items to find.
	 * 
	 * @return The index which contains items to find. This value will not be
	 *         null.
	 */
	private JavaItemIndex getIndex() {
		return javaSourceTask.getContext().getIndex();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * In this implementation, false will also be returned if the caller is
	 * null.
	 */
	protected boolean canVisit(ASTNode node) {
		return caller != null && super.canVisit(node);
	}

	/**
	 * Returns whether we are collecting for a fake method or not.
	 * 
	 * @return True if we are collecting for a fake method, false otherwise.
	 */
	private boolean isCollectingMode() {
		return ancestor != null;
	}

	/**
	 * Creates an untracked method item for a method, which can be used for
	 * comparison purposes.
	 * 
	 * @param node
	 *            The method to create a fake item for. This value cannot be
	 *            null.
	 * @param javaClass
	 *            The class item, whose class dependencies are used to find the
	 *            types of the method parameters. This value cannot be null.
	 * 
	 * @return A fake method item which can be used to search for the real
	 *         method item in the index. This value will not be null.
	 */
	JavaItem createFakeMethod(MethodDeclaration node, JavaItem javaClass) {
		List<Integer> parameterTypeIDs = getMethodParameters(node, javaClass);

		String name = node.getName().getFullyQualifiedName();
		JavaItem method = getFactory().createUntracked(name, JavaItemType.METHOD);
		method.setAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES, parameterTypeIDs);

		return method;
	}

	/**
	 * Creates an untracked method item for an initializer, which can be used
	 * for comparison purposes.
	 * 
	 * @param initializerIndex
	 *            The index of the current initializer, which is used to create
	 *            the name.
	 * 
	 * @return A fake method item which can be used to search for the real
	 *         method item in the index. This value will not be null.
	 */
	JavaItem createFakeMethod(int initializerIndex) {
		String name = "#initializer" + initializerIndex;
		JavaItem method = getFactory().createUntracked(name, JavaItemType.METHOD);
		method.setAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES, Collections.emptyList());

		return method;
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
			JavaItem parameterType = getUtil().findClassForType(p.getType(), javaClass);
			if (parameterType == null) {
				parameterType = getUtil().getWildcardType();
			}
			parameterTypeIDs.add(parameterType.getID());
		}
		return parameterTypeIDs;
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
	private boolean isArraysEqual(JavaItem array1, JavaItem array2) {
		Integer baseClassID1 = array1.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS);
		Integer baseClassID2 = array2.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS);
		JavaItem baseClass1 = getIndex().getItem(baseClassID1);
		JavaItem baseClass2 = getIndex().getItem(baseClassID2);

		boolean equal;
		if (isArray(baseClass1) && isArray(baseClass2)) {
			equal = isArraysEqual(baseClass1, baseClass2);
		} else {
			equal = baseClass1 == baseClass2;
		}

		return equal;
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
	public boolean isArray(JavaItem typeClass) {
		return typeClass.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS) != null;
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
	public boolean isMethodsEqual(JavaItem method, JavaItem fakeMethod) {
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
			if (paramID1.equals(WILDCARD_ID) || paramID2.equals(WILDCARD_ID)) {
				continue;
			}

			if (!paramID1.equals(paramID2)) {
				// maybe because they are arrays
				JavaItem param1 = getIndex().getItem(paramID1);
				JavaItem param2 = getIndex().getItem(paramID2);
				if (isArray(param1) && isArray(param2)) {
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
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		if (!this.javaSourceTask.allow(node.getModifiers(), javaClass)) {
			// it's private, ignore it
			return super.visit(node);
		}

		JavaItem fakeMethod = createFakeMethod(node, javaClass);
		for (JavaItem m : javaClass.getChildren()) {
			if (m.getType() == JavaItemType.METHOD
					&& this.javaSourceTask.getContext().getUtil().isMethodsEqual(m, fakeMethod)) {
				caller = m;
				break;
			}
		}

		return super.visit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(MethodDeclaration node) {
		caller = null;
		super.endVisit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(Initializer node) {
		JavaItem fakeMethod = createFakeMethod(initializerIndex++);
		for (JavaItem m : javaClass.getChildren()) {
			if (m.getType() == JavaItemType.METHOD
					&& this.javaSourceTask.getContext().getUtil().isMethodsEqual(m, fakeMethod)) {
				caller = m;
				break;
			}
		}

		return super.visit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(Initializer node) {
		caller = null;
		super.endVisit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(MethodInvocation node) {
		if (!canVisit(node)) {
			return false;
		}

		SimpleName nodeName = node.getName();
		List<?> nodeArguments = node.arguments();
		JavaItem fakeMethod = this.javaSourceTask.getContext().getUtil().createFakeMethodUsingScope(nodeName,
				nodeArguments, javaClass, scope);

		JavaItem calledMethod = null;
		JavaItem classForMethod = this.javaSourceTask.getContext().getUtil().getTypeForExpression(node.getExpression(),
				javaClass, scope);
		if (classForMethod == null) {
			// no prefix, it means the method is in this class or a
			// superclass
			calledMethod = this.javaSourceTask.getContext().getUtil().findMethodInClassOrSupers(fakeMethod, javaClass);

			if (calledMethod == null) {
				// it's not found in the class hierarchy, add it to the
				// superclass, unless it's Object
				if (createMethods) {
					Integer superClassID = javaClass.getAttribute(JavaItem.ATTR_SUPERCLASS);
					if (superClassID != null) {
						JavaItem superClass = this.javaSourceTask.getContext().getIndex().getItem(superClassID);
						if (superClass.getName() != "Object") {
							String name = node.getName().getFullyQualifiedName();
							List<Integer> parameterTypes = fakeMethod.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
							calledMethod = this.javaSourceTask.getContext().getFactory().createMethod(superClass, name, parameterTypes);
						}
					}
				}
			}
		} else {
			calledMethod = this.javaSourceTask.getContext().getUtil().findMethodInClassOrSupers(fakeMethod,
					classForMethod);

			if (calledMethod == null) {
				// it's not found in that class, add it
				if (createMethods) {
					String name = node.getName().getFullyQualifiedName();
					List<Integer> parameterTypes = fakeMethod.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
					calledMethod = this.javaSourceTask.getContext().getFactory().createMethod(classForMethod, name, parameterTypes);
				}
			}
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
			if (!createMethods) {
				System.out.println("Could not resolve method "
						+ this.javaSourceTask.getContext().getUtil().getStringForMethod(fakeMethod)
						+ " to determine what is being called, called from " + caller);
			}
		}

		return super.visit(node);
	}

}