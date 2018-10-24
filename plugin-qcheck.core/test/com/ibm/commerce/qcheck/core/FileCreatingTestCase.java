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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

/**
 * FileCreatingTestCase can create temporary files and automatically destroy
 * them in {@link #tearDown()}.
 * 
 * @author Trent Hoeppner
 */
public abstract class FileCreatingTestCase extends TestCase {

	protected static final String LINE_SEPARATOR = System.getProperty("line.separator");

	protected List<File> tempFiles = new ArrayList<File>();

	protected Set<File> tempFilesToUnconditionallyDelete = new HashSet<File>();

	protected List<File> tempDirsToDelete = new ArrayList<File>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void tearDown() throws Exception {
		try {
			StringBuffer filesDeleted = new StringBuffer();
			for (File tempFile : tempFilesToUnconditionallyDelete) {
				try {
					delete(tempFile, null);
				} catch (IOException e) {
					filesDeleted.append("File " + tempFile + " could not be deleted, ignoring.\n");
				}
			}

			for (File tempDir : tempDirsToDelete) {
				delete(tempDir, filesDeleted);
			}

			for (File tempFile : tempFiles) {
				if (tempFile.exists()) {
					boolean deleted = tempFile.delete();
					if (!deleted && !tempFilesToUnconditionallyDelete.contains(tempFile)) {
						filesDeleted.append("File " + tempFile + " could not be deleted.\n");
					}
				}
			}

			if (filesDeleted.length() > 0) {
				fail(filesDeleted.toString());
			}
		} finally {
			tempFiles.clear();
			tempFilesToUnconditionallyDelete.clear();
			tempDirsToDelete.clear();
		}
	}

	protected File createTempFile(boolean create, boolean isDirectory) throws IOException {
		return createTempFile(create, isDirectory, "junk.vhd");
	}

	/**
	 * Creates a File object without creating the file, and adds it to the
	 * temporary files to delete later. This allows a test method to anticipate
	 * a file that will be created by the tested code, and to clean it up after.
	 *
	 * @param isDirectory
	 *            True indicates that a directory should be created, false
	 *            indicates a file.
	 * @param name
	 *            The name of the file or directory to create. Cannot be null or
	 *            empty.
	 *
	 * @return The temporary file. Will not be null.
	 *
	 * @throws IOException
	 *             If an error occurs when creating the file object.
	 */
	protected File createTempFileToDelete(boolean isDirectory, String name) throws IOException {
		File file = createTempFile(false, isDirectory, name);
		tempFilesToUnconditionallyDelete.add(file);
		return file;
	}

	protected File createTempDirWhichDeletesAllChildren(String name) throws IOException {
		File tempDir = createTempFile(true, true, true, false, name);
		tempDirsToDelete.add(0, tempDir);
		return tempDir;
	}

	protected File createTempFile(boolean create, boolean isDirectory, String name, String... lines)
			throws IOException {
		return createTempFile(create, isDirectory, false, false, name, lines);
	}

	protected File createTempFile(boolean create, boolean isDirectory, boolean makeParents, boolean absolute,
			String name, String... lines) throws IOException {
		return createTempFile(create, isDirectory, makeParents, absolute, false, name, lines);

	}

