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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * StringModelFactory can generate {@link StringModel} instances.
 * 
 * @author Trent Hoeppner
 */
public class StringModelFactory implements ModelFactory<StringModel> {

	/**
	 * The number of bytes for the buffer when reading in files.
	 */
	private static final int READ_FILE_BUFFER_SIZE = 4096;

	/**
	 * Constructor for this.
	 */
	public StringModelFactory() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StringModel createModel(ValidatorResource resource) {
		String stringDoc = null;
		if (resource instanceof WorkingValidatorResource) {
			WorkingValidatorResource workingResource = (WorkingValidatorResource) resource;
			stringDoc = workingResource.getWorkingVersion();
		} else {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(openStream(resource)));
				StringBuffer data = new StringBuffer();
				char[] buf = new char[READ_FILE_BUFFER_SIZE];
				int charsRead = reader.read(buf);
				while (charsRead >= 0) {
					data.append(buf, 0, charsRead);
					charsRead = reader.read(buf);
				}
				stringDoc = data.toString();
			} catch (IOException e) {
				Debug.FRAMEWORK.log(e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Debug.FRAMEWORK.log(e);
					}
				}
			}
		}
		return new StringModel(stringDoc);
	}

	InputStream openStream(ValidatorResource resource) throws IOException {
		File file = resource.getFileAsFile();
		return new FileInputStream(file);
	}
}
