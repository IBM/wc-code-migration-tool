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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The encodings that can be detected by CHKPII.
 * 
 * @author Trent Hoeppner
 */
public enum CheckPIIEncoding {
	/**
	 * All of the characters in the file are compatible with ASCII, ANSI, and
	 * UTF-8. No variant characters are used.
	 */
	ANY("ANY", null),

	/**
	 * All of the variant characters in the file are compatible with the ANSI
	 * code page for this language. There are no characters which can
	 * distinguish between the ISO and Windows ANSI code pages. Variant
	 * characters exist which are not valid for ASCII, UTF-8 and escaped
	 * Unicode.
	 */
	ANSI("ANSI", "ANSI"),

	/**
	 * All of the variant characters in the file are compatible with the ISO
	 * ANSI code page for this language. Variant characters exist which are not
	 * valid for ASCII, Windows version of ANSI, UTF-8 and escaped Unicode.
	 */
	ANSI_ISO("ANSI (ISO)", "ANSI"),

	/**
	 * All of the variant characters in the file are compatible with the Windows
	 * ANSI code page for this language. Variant characters exist which are not
	 * valid for ASCII, ISO version of ANSI, UTF-8 and escaped Unicode.
	 */
	ANSI_WIN("ANSI (WIN)", "ANSI"),

	/**
	 * All of the variant characters in the file are compatible with the ASCII
	 * code page for this language. Variant characters exist which are not valid
	 * for ANSI, UTF-8 and escaped Unicode.
	 */
	ASCII("ASCII", "ASCII"),

	/**
	 * The file format is probably a binary format for which a code page can not
	 * be determined (like a Microsoft Word document).
	 */
	NA("N/A", null),

	/**
	 * All of the variant characters in the file are compatible with the UTF-8
	 * code page.
	 */
	UTF8("UTF-8", "UTF-8"),

	/**
	 * All of the characters in the file are compatible with the UTF-16 code
	 * page.
	 */
	UTF16("UTF-16", null),

	/**
	 * All of the characters in the file are compatible with the escaped Unicode
	 * code page. Escaped Unicode requires that all variant characters are
	 * presented in their Unicode representation ("\\u____").
	 */
	UNICODE("UNICODE", "UNICODE");

	private static final Map<CheckPIIEncoding, List<CheckPIIEncoding>> keepsSingleMap = new HashMap<CheckPIIEncoding, List<CheckPIIEncoding>>();

	private static final Map<CheckPIIEncoding, List<CheckPIIEncoding>> keepsDoubleMap = new HashMap<CheckPIIEncoding, List<CheckPIIEncoding>>();

	static {
		keepsSingleMap.put(ANY, Arrays.asList(ANY, ANSI, ANSI_ISO, ANSI_WIN, ASCII, UTF8, UTF16, UNICODE));
		keepsSingleMap.put(ANSI, Arrays.asList(ANSI, ANSI_ISO, ANSI_WIN));
		keepsSingleMap.put(ANSI_ISO, Arrays.asList(ANSI_ISO));
		keepsSingleMap.put(ANSI_WIN, Arrays.asList(ANSI_WIN));
		keepsSingleMap.put(ASCII, Arrays.asList(ASCII));
		keepsSingleMap.put(UTF8, Arrays.asList(UTF8));
		keepsSingleMap.put(UTF16, Arrays.asList(UTF16));
		keepsSingleMap.put(UNICODE, Arrays.asList(UNICODE));

		keepsDoubleMap.put(ANY, Arrays.asList(ANY, ANSI, ANSI_ISO, ANSI_WIN, ASCII, UTF8, UTF16, UNICODE));
		keepsDoubleMap.put(ANSI, Arrays.asList(ANSI, ANSI_ISO, ANSI_WIN, ASCII));
		keepsDoubleMap.put(ANSI_ISO, Arrays.asList(ANSI_ISO));
		keepsDoubleMap.put(ANSI_WIN, Arrays.asList(ANSI_WIN));
		keepsDoubleMap.put(ASCII, Arrays.asList(ANSI, ANSI_ISO, ANSI_WIN, ASCII));
		keepsDoubleMap.put(UTF8, Arrays.asList(UTF8));
		keepsDoubleMap.put(UTF16, Arrays.asList(UTF16));
		keepsDoubleMap.put(UNICODE, Arrays.asList(UNICODE));
	}

	private String parsedCode;

	private String nlsEncodingValue;

	private CheckPIIEncoding(String parsedCode, String nlsEncodingValue) {
		this.parsedCode = parsedCode;
		this.nlsEncodingValue = nlsEncodingValue;
	}

	public void eliminateImpossible(Set<CheckPIIEncoding> encodings, boolean isDouble) {
		Map<CheckPIIEncoding, List<CheckPIIEncoding>> map;
		if (isDouble) {
			map = keepsDoubleMap;
		} else {
			map = keepsSingleMap;
		}

		List<CheckPIIEncoding> encodingsToKeep = map.get(this);
		Iterator<CheckPIIEncoding> iterator = encodings.iterator();
		while (iterator.hasNext()) {
			CheckPIIEncoding encoding = iterator.next();
			if (!encodingsToKeep.contains(encoding)) {
				iterator.remove();
			}
		}
	}

	public String toNLSEncodingValue() {
		return nlsEncodingValue;
	}

	public static CheckPIIEncoding parse(String code) {
		CheckPIIEncoding found = null;
		for (CheckPIIEncoding encoding : values()) {
			if (encoding.parsedCode.equals(code)) {
				found = encoding;
				break;
			}
		}

		return found;
	}
}
