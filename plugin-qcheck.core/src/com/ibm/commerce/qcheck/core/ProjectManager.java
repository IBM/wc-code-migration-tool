package com.ibm.commerce.qcheck.core;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * ProjectManager provides a simplified way to interact with eclipse projects.
 * It provides a way to create and delete a dummy project specifically for the
 * purpose of using validators that will only work with <code>IResources</code>.
 * 
 * @author Trent Hoeppner
 */
public class ProjectManager {

	/**
	 * The singleton instance of this.
	 */
	private static final ProjectManager INSTANCE = new ProjectManager();

	/**
	 * Returns the singleton instance of this.
	 * 
	 * @return The singleton instance of this. Will not be null.
	 */
	public static ProjectManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Constructor for ProjectManager. Private to prevent instantiation.
	 */
	private ProjectManager() {
		// do nothing
	}

	/**
	 * A mapping from directory names to folders which are linked to the actual
	 * folder in the file system. Each directory is the base directory of a file
	 * that has a namespace. For Java files, this means the directory at the top
	 * of a package hierarchy.
	 */
	private Map<String, IFolder> baseDirToFolderMap = new HashMap<String, IFolder>();

	/**
	 * The dummy project for use with some validators.
	 */
	private IProject dummyProject = null;

	/**
	 * Deletes the dummy project in the root folder and the hierarchy. If no
	 * dummy project has been created then this method will have no effect.
	 * 
	 * @exception CoreException
	 *                If an error occurs when deleting the project.
	 */
	public void deleteDummyProject() throws CoreException {
		if (dummyProject != null) {
			dummyProject.delete(true, true, null);
			dummyProject = null;
			baseDirToFolderMap.clear();
		}
	}

	/**
	 * Returns the dummy project for validation purposes, creating one if
	 * necessary.
	 * 
	 * @return The project that can be used to create folders and files with.
	 *         Will not be null.
	 * 
	 * @exception CoreException
	 *                If an error occurs when creating the project.
	 */
	public IProject getDummyProject() throws CoreException {
		if (dummyProject == null) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("ValidationDummy");
			boolean exists = project.exists();
			if (!exists) {
				project.create(null);
			}
			if (!project.isOpen()) {
				project.open(null);
			}
			if (!exists) {
				IProjectDescription description = project.getDescription();
				String[] natureIDArray = description.getNatureIds();
				List<String> natureIDs = new ArrayList<String>(Arrays.asList(natureIDArray));
				if (!natureIDs.contains("org.eclipse.jdt.core.javanature")) {
					natureIDs.add("org.eclipse.jdt.core.javanature");
					description.setNatureIds(natureIDs.toArray(natureIDArray));
					project.setDescription(description, null);
				}
			}
			dummyProject = project;
		}
		return dummyProject;
	}

	/**
	 * Returns a project folder which is linked to the given base folder in the
	 * operating system. All files in the indicated directory will be available
	 * as <code>IResources</code> in the returned folder.
	 * <p>
	 * If the dummy project does not yet exist, it will be created.
	 * 
	 * @param baseDir
	 *            The absolute path of the directory to link to, using the file
	 *            separator of the local file system. Cannot be null or empty.
	 * 
	 * @return A folder which links to the base directory. Will not be null.
	 * 
	 * @exception CoreException
	 *                If an error occurs when creating the folder or linking to
	 *                the local file system.
	 */
	public IFolder getDummyProjectFolder(String baseDir) throws CoreException {
		IFolder folder = baseDirToFolderMap.get(baseDir);
		if (folder == null) {
			IProject project = getDummyProject();
			IJavaProject javaProject = JavaCore.create(project);
			String sourceDirName = toSourceDirName(baseDir);
			folder = project.getFolder(sourceDirName);
			if (!folder.exists()) {
				folder.createLink(new Path(baseDir), IResource.NONE, null);
				IClasspathEntry newEntry = JavaCore.newSourceEntry(folder.getFullPath());
				IClasspathEntry[] entryArray = javaProject.getRawClasspath();
				List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(entryArray));
				for (Iterator<IClasspathEntry> iterator = entries.iterator(); iterator.hasNext();) {
					IClasspathEntry current = iterator.next();
					if (current.getPath().lastSegment().equals("ValidationDummy")) {
						iterator.remove();
						break;
					}
				}
				entries.add(newEntry);
				javaProject.setRawClasspath(entries.toArray(entryArray), null);
			}

			baseDirToFolderMap.put(baseDir, folder);
		}

		return folder;
	}

	/**
	 * Converts the given absolute path to a name which can be used as the name
	 * of a single folder in the local file system. Characters which are not
	 * allowed will be stripped or converted to make a valid name.
	 * 
	 * @param baseDir
	 *            The absolute path to convert. Cannot be null.
	 * 
	 * @return The converted filename. If the given directory is empty, the name
	 *         "default" will be returned. Will not be null or empty.
	 */
	private String toSourceDirName(String baseDir) {
		String sourceDir = baseDir.replace(File.separatorChar, '.');

		int colonIndex = sourceDir.indexOf(':');
		if (colonIndex >= 0) {
			sourceDir = sourceDir.substring(colonIndex + 1);
		}

		if (sourceDir.startsWith(".")) {
			sourceDir = sourceDir.substring(1);
		}

		if (sourceDir.endsWith(".")) {
			sourceDir = sourceDir.substring(0, sourceDir.length() - 1);
		}

		if (sourceDir.isEmpty()) {
			sourceDir = "default";
		}

		return sourceDir;
	}
}
