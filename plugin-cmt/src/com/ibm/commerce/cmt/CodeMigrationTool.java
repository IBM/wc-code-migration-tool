package com.ibm.commerce.cmt;

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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import com.ibm.commerce.cmt.plan.IDGenerator;
import com.ibm.commerce.cmt.plan.Plan;
import com.ibm.commerce.dependency.load.APIFileManager;
import com.ibm.commerce.dependency.load.LoadingManager;
import com.ibm.commerce.dependency.load.Priority;
import com.ibm.commerce.dependency.model.EclipseWorkspace;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemFactory;
import com.ibm.commerce.dependency.model.JavaItemIndex;
import com.ibm.commerce.dependency.model.JavaItemType;
import com.ibm.commerce.dependency.model.JavaItemUtil2;
import com.ibm.commerce.dependency.model.ProjectLoader;
import com.ibm.commerce.dependency.model.Workspace;
import com.ibm.commerce.dependency.model.eclipse.IJavaProjectWrapper;
import com.ibm.commerce.dependency.model.eclipse.IJavaProjectWrapperExternal;
import com.ibm.commerce.dependency.model.eclipse.IJavaProjectWrapperJAR;
import com.ibm.commerce.dependency.model.eclipse.IWorkspaceWrapper;
import com.ibm.commerce.dependency.model.eclipse.IWorkspaceWrapperExternal;
import com.ibm.commerce.dependency.task.Task;
import com.ibm.commerce.dependency.task.TaskContext;
import com.ibm.commerce.dependency.task.TaskList;

/**
 * This class is the main starting point for the Code Migration Tool.
 * 
 * @author Trent Hoeppner
 */
public class CodeMigrationTool implements IApplication {

	private static final String CMT_VERSION = "1.2";

	/**
	 * The default name of the ZIP file that contains the version 8 API
	 * information.
	 */
	public static final String API_ZIP_FILENAME = "api-v8.zip";

	/**
	 * Variable for the input JavaItemIndex to create a delta from.
	 */
	private static final String BASE_JAVA_ITEM_INDEX = "BaseJavaItemIndex";

	/**
	 * Variable for the output JavaItemIndex that is the delta of a base index.
	 */
	private static final String NEXT_INCREMENTAL_INDEX = "NextIncrementalIndex";

	/**
	 * The mode that this tool is run under from the command line, default value
	 * is "plan". Valid values are "plan", "migrate", "extract", and "dumpapi".
	 */
	private String mode = "plan";

	/**
	 * The version to save the API as in "extract" mode.
	 */
	private String version;

	private String planFilename = "cmtplan.xml";

	private String logFilename = "cmt.log";

	private List<String> patternFilenames = new ArrayList<>();

	private List<String> filteredProjectNames = new ArrayList<>();

	private BufferedWriter writer;

	private JavaItemIndex index;

	private Workspace workspace;

	private JavaItemFactory factory;

	/**
	 * This is the task list which is used to proactively create the next delta
	 * JavaItemIndex. This is used by the UI plugin for incremental builds. By
	 * proactively creating the next delta JavaItemIndex (which takes 3-5
	 * seconds), we avoid delays for the user who wants to update files
	 * frequently and see the results.
	 */
	private TaskList nextTaskList;

	/**
	 * This is the context which contains the output delta JavaItemIndex of the
	 * {@link #nextTaskList}
	 */
	private TaskContext nextTaskContext;

	/**
	 * Constructor for this with a default log filename.
	 */
	public CodeMigrationTool() {
		// do nothing
	}

	/**
	 * Constructor for this with a log filename.
	 * 
	 * @param logFilename
	 *            The name of the file to log to. This value cannot be null or
	 *            empty.
	 */
	public CodeMigrationTool(String logFilename) {
		this.logFilename = logFilename;
	}

