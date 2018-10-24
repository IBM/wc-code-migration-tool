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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.Param;

/**
 * This class represents a PII file and the attributes that are described by the
 * output of a CHKPII command. This class is created from {@link CheckPIIOutput}
 * 
 * @author Trent Hoeppner
 */
public class PIIFile implements Comparable<PIIFile> {

	private final static Pattern namePattern = Pattern.compile("(.+?)_([A-Za-z]{2})(_([A-Za-z]{2}))?\\.(.+)");

	private final static Pattern dirPattern = Pattern.compile("(.+)\\\\([A-Za-z]{2})(_([A-Za-z]{2}))?\\\\");

	private String dir;

	private String baseName;

	private boolean variantInParentDir;

	private Map<Locale, Map<String, String>> variantToPropertiesMap;

	public PIIFile(String dir, String baseName, boolean variantInParentDir) {
		Param.notNull(dir, "dir");
		Param.notNullOrEmpty(baseName, "baseName");

		this.dir = dir;
		this.baseName = baseName;
		this.variantInParentDir = variantInParentDir;
		variantToPropertiesMap = new LinkedHashMap<Locale, Map<String, String>>();
	}

	public static PIIFile create(String dir, String baseName) {
		boolean variantInParent;
		Matcher baseNameMatcher = namePattern.matcher(baseName);
		String language = null;
		String country = null;
		if (baseNameMatcher.matches()) {
			language = baseNameMatcher.group(2).toLowerCase();
			country = baseNameMatcher.group(4);
			baseName = baseNameMatcher.group(1) + "." + baseNameMatcher.group(5);
			variantInParent = false;
		} else {
			Matcher dirMatcher = dirPattern.matcher(dir);
			if (dirMatcher.matches()) {
				language = dirMatcher.group(2).toLowerCase();
				country = dirMatcher.group(4);
				dir = dirMatcher.group(1) + "\\";
			} else {
				if (Debug.VALIDATOR.isActive()) {
					Debug.VALIDATOR.log("PIIFile.create()  no pattern matches ", baseName, " or ", dir);
				}
			}
			variantInParent = true;
		}

		PIIFile file = new PIIFile(dir, baseName, variantInParent);

		Locale locale;
		if (country == null) {
			locale = new Locale(language);
		} else {
			locale = new Locale(language, country);
		}
		file.addVariant(locale);

		return file;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		String thisBaseName = baseName.toLowerCase();
		String thisDir = dir.toLowerCase();
		result = prime * result + ((thisBaseName == null) ? 0 : thisBaseName.hashCode());
		result = prime * result + ((thisDir == null) ? 0 : thisDir.hashCode());
		result = prime * result + (variantInParentDir ? 1231 : 1237);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		PIIFile other = (PIIFile) obj;
		String thisBaseName = baseName.toLowerCase();
		String otherBaseName = other.baseName.toLowerCase();
		if (thisBaseName == null) {
			if (otherBaseName != null) {
				return false;
			}
		} else if (!thisBaseName.equals(otherBaseName)) {
			return false;
		}

		String thisDir = dir.toLowerCase();
		String otherDir = other.dir.toLowerCase();
		if (thisDir == null) {
			if (otherDir != null) {
				return false;
			}
		} else if (!thisDir.equals(otherDir)) {
			return false;
		}

		if (variantInParentDir != other.variantInParentDir) {
			return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(PIIFile o) {
		String thisDir = dir.toLowerCase();
		String otherDir = o.dir.toLowerCase();
		int result = thisDir.compareTo(otherDir);
		if (result != 0) {
			return result;
		}

		String thisBaseName = baseName.toLowerCase();
		String otherBaseName = o.baseName.toLowerCase();
		result = thisBaseName.compareTo(otherBaseName);
		if (result != 0) {
			return result;
		}

		result = toInt(variantInParentDir) - toInt(o.variantInParentDir);

		return result;
	}

	private int toInt(boolean b) {
		return b ? 1 : 0;
	}

	/**
	 * Returns the base directory of the file.
	 *
	 * @return The base directory. This value will not be null, but may be
	 *         empty.
	 */
	public String getDir() {
		return dir;
	}

	/**
	 * Returns the base name of the file. This is the name of the file without
	 * any path information or locale.
	 *
	 * @return The base name of the file. This value will not be null or empty.
	 */
	public String getBaseName() {
		return baseName;
	}

	/**
	 * Returns the directory of the file for the given variant. If
	 * {@link #isVariantInParentDir()} is true, this will have the locale added
	 * as an additional directory, otherwise it will be the same as
	 * {@link #getDir()}.
	 *
	 * @return The directory for the variant. This value will not be null, but
	 *         may be empty.
	 */
	public String getDir(Locale locale) {
		String variantDir = getDir();
		if (variantInParentDir) {
			variantDir += locale.getLanguage().toUpperCase();
			if (!locale.getCountry().isEmpty()) {
				variantDir += "_" + locale.getCountry();
			}
			variantDir += "\\";
		}
		return variantDir;
	}

	/**
	 * Returns the name of the file for the given variant. If
	 * {@link #isVariantInParentDir()} is false, this will have the locale added
	 * before the extension, otherwise it will be the same as
	 * {@link #getBaseName()}.
	 *
	 * @param locale
	 *            The locale to get the name for.
	 *
	 * @return The name for the variant. This value will not be null or empty.
	 */
	public String getName(Locale locale) {
		String variantName = getBaseName();
		if (!variantInParentDir) {
			Pattern pattern = Pattern.compile("^([^\\.]+)\\.(.+)$");
			Matcher matcher = pattern.matcher(variantName);
			if (!matcher.matches()) {
				throw new IllegalStateException("Could not find where to insert the locale in " + variantName + ".");
			}

			String localeString = "_" + locale.getLanguage();
			if (!locale.getCountry().isEmpty()) {
				localeString += "_" + locale.getCountry();
			}
			variantName = matcher.replaceFirst("$1" + localeString.toUpperCase() + ".$2");
		}
		return variantName;
	}

	/**
	 * Returns whether variants are the names of parent dirs, or are added to
	 * the base file name.
	 *
	 * @return True if variants are the names of parent dirs, false otherwise.
	 */
	public boolean isVariantInParentDir() {
		return variantInParentDir;
	}

	/**
	 * Adds the given Locale as a variant on this.
	 *
	 * @param locale
	 *            The locale that identifies the variant.
	 */
	public void addVariant(Locale locale) {
		Param.notNull(locale, "locale");

		variantToPropertiesMap.put(locale, new LinkedHashMap<String, String>());
	}

	/**
	 * Returns the list of variants that have attributes for this file.
	 *
	 * @return The variants. This value will not be null, but may be empty.
	 */
	public Set<Locale> getVariants() {
		return variantToPropertiesMap.keySet();
	}

	/**
	 * Sets a property for the given variant.
	 *
	 * @param locale
	 *            The variant to set the property for. This value cannot be
	 *            null.
	 * @param name
	 *            The name of the property to set. This value cannot be null or
	 *            empty.
	 * @param value
	 *            The value to set for the property. This value cannot be null,
	 *            but may be empty.
	 */
	public void setProperty(Locale locale, String name, String value) {
		Param.notNull(locale, "locale");
		Param.notNullOrEmpty(name, "name");
		Param.notNull(value, "value");

		if (!variantToPropertiesMap.containsKey(locale)) {
			throw new IllegalArgumentException("Variant " + locale + " has not been added yet.");
		}

		Map<String, String> properties = variantToPropertiesMap.get(locale);
		properties.put(name, value);
	}

	/**
	 * Returns the value for the given variant and property.
	 *
	 * @param locale
	 *            The variant to get the property for. This value cannot be
	 *            null.
	 * @param name
	 *            The name of the property to get. This value cannot be null or
	 *            empty.
	 *
	 * @return The value of the property, or null if no value has been set.
	 */
	public String getProperty(Locale locale, String name) {
		Param.notNull(locale, "locale");
		Param.notNullOrEmpty(name, "name");

		if (!variantToPropertiesMap.containsKey(locale)) {
			throw new IllegalArgumentException("Variant " + locale + " has not been added yet.");
		}

		return variantToPropertiesMap.get(locale).get(name);
	}

	/**
	 * Returns the set of property names for the given variant.
	 *
	 * @param locale
	 *            The variant to get property names for. This value cannot be
	 *            null.
	 *
	 * @return The property names. This value will not be null, but may be
	 *         empty.
	 */
	public Set<String> getProperties(Locale locale) {
		return variantToPropertiesMap.get(locale).keySet();
	}

	@Override
	public String toString() {
		String string = dir;
		if (dir.endsWith("\\")) {
			dir = dir.substring(0, dir.length() - 1);
		}

		return string + '\\' + baseName;
	}

}
