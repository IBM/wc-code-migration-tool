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
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a workspace from an external folder (as opposed to an
 * Eclipse object implementation).
 * 
 * @author Trent Hoeppner
 */
public class IWorkspaceWrapperExternal implements IWorkspaceWrapper {

	private File dir;

	private ArrayList<IJavaProjectWrapper> projects;

	private boolean useJARsInWC;

	private boolean searchBinaryJARs;

	private FileFilter jarFilter;

	public IWorkspaceWrapperExternal(File dir) {
		this(dir, false, false, null);
	}

	public IWorkspaceWrapperExternal(File dir, boolean useJARsInWC, boolean searchBinaryJARs, FileFilter jarFilter) {
		if (dir == null) {
			throw new NullPointerException("dir cannot be null.");
		}

		this.dir = dir;
		this.useJARsInWC = useJARsInWC;
		this.searchBinaryJARs = searchBinaryJARs;
		this.jarFilter = jarFilter;
	}

	public File getDir() {
		return dir;
	}

	public boolean isUsingJARsInWC() {
		return useJARsInWC;
	}

	@Override
	public List<IJavaProjectWrapper> getProjects() {
		if (projects == null) {
			// lazy-load the projects
			projects = new ArrayList<IJavaProjectWrapper>();

			File[] dirs = dir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}

			});

			File metadataDir = new File(dir, ".metadata\\.plugins\\org.eclipse.core.resources\\.projects");
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
					projects.add(new IJavaProjectWrapperExternal(projectSubFile, false));
				}

				if (projectSubFile.getName().equals("WC")) {
					if (useJARsInWC) {
						addWCJARsWithSource(projectSubFile);
					}

					if (searchBinaryJARs) {
						addWCJARsInLib(new File(projectSubFile, "lib"));
						// TODO this is hardcoded
						addWCJARsInLib(new File("O:/SDP/runtimes/base_v7/plugins"));
					}
				}
			}
		}

		return projects;
	}

	private void addWCJARsInLib(File libFolder) {
		File[] jars = libFolder.listFiles(new JARFilter());

		if (jars != null) {
			for (File jar : jars) {
				String name = jar.getName().substring(0, jar.getName().length() - 4);
				boolean found = false;
				for (IJavaProjectWrapper project : projects) {
					if (project.getName().equals(name)) {
						found = true;
						break;
					}
				}

				if (!found) {
					projects.add(new IJavaProjectWrapperJAR(jar, true));
				}

			}
		}
	}

	private void addWCJARsWithSource(File wcDir) {
		File[] jars = wcDir.listFiles(new JARFilter());

		if (jars != null) {
			for (File jar : jars) {
				String name = jar.getName().substring(0, jar.getName().length() - 4);
				boolean found = false;
				for (IJavaProjectWrapper project : projects) {
					if (project.getName().equals(name)) {
						found = true;
						break;
					}
				}

				if (!found) {
					projects.add(new IJavaProjectWrapperJAR(jar));
				}

			}
		}
	}

	private final class JARFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			boolean endsWith = pathname.getName().toLowerCase().endsWith(".jar");
			if (endsWith) {
				if (jarFilter == null) {
					return true;
				} else {
					boolean isInJarFilter = jarFilter.accept(pathname);
					return isInJarFilter;
				}
			}

			return false;
		}
	}

}
