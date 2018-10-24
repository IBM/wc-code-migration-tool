package com.ibm.commerce.dependency.load;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.commerce.dependency.task.Task;

/**
 * This class finds the JARs in a set of directories, optionally restricted by a
 * filter. The directories are searched recursively.
 * 
 * @author Trent Hoeppner
 */
public class FindProjectJARsTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(
			Arrays.asList(Name.JAR_DIRECTORIES, Name.THIRD_PARTY_DIRECTORIES, Name.FILTER));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.PROJECT_JARS, Name.THIRD_PARTY_JARS));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public FindProjectJARsTask(String name, LoadingContext context) {
		super(name, context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getInputConstraints() {
		return INPUTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getOutputConstraints() {
		return OUTPUTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(LoadingContext context) throws Exception {
		Set<File> jarDirectories = context.get(Name.JAR_DIRECTORIES);
		Set<File> thirdPartyDirs = context.get(Name.THIRD_PARTY_DIRECTORIES);
		final FileFilter filter = context.get(Name.FILTER);

		List<File> allDirs = new ArrayList<>();
		Map<File, Boolean> isThirdPartyMap = new HashMap<>();
		for (File dir : jarDirectories) {
			boolean isThirdParty = thirdPartyDirs.contains(dir);
			findAllSubDirs(dir, isThirdParty, allDirs, isThirdPartyMap);
		}

		Set<File> projectJARs = new HashSet<>();
		Set<File> thirdPartyJARs = new HashSet<>();
		for (File jarDirectory : allDirs) {
			File[] jars = jarDirectory.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return (filter == null || filter.accept(pathname)) && pathname.isFile()
							&& (pathname.getName().toLowerCase().endsWith(".jar")
									|| pathname.getName().toLowerCase().endsWith(".rar"));
				}

			});

			if (jars != null) {
				for (File jar : jars) {
					projectJARs.add(jar);

					File parentDir = jar.getParentFile();
					Boolean isThirdParty = isThirdPartyMap.get(parentDir);
					if (isThirdParty == null) {
						throw new IllegalArgumentException(
								"All directories should be flagged as third-party or not, but the following was not flagged: "
										+ parentDir);
					}

					if (isThirdParty) {
						thirdPartyJARs.add(jar);
					}

				}
			} else {
				System.out.println("jarDirectory might not exist: " + jarDirectory);
			}
		}

		context.put(Name.PROJECT_JARS, projectJARs);
		context.put(Name.THIRD_PARTY_JARS, thirdPartyJARs);
	}

	/**
	 * Adds the given directory to the list and recursively finds all
	 * sub-directories of the given directory.
	 * 
	 * @param dir
	 *            The directory to add. This value cannot be null.
	 * @param isThirdParty
	 *            True indicates that the given directory is for a third party,
	 *            false indicates that it is for the product.
	 * @param allDirs
	 *            The list of directories found so far. This value cannot be
	 *            null.
	 * @param isThirdPartyMap
	 *            A mapping from directory to each directory and a boolean
	 *            indicating whether the directory is a third-party directory.
	 */
	private void findAllSubDirs(File dir, boolean isThirdParty, List<File> allDirs,
			Map<File, Boolean> isThirdPartyMap) {
		allDirs.add(dir);
		isThirdPartyMap.put(dir, isThirdParty);

		File[] files = dir.listFiles();
		if (files != null) {
			for (File subFile : files) {
				if (subFile.isDirectory()) {
					findAllSubDirs(subFile, isThirdParty, allDirs, isThirdPartyMap);
				}
			}
		}
	}
}