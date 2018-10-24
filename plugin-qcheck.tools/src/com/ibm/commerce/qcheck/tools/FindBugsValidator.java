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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.EclipseUtil;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ModelRegistry;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.ValidatorResource;

/**
 * FindBugsValidator runs FindBugs on the required input files.
 * 
 * @author Trent Hoeppner
 */
public class FindBugsValidator extends BaseExternalValidator {

	private static final String PRIMARY = "primary";

	private static final String TRUE = "true";

	private static final String TYPE_ATTRIBUTE = "type";

	/**
	 * Constructor for this. Initializes the runtime variable and the output
	 * path.
	 */
	public FindBugsValidator() {
		super("FindBugs", "findbugs");
		getBaseCommand().append("java -jar ").append(QUOTE).append(getValidatorDir()).append(File.separator)
				.append("findbugs.jar").append(QUOTE);
		getBaseCommand().append(" -textui");
		getBaseCommand().append(" -low");
		getBaseCommand().append(" -xml:withMessages");
		getBaseCommand().append(" -exclude ").append(QUOTE).append(getValidatorDir()).append(File.separator)
				.append("exclude.xml").append(QUOTE);
		getBaseCommand().append(" -project ").append(QUOTE).append(getOutputDir()).append(File.separator)
				.append("findbugsproject.fbp\"");
		getBaseCommand().append(" -output ").append(QUOTE).append(getOutputDir()).append(File.separator);
	}

