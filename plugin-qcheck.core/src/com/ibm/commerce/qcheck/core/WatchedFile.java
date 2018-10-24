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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * WatchedFile represents a file or URL that is watched and reloaded as needed.
 * This class allows the user to treat both files and URLs as the same for the
 * purposes of reading.
 *
 * @param T
 *            The object that is loaded in the file.
 * 
 * @author Trent Hoeppner
 */
public abstract class WatchedFile<T> {

	/**
	 * The file that contains the configuration data. Either this or
	 * {@link #url} must be non-null, and the other must be null.
	 */
	private File file;

	/**
	 * The URL that points to a resource that contains configuration data.
	 * Either this or {@link #file} must be non-null, and the other must be
	 * null.
	 */
	private URL url;

	/**
	 * The time stamp of the configuration file that is currently in memory.
	 * This is used to determine if the configuration in memory is in sync with
	 * the configuration on disk.
	 */
	private long inMemoryLastModified;

	/**
	 * The object which represents the required information in the file.
	 */
	private T loadedObject;

	/**
	 * Constructor for ConfigFile.
	 *
	 * @param newFile
	 *            The file that contains the configuration data. Cannot be null.
	 */
	public WatchedFile(File newFile) {
		this.file = newFile;
	}

	/**
	 * Constructor for ConfigFile.
	 *
	 * @param newURL
	 *            The URL that points to a resource that contains the
	 *            configuration data. Cannot be null.
	 */
	public WatchedFile(URL newURL) {
		String path = newURL.getPath();
		if (newURL.getProtocol().equals("file") && path != null) {
			this.file = new File(path);
		} else {
			this.url = newURL;
		}
	}

	/**
	 * Returns the object that contains information parsed from the file being
	 * watched.
	 *
	 * @return The information object obtained from the file.
	 */
	public T getLoadedObject() {
		return loadedObject;
	}

	/**
	 * Sets the object that contains the information from the parsed file. This
	 * method should normally be called from {@link #syncWithSystem()}.
	 *
	 * @param loadedObject
	 *            The object to be loaded. If null, then {@link #changed()} will
	 *            return true to force a reload when
	 *            {@link #ensureLatestLoaded()} is called.
	 */
	public void setLoadedObject(T loadedObject) {
		this.loadedObject = loadedObject;
	}

	/**
	 * Returns a new stream that can be used to read the configuration data.
	 *
	 * @return A new stream that can be used to read the configuration data.
	 *         Will not be null.
	 *
	 * @throws IOException
	 *             If there was an error finding or reading the file or
	 *             resource.
	 */
	public InputStream openInputStream() throws IOException {
		InputStream in;
		if (file != null) {
			in = new FileInputStream(file);
		} else if (url != null) {
			in = url.openStream();
		} else {
			throw new IllegalStateException("This does not represent an internal or external file.");
		}

		return in;
	}

	/**
	 * Returns whether this file has been changed on disk.
	 *
	 * @return True if {@link #getLoadedObject()} returns null or the file has
	 *         changed since it was last loaded, false otherwise.
	 */
	public boolean changed() {
		return loadedObject == null || lastModified() > inMemoryLastModified;
	}

	/**
	 * Checks if the file has {@link #changed()}, and if so, reloads it. This
	 * method should always be called instead of {@link #syncWithSystem()},
	 * since the time stamp will not be updated with that method.
	 */
	public void ensureLatestLoaded() {
		if (changed()) {
			syncWithSystem();
			inMemoryLastModified = lastModified();
		}
	}

	/**
	 * Loads the configuration from the file. When loading definitions for
	 * validators, if a validator with a name has already been loaded, that
	 * valiator's definition will not be overwritten by this method.
	 * <p>
	 * This method should not be called by API users, since it will not update
	 * the time stamp which is used to detect changes. Instead, call
	 * {@link #ensureLatestLoaded()}.
	 */
	protected abstract void syncWithSystem();

	/**
	 * Returns the name of the file or URL.
	 *
	 * @return A name for this which can be used for debugging purposes. This
	 *         value will not be null or empty.
	 */
	protected String getName() {
		String name;
		if (file != null) {
			name = file.getAbsolutePath();
		} else if (url != null) {
			name = url.toExternalForm();
		} else {
			throw new IllegalStateException("This does not represent an internal or external file.");
		}

		return name;
	}

	/**
	 * Returns the last modified time stamp for this configuration. If a URL is
	 * used, the file is assumed to be in an internal bundle, and the time stamp
	 * will always be the same.
	 *
	 * @return The time stamp when the configuration data was last modified.
	 */
	private long lastModified() {
		long in;
		if (file != null) {
			in = file.lastModified();
		} else if (url != null) {
			in = 0L;
		} else {
			throw new IllegalStateException("This does not represent an internal or external file.");
		}

		return in;
	}
}
