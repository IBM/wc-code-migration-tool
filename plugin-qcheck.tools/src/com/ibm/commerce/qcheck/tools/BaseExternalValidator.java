package com.ibm.commerce.qcheck.tools;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.Util;
import com.ibm.commerce.qcheck.core.ValidationException;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;

/**
 * BaseExternalValidator has some common methods that are useful to external
 * validators.
 * 
 * @author Trent Hoeppner
 */
public abstract class BaseExternalValidator implements Validator {

	private static final List<ModelEnum> REQUIRED_MODELS = Arrays.asList(ModelEnum.EXTERNAL);

	/**
	 * The max count of files to be validated each time.
	 */
	private static final int MAX_VALIDATED_PER_RUN = 300;

	/**
	 * The platform-specific line separator, to be used in writing output files.
	 */
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

	/**
	 * Double quote mark for use in creating the command line.
	 */
	public static final String QUOTE = "\"";

	/**
	 * This is where you want to locate the files.
	 */
	static final String RAD_INSTALL_LOCATION = System.getProperty("osgi.install.area");

	/**
	 * The directory where executable files and configuration files are located.
	 * This value will never be null or empty. See {@link #getValidatorDir} for
	 * details.
	 */
	private String validatorDir;

	/**
	 * The directory where temporary output files will be stored. This value
	 * will never be null or empty. See {@link #getOutputDir} for details.
	 */
	private String outputDir;

	/**
	 * The base command but not including the files absolute path to be
	 * validated. This value will never be null. See {@link #getBaseCommand} for
	 * details.
	 */
	private StringBuffer baseCommand;

	/**
	 * The name of the validator, intended for users to see in problem markers
	 * and in the log. This value will not be null or empty. See
	 * {@link #getValidatorName} for details.
	 */
	private String validatorName;

	/**
	 * Constructor for this. Subclasses should add the executable and parameters
	 * to {@link #baseCommand} in their constructors.
	 *
	 * @param validatorName
	 *            The name of the validator, intended for users to see. Cannot
	 *            be null or empty.
	 * @param validatorDirName
	 *            The name of the validator directory. This is only the name of
	 *            the last name in the directory's full path. Cannot be null or
	 *            empty.
	 */
	public BaseExternalValidator(String validatorName, String validatorDirName) {
		this.validatorName = validatorName;

		String radInstallLocation = RAD_INSTALL_LOCATION;
		if (radInstallLocation.startsWith("file:/")) {
			radInstallLocation = radInstallLocation.substring("file:/".length());
		}

		validatorDir = radInstallLocation.replace('/', '\\').replace('\\', File.separatorChar) + "dropins"
				+ File.separator + "wizard" + File.separator + validatorDirName;
		outputDir = getValidatorDir() + File.separator + "output";
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists()) {
			boolean directoriesMade = outputDirFile.mkdirs();
			if (!directoriesMade) {
				throw new IllegalStateException("Failed to create directory: " + outputDir);
			}
		}

