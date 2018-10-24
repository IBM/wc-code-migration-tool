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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ModelRegistry;
import com.ibm.commerce.qcheck.core.Param;
import com.ibm.commerce.qcheck.core.ProblemAction;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.TextPositions;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.VersionedFile;
import com.ibm.commerce.qcheck.tools.config.Level;

/**
 * CheckPIIValidator runs the CHKPII tool against property files and XML files
 * related to globalization.
 * 
 * @author Trent Hoeppner
 */
public class CheckPIIValidator extends BaseExternalValidator implements ConfigurableValidator {

	private static final Pattern FILE_LINE_PATTERN = Pattern.compile("  ([^ ]+)\\s+([^ ]+)\\s+(\\d+)\\s+(.*)");

	private static final Pattern DIR_LINE_PATTERN = Pattern.compile(".*\\\\");

	private static final Pattern ERROR_LINE_PATTERN = Pattern.compile("  \\s+(\\d+)\\s+(.*)");

	private static final Pattern MESSAGE_PATTERN = Pattern.compile("(\\(W\\) )?(.+?)(Line(s)?: ([\\d, ]+))?\\s*");

	private static final Pattern LINE_PATTERN = Pattern.compile("(\\d+)");

	private static final Pattern NLS_ENCODING_PATTERN = Pattern.compile("NLS_ENCODING\\s*=\\s*(\\w+)");

	/**
	 * This error code triggers a check if a new version of
	 * checkpii.encoding.zip needs to be downloaded.
	 */
	private static final String ERROR_CODE_NLS_ENCODING = "941";

	private Level level;

