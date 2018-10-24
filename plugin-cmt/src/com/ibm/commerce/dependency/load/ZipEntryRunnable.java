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

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This interface represents code that can be run on the ZIP entries of a ZIP
 * file.
 * 
 * @author Trent Hoeppner
 */
public interface ZipEntryRunnable {

	/**
	 * Processes the given entry.
	 * 
	 * @param zipFile
	 *            The file which contains the ZIP entry. This value cannot be
	 *            null.
	 * @param entry
	 *            The entry to process. This value cannot be null.
	 * 
	 * @return True to stop execution on other ZIP entries, false to continue
	 *         processing other ZIP entries.
	 * 
	 * @throws IOException
	 *             If an error occurs while reading the entry.
	 */
	boolean run(ZipFile zipFile, ZipEntry entry) throws IOException;

	/**
	 * Adds the properties set by this to the given context.
	 * 
	 * @param context
	 *            The context to add the properties to. This value cannot be
	 *            null.
	 */
	void addProperties(LoadingContext context);
}
