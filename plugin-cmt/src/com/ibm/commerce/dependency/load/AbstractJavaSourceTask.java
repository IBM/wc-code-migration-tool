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

import java.lang.reflect.Modifier;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemType;
import com.ibm.commerce.dependency.task.Task;

/**
 * This is a base class for tasks that analyze a Java source file.
 * 
 * @author Trent Hoeppner
 */
public abstract class AbstractJavaSourceTask extends Task<LoadingContext> {

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public AbstractJavaSourceTask(String name, LoadingContext context) {
		super(name, context);
	}

	/**
	 * Determines if the given modifiers are allowed to be visible based on the
	 * {@link JavaItem#ATTR_PROJECT_PRIVATE_VISIBLE} attribute.
	 * 
	 * @param modifiers
	 *            The modifiers that came from an Eclipse node.
	 * @param item
	 *            The class item that determines whether the modifiers are
	 *            allowed or not. This value cannot be null.
	 * 
	 * @return True if the modifiers are allowed to be visible, false if they
	 *         are not.
	 */
	protected boolean allow(int modifiers, JavaItem item) {
		int protectedBit = modifiers & Modifier.PROTECTED;
		int publicBit = modifiers & Modifier.PUBLIC;
		boolean isPublic = publicBit > 0 || protectedBit > 0;
		return isPublic || isPrivateVisible(item);
	}

	/**
	 * Returns whether the given item's project allows private and default
	 * objects to be visible for analysis purposes. Normally this will be true
	 * when analyzing code that needs to be updated, and false when extracting
	 * APIs for later reference.
	 * 
	 * @param item
	 *            The item with a project. If null, false will be returned.
	 * 
	 * @return True if private and default objects are visible, false otherwise.
	 */
	private boolean isPrivateVisible(JavaItem item) {
		boolean visible = false;
		JavaItem current = item;
		while (current != null && current.getType() != JavaItemType.PROJECT) {
			current = current.getParent();
		}

		if (current != null && current.getType() == JavaItemType.PROJECT) {
			visible = current.getAttribute(JavaItem.ATTR_PROJECT_PRIVATE_VISIBLE);
		}

		return visible;
	}

	/**
	 * Returns whether the given type is declared anonymously (with no class
	 * name).
	 * 
	 * @param node
	 *            The node to check. This value cannot be null.
	 * 
	 * @return True if the type is anonymous, false otherwise.
	 */
	private boolean isAnonymous(TypeDeclaration node) {
		return node.getParent() instanceof AnonymousClassDeclaration;
	}

	/**
	 * Returns whether the given type is declared inside a method.
	 * 
	 * @param node
	 *            The node to check. This value cannot be null.
	 * 
	 * @return True if the type is declared inside a method, false otherwise.
	 */
	private boolean isInMethod(TypeDeclaration node) {
		boolean isInMethod = false;
		ASTNode current = node.getParent();
		while (current != null) {
			if (current.getNodeType() == ASTNode.METHOD_DECLARATION) {
				isInMethod = true;
				break;
			}

			current = current.getParent();
		}

		return isInMethod;
	}

	/**
	 * Returns whether the given type is useful for analyzing.
	 * 
	 * @param node
	 *            The node to check. This value cannot be null.
	 * 
	 * @return True if the type is not anonymous and not declared inside a
	 *         method, false otherwise.
	 */
	protected boolean isUseful(TypeDeclaration node) {
		return !isAnonymous(node) && !isInMethod(node);
	}

}
