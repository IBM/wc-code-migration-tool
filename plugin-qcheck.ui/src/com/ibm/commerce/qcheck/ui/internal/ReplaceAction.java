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

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ProblemAction;

/**
 * This class replaces some text in the target file.
 * 
 * @author Trent Hoeppner
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ReplaceAction implements ProblemAction {

	@XmlAttribute
	private String resourcePath;

	@XmlAttribute
	private int startPosition;

	@XmlAttribute
	private int endPosition;

	@XmlAttribute
	private String replacement;

	/**
	 * Constructor for this.
	 *
	 * @param resourcePath
	 *            The Eclipse path which points to the resource being analyzed.
	 *            This value cannot be null or empty.
	 * @param startPosition
	 *            The 0-based index into the file which marks the start of the
	 *            section to be replaced. This value must be &gt;= 0.
	 * @param endPosition
	 *            The 0-based index into the file which marks the end
	 *            (exclusive) of the section to be replaced. This value must be
	 *            &gt;= 0.
	 * @param replacement
	 *            The value to replace the marked section with. This value
	 *            cannot be null, but may be empty.
	 */
	public ReplaceAction(String resourcePath, int startPosition, int endPosition, String replacement) {
		this.resourcePath = resourcePath;
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.replacement = replacement;
	}

	/**
	 * Constructor for JAXB to instantiate this.
	 */
	private ReplaceAction() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute() throws IOException {
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath);
		IFile file = (IFile) resource;
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
		IDocument document = buffer.getDocument();
		int length = endPosition - startPosition;
		if (startPosition <= 0 || length <= 0) {
			Debug.FRAMEWORK.log("Invalid startPosition (", startPosition, ") or endPosition (", endPosition, ")");
			return;
		}

		try {
			document.replace(startPosition, length, replacement);
		} catch (BadLocationException e) {
			Debug.FRAMEWORK.log(e);
		}
		try {
			buffer.commit(null, false);
		} catch (CoreException e) {
			Debug.FRAMEWORK.log(e);
		}

		try {
			throw new Exception();
		} catch (Exception e) {
			Debug.FRAMEWORK.log(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Replace with \'" + replacement + "\'";
	}

}
