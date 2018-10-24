package com.ibm.commerce.cmt;

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

import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This class is used to write objects to an output stream as XML.
 * 
 * @author Trent Hoeppner
 */
public class XMLUtil {

	public static void writeXML(XMLConvertable xmlConvertable, OutputStream out) {
		Document doc = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.newDocument();

			Node node = xmlConvertable.toXML(doc);
			doc.appendChild(node);

			writeXMLDoc(doc, out);
		} catch (ParserConfigurationException e) {
			throw new IllegalArgumentException("Could not configure parser for XML Document", e);
		}
	}

	public static void writeXMLDoc(Document doc, OutputStream out) {
		try {
			StreamResult result = new StreamResult(out);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new IllegalArgumentException("Could not transform doc: " + doc.getNodeName(), e);
		}
	}
}
