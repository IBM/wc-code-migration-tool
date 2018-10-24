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

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.ibm.commerce.cmt.Check;

/**
 * This class tracks variables that are available in the current scope.
 * Subclasses should call super.visit() for each visit() that is overridden to
 * ensure that variables are tracked.
 * 
 * @author Trent Hoeppner
 */
public class VariableVisitor extends ASTVisitor {

	/**
	 * The current scope that contains the available variables. When the scope
	 * changes, a sub-scope will be created or this will be changed to the
	 * parent scope.
	 */
	protected Scope scope = new Scope();

	/**
	 * The utility object which has methods to use in the analysis.
	 */
	private JavaItemUtil2 util;

	/**
	 * The current class that is being analyzed.
	 */
	protected JavaItem javaClass;

	/**
	 * A node that must be an ancestor of the current node for
	 * {@link #canVisit(ASTNode)} to return true. If this value is null, there
	 * will not be any restriction on the ancestor.
	 * <p>
	 * This is used to control the traversal to only nodes under a certain node,
	 * for example, within a class.
	 */
	protected ASTNode ancestor;

	/**
	 * A node that signals the last node to search for
	 * {@link #canVisit(ASTNode)} to return true. If this value is null, there
	 * is no limit to the scope that can be searched.
	 * <p>
	 * This is used to stop the traversal after a specific node is reached, for
	 * example, the end of a method.
	 */
	protected ASTNode upToNode;

	/**
	 * Tracks whether the {@link #upToNode} has been reached yet.
	 */
	protected boolean upToNodeReached = false;

	/**
	 * Constructor for this.
	 * 
	 * @param util
	 *            The utility object which has methods to use in the analysis.
	 *            This value cannot be null.
	 * @param javaClass
	 *            The class that is used to restrict the search for the
	 *            appropriate type of a variable. This value cannot be null.
	 * @param ancestor
	 *            A node that must be an ancestor of the current node for
	 *            {@link #canVisit(ASTNode)} to return true. If this value is
	 *            null, there will not be any restriction on the ancestor. This
	 *            is used to control the traversal to only nodes under a certain
	 *            node, for example, within a class.
	 * @param upToNode
	 *            A node that signals the last node to search for
	 *            {@link #canVisit(ASTNode)} to return true. If this value is
	 *            null, there is no limit to the scope that can be searched.
	 *            This is used to stop the traversal after a specific node is
	 *            reached, for example, the end of a method.
	 */
	public VariableVisitor(JavaItemUtil2 util, JavaItem javaClass, ASTNode ancestor, ASTNode upToNode) {
		this(util, javaClass, ancestor, upToNode, null);
	}

	/**
	 * Constructor for this.
	 * 
	 * @param util
	 *            The utility object which has methods to use in the analysis.
	 *            This value cannot be null.
	 * @param javaClass
	 *            The class that is used to restrict the search for the
	 *            appropriate type of a variable. This value cannot be null.
	 * @param ancestor
	 *            A node that must be an ancestor of the current node for
	 *            {@link #canVisit(ASTNode)} to return true. If this value is
	 *            null, there will not be any restriction on the ancestor. This
	 *            is used to control the traversal to only nodes under a certain
	 *            node, for example, within a class.
	 * @param upToNode
	 *            A node that signals the last node to search for
	 *            {@link #canVisit(ASTNode)} to return true. If this value is
	 *            null, there is no limit to the scope that can be searched.
	 *            This is used to stop the traversal after a specific node is
	 *            reached, for example, the end of a method.
	 * @param initialScope
	 *            The initial scope before visiting nodes. If null, an empty
	 *            scope will be used initially.
	 */
	public VariableVisitor(JavaItemUtil2 util, JavaItem javaClass, ASTNode ancestor, ASTNode upToNode,
			Scope initialScope) {
		Check.notNull(util, "util");
		Check.notNull(javaClass, "javaClass");

		this.util = util;
		this.javaClass = javaClass;
		this.ancestor = ancestor;
		this.upToNode = upToNode;

		if (initialScope != null) {
			this.scope = initialScope;
		}
	}

