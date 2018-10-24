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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ibm.commerce.dependency.task.Task;

/**
 * This class loads a file into memory from an input stream.
 * 
 * @author Trent Hoeppner
 */
public class LoadFromInputStreamTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(Arrays.asList(Name.INPUT_STREAM));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.TEXT_CONTENT));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadFromInputStreamTask(String name, LoadingContext context) {
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
		InputStream in = context.get(Name.INPUT_STREAM);

		String content = null;
		if (in != null) {
			content = loadContent(in);
		}

		context.put(Name.TEXT_CONTENT, content);
	}

	/**
	 * Loads from the given stream and returns its contents as a string. The
	 * input stream will be closed after this method exits.
	 *
	 * @param in
	 *            The stream with the file content to load. Cannot be null.
	 *
	 * @return The contents of the stream. Will not be null, but may be empty.
	 *
	 * @throws IOException
	 *             If there was an error reading the stream.
	 */
	private String loadContent(InputStream in) throws IOException {
		StringBuffer buf = new StringBuffer();

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in));
			int nextChar = reader.read();
			while (nextChar > -1) {
				buf.append((char) nextChar);
				nextChar = reader.read();
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// we don't want to interrupt the flow if it fails to close
					e.printStackTrace();
				}
			}
		}

		return buf.toString();
	}

}