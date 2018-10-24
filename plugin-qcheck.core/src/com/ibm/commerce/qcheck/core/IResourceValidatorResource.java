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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

/**
 * IResourceValidatorResource represents {@link IResource}s so that they can be
 * used by {@link Validator}s.
 * 
 * @author Trent Hoeppner
 */
public class IResourceValidatorResource extends BaseValidatorResource<IResource> {

	/**
	 * Constructor for IResourceValidatorResource.
	 * 
	 * @param resource
	 *            The resource that this represents. Cannot be null, and must
	 *            represent a Java file.
	 * 
	 * @exception IOException
	 *                If an error occurs when reading the file.
	 */
	public IResourceValidatorResource(IResource resource) throws IOException {
		super(resource, ModelRegistry.getDefault());
	}

	/**
	 * Constructor for IResourceValidatorResource.
	 * 
	 * @param resource
	 *            The resource that this represents. Cannot be null, and must
	 *            represent a Java file.
	 * 
	 * @exception IOException
	 *                If an error occurs when reading the file.
	 */
	public IResourceValidatorResource(IResource resource, ModelRegistry registry) throws IOException {
		super(resource, registry);
	}

	/**
	 * Returns the file that this represents.
	 * 
	 * @return The file that this represents. Will not be null.
	 */
	public IFile getFile() {
		return getResource().getProject().getFile(getResource().getProjectRelativePath());

	}

	/**
	 * {@inheritDoc}
	 */
	public File getFileAsFile() {
		File file = getResource().getRawLocation().toFile();
		return file;
	}

	/**
	 * {@inheritDoc}
	 */
	public IResource getFileAsResource() {
		return getResource();
	}
}
