package com.ibm.commerce.dependency.model.eclipse;

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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * IWorkspaceWrapperInternal uses the internal representation of IWorkspace for
 * its implementation.
 * 
 * @author Trent Hoeppner
 */
public class IWorkspaceWrapperInternal implements IWorkspaceWrapper {

	private IWorkspace workspace;

	private static final String MANIFEST_RELATIVE_PATH = "META-INF/MANIFEST.MF";

	/**
	 * Constructor for this.
	 *
	 * @param workspace
	 *            The Eclipse implementation that this wraps. Cannot be null.
	 */
	public IWorkspaceWrapperInternal(IWorkspace workspace) {
		if (workspace == null) {
			throw new NullPointerException("workspace cannot be null.");
		}

		this.workspace = workspace;
	}

	/**
	 * Returns the Eclipse implementation that this wraps.
	 *
	 * @return The Eclipse implementation. Will not be null.
	 */
	public IWorkspace getWorkspace() {
		return workspace;
	}

	/**
	 * Returns the open Java projects in this.
	 *
	 * @return The open Java projects. Will not be null, but may be empty.
	 */
	public List<IJavaProjectWrapper> getProjects() {
		List<IJavaProjectWrapper> javaProjects = new ArrayList<IJavaProjectWrapper>();

		IProject[] workspaceProjects = workspace.getRoot().getProjects();

		for (int i = 0; i < workspaceProjects.length; i++) {
			IProject workspaceProject = workspaceProjects[i];
			if (!workspaceProject.isOpen()) {
				// TODO add UT for this case
				continue;
			}

			String name = workspaceProject.getName();
			IJavaProject javaProject = JavaCore.create(workspaceProject);
			IJavaProjectWrapperInternal wrapperProject = new IJavaProjectWrapperInternal(javaProject);
			javaProjects.add(wrapperProject);
		}

		return javaProjects;
	}

}
