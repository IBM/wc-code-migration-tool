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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * EclipseUtil is a wrapper for eclipse functions that are needed by validation
 * tools.
 * 
 * @author Trent Hoeppner
 */
public class EclipseUtil implements EclipseUtility {

	private static EclipseUtility defaultInstance;

	/**
	 * The global debugging file which is monitored to determine whether
	 * debugging should be enabled or disabled. This file exists in the
	 * <code>dropins/wizard/userconfig</code> folder.
	 */
	private final DebugConfigFile globalDebugConfig = new DebugConfigFile(
			getDropinsURL("wizard/userconfig/debugging.txt"));

	/**
	 * The local debugging file which is monitored to determine whether
	 * debugging should be enabled or disabled. This file exists in the
	 * workspace folder.
	 */
	private final DebugConfigFile debugConfig = new DebugConfigFile(getWorkspaceRootURL("debugging.txt"));

	/**
	 * Returns the default instance for the application.
	 *
	 * @return The default instance. Will not be null.
	 */
	public static EclipseUtility getDefault() {
		if (defaultInstance == null) {
			defaultInstance = new EclipseUtil();
		}

		return defaultInstance;
	}

	/**
	 * Sets the default instance for the application. This should only be used
	 * for unit testing.
	 *
	 * @param newDefault
	 *            The instance to use as the default. Will not be null.
	 */
	public static void setDefault(EclipseUtility newDefault) {
		defaultInstance = newDefault;
	}

	/**
	 * Constructor for this, public to allow overriding.
	 */
	public EclipseUtil() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public File getClassFile(ValidatorResource resource) {
		File classFile = null;

		String className = resource.getClassName();

		File binDir = getClassBaseDir(resource);

		if (Debug.VALIDATOR.isActive()) {
			Debug.VALIDATOR.log("EclipseUtil.getClassFile(ValidatorResource) binDir = ", binDir);
		}

		// check that the last modified date for the class is later
		// than the source file
		String classPath = className.replace(".", "/") + ".class";
		classFile = new File(binDir, classPath);

		return classFile;
	}

