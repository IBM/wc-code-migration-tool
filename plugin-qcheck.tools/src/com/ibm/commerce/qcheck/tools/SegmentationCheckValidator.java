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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.ValidatorResource;

/**
 * This class runs the segchk utility to check if strings are created with
 * concatenation or are using proper internationalization techniques.
 * 
 * @author Trent Hoeppner
 */
public class SegmentationCheckValidator extends BaseExternalValidator {

	private static final File winINI = new File("C:\\Windows\\win.ini");

	/**
	 * Constructor for this.
	 */
	public SegmentationCheckValidator() {
		super("Segmentation Check", "segchk");
		getBaseCommand().append(QUOTE).append(getValidatorDir()).append(File.separator).append("segchk.exe")
				.append(QUOTE).append(" ");
		getBaseCommand().append(QUOTE).append("-out:").append(getOutputDir());
	}

	@Override
	protected Processor createProcessor(ProblemActionFactory actionFactory) {
		// TODO Auto-generated method stub
		return new SegmentationCheckProcessor();
	}

	private class SegmentationCheckProcessor implements Processor {

		@Override
		public String createName(ValidatorResource resource) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<File> findOutputFiles(File outputPath) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void prepareForFileGroup(StringBuffer buffer, int loopIndex) {
			// TODO Auto-generated method stub

		}

		@Override
		public void prepareToInvoke(StringBuffer buffer, Map<String, String> environment) {

			// modify the environment so that segchk can be run
			// File segchkHome = new File(getValidatorDir(), "segchk");
			// File transManagerHome = new File(getValidatorDir(), "EQF");
			File segchkHome = new File("S:", "segchk");
			File transManagerHome = new File("S:", "EQF");

			String currentPath = environment.get("PATH");
			StringBuffer newPath = new StringBuffer();
			newPath.append(currentPath);
			newPath.append(";").append(transManagerHome.getAbsolutePath()).append("\\WIN");
			newPath.append(";").append(transManagerHome.getAbsolutePath()).append("\\TABLE");
			newPath.append(";").append(segchkHome.getAbsolutePath()).append("\\bin");

			environment.put("SEGCHK_TM_HOME", transManagerHome.getAbsolutePath());
			environment.put("SEGCHK_HOME", segchkHome.getAbsolutePath());
			environment.put("PATH", newPath.toString());

			// boolean removeAfter = false;
			// boolean valuesRead = false;
			// try {
			// readValues();
			// valuesRead = true;
			// } catch (IOException e) {
			// Debug.VALIDATOR.log(e, "Could not read registry value.");
			// }
			//
			// if (!valuesRead) {
			// try {
			// writeValues();
			// removeAfter = true;
			// } catch (IOException e) {
			// Debug.VALIDATOR.log(e, "Could not write registry value.");
			// }
			// }
			try {
				modifyWinINI();
			} catch (IOException e) {
				Debug.VALIDATOR.log(e, "Could not update ", winINI.getAbsolutePath(), ".");
			}

			invokeTool(
					"S:\\segchk\\bin\\segchk.exe " + "-file:"
							+ "C:\\Info\\Projects\\QCheck\\project\\com.ibm.commerce.qcheck.tools\\fakeEclipseInstallDir\\junk_en.properties "
							+ "-out:"
							+ "C:\\Info\\Projects\\QCheck\\project\\com.ibm.commerce.qcheck.tools\\fakeEclipseInstallDir\\dropins\\wizard\\chkpii\\output\\segchkoutput.log",
					environment);

			// if (removeAfter) {
			// try {
			// removeValues();
			// } catch (IOException e) {
			// Debug.VALIDATOR.log(e, "Could not remove registry value.");
			// }
			// }
		}

		@Override
		public void prepareToProcessAll() {
			// TODO Auto-generated method stub

		}

		@Override
		public void processOutputFile(Map<String, ValidatorResource> nameToResourceMap, List<ValidationResult> results,
				File outputFile) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean processResource(StringBuffer buffer, ValidatorResource resource, boolean isLastResource) {
			// TODO Auto-generated method stub
			return false;
		}

