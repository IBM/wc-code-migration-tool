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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.task.Task;

/**
 * This class loads a manifest file from a content string and adds the project
 * dependencies to a project.
 * 
 * @author Trent Hoeppner
 */
public class LoadManifestDependenciesTask extends Task<LoadingContext> {

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
	 * A regular expression pattern that matches every instance of a ".jar"
	 * followed by a non-space, which is a defect in certain manifest files
	 * after removing line endings. Use {@link Matcher#replaceAll(String)} to
	 * add a space as appropriate.
	 */
	private static final Pattern FIXED_SMOOSHED_JARS_PATTERN = Pattern.compile("\\.jar[^ ]");

	/**
	 * A regular expression pattern that matches the JARs in a Class-Path
	 * attribute. Every call to {@link Matcher#find()} will match one name, and
	 * group 1 will return that name, not including the ".jar" suffix.
	 */
	private static final Pattern EXTRACT_JARS_PATTERN = Pattern.compile("(.+?)(\\.jar|\\.rar)?(\\s|$)");

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(Arrays.asList(Name.PROJECT_NAME, Name.TEXT_CONTENT));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.PROJECT_DEPENDENCIES_LOADED));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadManifestDependenciesTask(String name, LoadingContext context) {
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
		String name = context.get(Name.PROJECT_NAME);
		String manifestContent = context.get(Name.TEXT_CONTENT);

		boolean loaded = false;
		if (manifestContent != null) {
			loadManifestAndAddDependencies(context, name, manifestContent);
			loaded = true;
		}

		context.put(Name.PROJECT_DEPENDENCIES_LOADED, loaded);
	}

	/**
	 * Loads the manifest from the given stream and adds it to the project with
	 * the given name.
	 * 
	 * @param context
	 *            The context that is used to create projects. This value cannot
	 *            be null.
	 * @param projectName
	 *            The name of the project to add dependencies to. This value
	 *            cannot be null.
	 * @param manifestIn
	 *            The input stream to load the manifest file from. This stream
	 *            will be closed after reading all content. This value cannot be
	 *            null.
	 * 
	 * @throws IOException
	 *             If an error occurs while reading from the stream.
	 */
	protected void loadManifestAndAddDependencies(LoadingContext context, String projectName, String manifestContents)
			throws IOException {
		List<Integer> dependencies = new ArrayList<>();
		Matcher matcher = EXTRACT_CLASSPATH_PATTERN.matcher(manifestContents);
		if (matcher.find()) {
			String classPath = matcher.group(1);
			String standardForm = REMOVE_LINE_ENDINGS_PATTERN.matcher(classPath).replaceAll("");
			standardForm = FIXED_SMOOSHED_JARS_PATTERN.matcher(standardForm).replaceAll(".jar ");
			Matcher jarMatcher = EXTRACT_JARS_PATTERN.matcher(standardForm);
			while (jarMatcher.find()) {
				String dependencyName = jarMatcher.group(1);

				String jarName = removeDirectories(dependencyName);

				JavaItem dependency = context.getFactory().createProject(jarName);
				dependencies.add(dependency.getID());
			}
		}

		JavaItem loadedProject = context.getFactory().createProject(projectName);
		List<Integer> existingDependencies = loadedProject.getDependenciesIDs();
		existingDependencies.clear();
		existingDependencies.addAll(dependencies);

		for (JavaItem dependency : loadedProject.getDependencies()) {
			if (!dependency.getIncomingIDs().contains(loadedProject.getID())) {
				dependency.getIncomingIDs().add(loadedProject.getID());
			}
		}
	}

	/**
	 * Removes any directories from the dependency name listed in the
	 * MANIFEST.MF file.
	 * 
	 * @param dependencyName
	 *            The name from the manifest file. This value cannot be null or
	 *            empty.
	 * 
	 * @return The name of the jar without any directories. This value will not
	 *         be null or empty.
	 */
	private String removeDirectories(String dependencyName) {
		String normalizedDependencyName = dependencyName.trim().replace('\\', '/');

		String jarName;
		int lastSlashIndex = normalizedDependencyName.lastIndexOf('/');
		if (lastSlashIndex == normalizedDependencyName.length() - 1) {
			// this entry could be a directory with classes in it (e.g.
			// "classes/"), we consider the directory itself to be a project
			// remove the last slash
			normalizedDependencyName = normalizedDependencyName.substring(0, lastSlashIndex);
			lastSlashIndex = normalizedDependencyName.lastIndexOf('/');
		}

		if (lastSlashIndex >= 0) {
			jarName = normalizedDependencyName.substring(lastSlashIndex + 1);
		} else {
			jarName = normalizedDependencyName;
		}

		return jarName;
	}
}