	/**
	 * Constructor for this.
	 *
	 * @param level
	 *            The level with the URL to connect to download the data. This
	 *            value cannot be null.
	 */
	public CheckPIIValidator(Level level) {
		super("CHKPII", "chkpii");

		Param.notNull(level, "level");

		this.level = level;

		getBaseCommand().append(QUOTE).append(getValidatorDir()).append(File.separator).append("chkpii.exe")
				.append(QUOTE).append(" ");
		getBaseCommand().append(QUOTE).append("@").append(getOutputDir()).append(File.separator)
				.append("piifilelist.txt").append(QUOTE).append(" ");
		getBaseCommand().append("/c /e /o ");
		getBaseCommand().append(QUOTE).append(getOutputDir()).append(File.separator).append("output.log").append(QUOTE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Level getLevel() {
		return level;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Processor createProcessor(ProblemActionFactory actionFactory) {
		return new CheckPIIProcessor(actionFactory);
	}

	/**
	 * Downloads the data from the URL defined in
	 * {@link Options.Attributes#UPDATE_URL}. If the URL is empty, null or
	 * malformed, downloading will not occur.
	 */
	void download() {
		Options.ensureLoaded();
		String serverBase = Options.Attributes.UPDATE_URL.getValue();
		String validatorDirString = getValidatorDir();
		File validatorDir = new File(validatorDirString);
		URL validatorDirURL;
		try {
			validatorDirURL = validatorDir.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Could not get a URL for file " + validatorDir, e);
		}
		VersionedFile file = new VersionedFile(serverBase, validatorDirURL.toExternalForm(),
				"chkpii.encoding.version.txt", "chkpii.encoding.zip");

		file.downloadIfOutOfDate();
	}

	/**
	 * A processor to handle preparation for running CHKPII and parsing the
	 * output.
	 */
	private final class CheckPIIProcessor implements Processor {

		private BufferedWriter writer;

		private ProblemActionFactory actionFactory;

		/**
		 * Constructor for this.
		 *
		 * @param actionFactory
		 *            The factory used to generate actions to take in response
		 *            to errors. This value cannot be null.
		 */
		private CheckPIIProcessor(ProblemActionFactory actionFactory) {
			this.actionFactory = actionFactory;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String createName(ValidatorResource resource) {
			return resource.getFileAsFile().getAbsolutePath();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<File> findOutputFiles(File outputPath) {
			File outputFile = new File(getOutputDir(), "output.log");
			List<File> outputFiles = new ArrayList<File>();
			outputFiles.add(outputFile);

			return outputFiles;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void prepareForFileGroup(StringBuffer buffer, int loopIndex) {
			buffer.append(getBaseCommand());
			try {
				writer = new BufferedWriter(new FileWriter(new File(getOutputDir(), "piifilelist.txt")));
			} catch (IOException e) {
				Debug.VALIDATOR.log(e, "Could not open file ", new File(getOutputDir(), "piifilelist.txt"),
						" for writing.");
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void prepareToInvoke(StringBuffer buffer, Map<String, String> environment) {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					Debug.VALIDATOR.log(e, "Could not close file ", new File(getOutputDir(), "piifilelist.txt"),
							" after writing.");
				}
			}

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

			Map<String, String> shortenedToFullMap = createShortenedToFullNameMap(nameToResourceMap);

			Map<ValidatorResource, List<String>> resourceToLinesMap = createResourceToLinesMap(nameToResourceMap,
					outputFile, shortenedToFullMap);

			boolean downloadTriggered = false;
			CheckPIIOutput output = new CheckPIIOutput();

			Set<ValidatorResource> resourcesMissingEncoding = new HashSet<ValidatorResource>();
			for (ValidatorResource resource : resourceToLinesMap.keySet()) {
				List<String> originalLines = resourceToLinesMap.get(resource);

				for (String line : originalLines) {
					OneError error = new OneError();
					Matcher fileLineMatcher = FILE_LINE_PATTERN.matcher(line);
					if (fileLineMatcher.matches()) {
						error.setErrorCode(fileLineMatcher.group(3));
						error.setMessage(fileLineMatcher.group(4));
					} else {
						Matcher errorLineMatcher = ERROR_LINE_PATTERN.matcher(line);
						if (errorLineMatcher.matches()) {
							error.setErrorCode(errorLineMatcher.group(1));
							error.setMessage(errorLineMatcher.group(2));
						}
					}

					if (error.getErrorCode() != null && error.getErrorCode().equals(ERROR_CODE_NLS_ENCODING)) {

						resourcesMissingEncoding.add(resource);

						downloadTriggered = ensureOutputParsed(downloadTriggered, output);

						PIIFile resourceAsPII = createPIIFile(resource);
						PIIFileResults searchResults = output.findSimilar(resourceAsPII);
						if (Debug.VALIDATOR.isActive()) {
							Debug.VALIDATOR.log("Searched in chkpii output, isExactMatch=",
									searchResults.isExactMatch(), ", # matched files=",
									searchResults.getResults().size(), ", resourceAsPII=", resourceAsPII);
							for (PIIFile match : searchResults.getResults()) {
								Debug.VALIDATOR.log("    matched=", match.toString());
							}
						}
						if (searchResults.isExactMatch()) {
							// it already exists, we can recommend an encoding
							// based on existing PII files
							Set<String> allEncodings = new LinkedHashSet<String>();
							PIIFile matchedFile = searchResults.getResults().get(0);
							CheckPIIEncoding bestCodePage = findBestAndAllEncodings(matchedFile, allEncodings);

							if (bestCodePage != null) {
								String message = error.getMessage();
								message += "  This file has previously been translated.  The recommended encoding based on those files is "
										+ bestCodePage + ".  The complete list of encodings is: ";
								Iterator<String> iterator = allEncodings.iterator();
								while (iterator.hasNext()) {
									String encoding = iterator.next();
									message += encoding;
									if (iterator.hasNext()) {
										message += ", ";
									}
								}

								error.setMessage(message);
							} else {
								String message = error.getMessage();
								message += "  " + "This file has previously been translated.  ";
								if (allEncodings.isEmpty()) {
									message += "There are no encodings so its hard to "
											+ "recommend one, but ANSI is the most common in " + "Commerce.  ";
								} else {
									message += "There are conflicting encodings so its hard to "
											+ "recommend one, but ANSI is the most common in " + "Commerce.  ";
								}
								message += "The complete list of encodings is: ";
								Iterator<String> iterator = allEncodings.iterator();
								while (iterator.hasNext()) {
									String encoding = iterator.next();
									message += encoding;
									if (iterator.hasNext()) {
										message += ", ";
									}
								}

								error.setMessage(message);
							}
						} else {
							// TODO it's a new PII file, recommend an encoding
							// from similar circumstances
						}
					}

					if (error.canProcess()) {
						error.addMessages(resource, results, actionFactory);
					}
				}
			}

			// double check that the encoding is correct
			for (String name : nameToResourceMap.keySet()) {
				ValidatorResource resource = nameToResourceMap.get(name);
				if (resourcesMissingEncoding.contains(resource)) {
					continue;
				}

				// the resource has an encoding, but we want to check if it's
				// right
				String fileContents = ModelEnum.STRING.getData(resource);
				int nlsEncodingIndex = fileContents.indexOf("NLS_ENCODING");
				if (nlsEncodingIndex >= 0) {
					TextPositions positions = ModelEnum.TEXT_POS.getData(resource);
					int lineIndex = positions.getLineIndex(nlsEncodingIndex);
					String line = positions.getText(lineIndex);
					Matcher matcher = NLS_ENCODING_PATTERN.matcher(line);
					if (matcher.find()) {
						String actualEncoding = matcher.group(1);

						// find the expected encoding
						downloadTriggered = ensureOutputParsed(downloadTriggered, output);

						PIIFile resourceAsPII = createPIIFile(resource);
						PIIFileResults searchResults = output.findSimilar(resourceAsPII);
						if (searchResults.isExactMatch()) {
							// it already exists, we can recommend an encoding
							// based on existing PII files
							Set<String> allEncodings = new LinkedHashSet<String>();
							PIIFile matchedFile = searchResults.getResults().get(0);
							CheckPIIEncoding bestCodePage = findBestAndAllEncodings(matchedFile, allEncodings);

							if (bestCodePage != null) {
								if (!bestCodePage.toNLSEncodingValue().equals(actualEncoding)) {
									// the encodings don't match, create an
									// error
									String message = "The encoding might not be correct.  "
											+ "This file has previously been translated.  "
											+ "The recommended encoding based on those files is " + bestCodePage
											+ ".  The complete list of encodings is: ";
									Iterator<String> iterator = allEncodings.iterator();
									while (iterator.hasNext()) {
										String encoding = iterator.next();
										message += encoding;
										if (iterator.hasNext()) {
											message += ", ";
										}
									}

									OneError error = new OneError();
									error.setProcessedMessage(message, lineIndex);
									error.addResults(resource, results, actionFactory);
								}
							} else {
								if (!allEncodings.contains(actualEncoding)) {
									if (Debug.VALIDATOR.isActive()) {
										Debug.VALIDATOR.log(
												"    There is no best encoding, and the existing encoding (",
												actualEncoding, ") is not in the list of possible encodings ",
												allEncodings, ", file=", resourceAsPII);
									}
									String message = "The encoding might not be correct.  "
											+ "This file has previously been translated.  "
											+ "There are no encodings or conflicting encodings so its hard to "
											+ "recommend one, but ANSI is the most common in "
											+ "Commerce.  The complete list of encodings is: ";
									Iterator<String> iterator = allEncodings.iterator();
									while (iterator.hasNext()) {
										String encoding = iterator.next();
										message += encoding;
										if (iterator.hasNext()) {
											message += ", ";
										}
									}

									OneError error = new OneError();
									error.setProcessedMessage(message, lineIndex);
									error.addResults(resource, results, actionFactory);
								} else {
									if (Debug.VALIDATOR.isActive()) {
										Debug.VALIDATOR.log("There is no best encoding, but the existing encoding (",
												actualEncoding, ") is in the list of possible encodings ", allEncodings,
												", file=", resourceAsPII);
									}
								}
							}
						} else {
							// TODO it's a new PII file, recommend an encoding
							// from similar circumstances
						}
					}
				}

				ModelRegistry.getDefault().clearValidator(resource);
			}
		}

		private PIIFile createPIIFile(ValidatorResource resource) {
			File file = resource.getFileAsFile();
			// String baseDir = resource.getBaseDir();
			// if (baseDir.endsWith(File.separator)) {
			// baseDir = baseDir.substring(0, baseDir.length() - 1);
			// }

			String parentPath = file.getParentFile().getAbsolutePath();

			// String dir;
			// if (parentPath.equals(baseDir)) {
			// dir = "\\";
			// } else {
			// if (Debug.VALIDATOR.isActive()) {
			// Debug.VALIDATOR.log("createPIIFile() ", "parentPath=",
			// parentPath);
			// Debug.VALIDATOR.log("createPIIFile() ", "baseDir=", baseDir);
			// }
			// dir = parentPath.substring(baseDir.length() + 1) + "\\";
			// }
			String baseName = file.getName();
			PIIFile resourceAsPII = PIIFile.create(parentPath, baseName);
			return resourceAsPII;
		}

		private boolean ensureOutputParsed(boolean downloadTriggered, CheckPIIOutput output) {
			if (!downloadTriggered) {
				download();
				downloadTriggered = true;

				// check the encodings
				String validatorDirString = getValidatorDir();
				File validatorDir = new File(validatorDirString);
				File encodingZipFile = new File(validatorDir, "chkpii.encoding.zip");
				ZipInputStream in = null;
				try {
					in = new ZipInputStream(new BufferedInputStream(new FileInputStream(encodingZipFile)));
					in.getNextEntry();
					output.parse(in);
				} catch (IOException e) {
					Debug.VALIDATOR.log(e, "Could not read file ", encodingZipFile, ".");
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							Debug.VALIDATOR.log(e, "Could not close file ", encodingZipFile, ".");
						}
					}
				}
			}
			return downloadTriggered;
		}

		private CheckPIIEncoding findBestAndAllEncodings(PIIFile existingPII, Set<String> allEncodings) {
			EncodingResolver resolver = new EncodingResolver();
			for (Locale locale : existingPII.getVariants()) {
				String codePage = existingPII.getProperty(locale, "CodePage");
				if ("?".equals(codePage)) {
					String codePageText = existingPII.getProperty(locale, "CodePageText");

					codePage = findBestCodePageForUncertain(codePageText);
				}

				if (codePage != null) {
					CheckPIIEncoding encoding = CheckPIIEncoding.parse(codePage);

					allEncodings.add(codePage);
					resolver.add(locale, encoding);
				} else {
					if (Debug.VALIDATOR.isActive()) {
						String codePageText = existingPII.getProperty(locale, "CodePageText");
						Debug.VALIDATOR.log("Could not find code page for CodePageText=", codePageText);
					}
				}
			}

			CheckPIIEncoding bestCodePage = resolver.getBestEncoding();
			if (bestCodePage == CheckPIIEncoding.ANSI_ISO || bestCodePage == CheckPIIEncoding.ANSI_WIN) {
				bestCodePage = CheckPIIEncoding.ANSI;
			}

			return bestCodePage;
		}

		private String findBestCodePageForUncertain(String codePageText) {
			String codePage;
			String bestCodePage = null;
			Integer bestPercent = null;
			Pattern ENCODING_PERCENT_PATTERN = Pattern.compile("([A-Z][A-Z \\(\\)]+)=([\\d]+)%");
			Matcher matcher = ENCODING_PERCENT_PATTERN.matcher(codePageText);
			while (matcher.find()) {
				String currCodePage = matcher.group(1);
				String currPercentString = matcher.group(2);
				Integer currPercent = Integer.valueOf(currPercentString);
				if (bestCodePage == null) {
					bestCodePage = currCodePage;
					bestPercent = currPercent;
				} else {
					if (!"ANSI".equals(bestCodePage) && "ANSI".equals(currCodePage)
							&& currPercent.equals(bestPercent)) {
						bestCodePage = currCodePage;
						bestPercent = currPercent;
					} else if (currPercent > bestPercent) {
						bestCodePage = currCodePage;
						bestPercent = currPercent;
					}
				}
			}

			codePage = bestCodePage;
			return codePage;
		}

		private Map<ValidatorResource, List<String>> createResourceToLinesMap(
				Map<String, ValidatorResource> nameToResourceMap, File outputFile,
				Map<String, String> shortenedToFullMap) {
			Map<ValidatorResource, List<String>> resourceToLinesMap = new LinkedHashMap<ValidatorResource, List<String>>();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(outputFile));

				// skip to the errors
				boolean inErrorSection = false;
				String dir = "";
				String currentFile = null;
				List<String> currentLines = new ArrayList<String>();
				String line = reader.readLine();
				while (line != null) {
					if (line.startsWith("------------------------------")) {
						if (!inErrorSection) {
							inErrorSection = true;
						} else {
							// we have reached the end of the error section
							break;
						}
					} else if (line.isEmpty()) {
						putResourceWithLines(nameToResourceMap, shortenedToFullMap, resourceToLinesMap, dir,
								currentFile, currentLines, line);

						dir = "";
						currentFile = null;
					} else {
						Matcher fileLineMatcher = FILE_LINE_PATTERN.matcher(line);
						if (fileLineMatcher.matches()) {
							putResourceWithLines(nameToResourceMap, shortenedToFullMap, resourceToLinesMap, dir,
									currentFile, currentLines, line);

							// dir has not changed
							currentFile = fileLineMatcher.group(1);
						} else {
							Matcher dirLineMatcher = DIR_LINE_PATTERN.matcher(line);
							if (dirLineMatcher.matches()) {
								dir = line;
								currentFile = null;
							} else {
								// Matcher errorLineMatcher =
								// ERROR_LINE_PATTERN.matcher(line);
								// if (!errorLineMatcher.matches()) {
								// if (Debug.VALIDATOR.isActive()) {
								// Debug.VALIDATOR.log("A line did not match any
								// pattern: ",
								// line);
								// }
								// }
							}
						}
						currentLines.add(line);
					}

					line = reader.readLine();
				}
			} catch (IOException e) {
				Debug.VALIDATOR.log(e, "Could not read file ", outputFile, " to parse errors.");
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// swallow to allow an exception to escape from try
					}
				}
			}
			return resourceToLinesMap;
		}

		private void putResourceWithLines(Map<String, ValidatorResource> nameToResourceMap,
				Map<String, String> shortenedToFullMap, Map<ValidatorResource, List<String>> resourceToLinesMap,
				String dir, String currentFile, List<String> currentLines, String line) {
			String path = dir + currentFile;
			String fullFileName = shortenedToFullMap.get(path);
			ValidatorResource resource = nameToResourceMap.get(fullFileName);
			// if (Debug.VALIDATOR.isActive()) {
			// if (currentLines.size() > 0) {
			// Debug.VALIDATOR.log("putResourceWithLines() dir=", dir,
			// ", currentFile=", currentFile, ", line=",
			// line, ", currentLines=");
			// for (String line2 : currentLines) {
			// Debug.VALIDATOR.log(" |", line2);
			// }
			// }
			//
			// Debug.VALIDATOR.log("Found resource ", resource == null ?
			// resource
			// : resource.getFileAsFile(), ", path=", path, ", fullFileName=",
			// fullFileName);
			// }

			resourceToLinesMap.put(resource, new ArrayList<String>(currentLines));
			currentLines.clear();
		}

		// chkpii shortens the paths by removing the common parents, we have
		// to do the same to match the filenames to the resources
		private Map<String, String> createShortenedToFullNameMap(Map<String, ValidatorResource> nameToResourceMap) {
			Map<ModString, String> shortenedToFullNameMap = new HashMap<ModString, String>();
			ModInt commonInt = new ModInt(Integer.MAX_VALUE);
			ModString lastName = null;
			for (String name : nameToResourceMap.keySet()) {
				ModString newName = new ModString(name, commonInt);
				if (lastName != null) {
					newName.adjustCommonLength(lastName);
				} else {
					commonInt.value = name.lastIndexOf('\\') + 1;
				}

				shortenedToFullNameMap.put(newName, name);

				lastName = newName;
			}

			Map<String, String> shortenedToFullMap = new HashMap<String, String>();
			for (ModString modString : shortenedToFullNameMap.keySet()) {
				String full = shortenedToFullNameMap.get(modString);
				String shortened = modString.getValue();
				shortenedToFullMap.put(shortened, full);
			}
			return shortenedToFullMap;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean processResource(StringBuffer buffer, ValidatorResource resource, boolean isLastResource) {
			boolean success = false;
			try {
				writer.write(resource.getFileAsFile().getAbsolutePath());
				if (!isLastResource) {
					writer.write(LINE_SEPARATOR);
				}
				success = true;
			} catch (IOException e) {
				Debug.VALIDATOR.log(e, "Could not write to file ", new File(getOutputDir(), "piifilelist.txt"), ".");
			}

			return success;
		}

	}

	/**
	 * This class represents a single possible error in the output file.
	 */
	private final class OneError {

		/**
		 * This error code is ignored for WebSphere Commerce. It occurs when
		 * something like &amp;#163; is in a translatable string.
		 */
		private static final String ERROR_CODE_XML_HEX_DEC_NOT_ALLOWED = "968";

		private String errorCode = null;

		private String message = null;

		private String mainMessage = null;

		private List<Integer> lines = null;

		/**
		 * Constructor for this.
		 */
		private OneError() {
			// do nothing
		}

		/**
		 * Sets the CHKPII error code.
		 *
		 * @param errorCode
		 *            The error code. Cannot be null or empty.
		 */
		public void setErrorCode(String errorCode) {
			this.errorCode = errorCode;
		}

		/**
		 * Returns the error code.
		 *
		 * @return The error code. Will not be empty, but may be null if the
		 *         value has not been {@link #setErrorCode set} yet.
		 */
		public String getErrorCode() {
			return errorCode;
		}

		/**
		 * Sets the CHKPII error message.
		 *
		 * @param message
		 *            The error message. Cannot be null or empty.
		 */
		public void setMessage(String message) {
			this.message = message;
		}

		/**
		 * Returns the error message.
		 *
		 * @return The error message. Will not be empty, but may be null if the
		 *         value has not been {@link #setMessage(String) set} yet.
		 */
		public String getMessage() {
			return message;
		}

		public void setProcessedMessage(String message, int lineIndex) {
			mainMessage = message;
			if (lines == null) {
				lines = new ArrayList<Integer>(1);
			}

			lines.add(lineIndex + 1);
		}

		/**
		 * Returns whether this has messages to add.
		 *
		 * @return True if this has errors available, false otherwise.
		 */
		public boolean canProcess() {
			return message != null;
		}

		/**
		 * Adds the validation errors for the given resource to the given
		 * results.
		 *
		 * @param resource
		 *            The resource to add errors for. Cannot be null.
		 * @param results
		 *            The cumulative list of results to add to. Cannot be null,
		 *            but may be empty.
		 * @param actionFactory
		 *            The factory used to generate actions to take in response
		 *            to errors. This value cannot be null.
		 */
		public void addMessages(ValidatorResource resource, List<ValidationResult> results,
				ProblemActionFactory actionFactory) {
			separateLineNumbersAndMainMessage();

			if (mainMessage != null && !errorCode.equals(ERROR_CODE_XML_HEX_DEC_NOT_ALLOWED)) {
				addResults(resource, results, actionFactory);
			}
		}

		/**
		 * Parses {@link #message} into {@link #mainMessage} and {@link #lines}.
		 */
		private void separateLineNumbersAndMainMessage() {
			lines = new ArrayList<Integer>();
			Matcher messageMatcher = MESSAGE_PATTERN.matcher(message);
			if (messageMatcher.matches()) {
				mainMessage = messageMatcher.group(2).trim();
				String linesList = messageMatcher.group(5);

				if (linesList != null) {
					Matcher lineMatcher = LINE_PATTERN.matcher(linesList);
					while (lineMatcher.find()) {
						String currentLine = linesList.substring(lineMatcher.start(), lineMatcher.end());
						Integer lineNum = Integer.valueOf(currentLine);
						lines.add(lineNum);
					}
				} else {
					lines.add(1);
				}
			}
		}

		/**
		 * Adds the errors for this. This is called after other processing and
		 * checks are performed.
		 *
		 * @param resource
		 *            The resource to add errors for. Cannot be null.
		 * @param results
		 *            The cumulative list of results to add to. Cannot be null,
		 *            but may be empty.
		 * @param actionFactory
		 *            The factory used to generate actions to take in response
		 *            to errors. This value cannot be null.
		 */
		private void addResults(ValidatorResource resource, List<ValidationResult> results,
				ProblemActionFactory actionFactory) {
			TextPositions positions = ModelEnum.TEXT_POS.getData(resource);

			for (int i = 0; i < lines.size(); i++) {
				int lineNum = lines.get(i);

				String messageText = getMessageText(i);

				int lineIndex = lineNum - 1;
				if (lineIndex >= positions.getLineCount()) {
					if (Debug.VALIDATOR.isActive()) {
						Debug.VALIDATOR.log("The line index ", lineIndex, " is out of bounds, changing it to ",
								positions.getLineCount(), ", file=", resource.getFileAsFile(), ", message=", message);
					}
					lineIndex = positions.getLineCount() - 1;
				}
				int columnIndex = 0;
				int errorLength = positions.getLength(lineIndex);
				int startingPosition = positions.getStartPosition(lineIndex);

				List<ProblemAction> actions = new ArrayList<ProblemAction>();
				String urlString = null;
				if (messageText.startsWith("941")) {
					urlString = "https://commerce.torolab.ibm.com/wiki/index.php/Technology_specific#NLS_ENCODING";
				} else if (messageText.startsWith("922")) {
					urlString = "https://commerce.torolab.ibm.com/wiki/index.php/Technology_specific#NLS_MESSAGEFORMAT";
				}

				if (urlString != null) {
					try {
						URL url = new URL(urlString);
						ProblemAction action = actionFactory.buildLink(url);
						actions.add(action);
					} catch (MalformedURLException e) {
						Debug.VALIDATOR.log(e);
					}
				}

				ValidationResult result = new ValidationResult(messageText, resource, actions, lineIndex, columnIndex,
						errorLength, startingPosition, getValidatorName());
				results.add(result);
			}
		}

		/**
		 * Creates a message for a particular line. If the error is for more
		 * than one line, the given index will be used to exclude the line
		 * number from {@link #lines}.
		 *
		 * @param lineIndexToExclude
		 *            The index into {@link #lines} that should not be
		 *            mentioned. Must be &gt;= 0.
		 *
		 * @return The text for the error message. Will not be null or empty.
		 */
		private String getMessageText(int lineIndexToExclude) {
			StringBuffer messageText = new StringBuffer();
			if (errorCode != null) {
				messageText.append(errorCode).append(": ");
			}
			messageText.append(mainMessage);
			if (lines.size() > 1) {
				messageText.append(" Other lines are: ");
				for (int j = 0; j < lines.size(); j++) {
					if (j != lineIndexToExclude) {
						messageText.append(lines.get(j));

						int nextJInSmallerList = j + 1;
						if (nextJInSmallerList >= lineIndexToExclude) {
							nextJInSmallerList++;
						}

						boolean stillHaveLines = nextJInSmallerList < lines.size();

						if (stillHaveLines) {
							messageText.append(",");
						}
					}
				}
			}
			return messageText.toString();
		}
	}

	/**
	 * This class represents an integer that can be modified.
	 */
	private static final class ModInt {

		private int value;

		private ModInt(int value) {
			this.value = value;
		}
	}

	/**
	 * This class represents a string that has a common start string with
	 * several other strings. A {@link ModInt} represents the index at which
	 * there are differences between the group of strings. When this is changed,
	 * the {@link #getValue} method will reflect the changes.
	 * <p>
	 * This is required because filenames in the CHKPII output file are always
	 * shortened to the point of divergence. We use this to identify the
	 * original {@link ValidatorResource} for each error.
	 */
	private static final class ModString {

		private String name;

		private ModInt commonStart;

		/**
		 * Constructor for this.
		 *
		 * @param name
		 *            The string to represent. Cannot be null, but may be empty.
		 * @param commonStart
		 *            The integer that represents the first index at which
		 *            strings in a group are different. Cannot be null.
		 */
		private ModString(String name, ModInt commonStart) {
			this.name = name;
			this.commonStart = commonStart;
		}

		/**
		 * Returns the string starting from the point at which strings are
		 * different.
		 *
		 * @return The sub-string that is different. Will not be null, but may
		 *         be empty.
		 */
		public String getValue() {
			return name.substring(commonStart.value);
		}

		/**
		 * Finds the point at which two strings are different. The returned
		 * value is always on a path boundary. For example, consider these two
		 * strings:
		 *
		 * <pre>
		 * C:\project\directory\file1.properties
		 * C:\project\dir\file2.properties
		 * </pre>
		 *
		 * In this case, the common sub-string identified by this method would
		 * be:
		 *
		 * <pre>
		 * C:\project\
		 * </pre>
		 *
		 * @param other
		 *            The other string to compare this to. Cannot be null.
		 *
		 * @return The index of the last character on a path boundary that
		 *         differs between the two strings. This value will be &gt;= 0.
		 */
		private int findCommonPoint(ModString other) {
			int length = Math.min(name.length(), other.name.length());

			int commonLength = length;
			for (int i = 0; i < length; i++) {
				if (name.charAt(i) != other.name.charAt(i)) {
					commonLength = i;
					break;
				}
			}

			// now find the first filename separator before that
			int lastLength = name.lastIndexOf('\\', commonLength);
			if (lastLength >= 0) {
				commonLength = lastLength + 1;
			}

			return commonLength;
		}

		/**
		 * Adjusts {@link #commonStart} in this based on the common point
		 * between two values. This method assumes that both this and
		 * <code>other</code> have the same ModInt for the commonLength.
		 *
		 * @param other
		 *            The other path to consider. Cannot be null.
		 */
		void adjustCommonLength(ModString other) {
			int commonLength = findCommonPoint(other);
			if (commonLength < commonStart.value) {
				commonStart.value = commonLength;
			}
		}

	}

}
