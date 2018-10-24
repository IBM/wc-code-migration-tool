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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.commerce.qcheck.core.Debug;

/**
 * XMLWrapper makes it easier to parse XML documents by wrapping all the W3C XML
 * code. This also gives isolation from W3C code, making it possible to mock or
 * fake these functions.
 * 
 * @author Trent Hoeppner
 */
public class XMLWrapper {

	private Document doc;

	/**
	 * Constructor for this.
	 */
	public XMLWrapper() {
		// do nothing
	}

	/**
	 * Initializes this by parsing the given stream as an XML document. The
	 * stream will be closed before this method returns.
	 *
	 * @param in
	 *            The stream to parse. Cannot be null.
	 */
	public void init(BufferedInputStream in) {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = builderFactory.newDocumentBuilder();

			this.doc = builder.parse(in);
		} catch (ParserConfigurationException e) {
			Debug.VALIDATOR.log(e.getMessage());
		} catch (SAXException e) {
			Debug.VALIDATOR.log(e.getMessage());
		} catch (FileNotFoundException e) {
			Debug.VALIDATOR.log(e.getMessage());
		} catch (IOException e) {
			Debug.VALIDATOR.log(e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Debug.VALIDATOR.log(e.getMessage());
				}
			}
		}

	}

	/**
	 * Returns the list of nodes in this that match the given name.
	 *
	 * @param pattern
	 *            The name of the elements to search for. Cannot be null or
	 *            empty.
	 *
	 * @return The list of nodes, in the order that they occur in the document.
	 *         Will not be null, but may be empty if no matching nodes were
	 *         found.
	 */
	public List<XMLNode> getNodes(String pattern) {
		List<XMLNode> nodes = new ArrayList<XMLNode>();
		if (doc != null) {
			NodeList nodeList = doc.getElementsByTagName(pattern);
			for (int nodeIndex = 0; nodeIndex < nodeList.getLength(); nodeIndex++) {
				Node child = nodeList.item(nodeIndex);
				XMLNode childNode = new XMLNode(child);
				nodes.add(childNode);
			}
		}

		return nodes;
	}
}