	/**
	 * Runs the application in command line mode.
	 * 
	 * @param applicationContext
	 *            The context which contains the command line arguments. This
	 *            value cannot be null.
	 */
	@Override
	public Object start(IApplicationContext applicationContext) throws Exception {
		Map<?, ?> launchArgs = applicationContext.getArguments();
		if (launchArgs == null) {
			printUsage();
			return EXIT_OK;
		}

		String[] args = (String[]) launchArgs.get("application.args");
		if (args == null) {
			printUsage();
			return EXIT_OK;
		}

		Context context = new Context(new IDGenerator(1));

		File logFile = new File(logFilename);
		writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(logFile, true));
			context.set(Context.Prop.LOG_WRITER, writer);

			extractArguments(args);

			boolean ok = validateArguments();
			if (!ok) {
				printUsage();
				return EXIT_OK;
			}

			FileFilter dataJARFilter = createJARFilter();

			IWorkspace w = ResourcesPlugin.getWorkspace();

			JavaItemFactory factory = initialize();

			File serializedFile = new File("api-v" + version + ".zip");
			File workspaceDir = w.getRoot().getLocation().toFile();
			// List<JavaItem> projectList = null;
			if (mode.equals("extract")) {
				Set<File> thirdPartyDirs = new HashSet<>();
				thirdPartyDirs.add(new File(workspaceDir, "WC/lib"));

				if (version.equals("7")) {
					thirdPartyDirs.add(new File(workspaceDir, "../../SDP/runtimes/base_v7"));
				} else if (version.equals("8")) {
					thirdPartyDirs.add(new File(workspaceDir, "../../SDP/runtimes/base_v85_stub"));
				} else if (version.equals("9")) {
					thirdPartyDirs.add(new File(workspaceDir, "../../IBM/WebSphere/AppServer/lib"));
				}

				LoadingManager loadingManager = new LoadingManager();
				factory = loadingManager.loadProjects(factory, workspaceDir, thirdPartyDirs, dataJARFilter, true);
				this.factory = factory;
				index = factory.getIndex();
				// extractFromWorkspace(workspaceDir, factory, true, true,
				// dataJARFilter);

				APIFileManager apiFileManager = new APIFileManager();
				apiFileManager.writeAPI(index, serializedFile);
			} else {
				serializedFile = new File("api-v" + "8" + ".zip");
				if (!serializedFile.exists()) {
					log("File does not exist: " + serializedFile.getAbsolutePath());
					return EXIT_OK;
				}

				APIFileManager apiFileManager = new APIFileManager();
				index = apiFileManager.loadAPI(serializedFile);
				factory = new JavaItemFactory(index);
				this.factory = factory;

				// create plan
				if (mode.equals("dumpapi")) {
					dumpAPI();
				} else if (mode.equals("plan")) {
					JavaItemIndex workspaceIndex = new JavaItemIndex("workspace", index);
					JavaItemFactory workspaceFactory = new JavaItemFactory(workspaceIndex);

					LoadingManager loadingManager = new LoadingManager();
					factory = loadingManager.loadProjects(workspaceFactory, workspaceDir, Collections.emptySet(),
							dataJARFilter, false);
					this.factory = factory;
					index = factory.getIndex();

					workspace = new EclipseWorkspace(new IWorkspaceWrapperExternal(workspaceDir, false, false, null),
							new ProjectLoader(factory), factory, true);

					context.set(Context.Prop.JAVA_ITEM_INDEX, index);
					context.set(Context.Prop.DEPENDENCY_WORKSPACE, workspace);

					Configuration configuration = createConfiguration(workspace, patternFilenames, null);
					createPlan(configuration, context, true);
				} else if (mode.equals("migrate")) {
					// parseWorkspace(workspaceDir, factory, dataJARFilter);

					JavaItemIndex workspaceIndex = new JavaItemIndex("workspace", index);
					JavaItemFactory workspaceFactory = new JavaItemFactory(workspaceIndex);

					LoadingManager loadingManager = new LoadingManager();
					factory = loadingManager.loadProjects(workspaceFactory, workspaceDir, Collections.emptySet(),
							dataJARFilter, false);
					this.factory = factory;
					index = factory.getIndex();

					workspace = new EclipseWorkspace(new IWorkspaceWrapperExternal(workspaceDir, false, false, null),
							new ProjectLoader(factory), factory, true);

					context.set(Context.Prop.JAVA_ITEM_INDEX, index);
					context.set(Context.Prop.DEPENDENCY_WORKSPACE, workspace);

					Configuration configuration = createConfiguration(workspace, patternFilenames, null);
					// File planFile = new File(planFilename);
					Plan plan;
					// if (planFile.exists()) {
					// log("Loading plan");
					// // TODO
					// log("NEED TO IMPLEMENT LOADING OF AN EXISTING PLAN
					// FILE.");
					// return EXIT_OK;
					// } else {
					// log("Plan file does not exist, creating plan");
					plan = createPlan(configuration, context, true);
					// }

					log("Executing plan");
					plan.execute(configuration, context);
				} else {
					// something wrong, this shouldn't happen
					printUsage();
				}

				log("Done");
			}
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// swallow to allow main exception to escape
				}
			}
		}

		return EXIT_OK;
	}

	private void dumpAPI() throws IOException {
		List<JavaItem> projects = new ArrayList<>();
		for (JavaItem item : index.getItems()) {
			if (item.getType() == JavaItemType.PROJECT) {
				projects.add(item);
			}
		}

		File dumpdir = new File("dump");
		dumpdir.mkdirs();
		for (JavaItem project : projects) {
			BufferedWriter writer = null;
			try {
				String projectName = project.getName();
				projectName = projectName.replace('\\', '_');
				projectName = projectName.replace('/', '_');
				writer = new BufferedWriter(new FileWriter(new File(dumpdir, "api-" + projectName + ".txt")));
				writer.write(project.getName());
				writer.newLine();
				for (JavaItem packageItem : project.getChildren()) {
					writer.write("    ");
					writer.write(packageItem.getName());
					writer.newLine();
					for (JavaItem classItem : packageItem.getChildren()) {
						writer.write("        ");
						writer.write(classItem.getName());
						writer.newLine();
						for (JavaItem method : classItem.getChildren()) {
							writer.write("            ");
							writer.write(method.getName());
							writer.newLine();
						}
					}
				}
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						// swallow to allow main exception to escape
					}
				}
			}
		}

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(dumpdir, "api-all.txt")));
			for (JavaItem item : index.getItems()) {
				writer.write(item.toString());
				writer.newLine();
			}
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// swallow to allow the main exception to escape
				}
			}
		}
	}

	private FileFilter createJARFilter() {
		FileFilter dataJARFilter;
		if (filteredProjectNames.isEmpty()) {
			dataJARFilter = null;
		} else {
			dataJARFilter = new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					String name = pathname.getName();
					int lastDot = name.lastIndexOf('.');
					if (lastDot < 0) {
						lastDot = name.length();
					}

					String subString = name.substring(0, lastDot);
					if (filteredProjectNames.contains(subString)) {
						return true;
					}

					return false;
				}

			};
		}
		return dataJARFilter;
	}

	/**
	 * Creates a plan based on the given pattern files, and uses the given
	 * factory to create the objects. This is called by the QCheck plugin.
	 * 
	 * @param patternFilenames
	 *            The names of XML files that contain patterns. This value
	 *            cannot be null, but may be empty.
	 * @param javaFiles
	 *            The files to parse and generate a plan for. If null, all files
	 *            found will be analyzed.
	 * 
	 * @return The plan which describes what errors and in some cases, how to
	 *         fix them. This value will not be null.
	 * 
	 * @throws Exception
	 *             If an exception occurs when opening the log file, parsing the
	 *             patterns or input files, or creating or writing the plan
	 *             file.
	 */
	public Plan createPlan(List<String> patternFilenames, Set<File> javaFiles) throws Exception {
		if (nextTaskList == null) {
			startNextIncrementalIndex();
		}

		nextTaskList.waitForCompletion();
		nextTaskList = null;

		JavaItemIndex nextIncrementalIndex = nextTaskContext.get(NEXT_INCREMENTAL_INDEX);
		Context context = new Context(new IDGenerator(1));

		nextIncrementalIndex.mergeToBase();
		nextIncrementalIndex = nextIncrementalIndex.getBase();

		Plan plan = null;

		File logFile = new File(logFilename);
		writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(logFile, true));
			context.set(Context.Prop.LOG_WRITER, writer);

			IWorkspace w = ResourcesPlugin.getWorkspace();
			File workspaceDir = w.getRoot().getLocation().toFile();

			// JavaItemIndex incrementalIndex = new JavaItemIndex("incremental",
			// index);
			JavaItemFactory incrementalFactory = new JavaItemFactory(nextIncrementalIndex);

			LoadingManager loadingManager = new LoadingManager();
			loadingManager.loadFiles(incrementalFactory, workspaceDir, javaFiles);
			this.factory = incrementalFactory;
			index = incrementalFactory.getIndex();

			workspace = new EclipseWorkspace(new IWorkspaceWrapperExternal(workspaceDir, false, false, null),
					new ProjectLoader(factory), factory, true);

			context.set(Context.Prop.JAVA_ITEM_INDEX, index);
			context.set(Context.Prop.DEPENDENCY_WORKSPACE, workspace);

			Configuration configuration = createConfiguration(workspace, patternFilenames, javaFiles);

			plan = createPlan(configuration, context, false);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// swallow to allow main exception to escape
				}
			}
		}

		startNextIncrementalIndex();

		return plan;
	}

	/**
	 * Starts creating the next delta JavaItemIndex for an incremental build in
	 * another thread (this can take 3-5 seconds), to avoid delays in the UI.
	 * This method does not wait for the index to finish being built, but
	 * instead sets {@link #nextTaskList} so that
	 * {@link TaskList#waitForCompletion()} can be called, and sets
	 * {@link #nextTaskContext} so that the resulting delta JavaItemIndex can be
	 * retrieved from the result.
	 */
	private void startNextIncrementalIndex() {
		nextTaskList = new TaskList();
		nextTaskContext = new TaskContext(nextTaskList);
		nextTaskContext.put(BASE_JAVA_ITEM_INDEX, index);

		CreateDeltaIndexTask task = new CreateDeltaIndexTask("CreateDeltaIndex", nextTaskContext);

		nextTaskList.addTask(task, Priority.TOP_LEVEL);
		nextTaskList.start();
	}

	/**
	 * Creates the {@link Workspace} that is used to load projects and other
	 * information. This method is intended for the end user to perform
	 * analysis, rather than create a binary package which represents a
	 * toolkit's API.
	 * 
	 * @param workspaceDir
	 *            The directory of the workspace root. This value cannot be
	 *            null.
	 * @param factory
	 *            The factory which is used to generate new JavaItems and add
	 *            them to a JavaItemIndex. This value cannot be null.
	 * @param jarFilter
	 *            The filter which removes JARs from the search list in the
	 *            workspace. If this value is null, no filtering will be
	 *            performed, and additional JARs in the WC folder will be
	 *            ignored - only open projects in the workspace will be
	 *            analyzed.
	 * 
	 * @throws IOException
	 *             If an error occurred creating the workspace.
	 */
	private void parseWorkspace(File workspaceDir, JavaItemFactory factory, FileFilter jarFilter) throws IOException {
		log("Loading workspace information");
		workspace = extractFromWorkspace(workspaceDir, factory, jarFilter != null, false, jarFilter);
	}

	/**
	 * Initializes this with a {@link JavaItemIndex} and a
	 * {@link JavaItemFactory}. This is necessary before calling
	 * {@link #loadBaseIndex(File)}.
	 * 
	 * @return The factory that can create {@link JavaItem JavaItems}. This
	 *         value will not be null.
	 */
	public JavaItemFactory initialize() {
		index = new JavaItemIndex("v8");
		index.setIDGenerator(new IDGenerator(0));
		factory = new JavaItemFactory(index);
		return factory;
	}

	public void initialize(JavaItemIndex index) {
		this.index = index;
		factory = new JavaItemFactory(index);
	}

	/**
	 * Returns the factory that can be used to create {@link JavaItem JavaItems}
	 * and add them to the {@link JavaItemIndex index}.
	 * 
	 * @return The factory used to create JavaItems. This value will be null if
	 *         {@link #initialize()} has not been called yet.
	 */
	public JavaItemFactory getFactory() {
		return factory;
	}

	/**
	 * Converts objects in the workspace to {@link JavaItem JavaItems}. This
	 * includes projects, packages, and classes and methods, and their
	 * dependencies and attributes.
	 * 
	 * @param workspaceDir
	 *            The root directory of the workspace. This value cannot be
	 *            null.
	 * @param factory
	 *            The factory used to create each JavaItem. This value cannot be
	 *            null.
	 * @param useJARsInWC
	 *            If true, will look in JAR files in the WC project of the
	 *            workspace for projects, classes and methods.
	 * @param searchBinaryJARs
	 *            If true, will use a {@link ClassLoader} to find classes and
	 *            methods in JARs that do not have source code.
	 * @param jarFilter
	 *            A filter to avoid parsing certain JAR filenames. If null, no
	 *            filtering will be done.
	 * 
	 * @return The workspace object that was used to do the loading. This
	 *         normally should not be used after calling this method because the
	 *         extracted JavaItems already exist in the {@link JavaItemIndex}.
	 * 
	 * @throws IOException
	 *             If an error occurs while loading the data from disk.
	 */
	private Workspace extractFromWorkspace(File workspaceDir, JavaItemFactory factory, boolean useJARsInWC,
			boolean searchBinaryJARs, FileFilter jarFilter) throws IOException {
		List<JavaItem> projectList;
		Workspace workspace = new EclipseWorkspace(
				new IWorkspaceWrapperExternal(workspaceDir, useJARsInWC, searchBinaryJARs, jarFilter),
				new ProjectLoader(factory), factory, !useJARsInWC);

		projectList = workspace.getProjects();

		// if the jarFilter allowed them, we have to remove them and their
		// children and dependencies from the index
		EclipseWorkspace w = (EclipseWorkspace) workspace;
		IWorkspaceWrapperExternal we = (IWorkspaceWrapperExternal) w.getWrapper();

		if (jarFilter != null) {
			// this code will remove projects unnecessarily when the jarFilter
			// is null
			List<JavaItem> projectsToRemove = new ArrayList<>();
			for (JavaItem project : projectList) {
				IJavaProjectWrapperExternal found = null;
				for (IJavaProjectWrapper jp : we.getProjects()) {
					IJavaProjectWrapperExternal jpe = null;
					if (jp instanceof IJavaProjectWrapperExternal) {
						jpe = (IJavaProjectWrapperExternal) jp;
					} else if (jp instanceof IJavaProjectWrapperJAR) {
						jpe = ((IJavaProjectWrapperJAR) jp).getUnpackedWrapper();
					}

					if (jpe.getDir().getName().equals(project.getName())) {
						found = jpe;
						break;
					}
				}

				if (found != null) {
					// the project and all dependencies should be removed, do it
					// in another loop
					projectsToRemove.add(project);
				}
			}

			// need to remove the projects from the list and from the
			// JavaItemIndex
			// TODO better not to add than to remove, so we don't mess with the
			// IDs
			JavaItemIndex index = w.getJavaItemIndex();
			removeFromIndex(index, projectsToRemove);
		}

		DebugUtil.verifyExists(index, "after removing excluded from index");

		for (JavaItem project : projectList) {
			workspace.findPackages(project);
			for (Object packageObj : project.getChildren()) {
				JavaItem javaPackage = (JavaItem) packageObj;
				DebugUtil.stopAtPackage(javaPackage, "before finding classes");
				log("Project " + project.getName() + ": finding classes in package " + javaPackage.getName());
				workspace.findClasses(javaPackage);
			}
		}

		DebugUtil.verifyExists(index, "after finding classes");

		for (JavaItem project : projectList) {
			for (Object packageObj : project.getChildren()) {
				JavaItem javaPackage = (JavaItem) packageObj;
				for (Object classObj : javaPackage.getChildren()) {
					JavaItem javaClass = (JavaItem) classObj;
					log("Project " + project.getName() + ": finding dependencies for class " + javaClass.getName());
					workspace.findClassDependencies(javaClass);
				}
			}
		}

		DebugUtil.verifyExists(index, "after finding class dependencies");

		for (JavaItem project : projectList) {
			for (Object packageObj : project.getChildren()) {
				JavaItem javaPackage = (JavaItem) packageObj;
				for (Object classObj : javaPackage.getChildren()) {
					JavaItem javaClass = (JavaItem) classObj;
					workspace.findMethods(javaClass);
				}
			}
		}

		DebugUtil.verifyExists(index, "after finding methods");

		if (!useJARsInWC || jarFilter != null) {
			for (JavaItem project : projectList) {
				for (Object packageObj : project.getChildren()) {
					JavaItem javaPackage = (JavaItem) packageObj;
					for (Object classObj : javaPackage.getChildren()) {
						JavaItem javaClass = (JavaItem) classObj;
						workspace.findMethodDependencies(javaClass);
					}
				}
			}
		}

		DebugUtil.verifyExists(index, "after finding method dependencies");

		return workspace;
	}

	/**
	 * Removes the given items from the given index, along with all children and
	 * dependencies.
	 * 
	 * @param index
	 *            The index to remove items from. This value cannot be null.
	 * @param itemsToRemove
	 *            The items to remove. This value cannot be null but may be
	 *            empty.
	 */
	private void removeFromIndex(JavaItemIndex index, List<JavaItem> itemsToRemove) {
		for (JavaItem item : itemsToRemove) {
			index.removeItem(item);
			removeFromIndex(index, item.getChildren());

			for (JavaItem incoming : item.getIncoming()) {
				incoming.getDependenciesIDs().remove(item.getID());
			}

			for (JavaItem dependency : item.getDependencies()) {
				dependency.getIncomingIDs().remove(item.getID());
			}
		}

		index.consolidateIDs();
	}

	/**
	 * Loads the configuration including the patterns in the files with the
	 * given names.
	 * 
	 * @param w
	 *            The workspace to use to get the projects, which are used to
	 *            find the source code to analyze. This value cannot be null.
	 * @param patternFilenames
	 *            The names of files that contain patterns. This value cannot be
	 *            null but may be empty.
	 * @param javaFiles
	 *            The files to parse and generate a plan for. If null, all files
	 *            found will be analyzed.
	 * 
	 * @return The configuration information. This value will not be null.
	 * 
	 * @throws IOException
	 *             If an error occurs while reading any of the files.
	 */
	private Configuration createConfiguration(Workspace w, List<String> patternFilenames, Set<File> javaFiles)
			throws IOException {
		List<File> patternFiles = new ArrayList<>();
		for (String patternFilename : patternFilenames) {
			File patternFile = new File(patternFilename);
			patternFiles.add(patternFile);
		}

		IWorkspaceWrapper wrapper = ((EclipseWorkspace) w).getWrapper();

		List<File> sourceDirs = new ArrayList<>();
		// IWorkspace workspace = ResourcesPlugin.getWorkspace();
		for (IJavaProjectWrapper project : wrapper.getProjects()) {
			IJavaProjectWrapperExternal extProject;
			if (project instanceof IJavaProjectWrapperExternal) {
				extProject = (IJavaProjectWrapperExternal) project;
			} else {
				IJavaProjectWrapperJAR jarWrapper = (IJavaProjectWrapperJAR) project;
				extProject = jarWrapper.getUnpackedWrapper();
			}

			File projectDir = extProject.getDir();
			File[] children = projectDir.listFiles();
			if (children != null) {
				for (File childDir : children) {
					if (childDir.isDirectory() && !sourceDirs.contains(childDir)) {
						sourceDirs.add(childDir);
					}
				}
			}
		}

		Configuration configuration = new Configuration(patternFiles, sourceDirs, javaFiles);
		configuration.load();
		return configuration;
	}

	/**
	 * Creates an execution plan by analyzing the source files in the context,
	 * using the patterns in the configuration.
	 * 
	 * @param configuration
	 *            The configuration which defines the patterns to use in the
	 *            analysis. This value cannot be null.
	 * @param context
	 *            The context which defines the workspace with source files and
	 *            other variables. This value cannot be null.
	 * @param writeToFile
	 *            True to write the plan file to disk, false otherwise.
	 * 
	 * @return The plan which can be executed to make changes. This value will
	 *         not be null.
	 * 
	 * @throws IOException
	 *             If there was an error reading the source files or writing the
	 *             plan to disk.
	 */
	private Plan createPlan(Configuration configuration, Context context, boolean writeToFile) throws IOException {
		Plan plan = new Plan();
		List<File> allFilesFound = configuration.getFiles();
		// TODO load seed number for generator from other files found
		JavaItemUtil2 util = new JavaItemUtil2();
		util.initialize(factory);
		for (File source : allFilesFound) {
			context.reset();
			context.set(Context.Prop.LOG_WRITER, writer);
			context.set(Context.Prop.JAVA_ITEM_INDEX, index);
			context.set(Context.Prop.JAVA_ITEM_UTIL, util);
			context.set(Context.Prop.DEPENDENCY_WORKSPACE, workspace);

			int beforeIssues = plan.getIssues().size();
			long beforeTime = System.currentTimeMillis();
			context.set(Context.Prop.FILE, source);
			for (Pattern pattern : configuration.getPatterns()) {
				try {
					pattern.findInCurrentFileForPlan(context, plan);
				} catch (RuntimeException e) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					XMLUtil.writeXML(pattern, out);
					out.flush();
					out.close();
					String patternText = out.toString();
					log("Error occurred while analyzing file " + source.getAbsolutePath() + " with pattern "
							+ patternText);
					log(e);
				}
			}

			int afterIssues = plan.getIssues().size();
			long afterTime = System.currentTimeMillis();

			log("Found " + (afterIssues - beforeIssues) + " issues (" + (afterTime - beforeTime) + " ms) in "
					+ source.getAbsolutePath());
		}

		log("Found a total of " + plan.getIssues().size() + " issues");

		if (writeToFile) {
			// write plan to file
			log("Converting plan to XML and writing to " + planFilename);
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(planFilename)));
			XMLUtil.writeXML(plan, out);
		}

		return plan;
	}

	/**
	 * Validates the values in the command line arguments that were already
	 * parsed. In particular it checks that the "-mode" is a valid value and
	 * that there is at least one "-patternfile" argument.
	 * 
	 * @return True if all arguments are valid, false otherwise.
	 * 
	 * @throws IOException
	 *             If an error occurs while writing a message to the log file.
	 */
	private boolean validateArguments() throws IOException {
		if (mode == null || !mode.equals("plan") && !mode.equals("migrate") && !mode.equals("extract")
				&& !mode.equals("dumpapi")) {
			log("-mode is wrong: " + mode);
			return false;
		}

		if (mode.equals("extract") && (version == null || version.isEmpty())) {
			log("-version is missing, required for extract mode");
			return false;
		}

		if (patternFilenames.isEmpty()) {
			log("no -patternfile arguments specified");
			return false;
		}

		return true;
	}

	/**
	 * Logs the given exception to a writer which is linked to the log file.
	 * 
	 * @param t
	 *            The exception to log. This value cannot be null.
	 * 
	 * @throws IOException
	 *             If an error occurs while writing to the log file.
	 */
	private void log(Throwable t) throws IOException {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(new BufferedWriter(stringWriter));
		t.printStackTrace(printWriter);
		printWriter.flush();
		String error = stringWriter.toString();
		log(error);
	}

	/**
	 * Logs the given string to a writer which is linked to the log file.
	 * 
	 * @param string
	 *            The message to write. If null, the message will be "null".
	 * 
	 * @throws IOException
	 *             If an error occurs while writing to the log file.
	 */
	private void log(String string) throws IOException {
		System.out.println(string);
		writer.append(string);
		writer.append(System.getProperty("line.separator"));
		writer.flush();
	}

	/**
	 * Extracts the command-line arguments from the command line.
	 * 
	 * @param args
	 *            The arguments to parse. This value cannot be null.
	 */
	private void extractArguments(String[] args) {
		Iterator<String> argIterator = Arrays.asList(args).iterator();
		while (argIterator.hasNext()) {
			String arg = argIterator.next();
			if (arg.equals("-mode")) {
				mode = getNext(argIterator);
			} else if (arg.equals("-version")) {
				version = getNext(argIterator);
			} else if (arg.equals("-planfile")) {
				planFilename = getNext(argIterator);
			} else if (arg.equals("-logfile")) {
				logFilename = getNext(argIterator);
			} else if (arg.equals("-patternfile")) {
				String patternFilename = getNext(argIterator);
				patternFilenames.add(patternFilename);
			} else if (arg.equals("-project")) {
				String patternFilename = getNext(argIterator);
				filteredProjectNames.add(patternFilename);
			}
		}
	}

	/**
	 * Gets an argument after a command-line argument identifier. This avoids an
	 * exception if there is no next argument.
	 * 
	 * @param argIterator
	 *            The iterator that contains the command line arguments. This
	 *            value cannot be null.
	 * 
	 * @return Returns the next argument, or null if there is no argument.
	 */
	private String getNext(Iterator<String> argIterator) {
		String value = null;
		if (argIterator.hasNext()) {
			value = argIterator.next();
		}

		return value;
	}

	/**
	 * Prints the instructions to use this tool on the command line.
	 */
	private void printUsage() {
		System.out.println("CMT Version: " + CMT_VERSION);
		System.out.println("Usage:");
		// System.out.println("eclipsec [-mode <execution mode>] [-planfile
		// <plan filename>]");
		// System.out.println(" [-journalfile <journal filename>] [-logfile <log
		// filename>]");
		// System.out.println(" -patternfile <pattern filename> [-patternfile
		// <pattern filename> [ ... ]]");
		// System.out.println(" [-backupdir <backup directory>]");
		System.out.println("cmt.bat -patternfile <pattern filename> [-patternfile <pattern filename> [ ... ]]");
		System.out.println("   [-logfile <log filename>]");

		// System.out.println("-mode optional Valid values are \"plan\" or
		// \"migrate\".");
		// System.out.println("-planfile optional The file where the plan will
		// be written to.");
		// System.out.println("-journalfile optional The file which records
		// which issues have been fixed.");
		System.out.println("-logfile      optional   The file which records progress of the tool.");
		System.out.println("-patternfile  optional   The file that contains the search and action patterns.");
		System.out.println("                         Multiple -patternfile options may be specified.");
		// System.out.println("-backupdir required The directory to backup files
		// before migrating, only");
		// System.out.println(" required if -mode is set to \"migrate\".");
	}

	/**
	 * Stops this application. Does nothing, since this application exits in the
	 * {@link #start(IApplicationContext)} method.
	 */
	@Override
	public void stop() {
	}

	/**
	 * This is a task to create a delta JavaItemIndex, so that it can be run
	 * outside the UI thread.
	 */
	public static class CreateDeltaIndexTask extends Task<TaskContext> {

		/**
		 * The required inputs for this task.
		 */
		private static final Set<String> INPUTS = new HashSet<>(Arrays.asList(BASE_JAVA_ITEM_INDEX));

		/**
		 * The expected outputs for this task.
		 */
		private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(NEXT_INCREMENTAL_INDEX));

		/**
		 * Constructor for this.
		 * 
		 * @param name
		 *            The name of the task. This value cannot be null or empty.
		 * @param context
		 *            THe context in which this task is run. This value cannot
		 *            be null.
		 */
		public CreateDeltaIndexTask(String name, TaskContext context) {
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
		public void execute(TaskContext context) throws Exception {
			JavaItemIndex baseIndex = context.get(BASE_JAVA_ITEM_INDEX);
			JavaItemIndex incrementalIndex = new JavaItemIndex("incremental", baseIndex);
			context.put(NEXT_INCREMENTAL_INDEX, incrementalIndex);
		}

	}
}
