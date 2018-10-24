package com.ibm.commerce.qcheck.tools;

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
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XMLNode is a wrapped W3C node, which makes it easier to get attributes and
 * children, and isolates the user from W3C code.
 *
 * @see XMLWrapper
 * 
 * @author Trent Hoeppner
 */
public class XMLNode {

	private Node node;

	/**
	 * Constructor for this.
	 *
	 * @param node
	 *            The W3C node to wrap. Cannot be null.
	 */
	public XMLNode(Node node) {
		this.node = node;
	}

	/**
	 * Returns the value for the attribute with the given name.
	 *
	 * @param attributeName
	 *            The name of the attribute to get the value for. Cannot be null
	 *            or empty.
	 *
	 * @return The value of the given attribute. This value will be null if the
	 *         attribute does not exist. This value may be empty.
	 */
	public String get(String attributeName) {
		String value = null;

		if (node.hasAttributes()) {
			NamedNodeMap attributes = node.getAttributes();

			Node attribute = attributes.getNamedItem(attributeName);
			if (attribute != null) {
				value = attribute.getNodeValue();
			}
		}

		return value;
	}

	/**
	 * Returns the child nodes of this.
	 *
	 * @return The child nodes. This value will not be null, but may be empty if
	 *         there are no children.
	 */
	public List<XMLNode> getChildren() {
		return getChildren(null);
	}

	/**
	 * Returns the children of this which match the given name.
	 *
	 * @param name
	 *            The tag name for child elements to look for. Cannot be null or
	 *            empty.
	 *
	 * @return The children which match the given name. Will not be null, but
	 *         may be empty if no such children exist.
	 */
	public List<XMLNode> getChildren(String name) {
		List<XMLNode> nodes = new ArrayList<XMLNode>();
		NodeList nodeList = node.getChildNodes();
		for (int nodeIndex = 0; nodeIndex < nodeList.getLength(); nodeIndex++) {
			Node child = nodeList.item(nodeIndex);
			if (name == null || child.getNodeName().equals(name)) {
				XMLNode childNode = new XMLNode(child);
				nodes.add(childNode);
			}
		}
		return nodes;
	}

	/**
	 * Returns the first child that matches the given name. This is a
	 * convenience method to get a child node if only one of that type is
	 * expected.
	 *
	 * @param name
	 *            The tag name of the child element to look for. Cannot be null
	 *            or empty.
	 *
	 * @return The node that was found, or null if it was not found.
	 */
	public XMLNode getFirstChild(String name) {
		List<XMLNode> nodes = getChildren(name);

		XMLNode firstChild;
		if (!nodes.isEmpty()) {
			firstChild = nodes.get(0);
		} else {
			firstChild = null;
		}

		return firstChild;
	}

	/**
	 * Returns the text content of this.
	 *
	 * @return The text content. This value may be null or empty.
	 */
	public String getContent() {
		return node.getTextContent();
	}
}
