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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ModelRegistry;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.ValidatorResource;

/**
 * This class is used to validate files using Checkstyle.
 * 
 * @author Trent Hoeppner
 */
public class CheckstyleValidator extends BaseExternalValidator {

	/**
	 * Constructor of ChecckstyleValidator. Initialize the runtime variable and
	 * the output path.
	 */
	public CheckstyleValidator() {
		super("Checkstyle", "checkstyle");
		getBaseCommand().append("java -jar ").append(QUOTE).append(getValidatorDir()).append(File.separator)
				.append("checkstyle-all-5.1.jar").append(QUOTE);
		getBaseCommand().append(" -c  ").append(QUOTE).append(getValidatorDir()).append(File.separator)
				.append("checks.xml").append(QUOTE);
		getBaseCommand().append(" -f xml ");
		getBaseCommand().append(" -o ").append(QUOTE).append(getOutputDir()).append(File.separator);
	}

	/**
	 * Process the node of error messages.
	 *
	 * @param errorList
	 *            The current list of nodes, which is corresponding to the
	 *            resource. Cannot be null, but may be empty.
	 * @param resource
	 *            The absolute path of this resource is equal to the owner of
	 *            the errorList. Cannot be null.
	 * @param results
	 *            The list of exceptions that occurred in those resources. Will
	 *            not be null.
	 */
	private void processErrorMessages(List<XMLNode> errorList, ValidatorResource resource,
			List<ValidationResult> results) {
		for (XMLNode errorNode : errorList) {
			String message = errorNode.get("message");
			int line = Integer.parseInt(errorNode.get("line"));
			String columnString = errorNode.get("column");
			int column = 0;
			if (columnString != null) {
				column = Integer.parseInt(columnString);
			}

			CompilationUnit comp = ModelEnum.COMP_UNIT.getData(resource);
			int startPosition = comp.getPosition(line, column);
			int endPosition = comp.getPosition(line + 1, 0) - 1;
			int length = endPosition - startPosition;
			ValidationResult result = new ValidationResult(message, resource, Collections.EMPTY_LIST, line, column,
					length, startPosition, getValidatorName());
			results.add(result);
		}

		ModelRegistry.getDefault().clearValidator(resource);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Processor createProcessor(ProblemActionFactory actionFactory) {
		return new CheckStyleProcessor();
	}

	/**
	 * Used by the super class to call Checkstyle.
	 */
	private final class CheckStyleProcessor implements Processor {

		private static final String SPACE_CHAR = " ";

		/**
		 * Constructor for this.
		 */
		private CheckStyleProcessor() {
			// do nothing
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<File> findOutputFiles(File outputPath) {
			File[] outputFiles = outputPath.listFiles();
			return Arrays.asList(outputFiles);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void prepareForFileGroup(StringBuffer buffer, int loopIndex) {
			buffer.append(getBaseCommand()).append(loopIndex).append(".xml").append(QUOTE).append(SPACE_CHAR);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void prepareToInvoke(StringBuffer buffer, Map<String, String> environment) {
			// do nothing
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void prepareToProcessAll() {
			// do nothing
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

				// Process the list of node whose tag name is 'file'
				List<XMLNode> fileList = xml.getNodes("file");
				for (XMLNode fileNode : fileList) {

					String absolutePath = fileNode.get("name");

					ValidatorResource resource = nameToResourceMap.get(absolutePath);

					if (resource != null) {
						List<XMLNode> errorList = fileNode.getChildren("error");
						processErrorMessages(errorList, resource, results);
					}
				}
			} catch (FileNotFoundException e) {
				Debug.VALIDATOR.log(e.getMessage());
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean processResource(StringBuffer buffer, ValidatorResource resource, boolean isLastResource) {
			String baseDir = resource.getBaseDir();
			String fileName = resource.getPathFilename();
			buffer.append(QUOTE).append(baseDir).append(fileName).append(QUOTE).append(SPACE_CHAR);

			if (!isLastResource) {
				buffer.append(",");
			}

			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String createName(ValidatorResource resource) {
			return resource.getFileAsFile().getAbsolutePath();
		}

	}
}
