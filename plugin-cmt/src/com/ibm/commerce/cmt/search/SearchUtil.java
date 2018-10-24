package com.ibm.commerce.cmt.search;

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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemUtil2;
import com.ibm.commerce.dependency.model.Scope;

/**
 * This class contains utility methods to help find matches during a search.
 * 
 * @author Trent Hoeppner
 */
public class SearchUtil {

	/**
	 * Before calling this method, the Context.Prop.ALL_MATCHERS property must
	 * be set in the context.
	 * 
	 * @param node
	 * @param context
	 * @param nameParam
	 * @param classNameParam
	 * @param hasParamParam
	 * @return True if the method declaration matches, false otherwise.
	 */
	public static boolean matchMethodDeclaration(MethodDeclaration node, Context context, NameParam nameParam,
			NameParam classNameParam, HasParamParam hasParamParam) {
		if (hasParamParam != null) {
			if (!hasParamParam.accept(context, node)) {
				return false;
			}
		} else if (node.parameters().size() > 0) {
			// we don't have a <param> tag yet to specify individual
			// parameters, so if there is no <hasparam>, we only allow
			// methods with 0 parameters through
			return false;
		}

		boolean found = false;
		String methodName = node.getName().getFullyQualifiedName();

		JavaItemUtil2 util = context.get(Context.Prop.JAVA_ITEM_UTIL);
		if (nameParam.accept(context, methodName)) {
			JavaItem javaClass = util.findJavaClass(context, node);
			if (javaClass == null) {
				// TODO find out why some JSP files are not found
				return false;
			}

			String fullClassName = util.getFullClassNameForType(javaClass);
			if (classNameParam.accept(context, fullClassName)) {
				found = true;
			}
		}

		return found;
	}

	public static boolean matchMethodInvocation(ASTNode node, Context context, NameParam nameParam,
			NameParam classNameParam, HasSupertypeParam hasSupertypeParam, HasParamParam hasParamParam, Scope scope) {
		JavaItemUtil2 util = context.get(Context.Prop.JAVA_ITEM_UTIL);
		JavaItem javaClass = util.findJavaClass(context, node);
		if (javaClass == null) {
			// TODO for JSP generated Java files, this can happen, should
			// check if they are ever analyzed
			return false;
		}

		List<?> arguments = null;
		String name = null;
		if (node instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation) node;
			arguments = methodInvocation.arguments();
			name = methodInvocation.getName().getFullyQualifiedName();
		} else if (node instanceof ClassInstanceCreation) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) node;
			arguments = classInstanceCreation.arguments();
			name = util.getNameForType(classInstanceCreation.getType(), javaClass);
			int lastDotIndex = name.lastIndexOf('.');
			if (lastDotIndex >= 0) {
				name = name.substring(lastDotIndex + 1);
			}
		}

		if (hasParamParam != null) {
			if (!hasParamParam.accept(context, node)) {
				return false;
			}
		} else if (arguments.size() > 0) {
			// we don't have a <param> tag yet to specify individual
			// parameters, so if there is no <hasparam>, we only allow
			// methods with 0 parameters through
			return false;
		}

		JavaItem classForMethod = findClassForMethod(util, node, scope, javaClass);

		List<?> nodeArguments = arguments;
		JavaItem fakeMethod = util.createFakeMethodUsingScope(name, nodeArguments, javaClass, scope);
		JavaItem calledMethod = util.findMethodInClassOrSupers(fakeMethod, classForMethod);

		boolean found = false;
		if (calledMethod != null) {
			String methodSearchString = name;
			if (!nameParam.accept(context, methodSearchString)) {
				// the method name don't match
				return false;
			}

			boolean classNameFound = true;
			if (classNameParam != null) {
				// we are not sure until we search
				classNameFound = false;
				String classSearchString = util.getFullClassNameForType(classForMethod);
				if (classNameParam.accept(context, classSearchString)) {
					// the class name matches
					classNameFound = true;
				}
			}

			boolean oneSuperAccepted = true;
			if (hasSupertypeParam != null) {
				// we are not sure until we search
				oneSuperAccepted = false;
				NameParam supertypeNameParam = (NameParam) hasSupertypeParam.getSubParams().get(0);
				oneSuperAccepted = hasSupertype(context, supertypeNameParam, classForMethod);
			}

			if (classNameFound && oneSuperAccepted) {
				found = true;
			}
		}
		return found;
	}

	public static boolean hasSupertype(Context context, NameParam nameParam, JavaItem javaClass) {
		boolean accept = false;
		if (javaClass != null) {
			JavaItemUtil2 util = context.get(Context.Prop.JAVA_ITEM_UTIL);

			Set<JavaItem> allSupers = new LinkedHashSet<>();
			util.findAllSupers(javaClass, allSupers, true);

			for (JavaItem superType : allSupers) {
				String superTypeName = util.getFullClassNameForType(superType);
				if (nameParam.accept(context, superTypeName)) {
					accept = true;
					break;
				}
			}
		}
		return accept;
	}

	private static JavaItem findClassForMethod(JavaItemUtil2 util, ASTNode node, Scope scope, JavaItem javaClass) {
		JavaItem classForMethod = null;
		if (node instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation) node;
			classForMethod = util.getTypeForExpression(methodInvocation.getExpression(), javaClass, scope);
		} else if (node instanceof ClassInstanceCreation) {
			ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) node;
			classForMethod = util.findClassForType(classInstanceCreation.getType(), javaClass);
		}

		if (classForMethod == null) {
			// no prefix, it means the method is in this class or a
			// superclass
			classForMethod = javaClass;
		}

		return classForMethod;
	}

}
