package com.ibm.commerce.qcheck.core;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

/**
 * BaseValidatorResource provides some implementation for a resource of type
 * <code>T</code>. Examples include <code>File</code> and
 * <code>IResource</code>.
 *
 * @param <T>
 *            The type of the resource.
 * 
 * @author Trent Hoeppner
 */
public abstract class BaseValidatorResource<T> implements ValidatorResource {

	private static final List<ModelEnum> SUPPORTED_MODELS = Arrays.asList(ModelEnum.STRING, ModelEnum.COMP_UNIT,
			ModelEnum.EXTERNAL);

	/**
	 * The resource being represented.
	 */
	private T resource;

	/**
	 * The {@link #getBaseDir() base directory} of the resource being
	 * represented. This value is lazy-loaded.
	 */
	private String baseDir;

	/**
	 * The {@link #getPathFilename() relative path} of the resource being
	 * represented. This value is lazy-loaded.
	 */
	private String pathDir;

	private ModelRegistry registry;

	/**
	 * Constructor for BaseValidatorResource.
	 *
	 * @param newResource
	 *            The resource that this wraps. Cannot be null.
	 *
	 * @exception IOException
	 *                If there was an error accessing the data in the given
	 *                resource.
	 */
	public BaseValidatorResource(T newResource, ModelRegistry registry) {
		this.resource = newResource;
		this.registry = registry;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseDir == null) ? 0 : baseDir.hashCode());
		result = prime * result + ((pathDir == null) ? 0 : pathDir.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		BaseValidatorResource other = (BaseValidatorResource) obj;

		String thisBaseDir = getBaseDir();
		String otherBaseDir = other.getBaseDir();
		if (thisBaseDir == null) {
			if (otherBaseDir != null) {
				return false;
			}
		} else if (!thisBaseDir.equals(otherBaseDir)) {
			return false;
		}

		String thisPathDir = getPathFilename();
		String otherPathDir = other.getPathFilename();
		if (thisPathDir == null) {
			if (otherPathDir != null) {
				return false;
			}
		} else if (!thisPathDir.equals(otherPathDir)) {
			return false;
		}

		return true;
	}

	/**
	 * Returns the resource that this wraps.
	 *
	 * @return The resource that this wraps. Will not be null.
	 */
	public final T getResource() {
		return resource;
	}

	/**
	 * Returns a list of nodes that match the given, using the given node as the
	 * root.
	 *
	 * @param node
	 *            The root of the tree to start searching from. Cannot be null.
	 * @param nodeType
	 *            The type of node as defined by constants in {@link ASTNode}.
	 *            Must be a valid constant value.
	 * @param searchChildren
	 *            True indicates to search the child nodes of the given node,
	 *            false indicates that only the given node will be checked
	 *            against the node type.
	 *
	 * @return A new list with the nodes that match the given type. Will not be
	 *         null, but may be empty if there are no results.
	 */
	private List getTypedNodeList(ASTNode node, int nodeType, boolean searchChildren) {
		JavaNodeVisitor visitor = new JavaNodeVisitor(nodeType, searchChildren);
		node.accept(visitor);
		return visitor.getASTNodeList();
	}

	/**
	 * {@inheritDoc}
	 */
	public final List getTypedNodeList(int nodeType) {
		CompilationUnit resourceCompUnit = ModelEnum.COMP_UNIT.getData(this);
		return getTypedNodeList(resourceCompUnit, nodeType, true);
	}

	/**
	 * {@inheritDoc}
	 */
	public final String getFilename() {
		return getFileAsFile().getName();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getBaseDir() {
		if (baseDir == null) {
			String dir = getPathFilename();
			String fullPath = getFileAsFile().getAbsolutePath();
			int index = fullPath.indexOf(dir);
			if (index >= 0) {
				baseDir = fullPath.substring(0, index);
			} else {
				Debug.FRAMEWORK.log("could not get baseDir from fullPath=", fullPath, ", pathDir=", dir);
				baseDir = "";
			}
		}

		return baseDir;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPathFilename() {
		if (pathDir == null) {
			if (getFilename().toLowerCase().endsWith(".java")) {
				List packageNodes = getTypedNodeList(ASTNode.PACKAGE_DECLARATION);
				if (!packageNodes.isEmpty()) {
					PackageDeclaration packageDecl = (PackageDeclaration) packageNodes.get(0);
					String name = packageDecl.getName().getFullyQualifiedName();
					name = name.replace('.', File.separatorChar);
					name = name + File.separatorChar + getFilename();
					pathDir = name;
				} else {
					pathDir = getFilename();
				}
			} else {
				pathDir = getFilename();
			}
		}

		return pathDir;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPackageName() {
		String packageName = "";

		List packageNodes = getTypedNodeList(ASTNode.PACKAGE_DECLARATION);
		if (!packageNodes.isEmpty()) {
			PackageDeclaration packageDecl = (PackageDeclaration) packageNodes.get(0);
			packageName = packageDecl.getName().getFullyQualifiedName();
		}

		return packageName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClassName() {
		StringBuffer className = new StringBuffer(getPackageName());
		String filename = getFilename();

		int extensionIndex = filename.lastIndexOf('.');
		if (extensionIndex >= 0) {
			filename = filename.substring(0, extensionIndex);
		}

		if (className.length() > 0) {
			className.append('.');
		}

		className.append(filename);

		return className.toString();
	}

	public ModelRegistry getModelRegistry() {
		return registry;
	}

	/**
	 * JavaNodeVisitor is used to search for nodes of a given type and add them
	 * to a list if they match. Nodes are not guaranteed to be in any particular
	 * order.
	 */
	private class JavaNodeVisitor extends ASTVisitor {

		/**
		 * The list of nodes accumulated so far.
		 */
		private List astNodeList;

		/**
		 * The node types to search for.
		 */
		private int[] astNodeTypes;

		/**
		 * True indicates that child nodes of a given node will also be included
		 * in the search, false indicates that only nodes that are explicitly
		 * visited will be searched.
		 */
		private boolean searchChildren;

		/**
		 * Constructor for JavaNodeVisitor.
		 *
		 * @param nodeType
		 *            The type of node as defined by constants in
		 *            {@link ASTNode}. Must be a valid constant value.
		 * @param newSearchChildren
		 *            True indicates to search the child nodes of the given
		 *            node, false indicates that only the given node will be
		 *            checked against the node type.
		 */
		private JavaNodeVisitor(int nodeType, boolean newSearchChildren) {
			astNodeTypes = new int[1];
			astNodeTypes[0] = nodeType;
			astNodeList = new ArrayList(10);
			this.searchChildren = newSearchChildren;
		}

		/**
		 * Checks that the given node matches the given node type and if so,
		 * adds it to the internal list.
		 *
		 * @param node
		 *            The node currently being visited. Cannot be null.
		 * @param nodeTypee
		 *            The type of node as defined by constants in
		 *            {@link ASTNode}. Must be a valid constant value.
		 *
		 * @return True if child nodes of the given node should be visited,
		 *         false otherwise.
		 */
		private boolean visitAny(ASTNode node, int nodeTypee) {
			for (int iCtr = 0; iCtr < astNodeTypes.length; iCtr++) {
				if (astNodeTypes[iCtr] == nodeTypee) {
					astNodeList.add(node);
				}
			}

			// Visit children if required
			return searchChildren;
		}

		/**
		 * Checks whether the given node should be added to the list, and if so,
		 * adds it.
		 *
		 * @param node
		 *            The node which represents a Java doc comment. Cannot be
		 *            null.
		 *
		 * @return True if child nodes of the given node should be visited,
		 *         false otherwise.
		 */
		public boolean visit(Javadoc node) {
			return visitAny(node, ASTNode.JAVADOC);
		}

		/**
		 * Checks whether the given node should be added to the list, and if so,
		 * adds it.
		 *
		 * @param node
		 *            The node which represents a method declaration. Cannot be
		 *            null.
		 *
		 * @return True if child nodes of the given node should be visited,
		 *         false otherwise.
		 */
		public boolean visit(MethodDeclaration node) {
			return visitAny(node, ASTNode.METHOD_DECLARATION);
		}

		/**
		 * Checks whether the given node should be added to the list, and if so,
		 * adds it.
		 *
		 * @param node
		 *            The node which represents a field declaration. Cannot be
		 *            null.
		 *
		 * @return True if child nodes of the given node should be visited,
		 *         false otherwise.
		 */
		public boolean visit(FieldDeclaration node) {
			return visitAny(node, ASTNode.FIELD_DECLARATION);
		}

		/**
		 * Checks whether the given node should be added to the list, and if so,
		 * adds it.
		 *
		 * @param node
		 *            The node which represents a package declaration. Cannot be
		 *            null.
		 *
		 * @return True if child nodes of the given node should be visited,
		 *         false otherwise.
		 */
		public boolean visit(PackageDeclaration node) {
			return visitAny(node, ASTNode.PACKAGE_DECLARATION);
		}

		/**
		 * Returns the list of nodes collected during the search. This method
		 * should only be called after {@link ASTNode#accept(ASTVisitor)} has
		 * been called.
		 *
		 * @return The list of nodes that match the search criteria. Will not be
		 *         null.
		 */
		public List getASTNodeList() {
			return astNodeList;
		}
	}

	@Override
	public List<ModelEnum> getSupportedModels() {
		return SUPPORTED_MODELS;
	}
}