	/**
	 * {@inheritDoc}
	 */
	public File getClassBaseDir(ValidatorResource resource) {
		File classDir = null;

		IResource javaResource = resource.getFileAsResource();
		IProject iProject = javaResource.getProject();
		IJavaProject project = JavaCore.create(iProject);
		try {
			IPath projectEclipseRelativePath = iProject.getFullPath();
			String projectFilePath = iProject.getLocation().toOSString();

			IClasspathEntry[] entries = project.getResolvedClasspath(true);
			for (IClasspathEntry entry : entries) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath path = entry.getOutputLocation();
					if (path == null) {
						path = project.getOutputLocation();
					}

					if (projectEclipseRelativePath.isPrefixOf(path)) {
						classDir = new File(projectFilePath,
								path.removeFirstSegments(projectEclipseRelativePath.segmentCount()).toOSString());
						break;
					}

					if (Debug.VALIDATOR.isActive()) {
						Debug.VALIDATOR.log(
								"EclipseUtil.getClassFile(ValidatorResource) found output location for resource ",
								resource.getClassName(), ": ", path);
					}
				}
			}
		} catch (JavaModelException e) {
			Debug.VALIDATOR.log(e);
		}

		return classDir;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<File> getClassPathDirsAndJARs() {
		Set<File> auxDirs = new HashSet<File>();
		File rootFile = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject iProject : projects) {
			try {
				boolean javaNatureExists = iProject.isOpen()
						&& iProject.isNatureEnabled("org.eclipse.jdt.core.javanature");
				if (javaNatureExists) {
					IPath projectEclipseRelativePath = iProject.getFullPath();
					String projectFilePath = iProject.getLocation().toOSString();
					IJavaProject project = JavaCore.create(iProject);
					try {
						IClasspathEntry[] classPaths = project.getResolvedClasspath(true);
						for (IClasspathEntry entry : classPaths) {
							int kind = entry.getEntryKind();
							switch (kind) {
							case IClasspathEntry.CPE_CONTAINER:
								break;
							case IClasspathEntry.CPE_LIBRARY:
								IPath path = entry.getPath();
								File file = path.toFile();
								if (projectEclipseRelativePath.isPrefixOf(path)) {
									file = new File(projectFilePath,
											path.removeFirstSegments(projectEclipseRelativePath.segmentCount())
													.toOSString());
								}
								String fileString = file.toString();
								if (fileString.startsWith("\\")) {
									file = new File(rootFile, fileString.substring(1));
								}
								auxDirs.add(file);
								break;
							case IClasspathEntry.CPE_PROJECT:
								break;
							case IClasspathEntry.CPE_SOURCE:
								break;
							case IClasspathEntry.CPE_VARIABLE:
								break;
							default:
								break;
							}
						}
					} catch (JavaModelException e) {
						Debug.VALIDATOR.log(e);
					}

				}
			} catch (CoreException e1) {
				Debug.VALIDATOR.log(e1, "Could not examine projects in workspace.");
			}
		}

		return auxDirs;
	}

	/**
	 * {@inheritDoc}
	 */
	public void reloadDebugConfig() {
		globalDebugConfig.ensureLatestLoaded();
		debugConfig.ensureLatestLoaded();

		log("Debugging is on? ", isDebugging());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDebugging() {
		Boolean globalDebugging = debugConfig.getLoadedObject();
		if (globalDebugging == null) {
			globalDebugging = Boolean.FALSE;
		}

		Boolean localDebugging = debugConfig.getLoadedObject();
		if (localDebugging == null) {
			localDebugging = Boolean.FALSE;
		}

		return globalDebugging.booleanValue() || localDebugging.booleanValue();
	}

	/**
	 * {@inheritDoc}
	 */
	public void log(Object... strings) {
		String message = createMessage(strings);

		if (Activator.getDefault() != null) {
			Activator.getDefault().getLog().log(new Status(IStatus.INFO, Activator.PLUGIN_ID, message));
		}

		if (message != null) {
			System.out.println(message);
		} else {
			System.out.println();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void log(Throwable e, Object... strings) {
		String message = createMessage(strings);

		if (Activator.getDefault() != null) {
			String errorMessage = message;
			if (errorMessage == null || errorMessage.isEmpty()) {
				errorMessage = e.getMessage();
			}
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, errorMessage, e));
		}

		if (message != null) {
			System.out.println(message);
		} else {
			System.out.println();
		}
		e.printStackTrace();
	}

	/**
	 * Returns the concatenation of all Strings in the given list.
	 *
	 * @param strings
	 *            The objects to call {@link #toString()} on and concatenate
	 *            together to make the message. If empty, null will be returned.
	 *            If any value is null, "null" will be concatenated in its
	 *            place.
	 *
	 * @return The concatenated message. Will be null if no strings were given.
	 *         Will be empty if all objects in the list resulted in empty
	 *         strings before concatenation.
	 */
	private String createMessage(Object... strings) {
		String message = null;
		if (strings.length == 1) {
			message = convertToString(strings[0]);
		} else if (strings.length > 0) {
			StringBuffer buf = new StringBuffer();
			for (Object o : strings) {
				buf.append(convertToString(o));
			}
			message = buf.toString();
		}

		return message;
	}

	/**
	 * Converts the given object to a string using {@link #toString()}.
	 *
	 * @param o
	 *            The object to convert. If null, "null" will be returned.
	 *
	 * @return The string form of the object. Will not be null. Will be empty if
	 *         <code>o.toString()</code> results in an empty string.
	 */
	private String convertToString(Object o) {
		String s;
		if (o != null) {
			s = o.toString();
		} else {
			s = "null";
		}

		return s;
	}

	/**
	 * {@inheritDoc}
	 */
	public void checkCanceled(IProgressMonitor monitor) throws OperationCanceledException {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException("The current action has been cancelled.");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public URL getPluginsURL(String relativeFile) {
		return getDropinsURL("wizard/eclipse/plugins/" + relativeFile);
	}

	/**
	 * {@inheritDoc}
	 */
	public URL getUserConfigURL(String relativeFile) {
		return getDropinsURL("wizard/userconfig/" + relativeFile);
	}

	/**
	 * {@inheritDoc}
	 */
	public URL getDropinsURL(String relativeFile) {
		String sdpDirName = System.getProperty("osgi.install.area");
		sdpDirName = sdpDirName.substring(sdpDirName.indexOf(":") + 2, sdpDirName.length()).replace('/',
				File.separatorChar);
		File dropinsFolder = new File(sdpDirName, "dropins");

		File file = new File(dropinsFolder, relativeFile.replace('/', File.separatorChar));
		URL url;
		try {
			url = file.toURI().toURL();
		} catch (MalformedURLException e) {
			log(e);
			throw new IllegalArgumentException("Relative path \"" + relativeFile + "\" is not valid.", e);
		}

		return url;
	}

	/**
	 * {@inheritDoc}
	 */
	public URL getWorkspaceRootURL(String relativeFile) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot rootResource = workspace.getRoot();
		URI workspaceURI = rootResource.getRawLocationURI();

		URL localConfigURL;
		try {
			URL workspaceURL = workspaceURI.toURL();
			localConfigURL = new URL(workspaceURL.toString() + "/" + relativeFile);
		} catch (MalformedURLException e) {
			log(e);
			throw new IllegalArgumentException("Relative path \"" + relativeFile + "\" is not valid.", e);
		}

		return localConfigURL;
	}

	/**
	 * DebugConfigFile is a file called <code>debugging.txt</code> that
	 * indicates whether debugging should be on or not. If the file does not
	 * exist or the first line contains anything other than "true"
	 * (case-insensitive), debugging will be disabled. Otherwise, debugging will
	 * be enabled and messages will be logged in the Eclipse error log.
	 */
	private class DebugConfigFile extends WatchedFile<Boolean> {

		/**
		 * Constructor for DebugConfigFile.
		 */
		private DebugConfigFile(URL url) {
			super(url);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void syncWithSystem() {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(openInputStream()));
				String line = reader.readLine();
				while (line != null) {
					StringTokenizer tokenizer = new StringTokenizer(line, " \t=");
					String name = tokenizer.nextToken();
					String value = tokenizer.nextToken();
					boolean booleanValue = Boolean.parseBoolean(value);
					Debug debug = Debug.valueOf(name);
					debug.setActive(booleanValue);
					line = reader.readLine();
				}
				setLoadedObject(Boolean.TRUE);
			} catch (FileNotFoundException e) {
				setLoadedObject(Boolean.FALSE);
			} catch (IOException e) {
				log(e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						log(e);
					}
				}
			}
		}
	}

}