		/**
		 * <pre>
		 * [EqfStart]
		 * FirstTime=10000
		 * VISITM_x=521
		 * VISITM_y=88
		 * VISITM_cx=809
		 * VISITM_cy=737
		 * Drive=S:
		 * Path=EQF
		 * SysProp=S:\EQF\PROPERTY\EQFSYSW.PRP
		 * Version=6.1.1 Subset
		 * UserLang=English(U.S.)
		 * Licensenumber=10000
		 * TMHomepage=http://w3-03.ibm.com/globalization/page/2273
		 * TMDocumentation=eqf\document\tpdocu\eqfr5mst.pdf
		 * TMHTMLDocumentation=eqf\msg\eqfr5msttfrm.htm
		 * </pre>
		 */
		private void modifyWinINI() throws IOException {
			Map<String, String> expectedProperties = new LinkedHashMap<String, String>();
			expectedProperties.put("FirstTime", "10000");
			expectedProperties.put("VISITM_x", "521");
			expectedProperties.put("VISITM_y", "88");
			expectedProperties.put("VISITM_cx", "809");
			expectedProperties.put("VISITM_cy", "737");
			expectedProperties.put("Drive", "S:");
			expectedProperties.put("Path", "EQF");
			expectedProperties.put("SysProp", "S:\\EQF\\PROPERTY\\EQFSYSW.PRP");
			expectedProperties.put("Version", "6.1.1 Subset");
			expectedProperties.put("UserLang", "English(U.S.)");
			expectedProperties.put("Licensenumber", "10000");
			expectedProperties.put("TMHomepage", "http://w3-03.ibm.com/globalization/page/2273");
			expectedProperties.put("TMDocumentation", "eqf\\document\\tpdocu\\eqfr5mst.pdf");
			expectedProperties.put("TMHTMLDocumentation", "eqf\\msg\\eqfr5msttfrm.htm");

			boolean allExist = true;
			BufferedReader reader = null;
			StringBuffer buf = new StringBuffer();
			try {
				reader = new BufferedReader(new FileReader(winINI));
				String line = reader.readLine();
				boolean inEqfStart = false;
				while (line != null) {
					boolean finishingEqfStart = false;
					if (line.equals("[EqfStart]")) {
						inEqfStart = true;
					} else if (Pattern.matches("\\[.+\\]", line)) {
						if (inEqfStart) {
							finishingEqfStart = true;
						}

						inEqfStart = false;
					}

					if (finishingEqfStart) {
						// write all the missing properties
						for (String key : expectedProperties.keySet()) {
							String value = expectedProperties.get(key);
							buf.append(key).append("=").append(value).append(LINE_SEPARATOR);
							allExist = false;
						}
					} else if (inEqfStart) {
						// find the key in the file and remove it from
						// expectedProperties
						Pattern pattern = Pattern.compile("(.+)=(.*)");
						Matcher matcher = pattern.matcher(line);
						if (matcher.matches()) {
							String key = matcher.group(1);
							if (expectedProperties.containsKey(key)) {
								expectedProperties.remove(key);
							}
						}
					}

					buf.append(line).append(LINE_SEPARATOR);
					line = reader.readLine();
				}
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// swallow so the main try block's exception can escape
					}
				}
			}

			// only write the file if some required properties are missing
			if (!allExist) {
				BufferedWriter writer = null;
				try {
					writer = new BufferedWriter(new FileWriter(winINI));
					writer.write(buf.toString());
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException e) {
							// swallow so the main try block's exception can
							// escape
						}
					}
				}
			}
		}
		//
		//
		// private void readValues() throws IOException {
		// readRegistyValue("Path", KEY_EQFDLL);
		// readRegistyValue("", KEY_EQFDLL);
		// readRegistyValue("Path", KEY_NONNODE_EQFDLL);
		// readRegistyValue("", KEY_NONNODE_EQFDLL);
		// readRegistyValue("", KEY_IBM_TRANSLATIONMANAGER);
		// readRegistyValue("", KEY_VERSION);
		// readRegistyValue("SEGCHK_TM_HOME", KEY_ENV);
		// readRegistyValue("SEGCHK_HOME", KEY_ENV);
		// }
		//
		//
		// private void writeValues() throws IOException {
		// writeRegistyValue("Path", "S:\\EQF", KEY_EQFDLL,
		// RegKeyDataType.REG_SZ);
		// writeRegistyValue("", "S:\\EQF\\EQFDLL.DLL", KEY_EQFDLL,
		// RegKeyDataType.REG_SZ);
		// writeRegistyValue("Path", "S:\\EQF", KEY_NONNODE_EQFDLL,
		// RegKeyDataType.REG_SZ);
		// writeRegistyValue("", "S:\\EQF\\EQFDLL.DLL", KEY_NONNODE_EQFDLL,
		// RegKeyDataType.REG_SZ);
		// writeRegistyValue("", null, KEY_IBM_TRANSLATIONMANAGER, null);
		// writeRegistyValue("", null, KEY_VERSION, null);
		// writeRegistyValue("SEGCHK_TM_HOME", "S:\\EQF", KEY_ENV,
		// RegKeyDataType.REG_EXPAND_SZ);
		// writeRegistyValue("SEGCHK_HOME", "S:\\segchk", KEY_ENV,
		// RegKeyDataType.REG_EXPAND_SZ);
		// }
		//
		//
		// private void removeValues() throws IOException {
		// try {
		// removeRegistyValue("Path", KEY_EQFDLL);
		// } catch (IOException e) {
		// }
		// try {
		// removeRegistyValue("", KEY_EQFDLL);
		// } catch (IOException e) {
		// }
		// try {
		// removeRegistyValue("Path", KEY_NONNODE_EQFDLL);
		// } catch (IOException e) {
		// }
		// try {
		// removeRegistyValue("", KEY_NONNODE_EQFDLL);
		// } catch (IOException e) {
		// }
		// try {
		// removeRegistyValue("", KEY_IBM_TRANSLATIONMANAGER);
		// } catch (IOException e) {
		// }
		// try {
		// removeRegistyValue("", KEY_VERSION);
		// } catch (IOException e) {
		// }
		// try {
		// removeRegistyValue("SEGCHK_TM_HOME", KEY_ENV);
		// } catch (IOException e) {
		// }
		// try {
		// removeRegistyValue("SEGCHK_HOME", KEY_ENV);
		// } catch (IOException e) {
		//
		// }
		// }
		//

	}

}
