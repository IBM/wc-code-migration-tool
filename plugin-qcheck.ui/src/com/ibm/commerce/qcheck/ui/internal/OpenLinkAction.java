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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.swt.program.Program;

import com.ibm.commerce.qcheck.core.ProblemAction;

/**
 * This class opens a link in the platform default web browser.
 * 
 * @author Trent Hoeppner
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class OpenLinkAction implements ProblemAction {

	@XmlAttribute
	private String urlString;

	/**
	 * Constructor for this.
	 */
	public OpenLinkAction(String urlString) {
		this.urlString = urlString;
	}

	/**
	 * Constructor for JAXB to instantiate this.
	 */
	private OpenLinkAction() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute() throws IOException {
		Program.launch(urlString);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Open a link for more information.";
	}

}
