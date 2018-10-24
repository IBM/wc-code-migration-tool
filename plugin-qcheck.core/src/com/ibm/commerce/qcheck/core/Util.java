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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>Util</code> is a utility class to provide some methods that are
 * generally useful, such as logging.
 * 
 * @author Trent Hoeppner
 */
public final class Util {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	/**
	 * Constructor for this. Private to prevent instantiation.
	 */
	private Util() {
		// do nothing
	}

	/**
	 * Checks that the given value is not null, and if it is, a
	 * NullPointerException is thrown. This is intended to check method
	 * parameters before using them, usually at the top of the method body.
	 *
	 * @param paramName
	 *            The name of the parameter, as declared in the Java file.
	 *            Cannot be null or empty.
	 * @param value
	 *            The parameter's value. If null, a NullPointerException will be
	 *            thrown.
	 */
	public static void checkNotNull(String paramName, Object value) {
		if (value == null) {
			throw new NullPointerException("Parameter " + paramName + " cannot be null.");
		}
	}

	/**
	 * Checks that the given value is not null or empty, and if it is, a
	 * NullPointerException or IllegalArgumentException is thrown. This is
	 * intended to check method parameters before using them, usually at the top
	 * of the method body.
	 *
	 * @param paramName
	 *            The name of the parameter, as declared in the Java file.
	 *            Cannot be null or empty.
	 * @param value
	 *            The parameter's value. If null, a NullPointerException will be
	 *            thrown. If empty, an IllegalArgumentException will be thrown.
	 */
	public static void checkNotNullOrEmpty(String paramName, String value) {
		checkNotNull(paramName, value);
		if (value.isEmpty()) {
			throw new IllegalArgumentException("Parameter " + paramName + " cannot be empty.");
		}
	}

	/**
	 * Copies the contents of the given soruce diretory to the target directory.
	 *
	 * @param sourceDir
	 *            The source directory to copy. Cannot be null, and must exist.
	 * @param targetDir
	 *            The target directory to copy. Cannot be null, and must exist.
	 *
	 * @throws IOException
	 *             If an error occurs while copying one of the files or
	 *             directories.
	 */
	public static void copyDir(File sourceDir, File targetDir) throws IOException {
		File[] children = sourceDir.listFiles();
		if (children == null) {
			return;
		}

		for (File child : children) {
			File targetChild = new File(targetDir, child.getName());
			if (child.isDirectory()) {
				targetChild.mkdir();
				copyDir(child, targetChild);
			} else {
				copyFile(child, targetChild);
			}
		}
	}

