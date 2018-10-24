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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

/**
 * IPackageFragmentWrapperInternal uses the internal representation of
 * IPackageFragment for its implementation.
 * 
 * @author Trent Hoeppner
 */
public class IPackageFragmentWrapperInternal implements IPackageFragmentWrapper {

	private IPackageFragment fragment;

	/**
	 * Constructor for this.
	 *
	 * @param fragment
	 *            The package fragment that this wraps. Cannot be null.
	 */
	public IPackageFragmentWrapperInternal(IPackageFragment fragment) {
		if (fragment == null) {
			throw new NullPointerException("fragment cannot be null.");
		}

		this.fragment = fragment;
	}

	/**
	 * Returns the package fragment that this wraps.
	 *
	 * @return The package fragment that this wraps. Will not be null.
	 */
	public IPackageFragment getFragment() {
		return fragment;
	}

	/**
	 * Returns the name of this.
	 *
	 * @return The fragment name. Will not be null or empty.
	 */
	public String getName() {
		return fragment.getElementName();
	}

	/**
	 * Returns the Java source files in this fragment.
	 *
	 * @return The Java source files in this. Will not be null, but may be
	 *         empty.
	 */
	public List<File> getFiles() {
		List<File> files = new ArrayList<File>();

		try {
			File packageDir = fragment.getCorrespondingResource().getLocation().toFile();
			File[] sourceFiles = packageDir.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".java");
				}

			});

			files.addAll(Arrays.asList(sourceFiles));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		return files;
	}

	public File getDir() {
		File dir = null;
		try {
			dir = fragment.getCorrespondingResource().getLocation().toFile();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		return dir;
	}

	@Override
	public boolean isBinary() {
		// it's never binary for internal
		return false;
	}

}
