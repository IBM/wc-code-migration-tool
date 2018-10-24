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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

/**
 * This class tests the {@link Configuration} class.
 * 
 * @author Trent Hoeppner
 */
public class ConfigurationTest extends TestCase {

	public void testConstructorIfCommandFilesNullExpectException() {
		try {
			new Configuration(null, Arrays.asList(new File("testDir")), null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	public void testConstructorIfCommandFilesEmptyExpectException() {
		try {
			new Configuration(Collections.emptyList(), Arrays.asList(new File("testData\\testDir")), null);
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	public void testConstructorIfCommandFileDoesNotExistExpectException() {
		try {
			new Configuration(Arrays.asList(new File("testData\\notExistInput.txt")),
					Arrays.asList(new File("testData\\testDir")), null);
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	public void testConstructorIfCommandFileIsADirectoryExpectException() {
		try {
			new Configuration(Arrays.asList(new File("testData\\notAFile")),
					Arrays.asList(new File("testData\\testDir")), null);
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	public void testConstructorIfSourceDirsNullExpectException() {
		try {
			new Configuration(Arrays.asList(new File("testData\\input.txt")), null, null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	public void testConstructorIfSourceDirsEmptyExpectException() {
		try {
			new Configuration(Arrays.asList(new File("testData\\input.txt")), Collections.emptyList(), null);
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	public void testConstructorIfSourceDirDoesNotExistExpectException() {
		try {
			new Configuration(Arrays.asList(new File("testData\\input.txt")),
					Arrays.asList(new File("testData\\notExistingDir")), null);
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	public void testConstructorIfSourceDirIsAFileExpectException() {
		try {
			new Configuration(Arrays.asList(new File("testData\\input.txt")),
					Arrays.asList(new File("testData\\notADir.txt")), null);
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	public void testConstructorIfParamsOkExpectCanBeRetrieved() {
		List<File> commandFileList = Arrays.asList(new File("testData\\input.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\sourceDir"));
		Configuration context = new Configuration(commandFileList, sourceDirList, null);
		assertEquals("command file list is wrong.", commandFileList, context.getCommandFileList());
		assertEquals("source dir list is wrong.", sourceDirList, context.getSourceDirList());
	}

	public void testLoadIfCommandFileEmptyExpectException() throws Exception {
		List<File> commandFileList = Arrays.asList(new File("testData\\empty.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\sourceDir"));
		Configuration context = new Configuration(commandFileList, sourceDirList, null);
		try {
			context.load();
			fail("RuntimeException was not thrown.");
		} catch (RuntimeException e) {
			// success
		}
	}

	public void testLoadIfCommandFileHasEmptyPatternsExpectCommandListEmpty() throws Exception {
		List<File> commandFileList = Arrays.asList(new File("testData\\input.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\sourceDir"));
		Configuration context = new Configuration(commandFileList, sourceDirList, null);
		context.load();
		assertEquals("pattern list is not empty.", true, context.getPatterns().isEmpty());
	}

	public void testLoadIfCommandFileContainsClassRefCommandExpectInCommandList() throws Exception {
		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\sourceDir"));
		Configuration context = new Configuration(commandFileList, sourceDirList, null);
		context.load();
		assertEquals("pattern list size is wrong.", 1, context.getPatterns().size());
		Pattern pattern = context.getPatterns().get(0);
		assertEquals("pattern 0 action name is wrong.", "classRefReplace", pattern.getAction().getName());
	}

}
