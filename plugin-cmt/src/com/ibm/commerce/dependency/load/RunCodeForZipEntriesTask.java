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

import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.commerce.dependency.task.Task;

/**
 * This class runs a {@link ZipEntryRunnable} for each ZipEntry in a ZipFile.
 * The ZipFile will be opened and closed by this task.
 * 
 * @author Trent Hoeppner
 */
public class RunCodeForZipEntriesTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(Arrays.asList(Name.FILE, Name.ZIP_ENTRY_RUNNABLE));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.ZIP_ENTRY_RUNNABLE_DONE));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public RunCodeForZipEntriesTask(String name, LoadingContext context) {
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
		File file = context.get(Name.FILE);
		ZipEntryRunnable runnable = context.get(Name.ZIP_ENTRY_RUNNABLE);

		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
			while (enumeration.hasMoreElements()) {
				ZipEntry entry = enumeration.nextElement();
				if (runnable.run(zipFile, entry)) {
					break;
				}
			}
		} finally {
			if (zipFile != null) {
				zipFile.close();
			}
		}

		runnable.addProperties(context);

		context.put(Name.ZIP_ENTRY_RUNNABLE_DONE, true);
	}

}