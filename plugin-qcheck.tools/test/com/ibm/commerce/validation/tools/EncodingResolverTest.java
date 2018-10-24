package com.ibm.commerce.validation.tools;

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

import java.util.Locale;

import com.ibm.commerce.qcheck.tools.CheckPIIEncoding;
import com.ibm.commerce.qcheck.tools.EncodingResolver;

import junit.framework.TestCase;

/**
 * This class tests the {@link EncodingResolver} class.
 * 
 * @author Trent Hoeppner
 */
public class EncodingResolverTest extends TestCase {

	private EncodingResolver resolver;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		resolver = new EncodingResolver();
	}

	/**
	 * Tests that if the locale is null, an exception will be thrown.
	 */
	public void testAddIfLocaleNullExpectException() {
		try {
			resolver.add(null, CheckPIIEncoding.ANY);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the encoding is null, an exception will be thrown.
	 */
	public void testAddIfEncodingNullExpectException() {
		try {
			resolver.add(Locale.US, null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if no locale and encoding are given, the best encoding will be
	 * ANSI.
	 */
	public void testAddIf0LocaleExpectBestEncodingIsAnsi() {
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.ANSI, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 1 locale and ANSI are given, the best encoding will be
	 * ANSI.
	 */
	public void testAddIf1LocaleAnsiGivenExpectBestEncodingIsAnsi() {
		resolver.add(Locale.US, CheckPIIEncoding.ANSI);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.ANSI, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 1 double byte locale and ANSI are given, the best encoding
	 * will be ANSI.
	 */
	public void testAddIf1DoubleLocaleAnsiGivenExpectBestEncodingIsAnsi() {
		resolver.add(Locale.JAPAN, CheckPIIEncoding.ANSI);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.ANSI, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 1 locale and ANY are given, the best encoding will be ANSI.
	 */
	public void testAddIf1LocaleAnyGivenExpectBestEncodingIsAnsi() {
		resolver.add(Locale.US, CheckPIIEncoding.ANY);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.ANSI, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 1 locale and ASCII are given, the best encoding will be
	 * ASCII.
	 */
	public void testAddIf1LocaleAsciiGivenExpectBestEncodingIsAscii() {
		resolver.add(Locale.US, CheckPIIEncoding.ASCII);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.ASCII, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 1 double byte locale and ASCII are given, the best encoding
	 * will be ANSI.
	 */
	public void testAddIf1DoubleLocaleAsciiGivenExpectBestEncodingIsAscii() {
		resolver.add(Locale.JAPAN, CheckPIIEncoding.ASCII);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.ANSI, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 1 locale and UNICODE are given, the best encoding will be
	 * UNICODE.
	 */
	public void testAddIf1LocaleUnicodeGivenExpectBestEncodingIsUnicode() {
		resolver.add(Locale.US, CheckPIIEncoding.UNICODE);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.UNICODE, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 1 locale and UTF8 are given, the best encoding will be
	 * UTF8.
	 */
	public void testAddIf1LocaleUTF8GivenExpectBestEncodingIsUTF8() {
		resolver.add(Locale.US, CheckPIIEncoding.UTF8);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.UTF8, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 1 locale and UTF16 are given, the best encoding will be
	 * UTF16.
	 */
	public void testAddIf1LocaleUTF16GivenExpectBestEncodingIsUTF16() {
		resolver.add(Locale.US, CheckPIIEncoding.UTF16);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.UTF16, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 1 locale and AnsiISO are given, the best encoding will be
	 * AnsiISO.
	 */
	public void testAddIf1LocaleAnsiISOGivenExpectBestEncodingIsAnsiISO() {
		resolver.add(Locale.US, CheckPIIEncoding.ANSI_ISO);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.ANSI_ISO, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 1 locale and AnsiWin are given, the best encoding will be
	 * AnsiWin.
	 */
	public void testAddIf1LocaleAnsiWinGivenExpectBestEncodingIsAnsiWin() {
		resolver.add(Locale.US, CheckPIIEncoding.ANSI_WIN);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.ANSI_WIN, resolver.getBestEncoding());
	}

	/**
	 * Tests that if 2 locale and ANY are given, the best encoding will be ANSI.
	 */
	public void testAddIf1LocaleAnsiWinGivenExpectBestEncodingIsAnsi() {
		resolver.add(Locale.US, CheckPIIEncoding.ANY);
		resolver.add(Locale.GERMANY, CheckPIIEncoding.ANY);
		assertEquals("Best encoding is wrong.", CheckPIIEncoding.ANSI, resolver.getBestEncoding());
	}
}
