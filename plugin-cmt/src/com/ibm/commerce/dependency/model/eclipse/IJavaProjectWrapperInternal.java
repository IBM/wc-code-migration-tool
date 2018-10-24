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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

/**
 * IJavaProjectWrapperInternal uses the internal representation of IJavaProject
 * for its implementation.
 * 
 * @author Trent Hoeppner
 */
public class IJavaProjectWrapperInternal implements IJavaProjectWrapper {

	private static final String MANIFEST_RELATIVE_PATH = "META-INF/MANIFEST.MF";

	private IJavaProject project;

	/**
	 * Constructor for this.
	 *
	 * @param project
	 *            The project to wrap. Cannot be null.
	 */
	public IJavaProjectWrapperInternal(IJavaProject project) {
		if (project == null) {
			throw new NullPointerException("project cannot be null.");
		}

		this.project = project;
	}

	/**
	 * Returns the wrapped project.
	 *
	 * @return The wrapped project. Will not be null.
	 */
	public IJavaProject getProject() {
		return project;
	}

	/**
	 * Returns the name of this project.
	 *
	 * @return The name of this. Will not be null or empty.
	 */
	@Override
	public String getName() {
		return project.getProject().getName();
	}

	/**
	 * Returns the manifest file for this project.
	 *
	 * @return The manifest file. May be null if no manifest file exists.
	 */
	@Override
	public File getManifestFile() {
		IProject workspaceProject = project.getProject();
		IResource manifestResource = workspaceProject.findMember(MANIFEST_RELATIVE_PATH);
		if (manifestResource == null) {
			try {
				IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
				for (IPackageFragmentRoot root : roots) {
					if (isSourceDir(root)) {
						IResource resource = root.getResource();
						IPath path = new Path(
								resource.getProjectRelativePath().toString() + "/" + MANIFEST_RELATIVE_PATH);
						IResource possibleManifest = workspaceProject.getFile(path);
						if (possibleManifest.exists()) {
							manifestResource = possibleManifest;
						}
						break;
					}
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}

		File manifestFile = null;
		if (manifestResource != null) {
			manifestFile = manifestResource.getRawLocation().toFile();
		}

		return manifestFile;
	}

	private boolean isSourceDir(IPackageFragmentRoot root) throws JavaModelException {
		return root.getKind() == IPackageFragmentRoot.K_SOURCE;
	}

	@Override
	public List<IPackageFragmentWrapper> getFragments() {
		List<IPackageFragmentWrapper> fragments = new ArrayList<IPackageFragmentWrapper>();
		try {
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			for (IPackageFragmentRoot root : roots) {
				if (isSourceDir(root)) {
					for (IJavaElement child : root.getChildren()) {
						IPackageFragment fragment = (IPackageFragment) child;
						addPackageIfNotEmpty(fragment, fragments);
					}
				}
			}
		} catch (JavaModelException e) {
			System.out.println("Error when getting source Java package fragments for project " + getName());
			e.printStackTrace();
		}

		return fragments;
	}

	private void addPackageIfNotEmpty(IPackageFragment fragment, List<IPackageFragmentWrapper> fragments)
			throws JavaModelException {
		if (fragment.containsJavaResources()) {
			IPackageFragmentWrapperInternal internalFragment = new IPackageFragmentWrapperInternal(fragment);
			fragments.add(internalFragment);
		}
	}

	@Override
	public boolean isBinary() {
		// it's never binary for internal
		return false;
	}

}
