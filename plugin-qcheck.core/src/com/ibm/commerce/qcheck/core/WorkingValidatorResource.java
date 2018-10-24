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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * WorkingValidatorResource represents a resource that is in working memory, but
 * which might not be reflected in the current file.
 * 
 * @author Trent Hoeppner
 */
public class WorkingValidatorResource extends BaseValidatorResource<IJavaElement> {

	private String bufferContents;

	private CompilationUnit comp;

	/**
	 * Constructor for WorkingValidatorResource.
	 *
	 * @param element
	 *            The element which represents the working state of the
	 *            resource. Cannot be null.
	 * @param bufferContents
	 *            The complete text of the file, as it exists in working memory.
	 *            Cannot be null, but may be empty.
	 * @param comp
	 *            The compiled form of the file to check, reflecting the state
	 *            in working memory. Cannot be null.
	 *
	 * @exception JavaModelException
	 *                If there was an error accessing the working memory buffer
	 *                for the element.
	 */
	public WorkingValidatorResource(IJavaElement element, String bufferContents, CompilationUnit comp)
			throws JavaModelException {
		super(element, ModelRegistry.getDefault());
		this.bufferContents = bufferContents;
		this.comp = comp;
	}

	/**
	 * Constructor for WorkingValidatorResource.
	 *
	 * @param element
	 *            The element which represents the working state of the
	 *            resource. Cannot be null.
	 * @param bufferContents
	 *            The complete text of the file, as it exists in working memory.
	 *            Cannot be null, but may be empty.
	 * @param comp
	 *            The compiled form of the file to check, reflecting the state
	 *            in working memory. Cannot be null.
	 *
	 * @exception JavaModelException
	 *                If there was an error accessing the working memory buffer
	 *                for the element.
	 */
	public WorkingValidatorResource(IJavaElement element, String bufferContents, CompilationUnit comp,
			ModelRegistry registry) {
		super(element, registry);
		this.bufferContents = bufferContents;
		this.comp = comp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public File getFileAsFile() {
		IFile file = (IFile) getFileAsResource();
		File javaFile = file.getRawLocation().toFile();
		return javaFile;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IResource getFileAsResource() {
		try {
			return getResource().getCorrespondingResource();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getWorkingVersion() {
		return bufferContents;
	}

	public CompilationUnit getWorkingCompUnit() {
		return comp;
	}

}