	/**
	 * Returns whether the given node has the ancestor passed into the
	 * constructor.
	 * 
	 * @param node
	 *            The node to check. If null, false will be returned.
	 * 
	 * @return True if the given node has the assigned ancestor, false
	 *         otherwise.
	 */
	protected boolean hasAncestor(ASTNode node) {
		if (ancestor == null) {
			return false;
		}

		ASTNode current = node;
		while (current != null && current != ancestor) {
			current = current.getParent();
		}

		return current == ancestor;
	}

	/**
	 * Returns whether the given node can be visited, restricted by the ancestor
	 * and upToNode passed into the constructor.
	 * 
	 * @param node
	 *            The node to potentially visit. This value cannot be null.
	 * 
	 * @return True if the node can be visited, false otherwise.
	 */
	protected boolean canVisit(ASTNode node) {
		return (ancestor == null || hasAncestor(node)) && (upToNode == null && !upToNodeReached);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(ImportDeclaration node) {
		String type = node.getName().toString();

		if (node.isOnDemand()) {
			// TODO
		} else {
			// TODO does not handle importing a nested class
			String packageName = "";
			String baseName = type;
			int lastDotIndex = type.lastIndexOf('.');
			if (lastDotIndex >= 0) {
				packageName = type.substring(0, lastDotIndex);
				baseName = type.substring(lastDotIndex + 1);
			}

			// find the type
			JavaItem importClass = javaClass.getIndex().findClass(packageName, baseName);

			if (importClass != null) {
				scope.addImportClass(baseName, importClass);
			}
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(ForStatement node) {
		if (!canVisit(node)) {
			return false;
		}

		scope = scope.createSubScope();

		return super.visit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(ForStatement node) {
		if (!canVisit(node)) {
			return;
		}

		scope = scope.getParent();

		super.endVisit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(Block node) {
		if (!canVisit(node)) {
			return false;
		}

		scope = scope.createSubScope();

		return super.visit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(Block node) {
		if (!canVisit(node)) {
			return;
		}

		scope = scope.getParent();

		super.endVisit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(SingleVariableDeclaration node) {
		if (!canVisit(node)) {
			return false;
		}

		String name = node.getName().getFullyQualifiedName();
		JavaItem type = util.findClassForType(node.getType(), javaClass);

		scope.addVariable(name, type);

		return super.visit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(VariableDeclarationExpression node) {
		if (!canVisit(node)) {
			return false;
		}

		addVariablesForFragments(node.getType(), node.fragments());

		return super.visit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(VariableDeclarationStatement node) {
		if (!canVisit(node)) {
			return false;
		}

		addVariablesForFragments(node.getType(), node.fragments());

		return super.visit(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(FieldDeclaration node) {
		if (!canVisit(node)) {
			return false;
		}

		addVariablesForFragments(node.getType(), node.fragments());
		return super.visit(node);
	}

	/**
	 * Finds the class item for the given type, and adds a variable name with
	 * that type for each fragment.
	 * 
	 * @param declarationType
	 *            The type of the variables. This value cannot be null.
	 * @param fragments
	 *            The fragments which contain the variable names. This value
	 *            cannot be null, but may be empty.
	 */
	private void addVariablesForFragments(Type declarationType, List<?> fragments) {
		JavaItem type = util.findClassForType(declarationType, javaClass);
		for (Object o : fragments) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
			String name = fragment.getName().getFullyQualifiedName();
			scope.addVariable(name, type);
		}
	}

	/**
	 * Returns the current scope with the variables available.
	 * 
	 * @return The current scope with the variables available. This value will
	 *         not be null.
	 */
	public Scope getScope() {
		return scope;
	}

}