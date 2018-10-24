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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ibm.commerce.qcheck.core.Param;

/**
 * This class tracks encodings for different locales for a file, and helps find
 * the encoding that can satisfy the encodings of all locales.
 * 
 * @author Trent Hoeppner
 */
public class EncodingResolver {

	private static final List<Locale> doubleLocales = Arrays.asList(Locale.CHINA, Locale.CHINESE, Locale.TAIWAN,
			Locale.TRADITIONAL_CHINESE, new Locale("hi", "IN"), new Locale("hi"), Locale.JAPAN, Locale.JAPANESE,
			Locale.KOREA, Locale.KOREAN, new Locale("th", "TH"), new Locale("th"));

	private Set<CheckPIIEncoding> encodings = new HashSet<CheckPIIEncoding>();

	public EncodingResolver() {
		encodings.addAll(Arrays.asList(CheckPIIEncoding.values()));
	}

	/**
	 * Adds the given encoding for the given locale, which could change the
	 * output of {@link #getBestEncoding()}.
	 *
	 * @param locale
	 *            The locale to register. This value cannot be null.
	 * @param encoding
	 *            The encoding that was output by CHKPII for the given locale.
	 *            This value cannot be null.
	 */
	public void add(Locale locale, CheckPIIEncoding encoding) {
		Param.notNull(locale, "locale");
		Param.notNull(encoding, "encoding");

		boolean isDouble = doubleLocales.contains(locale);

		encoding.eliminateImpossible(encodings, isDouble);
	}

	/**
	 * Returns the best encoding found so far.
	 *
	 * @return The best encoding. This value may be null if all encodings were
	 *         eliminated, but will not be {@link CheckPIIEncoding#ANY} or
	 *         {@link CheckPIIEncoding#NA}.
	 */
	public CheckPIIEncoding getBestEncoding() {
		CheckPIIEncoding best = null;
		if (encodings.size() > 1) {
			if (encodings.contains(CheckPIIEncoding.ANSI)) {
				best = CheckPIIEncoding.ANSI;
			} else {
				// return the first one
				best = encodings.iterator().next();
			}
		}
		if (best == null && !encodings.isEmpty()) {
			best = encodings.iterator().next();
		}

		return best;
	}

}
