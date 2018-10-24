package com.ibm.commerce.dependency.model;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ProjectLoader is used to load Project based on manifest files. Each loader
 * maintains projects it has loaded before and will update all projects with
 * dependencies when a new project is loaded.
 * 
 * @author Trent Hoeppner
 */
public class ProjectLoader {

	/**
	 * A regular expression pattern that matches the Class-Path attribute in a
	 * manifest. The first call to {@link Matcher#find()} will match the
	 * contents and group 1 will return the contents of the attribute.
	 */
	private static final Pattern EXTRACT_CLASSPATH_PATTERN = Pattern
			.compile("Class-Path: ([^\r\n]*((\r|\n|\r\n) .*)*)(\\z|(\r|\n|\r\n)[^ ])", Pattern.MULTILINE);

	/**
	 * A regular expression pattern that matches every instance of a line-ending
	 * followed by a space, which is a valid way to wrap lines in a manifest
	 * file. Use {@link Matcher#replaceAll(String)} to remove all line
	 * wrappings.
	 */
	private static final Pattern REMOVE_LINE_ENDINGS_PATTERN = Pattern.compile("(\r\n|\r|\n) ");

	/**
	 * A regular expression pattern that matches the JARs in a Class-Path
	 * attribute. Every call to {@link Matcher#find()} will match one name, and
	 * group 1 will return that name, not including the ".jar" suffix.
	 */
	private static final Pattern EXTRACT_JARS_PATTERN = Pattern.compile("(.+?)(\\.jar)?(\\s|$)");

	/**
	 * A mapping from project names to {@link EclipseJavaProject Projects}. This
	 * value will never be null.
	 */
	private Map<String, JavaItem> nameToProjectMap = new HashMap<String, JavaItem>();

	private JavaItemFactory factory;

	/**
	 * Constructor for this.
	 */
	public ProjectLoader(JavaItemFactory factory) {
		this.factory = factory;
	}

	/**
	 * Loads the project with the given name based on the given manifest
	 * contents. If the new project depends on any projects that have previously
	 * been loaded by this, the dependencies in the new project will return the
	 * previous references instead of new ones. Likewise, if any projects that
	 * were previously loaded by this depend on a project with the same name as
	 * the new project, the returned value will be a project that already exists
	 * as a dependency.
	 *
	 * @param projectName
	 *            The name of the new project. Cannot be null or empty.
	 * @param manifestContents
	 *            The contents of the manifest to load. Cannot be null, but may
	 *            be empty.
	 *
	 * @return A project with dependencies that are updated to reflect the most
	 *         recent status. Will not be null.
	 */
	public JavaItem load(String projectName, String manifestContents) {
		if (projectName == null) {
			throw new NullPointerException("projectName cannot be null.");
		}

		if (projectName.isEmpty()) {
			throw new IllegalArgumentException("projectName cannot be empty.");
		}

		if (manifestContents == null) {
			throw new NullPointerException("manifestContents cannot be null.");
		}

		List<Integer> dependencies = new ArrayList<>();
		Matcher matcher = EXTRACT_CLASSPATH_PATTERN.matcher(manifestContents);
		if (matcher.find()) {
			String classPath = matcher.group(1);
			String standardForm = REMOVE_LINE_ENDINGS_PATTERN.matcher(classPath).replaceAll("");
			Matcher jarMatcher = EXTRACT_JARS_PATTERN.matcher(standardForm);
			while (jarMatcher.find()) {
				String dependencyName = jarMatcher.group(1);

				JavaItem dependency = nameToProjectMap.get(dependencyName);
				if (dependency == null) {
					dependency = factory.createProject(dependencyName);
					nameToProjectMap.put(dependencyName, dependency);
				}

				dependencies.add(dependency.getID());
			}
		}

		JavaItem loadedProject = nameToProjectMap.get(projectName);
		if (loadedProject == null) {
			loadedProject = factory.createProject(projectName);
			loadedProject.getDependenciesIDs().addAll(dependencies);
			nameToProjectMap.put(projectName, loadedProject);
		} else {
			List<Integer> existingDependencies = loadedProject.getDependenciesIDs();
			existingDependencies.clear();
			existingDependencies.addAll(dependencies);
		}

		for (JavaItem dependency : loadedProject.getDependencies()) {
			if (!dependency.getIncomingIDs().contains(loadedProject.getID())) {
				dependency.getIncomingIDs().add(loadedProject.getID());
			}
		}

		return loadedProject;
	}

	/**
	 * Loads the given manifest file and returns a project with the dependencies
	 * described in the manifest file. This is a convenience method which calls
	 * {@link #loadManifest(File)} followed by {@link #load(String, String)}.
	 *
	 * @param projectName
	 *            The name of the new project. Cannot be null or empty.
	 * @param manifestFile
	 *            The file to load. Cannot be null.
	 *
	 * @return A project with dependencies that are updated to reflect the most
	 *         recent status. Will not be null.
	 *
	 * @throws IOException
	 *             If there was an error reading the manifest file.
	 */
	public JavaItem load(String projectName, File manifestFile) throws IOException {
		String manifestContents = loadManifest(manifestFile);
		return load(projectName, manifestContents);
	}

	/**
	 * Loads the manifest given manifest file and returns its contents as a
	 * string.
	 *
	 * @param manifestFile
	 *            The file to load. Cannot be null.
	 *
	 * @return The contents of the file. Will not be null, but may be empty.
	 *
	 * @throws IOException
	 *             If there was an error reading the manifest file.
	 */
	public String loadManifest(File manifestFile) throws IOException {
		if (manifestFile == null) {
			throw new NullPointerException("file cannot be null.");
		}

		StringBuffer buf = new StringBuffer();

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(manifestFile)));
			int nextChar = reader.read();
			while (nextChar > -1) {
				buf.append((char) nextChar);
				nextChar = reader.read();
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// we don't want to interrupt the flow if it fails to close
					e.printStackTrace();
				}
			}
		}

		return buf.toString();
	}

}
