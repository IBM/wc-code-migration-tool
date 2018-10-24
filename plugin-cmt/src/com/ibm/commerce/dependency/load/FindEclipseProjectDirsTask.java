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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.commerce.dependency.task.Task;

/**
 * This class finds the Eclipse projects in a workspace directory, optionally
 * restricted by a filter.
 * 
 * @author Trent Hoeppner
 */
public class FindEclipseProjectDirsTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(Arrays.asList(Name.WORKSPACE_DIR, Name.FILTER));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.ECLIPSE_PROJECT_DIRS));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public FindEclipseProjectDirsTask(String name, LoadingContext context) {
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
		File workspaceDir = context.get(Name.WORKSPACE_DIR);
		final FileFilter filter = context.get(Name.FILTER);

		List<File> projectDirs = new ArrayList<>();
		File[] dirs = workspaceDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return (filter == null || filter.accept(pathname)) && pathname.isDirectory();
			}

		});

		File metadataDir = new File(workspaceDir, ".metadata\\.plugins\\org.eclipse.core.resources\\.projects");
		String[] list = metadataDir.list();
		List<String> metadataProjects;
		if (list != null) {
			metadataProjects = Arrays.asList(list);
		} else {
			metadataProjects = new ArrayList<String>();
		}

		// check each project
		for (File projectSubFile : dirs) {
			// check if there is a .project
			File projectFile = new File(projectSubFile, ".project");
			if (!projectFile.exists()) {
				// this is not a project dir
				continue;
			}

			// check if this is in the .metadata dir
			if (metadataProjects.contains(projectSubFile.getName())) {
				projectDirs.add(projectSubFile);
			}

		}

		context.put(Name.ECLIPSE_PROJECT_DIRS, projectDirs);
	}
}