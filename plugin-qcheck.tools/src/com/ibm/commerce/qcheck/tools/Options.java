package com.ibm.commerce.qcheck.tools;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.EclipseUtil;
import com.ibm.commerce.qcheck.core.WatchedFile;

/**
 * Options represents the contents of a generic options file.
 * 
 * @author Trent Hoeppner
 */
public final class Options {

	/**
	 * The file which contains the options settings.
	 */
	private static final OptionsFile OPTIONS_FILE = new OptionsFile();

	/**
	 * Constructor for Options.
	 */
	private Options() {
		// do nothing
	}

	/**
	 * Makes sure that the most current options are loaded from disk.
	 */
	public static void ensureLoaded() {
		OPTIONS_FILE.ensureLatestLoaded();
	}

	/**
	 * Returns a mapping from option names to values.
	 *
	 * @return A mapping from option names to values. Each name is defined in an
	 *         {@link Attributes} <code>enum</code> value. Will not be null, but
	 *         may be empty.
	 */
	private static Map<String, Object> getMap() {
		return OPTIONS_FILE.getLoadedObject();
	}

	/**
	 * Attributes defines what the types of values that may be used.
	 */
	public static enum Attributes {

		/**
		 * A String option which indicates the location of the update site.
		 */
		UPDATE_URL("updateURL") {

			@Override
			String parse(String value) {
				String result = value;
				if (value == null) {
					result = "http://wcrad.torolab.ibm.com/validation";
				}

				return result;
			}
		},

		/**
		 * A Short option which indicates the number of seconds between checks.
		 */
		UPDATE_FREQUENCY("updateFrequency") {

			@Override
			Short parse(String value) {
				short result;
				try {
					result = Short.parseShort(value);
				} catch (NumberFormatException e) {
					result = (short) (15 * 60);
				}

				if (result <= 0) {
					result = (short) (15 * 60);
				}

				return result;
			}
		},

		/**
		 * A Boolean option which indicates if the as-you-type button is
		 * currently toggled on or off.
		 */
		AS_YOU_TYPE_BUTTON_ON("asYouTypeOn") {

			@Override
			Boolean parse(String value) {
				return Boolean.valueOf(value);
			}
		},

		/**
		 * A Boolean option which indicates if the build button is currently
		 * toggled on or off.
		 */
		BUILDER_BUTTON_ON("builderOn") {

			@Override
			Boolean parse(String value) {
				return Boolean.valueOf(value);
			}
		};

		/**
		 * The name to be used in the options file. This value will never be
		 * null or empty.
		 */
		private String name;

		/**
		 * Constructor for Attributes.
		 *
		 * @param name
		 *            The name of this attribute in the Options file. Cannot be
		 *            null or empty.
		 */
		private Attributes(String name) {
			this.name = name;
		}

		/**
		 * Returns the current value for this option. Care must be taken to
		 * declare the correct return type, since this will be used internally
		 * to cast the value.
		 *
		 * @param <T>
		 *            The type for this attribute.
		 *
		 * @return The value of this attribute in the options file. This value
		 *         will be null if the value is not defined in the file.
		 */
		public <T> T getValue() {
			String value = (String) getMap().get(name);
			return parse(value);
		}

		/**
		 * Sets the current value for this option. The given value's
		 * {@link #toString()} method will be used before writing (if value is
		 * not null). Calling this method will cause the options file to be
		 * rewritten.
		 *
		 * @param <T>
		 *            The type for this attribute.
		 *
		 * @param value
		 *            The value to set. If null, the value will be undefined.
		 */
		public <T> void setValue(T value) {
			String putValue = null;
			if (value != null) {
				putValue = value.toString();
			}

			getMap().put(name, putValue);

			OPTIONS_FILE.writeProperties();
		}

		/**
		 * Converts the given string into a value of the given type and returns
		 * it.
		 *
		 * @param <T>
		 *            The type for this attribute.
		 *
		 * @param value
		 *            The string form of the attribute. May be null or empty.
		 *
		 * @return The converted value, in the form expected by this attribute.
		 *         May be null.
		 */
		abstract <T> T parse(String value);
	}

	/**
	 * OptionsFile represents the options file called
	 * <code>dropins/wizard/userconfig/options.properties</code>.
	 */
	private static class OptionsFile extends WatchedFile<Map<String, Object>> {

		/**
		 * The URL of the options file.
		 */
		private final static URL PROPERTIES_URL = EclipseUtil.getDefault()
				.getDropinsURL("wizard/userconfig/options.properites");

		/**
		 * Constructor for OptionsFile.
		 */
		public OptionsFile() {
			super(PROPERTIES_URL);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected synchronized void syncWithSystem() {
			Properties properties = new Properties();
			InputStream in = null;
			try {
				in = openInputStream();
				properties.load(in);
			} catch (IOException e) {
				// if load failed, do nothing
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						// swallow to allow main exception to escape
					}
				}
			}

			Map<String, Object> map = new HashMap<String, Object>();
			for (String name : properties.stringPropertyNames()) {
				Object value = properties.get(name);
				map.put(name, value);
			}

			setLoadedObject(map);
		}

		/**
		 * Rewrites the options file based on the values in the
		 * {@link #getLoadedObject() loaded object}.
		 */
		synchronized void writeProperties() {
			File file = new File(PROPERTIES_URL.getPath());
			Properties properties = new Properties();
			try {
				properties.putAll(getLoadedObject());
				properties.store(new FileOutputStream(file), null);
			} catch (IOException e) {
				Debug.FRAMEWORK.log(e);
			}
		}

	}
}
