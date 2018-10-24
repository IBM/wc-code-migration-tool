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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ibm.commerce.dependency.task.ChainTask;
import com.ibm.commerce.dependency.task.Task;

/**
 * This class creates tasks to create project items and their dependencies based
 * on a set of JARs. When extracting an API, the projects will be set to not
 * expose private and default class members.
 * 
 * @author Trent Hoeppner
 */
public class LoadJARProjectsTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(
			Arrays.asList(Name.PROJECT_JARS, Name.THIRD_PARTY_JARS, Name.IS_EXTRACTING_API));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.JAR_PROJECT_LOADER_TASKS_ADDED));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadJARProjectsTask(String name, LoadingContext context) {
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
		Set<File> projectJARs = context.get(Name.PROJECT_JARS);
		Set<File> thirdPartyJARs = context.get(Name.THIRD_PARTY_JARS);
		boolean isExtractingAPI = context.get(Name.IS_EXTRACTING_API);

		for (File projectJAR : projectJARs) {
			boolean isThirdParty = thirdPartyJARs.contains(projectJAR);
			String jarName = projectJAR.getName();

			// the suffix is always a 4-character .jar or .rar, remove it
			String projectName = jarName.substring(0, jarName.length() - 4);

			LoadingContext addProjectContext = context.forNewTaskGroup();
			addProjectContext.put(Name.PROJECT_NAME, projectName);
			addProjectContext.put(Name.IS_WORKSPACE, false);
			addProjectContext.put(Name.IS_THIRD_PARTY, isThirdParty);
			addProjectContext.put(Name.FILE, projectJAR);
			context.addTask(new DetectJARBinaryTask("DetectIfJARIsBinary", addProjectContext),
					Priority.LOADING_PROJECTS);
			context.addTask(new CloseZipFileTask("CloseZipFile", addProjectContext), Priority.LOADING_PROJECTS);
			context.addTask(new LoadProjectTask("LoadProject", addProjectContext), Priority.LOADING_PROJECTS);

			LoadingContext addDependenciesContext = context.forNewTaskGroup();
			addDependenciesContext.put(Name.PROJECT_NAME, projectName);
			addDependenciesContext.put(Name.FILE, projectJAR);
			addDependenciesContext.put(Name.ZIP_ENTRY_NAME, "META-INF/MANIFEST.MF");
			ChainTask<LoadingContext> dependenciesChain = new ChainTask<>("AllDependenciesBarrier",
					addDependenciesContext);
			dependenciesChain.addTask(new FindZipEntryTask("FindZipEntry", addDependenciesContext));
			dependenciesChain.addTask(new ZipEntryToInputStreamTask("ZipEntryToInputStream", addDependenciesContext));
			dependenciesChain.addTask(new LoadFromInputStreamTask("LoadFromInputStream", addDependenciesContext));
			dependenciesChain.addTask(new CloseZipFileTask("CloseZipFile", addDependenciesContext));
			dependenciesChain
					.addTask(new LoadManifestDependenciesTask("LoadManifestDependencies", addDependenciesContext));
			context.addTask(dependenciesChain, Priority.LOADING_PROJECT_DEPENDENCIES);

			LoadingContext addPackagesAndClassesContext = context.forNewTaskGroup();
			addPackagesAndClassesContext.put(Name.PROJECT_NAME, projectName);
			addPackagesAndClassesContext.put(Name.IS_EXTRACTING_API, isExtractingAPI);
			addPackagesAndClassesContext.put(Name.FILE, projectJAR);
			addPackagesAndClassesContext.put(Name.OTHER_CONTEXT, addDependenciesContext);
			addPackagesAndClassesContext.put(Name.OTHER_CONTEXT_NAME, Name.PROJECT_DEPENDENCIES_LOADED);
			ChainTask<LoadingContext> addProjectChildrenChain = new ChainTask<>("AddProjectChildren",
					addPackagesAndClassesContext);
			addProjectChildrenChain
					.addTask(new AddFromOtherContextTask("AddFromOtherContext", addPackagesAndClassesContext));
			addProjectChildrenChain.addTask(new LoadJARProjectPackagesAndClassesTask("LoadJARProjectPackagesAndClasses",
					addPackagesAndClassesContext));
			context.addTask(addProjectChildrenChain, Priority.CREATING_TASKS_FOR_LOADING_CLASSES);
		}

		context.put(Name.JAR_PROJECT_LOADER_TASKS_ADDED, true);
	}
}