	/**
	 * Copies the source file to the target file. If the target already exists,
	 * it will be overwritten.
	 *
	 * @param source
	 *            The source file to copy. Cannot be null, and must be an
	 *            existing file.
	 * @param target
	 *            The target file to copy. Cannot be null.
	 *
	 * @throws IOException
	 *             If an error occurs while copying the file.
	 */
	public static void copyFile(File source, File target) throws IOException {
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try {
			in = new BufferedInputStream(new FileInputStream(source));
			out = new BufferedOutputStream(new FileOutputStream(target));

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
					// swallow to allow an exception the try block to escape
				}
			}

			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// swallow to allow an exception the try block to escape
				}
			}
		}
	}

	/**
	 * Runs the given command with the specified environment variables in the
	 * given working directory, and returns immediately.
	 *
	 * @param command
	 *            The command to run. Cannot be null or empty.
	 * @param environmentVariables
	 *            A mapping from variable names to values. Cannot be null, but
	 *            may be empty.
	 * @param workingDir
	 *            The working directory in which the command will be run. Cannot
	 *            be null or empty.
	 *
	 * @return A handle to the process that is being executed. Will not be null.
	 *
	 * @throws IOException
	 *             If there was an error executing the command.
	 */
	public static Process runButDontWait(String command, Map<String, String> environmentVariables, File workingDir)
			throws IOException {
		String[] envp;
		if (environmentVariables != null) {
			envp = new String[environmentVariables.size()];
			int i = 0;
			for (String var : environmentVariables.keySet()) {
				envp[i] = var + "=" + environmentVariables.get(var);
				i++;
			}
		} else {
			envp = null;
		}

		Process process = Runtime.getRuntime().exec(command, envp, workingDir);
		return process;
	}

	/**
	 * Runs the given command in the given working directory, and blocks until
	 * execution completes.
	 *
	 * @param command
	 *            The command to run. Cannot be null or empty.
	 * @param workingDir
	 *            The working directory in which the command will be run. Cannot
	 *            be null or empty.
	 *
	 * @return The output of the command execution. This includes standard
	 *         output stream but not standard error stream. Will not be null,
	 *         but may be empty.
	 *
	 * @throws IOException
	 *             If there was an error executing the command, or the error
	 *             code was non-zero.
	 */
	public static String runUntilCompletion(String command, File workingDir) throws IOException {
		return runUntilCompletion(command, new HashMap<String, String>(), workingDir, true);
	}

	/**
	 * Runs the given command in the given working directory, and blocks until
	 * execution completes, optionally throwing an exception on a non-zero exit
	 * code.
	 *
	 * @param command
	 *            The command to run. Cannot be null or empty.
	 * @param workingDir
	 *            The working directory in which the command will be run. Cannot
	 *            be null or empty.
	 * @param exceptionOnError
	 *            True indicates that an exception will be thrown for a non-zero
	 *            exit code, false indicates that an execution will not be
	 *            thrown.
	 *
	 * @return The output of the command execution. This includes standard
	 *         output stream but not standard error stream. Will not be null,
	 *         but may be empty.
	 *
	 * @throws IOException
	 *             If there was an error executing the command, or the error
	 *             code was non-zero and <code>exceptionOnError</code> is true.
	 */
	public static String runUntilCompletion(String command, File workingDir, boolean exceptionOnError)
			throws IOException {
		return runUntilCompletion(command, new HashMap<String, String>(), workingDir, exceptionOnError);
	}

	/**
	 * Runs the given command with the specified environment variables in the
	 * given working directory, and blocks until execution completes, optionally
	 * throwing an exception on a non-zero exit code.
	 *
	 * @param command
	 *            The command to run. Cannot be null or empty.
	 * @param environmentVariables
	 *            A mapping from variable names to values. Cannot be null, but
	 *            may be empty.
	 * @param workingDir
	 *            The working directory in which the command will be run. Cannot
	 *            be null or empty.
	 * @param exceptionOnError
	 *            True indicates that an exception will be thrown for a non-zero
	 *            exit code, false indicates that an execution will not be
	 *            thrown.
	 *
	 * @return The output of the command execution. This includes standard
	 *         output stream but not standard error stream. Will not be null,
	 *         but may be empty.
	 *
	 * @throws IOException
	 *             If there was an error executing the command, or the error
	 *             code was non-zero and <code>exceptionOnError</code> is true.
	 */
	public static String runUntilCompletion(String command, Map<String, String> environmentVariables, File workingDir,
			boolean exceptionOnError) throws IOException {
		Process process = runButDontWait(command, environmentVariables, workingDir);

		String output = null;
		boolean safeTermination = true;

		BufferedReader stdOut = null;
		BufferedReader stdErr = null;

		try {
			stdOut = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getInputStream())));
			stdErr = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getErrorStream())));
			final StringBuffer stdOutBuffer = new StringBuffer();
			final StringBuffer stdErrBuffer = new StringBuffer();

			ReadOutputRunnable stdOutRunnable = new ReadOutputRunnable(stdOut, stdOutBuffer);
			ReadOutputRunnable stdErrRunnable = new ReadOutputRunnable(stdErr, stdErrBuffer);

			Thread stdOutThread = new Thread(stdOutRunnable);
			stdOutThread.start();
			Thread stdErrThread = new Thread(stdErrRunnable);
			stdErrThread.start();

			while ((stdOutThread.isAlive() || stdErrThread.isAlive()) && isRunning(process)) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// just keep checking
				}
			}

			if (stdOutThread.isAlive()) {
				safeTermination = false;
				stdOutThread.stop();
			}

			if (stdErrThread.isAlive()) {
				safeTermination = false;
				stdErrThread.stop();
			}

			output = stdOutBuffer.toString();
			String err = stdErrBuffer.toString();
			Debug.VALIDATOR.log(output);
			if (process.exitValue() != 0 && exceptionOnError) {
				throw new IOException(
						"Execution of " + command + " failed with exit value " + isRunning(process) + ": " + err);
			}
		} finally {
			if (stdOut != null && safeTermination) {
				try {
					stdOut.close();
				} catch (IOException e) {
					// swallow to allow exceptions in try block to escape
				}
			}

			if (stdErr != null && safeTermination) {
				try {
					stdErr.close();
				} catch (IOException e) {
					// swallow to allow exceptions in try block to escape
				}
			}
		}

		return output;
	}

	/**
	 * Returns whether the given process is still running.
	 *
	 * @param process
	 *            The process to check. Cannot be null.
	 *
	 * @return True if the process is running, false otherwise.
	 */
	private static boolean isRunning(Process process) {
		boolean running;
		try {
			process.exitValue();
			running = false;
		} catch (IllegalThreadStateException e) {
			running = true;
		}

		return running;
	}

	/**
	 * Consumes output from a stream. This is used to consume output from
	 * <code>stdout</code> and <code>stderr</code> in separate threads, so that
	 * the main thread is not blocked waiting for either one of them.
	 */
	private static class ReadOutputRunnable implements Runnable {

		private BufferedReader reader;

		private StringBuffer buffer;

		private IOException e;

		/**
		 * Constructor for this.
		 *
		 * @param reader
		 *            The reader to consume. Cannot be null.
		 * @param buffer
		 *            The buffer to append output to. Cannot be null.
		 */
		private ReadOutputRunnable(BufferedReader reader, StringBuffer buffer) {
			this.reader = reader;
			this.buffer = buffer;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			try {
				String line = reader.readLine();
				while (line != null) {
					if (line != null) {
						buffer.append(line).append(LINE_SEPARATOR);
						line = reader.readLine();
					}
				}

				if (line != null) {
					buffer.append(line).append(LINE_SEPARATOR);
					line = reader.readLine();
				}
			} catch (IOException e) {
				this.e = e;
			}
		}
	}

}
