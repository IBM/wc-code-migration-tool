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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.dependency.task.ChainTask;
import com.ibm.commerce.dependency.task.Task;

/**
 * This class creates tasks to create project items and their dependencies based
 * on a set of Eclipse project directories. When extracting an API, the projects
 * will be set to not expose private and default class members.
 * 
 * @author Trent Hoeppner
 */
public class LoadEclipseProjectsTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(Arrays.asList(Name.ECLIPSE_PROJECT_DIRS));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.ECLIPSE_PROJECT_LOADER_TASKS_ADDED));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadEclipseProjectsTask(String name, LoadingContext context) {
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
		List<File> projectDirs = context.get(Name.ECLIPSE_PROJECT_DIRS);
		// optional parameter
		Set<File> javaFiles = context.get(Name.JAVA_FILES);

		for (File projectDir : projectDirs) {
			String name = projectDir.getName();
			File manifestFile = getManifestFile(projectDir);

			LoadingContext addProjectContext = context.forNewTaskGroup();
			addProjectContext.put(Name.PROJECT_NAME, name);
			addProjectContext.put(Name.IS_WORKSPACE, true);
			addProjectContext.put(Name.IS_BINARY, false);
			addProjectContext.put(Name.IS_THIRD_PARTY, false);
			context.addTask(new LoadProjectTask("LoadProject", addProjectContext), Priority.LOADING_PROJECTS);

			LoadingContext addDependenciesContext = context.forNewTaskGroup();
			addDependenciesContext.put(Name.PROJECT_NAME, name);
			addDependenciesContext.put(Name.FILE, manifestFile);
			ChainTask<LoadingContext> dependenciesChain = new ChainTask<>("LoadDependencies", addDependenciesContext);
			dependenciesChain.addTask(new FileToInputStreamTask("FileToInputStream", addDependenciesContext));
			dependenciesChain.addTask(new LoadFromInputStreamTask("LoadFromInputStream", addDependenciesContext));
			dependenciesChain
					.addTask(new LoadManifestDependenciesTask("LoadManifestDependencies", addDependenciesContext));
			context.addTask(dependenciesChain, Priority.LOADING_PROJECT_DEPENDENCIES);

			LoadingContext addPackagesAndClassesContext = context.forNewTaskGroup();
			addPackagesAndClassesContext.put(Name.PROJECT_NAME, name);
			addPackagesAndClassesContext.put(Name.PROJECT_DIR, projectDir);
			addPackagesAndClassesContext.put(Name.JAVA_FILES, javaFiles);
			addPackagesAndClassesContext
					.addTask(new LoadEclipseProjectPackagesAndClassesTask("LoadEclipseProjectPackagesAndClasses",
							addPackagesAndClassesContext), Priority.CREATING_TASKS_FOR_LOADING_CLASSES);
		}

		context.put(Name.ECLIPSE_PROJECT_LOADER_TASKS_ADDED, true);
	}

	/**
	 * Find the MANIFEST.MF file in an appropriate sub-directory of the given
	 * directory.
	 * 
	 * @param dir
	 *            The directory to search in. This value cannot be null.
	 * 
	 * @return The manifest that was found, or null if none could be found.
	 */
	private File getManifestFile(File dir) {
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
}