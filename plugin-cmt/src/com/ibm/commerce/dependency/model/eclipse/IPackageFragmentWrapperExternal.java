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
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a package fragment from an external folder (as opposed
 * to an Eclipse object implementation).
 * 
 * @author Trent Hoeppner
 */
public class IPackageFragmentWrapperExternal implements IPackageFragmentWrapper {

	private File dir;

	private String name;

	private List<File> files;

	private boolean binary;

	public IPackageFragmentWrapperExternal(String baseDirString, File dir, boolean binary) {
		if (baseDirString == null) {
			throw new NullPointerException("baseDirString cannot be null.");
		}

		if (dir == null) {
			throw new NullPointerException("dir cannot be null.");
		}

		this.dir = dir;
		this.binary = binary;

		String dirString = dir.getAbsolutePath();
		String pathName;
		if (!dirString.equals(baseDirString)) {
			pathName = dirString.substring(baseDirString.length() + 1);
		} else {
			pathName = "";
		}

		this.name = pathName.replaceAll("\\\\|/", ".");
	}

	@Override
	public File getDir() {
		return dir;
	}

	@Override
	public boolean isBinary() {
		return binary;
	}

	@Override
	public List<File> getFiles() {

		if (files == null) {
			// lazy-load the files
			File[] subDirs = dir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					String lowerCase = pathname.getName().toLowerCase();
					return pathname.isFile()
							&& (!binary && lowerCase.endsWith(".java") || binary && lowerCase.endsWith(".class"));
				}

			});

			files = Arrays.asList(subDirs);
		}

		return files;
	}

	@Override
	public String getName() {
		return name;
	}

}
