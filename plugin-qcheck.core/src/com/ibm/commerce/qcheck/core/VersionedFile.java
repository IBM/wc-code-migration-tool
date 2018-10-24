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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;

/**
 * This class represents a file with a local version and a version on a server.
 * The server version is updated periodically, and the local version needs to be
 * checked for updates.
 * 
 * @author Trent Hoeppner
 */
public class VersionedFile {

	/**
	 * The format of time.
	 */
	private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy.MM.dd-HH.mm");

	private VersionFile serverVersion;

	private VersionFile localVersion;

	private VersionFile serverFile;

	private VersionFile localFile;

	public VersionedFile(String serverBase, String localBase, String versionName, String fileName) {
		serverVersion = createVersionFile(serverBase, versionName);
		localVersion = createVersionFile(localBase, versionName);
		serverFile = createVersionFile(serverBase, fileName);
		localFile = createVersionFile(localBase, fileName);
	}

	private VersionFile createVersionFile(String base, String name) {
		URL url = createURL(base + "/" + name);
		VersionFile versionFile = new VersionFile(url);
		return versionFile;
	}

	private URL createURL(String urlString) {
		URL url;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e1) {
			Debug.FRAMEWORK.log(e1);
			throw new IllegalStateException("Could not create a URL with " + urlString + ".");
		}
		return url;
	}

	public void downloadIfOutOfDate() {
		VersionedFileStatus status = compareVersions();
		if (status == VersionedFileStatus.OUT_OF_DATE) {
			download(serverVersion, localVersion);
			download(serverFile, localFile);
		}
	}

	private void download(VersionFile server, VersionFile local) {
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try {
			in = new BufferedInputStream(server.openInputStream());
			out = new BufferedOutputStream(new FileOutputStream(new File(local.getName())));

			byte[] buf = new byte[20 * 1024];
			int length = in.read(buf);
			while (length >= 0) {
				out.write(buf, 0, length);
				length = in.read(buf);
			}
		} catch (IOException e) {
			Debug.FRAMEWORK.log(e, "Error downloading file ", server.getName(), " to ", local.getName());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Debug.FRAMEWORK.log(e, "Error closing stream for file ", server.getName(), ".");
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					Debug.FRAMEWORK.log(e, "Error closing stream for file ", local.getName(), ".");
				}
			}
		}
	}

	/**
	 * This method compares the current version file to the server version file.
	 *
	 * @return The status which represent if there are available updates or not.
	 *         Will not be null.
	 */
	private VersionedFileStatus compareVersions() {
		serverVersion.ensureLatestLoaded();
		localVersion.ensureLatestLoaded();

		long serverTime = serverVersion.getLoadedObject();
		long clientTime = localVersion.getLoadedObject();

		if (serverTime > clientTime) {
			// There are new available updates
			return VersionedFileStatus.OUT_OF_DATE;
		}

		return VersionedFileStatus.UP_TO_DATE;
	}

	private static enum VersionedFileStatus {
		UP_TO_DATE, ERROR, OUT_OF_DATE;
	}

	private static class VersionFile extends WatchedFile<Long> {

		public VersionFile(URL newURL) {
			super(newURL);
		}

		@Override
		protected void syncWithSystem() {
			BufferedInputStream in = null;
			try {
				in = new BufferedInputStream(openInputStream());
				Properties versionProperties = new Properties();
				versionProperties.load(in);
				String timeString = versionProperties.getProperty("timestamp");
				long time = 0;
				if (timeString != null) {
					time = TIMESTAMP_FORMAT.parse(timeString).getTime();
				}
				setLoadedObject(time);
			} catch (IOException e) {
				Debug.FRAMEWORK.log(e, "Version file ", getName(), " could not be opened.");
				setLoadedObject(0L);
			} catch (ParseException e) {
				Debug.FRAMEWORK.log(e, "Version file ", getName(), " does not contain a valid timestamp.");
				setLoadedObject(0L);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						Debug.FRAMEWORK.log(e, "Error closing stream for file ", getName(), ".");
					}
				}
			}
		}

	}
}
