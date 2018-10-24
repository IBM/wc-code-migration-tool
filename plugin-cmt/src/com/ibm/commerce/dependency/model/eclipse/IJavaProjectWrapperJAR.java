package com.ibm.commerce.dependency.model.eclipse;

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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.ibm.commerce.dependency.model.JavaItemUtil;

/**
 * This class represents a project from a JAR file (as opposed to an Eclipse
 * object implementation). The JAR will be unzipped before being used.
 * 
 * @author Trent Hoeppner
 */
public class IJavaProjectWrapperJAR implements IJavaProjectWrapper {

	private File jar;

	private String name;

	private File unzippedRootDir;

	private IJavaProjectWrapperExternal external;

	private boolean binary;

	public IJavaProjectWrapperJAR(File jar) {
		if (jar == null) {
			throw new NullPointerException("jar cannot be null.");
		}

		this.jar = jar;
		this.name = jar.getName().substring(0, jar.getName().length() - 4);

	}

	public IJavaProjectWrapperJAR(File jar, boolean binary) {
		this(jar);
		this.binary = binary;
	}

	public File getJAR() {
		return jar;
	}

	public IJavaProjectWrapperExternal getUnpackedWrapper() {
		return external;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public File getManifestFile() {
		ensureUnzipped();

		return external.getManifestFile();
	}

	@Override
	public List<IPackageFragmentWrapper> getFragments() {
		ensureUnzipped();

		return external.getFragments();
	}

	private void ensureUnzipped() {
		if (external == null) {
			File userDir = new File(System.getProperty("user.home"));
			File tempDir = new File(userDir, "dependencyAnalysisTemp");
			File unzippedRootDir = new File(tempDir, name);

			// if (unzippedRootDir.exists()) {
			// try {
			// delete(unzippedRootDir, false);
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			// }
			//
			// File srcDir = new File(unzippedRootDir, "src");
			//
			// try {
			// unzip(jar, srcDir);
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			//
			// external = new IJavaProjectWrapperExternal(unzippedRootDir);

			if (unzippedRootDir.exists()) {
				external = new IJavaProjectWrapperExternal(unzippedRootDir, binary);
			} else {
				File srcDir = new File(unzippedRootDir, "src");

				try {
					unzip(jar, srcDir);
				} catch (IOException e) {
					e.printStackTrace();
				}

				external = new IJavaProjectWrapperExternal(unzippedRootDir, binary);
			}

			if (binary) {
				// we also do classloading here so we can use reflection later
				JavaItemUtil.addClassLoader(jar);
			}
		}
	}

	public static void unzip(File zipFile, File dir) throws IOException {
		if (!dir.exists()) {
			boolean result = dir.mkdirs();
			if (!result) {
				throw new IOException("Could not create dirs for " + dir + ".");
			}
		}

		ZipInputStream in = null;
		try {
			in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));

			byte[] buffer = new byte[10240];

			ZipEntry currentEntry = in.getNextEntry();
			while (currentEntry != null) {
				File outFile = new File(dir, currentEntry.getName());

				if (currentEntry.isDirectory()) {
					in.closeEntry();
					currentEntry = in.getNextEntry();
					continue;
				}

				File parentFile = outFile.getParentFile();
				if (!parentFile.exists()) {
					boolean result = parentFile.mkdirs();
					if (!result) {
						throw new IOException("Could not create dirs for " + parentFile + ".");
					}
				}

				OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
				int length = in.read(buffer);
				while (length >= 0) {
					out.write(buffer, 0, length);
					length = in.read(buffer);
				}
				out.close();

				in.closeEntry();
				currentEntry = in.getNextEntry();
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// swallow to allow main exception to escape
				}
			}
		}
	}

	/**
	 * Deletes the given file from the file system. If the file is a directory,
	 * all the files and sub-directories underneath it will also be deleted.
	 *
	 * @param file
	 *            The file to delete. Cannot be null.
	 * @param emptyOnly
	 *            True indicates that only empty directories will be deleted,
	 *            false indicates that non-empty directories will have their
	 *            contents deleted first.
	 *
	 * @throws IOException
	 *             If the given file or file in a sub-directory could not be
	 *             deleted.
	 */
	public static void delete(File file, boolean emptyOnly) throws IOException {
		if (!file.exists()) {
			return;
		}

		if (file.isDirectory()) {
			File[] subFiles = file.listFiles();
			if (!emptyOnly) {
				for (File subFile : subFiles) {
					delete(subFile, false);
				}
			} else {
				return;
			}
		}

		boolean deleted = file.delete();
		if (!deleted) {
			throw new IOException("Could not delete " + file.getAbsolutePath() + ".");
		}
	}

	@Override
	public boolean isBinary() {
		return binary;
	}
}
