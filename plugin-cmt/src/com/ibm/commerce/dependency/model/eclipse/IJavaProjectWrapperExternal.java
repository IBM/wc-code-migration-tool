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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.commerce.cmt.Check;

/**
 * This class represents a project from an external folder (as opposed to an
 * Eclipse object implementation).
 * 
 * @author Trent Hoeppner
 */
public class IJavaProjectWrapperExternal implements IJavaProjectWrapper {

	private File dir;

	private ArrayList<IPackageFragmentWrapper> fragments;

	private boolean binary;

	public IJavaProjectWrapperExternal(File dir, boolean binary) {
		if (dir == null) {
			throw new NullPointerException("dir cannot be null.");
		}

		this.dir = dir;
		this.binary = binary;
	}

	public File getDir() {
		return dir;
	}

	@Override
	public List<IPackageFragmentWrapper> getFragments() {
		if (fragments == null) {
			// lazy-load the fragments
			fragments = new ArrayList<IPackageFragmentWrapper>();
			File[] subDirs = dir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory() && !pathname.getName().startsWith(".")
							&& !pathname.getName().startsWith("META-INF");
				}

			});

			for (File subDir : subDirs) {
				addPackages(subDir, fragments);
			}
		}

		return fragments;
	}

	private void addPackages(File baseDir, List<IPackageFragmentWrapper> fragments) {
		String baseString = baseDir.getAbsolutePath();
		File[] subFiles = baseDir.listFiles();
		boolean defaultPackageAdded = false;
		for (File subFile : subFiles) {
			if (subFile.isDirectory()) {
				addPackages(baseString, subFile, fragments);
			} else if (!defaultPackageAdded && subFile.isFile() && isFileForPackage(subFile)) {
				fragments.add(new IPackageFragmentWrapperExternal(baseString, baseDir, binary));
				defaultPackageAdded = true;
			}
		}
	}

	private void addPackages(String baseString, File dir, List<IPackageFragmentWrapper> fragments) {
		File[] subFiles = dir.listFiles();
		boolean packageAdded = false;
		for (File subFile : subFiles) {
			if (subFile.isDirectory()) {
				addPackages(baseString, subFile, fragments);
			} else if (!packageAdded && subFile.isFile() && isFileForPackage(subFile)) {
				fragments.add(new IPackageFragmentWrapperExternal(baseString, dir, binary));
				packageAdded = true;
			}
		}
	}

	private boolean isFileForPackage(File file) {
		String lowerCase = file.getName().toLowerCase();
		return !binary && lowerCase.endsWith(".java") || binary && lowerCase.endsWith(".class");
	}

	@Override
	public File getManifestFile() {
		File manifest = null;

		String manifestSubPath = "META-INF\\MANIFEST.MF";
		File topDirManifest = new File(dir, manifestSubPath);
		if (topDirManifest.exists()) {
			manifest = topDirManifest;
		} else {
			File[] subDirs = dir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}

			});

			Check.notNull(subDirs, "subDirs");
			for (File subDir : subDirs) {
				File subDirManifest = new File(subDir, manifestSubPath);
				if (subDirManifest.exists()) {
					manifest = subDirManifest;
					break;
				}
			}
		}

		return manifest;
	}

	@Override
	public String getName() {
		return dir.getName();
	}

	@Override
	public boolean isBinary() {
		return binary;
	}

}