	/**
	 * Creates a FindBugs project file for use by the command line, which
	 * contains the given class directories to analyze, and any auxiliary JARs
	 * or class directories that are used in the workspace that may be required.
	 *
	 * @param buffer
	 *            The buffer to use for creating the file. Cannot be null.
	 * @param classDirs
	 *            The set of class directories which contain classes that we
	 *            want to analyze. Cannot be null. If empty, this may result in
	 *            an error from FindBugs when run.
	 */
	private void createFindBugsProjectTempFile(StringBuffer buffer, Set<File> classDirs) {
		buffer.append("<Project projectName=\"QCheck FindBugs Project\">").append(LINE_SEPARATOR);

		// find the auxiliary directories
		Set<File> auxDirs = EclipseUtil.getDefault().getClassPathDirsAndJARs();

		for (File classDir : classDirs) {
			buffer.append("<Jar>").append(classDir).append("</Jar>").append(LINE_SEPARATOR);
		}

		for (File auxDir : auxDirs) {
			if (!classDirs.contains(auxDir)) {
				buffer.append("<AuxClasspathEntry>").append(auxDir.getAbsolutePath());
				buffer.append("</AuxClasspathEntry>").append(LINE_SEPARATOR);
			}
		}

		buffer.append("</Project>").append(LINE_SEPARATOR);

		if (Debug.VALIDATOR.isActive()) {
			Debug.VALIDATOR.log("FindBugsValidator.createFindBugsProjectTempFile() project file:", LINE_SEPARATOR,
					buffer.toString());
		}

		File projectFile = new File(getOutputDir(), "findbugsproject.fbp");
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(projectFile), Charset.forName("UTF-8")));
			writer.write(buffer.toString());
		} catch (IOException e) {
			Debug.VALIDATOR.log(e, "Could not write the FindBugs project file at ", projectFile);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					Debug.VALIDATOR.log(e, "Failed to close writer for project file at ", projectFile);
				}
			}
		}
	}

	/**
	 * Parses the given XML object for BugInstance tags, and creates validation
	 * errors out of them, then adds them to the results.
	 *
	 * @param xml
	 *            The XML object to parse. Cannot be null.
	 * @param resources
	 *            The resources that are used to match up with bug instances.
	 *            Cannot be null. If empty, no validation errors can be
	 *            reported.
	 * @param patternNameToDetailMap
	 *            The mapping from bug pattern names to the detailed text
	 *            describing each bug pattern. This is used to enhance the text
	 *            for validation errors. Cannot be null, but may be empty.
	 * @param warningClasses
	 *            The fully-qualified class names which have last modified dates
	 *            before the Java source files. Cannot be null, but may be
	 *            empty.
	 * @param results
	 *            The current list of validation errors, to which new errors
	 *            will be added. Cannot be null, but may be empty.
	 */
	private void parseBugInstances(XMLWrapper xml, Map<String, ValidatorResource> nameToResourceMap,
			Map<String, String> patternNameToDetailMap, Set<String> warningClassNames, List<ValidationResult> results) {
		List<XMLNode> bugInstanceList = xml.getNodes("BugInstance");
		for (XMLNode bug : bugInstanceList) {

			// get the bug code
			String bugType = bug.get(TYPE_ATTRIBUTE);
			String detail = patternNameToDetailMap.get(bugType);

			// get the long message
			XMLNode longMessageNode = bug.getFirstChild("LongMessage");
			String longMessage = longMessageNode.getContent();

			if (detail != null) {
				longMessage += "  Reason: " + detail;
			}

			// get the class name
			List<XMLNode> classNodes = bug.getChildren("Class");
			String className = null;
			for (XMLNode childNode : classNodes) {
				if (TRUE.equals(childNode.get(PRIMARY))) {
					className = childNode.get("classname");
					break;
				}
			}

			// get the line numbers
			int startLine = 1;
			int endLine = 1;
			List<XMLNode> sourceNodes = bug.getChildren("SourceLine");
			for (XMLNode childNode : sourceNodes) {
				String primary = childNode.get(PRIMARY);
				if (TRUE.equals(primary)) {
					String startLineString = childNode.get("start");
					startLine = Integer.parseInt(startLineString);
					String endLineString = childNode.get("end");
					endLine = Integer.parseInt(endLineString);
					break;
				}
			}

			ValidatorResource resource = nameToResourceMap.get(className);

			boolean classIsOld;
			if (warningClassNames.contains(className)) {
				classIsOld = true;
			} else {
				classIsOld = false;
			}

			if (resource != null) {
				ValidationResult result = createErrorMessage(resource, startLine, endLine, longMessage, null,
						classIsOld);
				results.add(result);
			}
		}
	}

	/**
	 * Appends the class file for the given resource to the given buffer. If the
	 * class file is out-of-date, its fully-qualified class name will be added
	 * to the set of warning classes.
	 *
	 * @param resource
	 *            The resource to find and append the class file for. Cannot be
	 *            null.
	 * @param buffer
	 *            The buffer to append to. Cannot be null.
	 * @param warningClassNames
	 *            The set fully-qualified of class names that are out-of-date.
	 *            Cannot be null, but may be empty.
	 *
	 * @return True if the class name was appended, false otherwise.
	 */
	private boolean appendResource(ValidatorResource resource, StringBuffer buffer, Set<String> warningClassNames) {
		boolean appended = false;

		File classFile = EclipseUtil.getDefault().getClassFile(resource);

		if (classFile.exists()) {
			String className = resource.getClassName();
			buffer.append(className);
			appended = true;

			File javaFile = resource.getFileAsFile();
			if (classFile.lastModified() < javaFile.lastModified()) {
				warningClassNames.add(className);
			}
		} else {
			if (Debug.VALIDATOR.isActive()) {
				Debug.VALIDATOR.log("FindBugsValidator.appendResource() class file for ", resource.getClassName(),
						" does not exist");
			}
		}

		return appended;
	}

	/**
	 * Parses the given XML object to find BugPattern detailed text, and returns
	 * them.
	 *
	 * @param xml
	 *            The XML object to parse. Cannot be null.
	 *
	 * @return A mapping from BugPattern names to the detailed explanation of
	 *         each one. Will not be null, but may be empty.
	 */
	private Map<String, String> parseBugPatterns(XMLWrapper xml) {
		Map<String, String> patternNameToDetailMap = new HashMap<String, String>();
		List<XMLNode> bugPatternList = xml.getNodes("BugPattern");
		for (XMLNode pattern : bugPatternList) {

			// get the bug code
			String bugType = pattern.get(TYPE_ATTRIBUTE);

			// get the long message
			XMLNode detailsNode = pattern.getFirstChild("Details");
			String detail = detailsNode.getContent();

			// remove extra stuff from detail
			Pattern tagPattern = Pattern.compile("</?\\w+>");
			Matcher matcher = tagPattern.matcher(detail);
			if (matcher.find()) {
				detail = matcher.replaceAll("");
			}

			detail = detail.replace("&nbsp;", "");

			Pattern spacePattern = Pattern.compile("\\s+");
			matcher = spacePattern.matcher(detail);
			if (matcher.find()) {
				detail = matcher.replaceAll(" ");
			}

			patternNameToDetailMap.put(bugType, detail);
		}
		return patternNameToDetailMap;
	}

	/**
	 * Process the node of error messages.
	 *
	 * @param resource
	 *            The absolute path of this resource is equal to the owner of
	 *            the errorList. Cannot be null.
	 * @param startLine
	 *            The 0-based index indicating the line number where the error
	 *            starts. Must be >= 0.
	 * @param endLine
	 *            The 0-based index indicating the line number where the error
	 *            ends. Must be >= startLine.
	 * @param longMessage
	 *            The description of the error. Cannot be null or empty.
	 * @param classIsOld
	 *            True indicates that the class is older than the java source
	 *            file, false indicates that the java source file is older.
	 */
	private ValidationResult createErrorMessage(ValidatorResource resource, int startLine, int endLine,
			String longMessage, String explanation, boolean classIsOld) {

		int column = 0;

		CompilationUnit comp = ModelEnum.COMP_UNIT.getData(resource);
		int startPosition = comp.getPosition(startLine, column);
		int endPosition = comp.getPosition(endLine + 1, 0) - 1;
		int length = endPosition - startPosition;
		String fullMessage = longMessage;
		if (classIsOld) {
			fullMessage = "(Warning: Out-of-date class file may make the line number inacccurate) " + longMessage;
		}
		ValidationResult result = new ValidationResult(fullMessage, resource, Collections.EMPTY_LIST, startLine, column,
				length, startPosition, getValidatorName());

		ModelRegistry.getDefault().clearValidator(resource);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Processor createProcessor(ProblemActionFactory actionFactory) {
		return new FindBugsProcessor();
	}

	/**
	 * Used by the super class to call FindBugs.
	 */
	private final class FindBugsProcessor implements Processor {

		private Set<String> warningClassNames;

		private Set<File> classDirs;

		/**
		 * Constructor for this.
		 */
		private FindBugsProcessor() {
			// do nothing
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void prepareForFileGroup(StringBuffer buffer, int loopIndex) {
			buffer.append(getBaseCommand()).append(loopIndex).append(".xml").append(QUOTE).append(" -onlyAnalyze ");

			classDirs = new HashSet<File>();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void prepareToProcessAll() {
			warningClassNames = new HashSet<String>();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean processResource(StringBuffer buffer, ValidatorResource resource, boolean isLastResource) {
			boolean appended = appendResource(resource, buffer, warningClassNames);
			File classDir = EclipseUtil.getDefault().getClassBaseDir(resource);
			classDirs.add(classDir);

			if (!isLastResource) {
				buffer.append(",");
			}

			return appended;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void prepareToInvoke(StringBuffer buffer, Map<String, String> environment) {
			createFindBugsProjectTempFile(buffer, classDirs);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<File> findOutputFiles(File outputPath) {
			final Pattern filePattern = Pattern.compile("\\d+\\.xml");
			File[] outputFiles = outputPath.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return filePattern.matcher(pathname.getName()).matches();
				}

			});

			return Arrays.asList(outputFiles);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void processOutputFile(Map<String, ValidatorResource> nameToResourceMap, List<ValidationResult> results,
				File outputFile) {
			XMLWrapper xml = new XMLWrapper();
			try {
				xml.init(new BufferedInputStream(new FileInputStream(outputFile)));

				if (Debug.VALIDATOR.isActive()) {
					String contents = loadFile(outputFile);
					Debug.VALIDATOR.log("FindBugsValidator.processOutputFile() output file ", outputFile, " contents:",
							LINE_SEPARATOR, contents);
				}

				// first get the bug patterns
				Map<String, String> patternNameToDetailMap = parseBugPatterns(xml);

				// process the list of bug instance nodes
				parseBugInstances(xml, nameToResourceMap, patternNameToDetailMap, warningClassNames, results);
			} catch (FileNotFoundException e) {
				Debug.VALIDATOR.log(e.getMessage());
			}
		}

		/**
		 * Loads the given file into a string and returns it.
		 *
		 * @param file
		 *            The file to load. This value cannot be null.
		 *
		 * @return The contents of the file. Lines endings are replaced with
		 *         \r\n. This value will not be null, but may be empty.
		 */
		private String loadFile(File file) {
			StringBuffer buf = new StringBuffer();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String line = reader.readLine();
				while (line != null) {
					buf.append(line);
					buf.append(LINE_SEPARATOR);
					line = reader.readLine();
				}
			} catch (IOException e) {
				Debug.VALIDATOR.log(e.getMessage());
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// swallow to let an exception in the try to
						// escape
					}
				}
			}

			return buf.toString();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String createName(ValidatorResource resource) {
			return resource.getClassName();
		}

	}

}