		baseCommand = new StringBuffer();
	}

	/**
	 * Returns the absolute directory where the validator files are stored.
	 *
	 * @return The absolute directory where the validator files are stored. Will
	 *         not be null or empty.
	 */
	protected String getValidatorDir() {
		return validatorDir;
	}

	/**
	 * Returns the absolute directory where temporary output files of the
	 * validator will be stored.
	 *
	 * @return The directory for temporary output files. Will not be null or
	 *         empty.
	 */
	protected String getOutputDir() {
		return outputDir;
	}

	/**
	 * Returns the initial command used for every validation run, such as
	 * executable and some required parameters. This must be appended to by the
	 * subclass in its constructor.
	 *
	 * @return The initial command. Will not be null, but may be empty.
	 */
	protected StringBuffer getBaseCommand() {
		return baseCommand;
	}

	/**
	 * Returns the name of the validator, intended for users to see in problem
	 * markers and log files.
	 *
	 * @return The name of the validator. Will not be null or empty.
	 */
	protected String getValidatorName() {
		return validatorName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ModelEnum> getRequiredModels() {
		return REQUIRED_MODELS;
	}

	/**
	 * Creates a processor used to customize validator-specific behavior for the
	 * algorithm.
	 *
	 * @param actionFactory
	 *            The factory used to generate actions to take in response to
	 *            errors. This value cannot be null.
	 *
	 * @return The processor to create. Will not be null.
	 */
	protected abstract Processor createProcessor(ProblemActionFactory actionFactory);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ValidationResult> analyze(List<ValidatorResource> resources, ProblemActionFactory actionFactory,
			IProgressMonitor monitor) throws ValidationException, IOException, OperationCanceledException {

		try {
			monitor.beginTask("Checking Java files with " + getValidatorName(), resources.size());

			File outputPath = new File(getOutputDir());
			if (!outputPath.exists()) {
				if (!outputPath.mkdirs()) {
					throw new IOException("Could not create the directories for " + outputPath);
				}
			}

			Processor processor = createProcessor(actionFactory);
			Map<String, ValidatorResource> nameToResourceMap = new LinkedHashMap<String, ValidatorResource>();
			for (ValidatorResource resource : resources) {
				String name = processor.createName(resource);
				nameToResourceMap.put(name, resource);
			}

			long begin = System.currentTimeMillis();
			processResources(nameToResourceMap, processor);
			long afterValidator = System.currentTimeMillis();

			List<ValidationResult> results = new ArrayList<ValidationResult>();
			// TODO fix this
			List<File> outputFiles = processor.findOutputFiles(outputPath);
			if (outputFiles != null) {
				if (!outputFiles.isEmpty()) {
					parseFiles(outputFiles, nameToResourceMap, results, processor);
				}
			}

			// Cleanup the temporary output files of validating.
			deleteOutputFiles(outputPath);

			long over = System.currentTimeMillis();

			Debug.VALIDATOR.log("Validation time for ", getValidatorName(), ": ", afterValidator - begin,
					" ms, Processing output time: ", over - afterValidator, " ms");

			return results;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Process the resources. This method is used to build the command line
	 * which is passed to invokeFindBugs.
	 *
	 * @param resources
	 *            The resources to be validated. Will not be null.
	 */
	private void processResources(Map<String, ValidatorResource> nameToResourceMap, Processor processor) {

		if (Debug.VALIDATOR.isActive()) {
			Debug.VALIDATOR.log("Validating file...");
		}

		// The command to be executed
		StringBuffer buffer = new StringBuffer();

		int totalLoops = nameToResourceMap.size() / MAX_VALIDATED_PER_RUN;
		int lastLoop = nameToResourceMap.size() % MAX_VALIDATED_PER_RUN;
		if (lastLoop == 0) {
			lastLoop = MAX_VALIDATED_PER_RUN;
		} else {
			totalLoops += 1;
		}

		processor.prepareToProcessAll();

		Iterator<ValidatorResource> resourceIterator = nameToResourceMap.values().iterator();
		int resourceIndex = 0;
		for (int loopIndex = 0; loopIndex < totalLoops; loopIndex++) {
			buffer.setLength(0);

			processor.prepareForFileGroup(buffer, loopIndex);

			int timesInLoop = MAX_VALIDATED_PER_RUN;
			if (loopIndex == totalLoops - 1) {
				timesInLoop = lastLoop;
			}

			int totalClasses = 0;
			for (int i = 0; i < timesInLoop; i++) {
				ValidatorResource resource = resourceIterator.next();
				boolean appended = processor.processResource(buffer, resource, resourceIndex == timesInLoop - 1);
				if (appended) {
					totalClasses++;
				}

				resourceIndex++;
			}
			String command = buffer.toString();

			buffer.setLength(0);
			Map<String, String> sourceEnvironment = System.getenv();
			Map<String, String> environment = new HashMap<String, String>(sourceEnvironment);
			processor.prepareToInvoke(buffer, environment);

			if (totalClasses > 0) {
				invokeTool(command, environment);
			}
		}
	}

	/**
	 * Run the tool to validate files via command line.
	 *
	 * @param command
	 *            The command to run the tool. Will not be null or an empty
	 *            string.
	 * @param environment
	 *            The environment variables to be used on the command line.
	 *            Cannot be null, but may be empty.
	 */
	void invokeTool(String command, Map<String, String> environment) {
		Debug.VALIDATOR.log(getValidatorName(), " command is ", command);
		try {
			if (command == null || command.isEmpty()) {
				return;
			} else {
				String output = Util.runUntilCompletion(command, environment, new File(getValidatorDir()), true);
				if (Debug.VALIDATOR.isActive()) {
					Debug.VALIDATOR.log(output);
				}
			}
		} catch (IOException e) {
			Debug.VALIDATOR.log(e);
		}
	}

	/**
	 * Parse the outputFiles one by one.
	 *
	 * @param outputFiles
	 *            The files to be parsed. Cannot be null, but may be empty.
	 * @param resources
	 *            The list of resources to check. Cannot be null.
	 * @param results
	 *            The list of exceptions that occurred in those resources. Will
	 *            not be null.
	 */
	private void parseFiles(List<File> outputFiles, Map<String, ValidatorResource> nameToResourceMap,
			List<ValidationResult> results, Processor processor) {
		for (File outputFile : outputFiles) {
			if (Debug.VALIDATOR.isActive()) {
				Debug.VALIDATOR.log("Analyzing output file: " + outputFile);
			}

			processor.processOutputFile(nameToResourceMap, results, outputFile);

		}
	}

	/**
	 * Delete all of the output files generated last time.
	 *
	 * @param outputPath
	 *            The files in which to be deleted. Will not be null.
	 *
	 * @throws IOException
	 *             If a file could not be deleted.
	 */
	private synchronized void deleteOutputFiles(File outputPath) throws IOException {
		if (outputPath != null) {
			File[] outputFiles = outputPath.listFiles();
			if (outputFiles != null) {
				if (outputFiles.length != 0) {
					for (File file : outputFiles) {
						if (!file.delete()) {
							throw new IOException("Could not delete " + file);
						}
					}
				}
			}
		}
	}

	/**
	 * An interface for callback methods, used by the algorithm at appropriate
	 * times.
	 */
	protected static interface Processor {

		/**
		 * Called before processing any resources.
		 */
		void prepareToProcessAll();

		/**
		 * Creates a unique name for the resource. This will be used as the key
		 * for the map given in {@link #processOutputFile(Map, List, File)}.
		 *
		 * @param resource
		 *            The resource to create a name for. Cannot be null.
		 *
		 * @return The name for the resource. Will not be null or empty.
		 */
		String createName(ValidatorResource resource);

		/**
		 * Called before processing a group of files. Due to limitations of the
		 * command line, the command will fail if it is too long. For this
		 * reason, a list of files is divided into groups that limit the size of
		 * the list. Each group will result in one execution of the external
		 * tool. There may be multiple groups of files.
		 *
		 * @param buffer
		 *            A buffer representing the command line. Cannot be null.
		 * @param loopIndex
		 *            The index into the list of groups to run. Must be >= 0.
		 */
		void prepareForFileGroup(StringBuffer buffer, int loopIndex);

		/**
		 * Called to handle a single resource. The resource may be added to the
		 * command line at this step.
		 *
		 * @param buffer
		 *            A buffer representing the command line. Cannot be null.
		 * @param resource
		 *            The resource to add to the command line. Cannot be null.
		 * @param isLastResource
		 *            True if this is the last resource to process, false
		 *            otherwise.
		 *
		 * @return True if the resource was added, false otherwise.
		 */
		boolean processResource(StringBuffer buffer, ValidatorResource resource, boolean isLastResource);

		/**
		 * Called after processing all resources in a group, but before invoking
		 * the tool on the command line.
		 *
		 * @param buffer
		 *            A buffer representing the command line. Cannot be null.
		 */
		void prepareToInvoke(StringBuffer buffer, Map<String, String> environment);

		/**
		 * Finds and returns the output files that were output from multiple
		 * runs of the command line.
		 *
		 * @param outputPath
		 *            The directory where output files exist. Cannot be null.
		 * @param filePattern
		 *            The pattern to use to find files. Cannot be null.
		 *
		 * @return The list of output files that were found. Will not be null,
		 *         but may be empty if none were found.
		 */
		List<File> findOutputFiles(File outputPath);

		/**
		 * Called to process the given output file. Validation errors should be
		 * added to the results in this step.
		 *
		 * @param nameToResourceMap
		 *            A mapping of names to resources. Names will have been
		 *            created in {@link #createName(ValidatorResource)}. Cannot
		 *            be null, but may be empty.
		 * @param results
		 *            The list of cumulative results to add validation errors
		 *            to. Cannot be null, but may be empty.
		 * @param outputFile
		 *            The output file to process. Cannot be null.
		 */
		void processOutputFile(Map<String, ValidatorResource> nameToResourceMap, List<ValidationResult> results,
				File outputFile);
	}
}
