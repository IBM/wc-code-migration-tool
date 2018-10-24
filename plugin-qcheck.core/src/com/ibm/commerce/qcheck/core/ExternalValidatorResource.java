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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * ExternalValidatorResource represents a file that is outside of the eclipse
 * workspace. When an <code>IResource</code> is required, a dummy project is
 * obtained using {@link ProjectManager#getDummyProject()}. It is up to the API
 * user to call {@link ProjectManager#deleteDummyProject()} to clean-up after
 * validation has completed.
 * 
 * @author Trent Hoeppner
 */
public class ExternalValidatorResource extends BaseValidatorResource<File> {

	/**
	 * Constructor for ExternalValidatorResource.
	 *
	 * @param resource
	 *            The resource to wrap. Cannot be null.
	 *
	 * @exception IOException
	 *                If an error occurs when accessing the file.
	 */
	public ExternalValidatorResource(File resource) throws IOException {
		super(resource, ModelRegistry.getDefault());
	}

	/**
	 * Constructor for ExternalValidatorResource.
	 *
	 * @param resource
	 *            The resource to wrap. Cannot be null.
	 *
	 * @exception IOException
	 *                If an error occurs when accessing the file.
	 */
	public ExternalValidatorResource(File resource, ModelRegistry registry) throws IOException {
		super(resource, registry);
	}

	/**
	 * {@inheritDoc}
	 */
	public File getFileAsFile() {
		return getResource();
	}

	/**
	 * Returns this as an IResource within a project in the workspace. A dummy
	 * project will be created if one does not already exists, and a folder will
	 * be created which links to the base folder of the file that this wraps.
	 * <p>
	 * The API user must call {@link ProjectManager#deleteDummyProject()} to
	 * clean-up resources created as a result of this process.
	 *
	 * @return The resource wrapped by this as an Eclipse resource within the
	 *         workspace. Will not be null.
	 */
	public IResource getFileAsResource() {
		IResource resource = null;

		ProjectManager manager = ProjectManager.getInstance();

		String baseDir = getBaseDir();
		try {
			IFolder folder = manager.getDummyProjectFolder(baseDir);
			resource = folder.findMember(getPathFilename());
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return resource;
	}

}
