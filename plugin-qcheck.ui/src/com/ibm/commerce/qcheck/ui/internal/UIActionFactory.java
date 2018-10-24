package com.ibm.commerce.qcheck.ui.internal;

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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ProblemAction;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.ValidatorResource;

/**
 * This class provides actions that can be used in the Eclipse editor UI for
 * different files.
 * 
 * @author Trent Hoeppner
 */
public class UIActionFactory implements ProblemActionFactory {

	private JAXBContext context;

	/**
	 * Constructor for this.
	 */
	public UIActionFactory() {
		try {
			context = JAXBContext.newInstance(ReplaceAction.class, OpenLinkAction.class);
		} catch (JAXBException e) {
			Debug.FRAMEWORK.log(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProblemAction buildLink(URL url) {
		return new OpenLinkAction(url.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProblemAction buildReplace(ValidatorResource resource, int startPosition, int endPosition,
			String replacement) {
		String resourcePath = resource.getFileAsResource().getFullPath().toString();
		return new ReplaceAction(resourcePath, startPosition, endPosition, replacement);
	}

	/**
	 * Converts the given action to XML so that it can be stored in an Eclipse
	 * IMarker object.
	 *
	 * @param action
	 *            The action to convert. This value cannot be null.
	 *
	 * @return The XML form of the action. This value will not be null or empty.
	 *
	 * @throws IOException
	 *             If an error occurs in the conversion process.
	 */
	public String marshal(ProblemAction action) throws IOException {
		StringWriter writer = new StringWriter();
		try {
			Marshaller m = context.createMarshaller();
			m.marshal(action, writer);
		} catch (JAXBException e) {
			throw new IOException(e);
		}

		return writer.toString();
	}

	/**
	 * Converts the given XML from an Eclipse IMarker object into an action so
	 * that it can be used.
	 *
	 * @param xml
	 *            The XML to convert. This value cannot be null or empty.
	 *
	 * @return The action that was converted. This value will not be null.
	 *
	 * @throws IOException
	 *             If an error occurs in the conversion process.
	 */
	public ProblemAction unmarshal(String xml) throws IOException {
		try {
			StringReader reader = new StringReader(xml);
			Unmarshaller m = context.createUnmarshaller();
			ProblemAction action = (ProblemAction) m.unmarshal(reader);
			return action;
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

}
