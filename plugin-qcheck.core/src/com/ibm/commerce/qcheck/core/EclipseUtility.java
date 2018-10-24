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
import java.net.URL;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * This interface provides Eclipse functionality but hides the implementation so
 * that other implementations can be used in unit tests. The main implementation
 * is {@link EclipseUtil}.
 * 
 * @author Trent Hoeppner
 */
public interface EclipseUtility {

	/**
	 * Returns the class file for the given resource.
	 *
	 * @param resource
	 *            The resource to get the class file of. Cannot be null.
	 *
	 * @return The class file for the resource. Will be null if the class file
	 *         could not be found.
	 */
	File getClassFile(ValidatorResource resource);

	/**
	 * Returns the base directory that contains the class file for the given
	 * resource. The resource is assumed to be in a project in the workspace,
	 * with a corresponding output directory.
	 *
	 * @param resource
	 *            The resource to get the base output director for. Cannot be
	 *            null.
	 *
	 * @return The base directory which contains classes for the resource. Will
	 *         be null if the directory could not be found.
	 */
	File getClassBaseDir(ValidatorResource resource);

	/**
	 * Returns all the binary directories and JAR files on the class path for
	 * all projects in the workspace.
	 *
	 * @return The binary directories and JAR files for all projects. Will not
	 *         be null, but may be empty if none could be found.
	 */
	Set<File> getClassPathDirsAndJARs();

	/**
	 * Checks if the debugging files have changed, and if so, enables or
	 * disables debugging accordingly. To enable debugging, a file called
	 * <code>debugging.txt</code> must exist in the workspace root folder and/or
	 * <code>dropins/wizard/userconfig</code>, and each non-empty line must turn
	 * on or off each value in {@link Debug} class. Example contents of a
	 * <code>debugging.txt</code> file:
	 *
	 * <pre>
	 * CONFIG = true
	 * COMMENT = false
	 * FRAMEWORK = true
	 * VALIDATOR = true
	 * WIZARD = true
	 * </pre>
	 *
	 * All other situations will disable debugging.
	 */
	void reloadDebugConfig();

	/**
	 * Returns whether the plug-in is debugging or not. Use this method before
	 * performing complex calculations that are only used for debugging
	 * purposes.
	 *
	 * @return True if the plug-in is debugging, false otherwise.
	 */
	boolean isDebugging();

	/**
	 * Logs the given message to standard out with a line separator, and logs
	 * the message in the Eclipse log file.
	 *
	 * @param strings
	 *            The strings that, when concatenated, form the message to log.
	 *            If no string is supplied, an empty line will be printed. If
	 *            one of the strings is null, "null" will be part of the
	 *            message.
	 */
	void log(Object... strings);

	/**
	 * Logs the given exception to standard out with a line separator, and logs
	 * the message and exception in the Eclipse log file.
	 *
	 * @param e
	 *            The exception to log. Cannot be null.
	 * @param strings
	 *            The strings that, when concatenated, form the message to log.
	 *            If no string is supplied, an empty line will be printed. If
	 *            one of the strings is null, "null" will be part of the
	 *            message.
	 */
	void log(Throwable e, Object... strings);

	/**
	 * Checks whether the given monitor has been canceled or not, and if so,
	 * throws an exception.
	 *
	 * @param monitor
	 *            The monitor to check. Cannot be null.
	 *
	 * @throws OperationCanceledException
	 *             If the given monitor has been canceled.
	 */
	void checkCanceled(IProgressMonitor monitor) throws OperationCanceledException;

	/**
	 * Returns a URL for the given file that exists within under the
	 * <code>SDP/dropins/wizard/eclipse/plugins</code> folder (or a sub-folder).
	 *
	 * @param relativeFile
	 *            The path of the file, relative to the
	 *            <code>dropins/wizard/eclipse/plugins</code> folder. The path
	 *            must use "/" to separate directory names and must not begin
	 *            with a "/". An empty path will return the URL for the
	 *            workspace root folder itself. Cannot be null.
	 *
	 * @return The location of the desired file. Will not be null.
	 */
	URL getPluginsURL(String relativeFile);

	/**
	 * Returns a URL for the given file that exists within under the
	 * <code>SDP/dropins/wizard/userconfig</code> folder (or a sub-folder).
	 *
	 * @param relativeFile
	 *            The path of the file, relative to the
	 *            <code>dropins/wizard</code> folder. The path must use "/" to
	 *            separate directory names and must not begin with a "/". An
	 *            empty path will return the URL for the workspace root folder
	 *            itself. Cannot be null.
	 *
	 * @return The location of the desired file. Will not be null.
	 */
	URL getUserConfigURL(String relativeFile);

	/**
	 * Returns a URL for the given file that exists within the
	 * <code>SDP/dropins</code> folder (or a sub-folder).
	 *
	 * @param relativeFile
	 *            The path of the file, relative to the <code>dropins</code>
	 *            folder. The path must use "/" to separate directory names and
	 *            must not begin with a "/". An empty path will return the URL
	 *            for the workspace root folder itself. Cannot be null.
	 *
	 * @return The location of the desired file. Will not be null.
	 */
	URL getDropinsURL(String relativeFile);

	/**
	 * Returns a URL for the given file that exists within the workspace folder
	 * (or a sub-folder).
	 *
	 * @param relativeFile
	 *            The path of the file, relative to the workspace root folder.
	 *            The path must use "/" to separate directory names and must not
	 *            begin with a "/". An empty path will return the URL for the
	 *            workspace root folder itself. Cannot be null.
	 *
	 * @return The location of the desired file. Will not be null.
	 */
	URL getWorkspaceRootURL(String relativeFile);

}