	protected File createTempFile(boolean create, boolean isDirectory, boolean makeParents, boolean absolute,
			boolean addBOM, String name, String... lines) throws IOException {
		File file = new File(name);

		if (absolute) {
			file = file.getAbsoluteFile();
		}

		if (create) {
			if (makeParents && file.getParentFile() != null && !file.getParentFile().exists()) {
				System.out.println("Creating " + file.getParent());
				createTempFile(true, true, true, absolute, file.getParent());
			}

			boolean created;
			if (!isDirectory) {
				// created = file.createNewFile();

				BufferedOutputStream out = null;
				Writer writer = null;
				try {
					out = new BufferedOutputStream(new FileOutputStream(file));
					if (addBOM) {
						out.write(0xEF);
						out.write(0xBB);
						out.write(0xBF);
					}
					if (lines != null && lines.length > 0) {
						writer = new OutputStreamWriter(out);
						out = null;
						for (String line : lines) {
							writer.write(line);
							writer.write(LINE_SEPARATOR);
						}
					}
					created = true;
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							// swallow to allow an exception inside the try to
							// be thrown
							System.out.println("Could not close " + file);
						}
					}
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException e) {
							// swallow to allow an exception inside the try to
							// be thrown
							System.out.println("Could not close " + file);
						}
					}
				}
			} else {
				created = file.mkdir();
			}

			boolean fileExists = file.exists();

			assertTrue("temp file '" + name + "' was not created. (exists = " + fileExists + ")", created);
		}

		// make the list in the reverse order that it was created so that
		// when we delete the files, we delete subdirectories and files
		// before parent directories
		tempFiles.add(0, file);

		return file;
	}

	public File createTempZipFile(boolean create, String zipName, File... files) throws IOException {
		return createTempZipFile(create, zipName, File.separatorChar, false, files);
	}

	public File createTempZipFile(boolean create, String zipName, String removePrefix, File... files)
			throws IOException {
		return createTempZipFile(create, zipName, File.separatorChar, false, removePrefix, files);
	}

	public File createTempZipFile(boolean create, String zipName, char slashChar, boolean startWithSlashes,
			File... files) throws IOException {
		return createTempZipFile(create, zipName, slashChar, startWithSlashes, "", files);
	}

	public File createTempZipFile(boolean create, String zipName, char slashChar, boolean startWithSlashes,
			String removePrefix, File... files) throws IOException {
		File zipFile = createTempFile(false, false, zipName);

		ZipOutputStream zipOut = null;
		try {
			zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
			for (File file : files) {
				String path = file.getPath();
				path = path.replace('/', '\\');
				path = path.replace('\\', slashChar);
				if (startWithSlashes) {
					path = slashChar + path;
				}
				if (file.isDirectory()) {
					path = path + slashChar;
				}

				if (path.startsWith(removePrefix)) {
					path = path.substring(removePrefix.length());
				}

				ZipEntry entry = new ZipEntry(path);
				entry.setSize(file.length());
				zipOut.putNextEntry(entry);

				if (file.isFile()) {
					BufferedInputStream in = null;
					try {
						in = new BufferedInputStream(new FileInputStream(file));
						int data = in.read();
						while (data > -1) {
							zipOut.write(data);
							data = in.read();
						}
						zipOut.closeEntry();
					} finally {
						if (in != null) {
							try {
								in.close();
							} catch (IOException e) {
								// swallow this so others can pass up
							}
						}
					}
				}
			}
		} finally {
			if (zipOut != null) {
				try {
					zipOut.close();
				} catch (IOException e) {
					System.out.println("Exception closing zip file: " + e.getMessage());
					// swallow this so others can pass up
				}
			}
		}

		return zipFile;
	}

	public URL createTempFileURL(boolean create, boolean isDirectory, String name, String... lines) throws IOException {
		File file = createTempFile(true, false, name, lines);

		URL url = toURL(file);

		return url;
	}

	public URL toURL(File file) throws MalformedURLException {
		String path = file.getAbsolutePath().replace('\\', '/');
		URL url = new URL("file:/" + path);
		return url;
	}

	public void assertEqualsContent(String message, String expectedContents, File file) throws IOException {
		assertEqualsContent(message, expectedContents, new FileInputStream(file));
	}

	public void assertEqualsContent(String message, String expectedContents, InputStream inputStream)
			throws IOException {
		StringBuffer buf = new StringBuffer();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(inputStream));
			int line = in.read();
			while (line >= 0) {
				buf.append((char) line);
				line = in.read();
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// swallow to allow other exceptions to get thrown up
					e.printStackTrace();
				}
			}
		}

		assertEquals(message, expectedContents, buf.toString());
	}

	public void assertEqualsContent(String message, InputStream expectedContents, InputStream inputStream)
			throws IOException {
		byte[] actualBytes = toByteArray(inputStream);
		byte[] expectedBytes = toByteArray(expectedContents);

		assertEquals(message + ": length not equal.", expectedBytes.length, actualBytes.length);
		for (int i = 0; i < expectedBytes.length; i++) {
			assertEquals(message + ": byte at index " + i + " not the same.", expectedBytes[i], actualBytes[i]);
		}
	}

	private byte[] toByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(inputStream);
			int data = in.read();
			while (data >= 0) {
				out.write(data);
				data = in.read();
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// swallow to allow other exceptions to get thrown up
					e.printStackTrace();
				}
			}
		}

		byte[] bytes = out.toByteArray();

		return bytes;
	}

	/**
	 * Deletes the given file from the file system. If the file is a directory,
	 * all the files and sub-directories underneath it will also be deleted.
	 *
	 * @param file
	 *            The file to delete. Cannot be null.
	 * @param buf
	 *            A buffer of the errors trying to delete the files. If this
	 *            value is null, an exception will be thrown if the file could
	 *            not be deleted.
	 *
	 * @throws IOException
	 *             If the buffer is null and the given file or file in a
	 *             sub-directory could not be deleted.
	 */
	public void delete(File file, StringBuffer buf) throws IOException {
		if (!file.exists()) {
			return;
		}

		if (file.isDirectory()) {
			File[] subFiles = file.listFiles();
			for (File subFile : subFiles) {
				delete(subFile, buf);
			}
		}

		boolean deleted = file.delete();
		if (!deleted) {
			String message = "File " + file + " could not be deleted.";
			if (buf != null) {
				buf.append(message).append("\n");
			} else {
				throw new IOException(message);
			}
		} else {
			tempFiles.remove(file);
		}
	}

	/**
	 * Replaces #n with {@link #LINE_SEPARATOR}, to avoid the RSAR errors. All
	 * parts will be appended using a StringBuffer for efficiency.
	 *
	 * @param parts
	 *            The parts of the string to replace line separators. Cannot be
	 *            null, but may be empty.
	 *
	 * @return The complete string with OS line separators. Will not be null,
	 *         but may be empty.
	 */
	public static String sep(String... parts) {
		StringBuffer buf = new StringBuffer();
		for (String part : parts) {
			buf.append(part.replace("#n", LINE_SEPARATOR));
		}

		return buf.toString();
	}

}
