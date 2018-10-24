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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TextPositions breaks down a string into lines, and tracks the number of
 * lines, start position of each line, and the length of each line.
 * 
 * @author Trent Hoeppner
 */
public class TextPositions {

	/**
	 * The line separator sequence for unix OS flavors.
	 */
	public static final String UNIX_LINE_SEPARATOR = "\n";

	/**
	 * The line separator sequence for the old MacIntosh OS.
	 */
	public static final String MAC_LINE_SEPARATOR = "\r";

	/**
	 * The line separator sequence for DOS and Windows.
	 */
	public static final String DOS_LINE_SEPARATOR = "\r\n";

	private String data;

	private List<Integer> startPositions;

	private List<String> lineEndings;

	/**
	 * Constructor for this.
	 *
	 * @param data
	 *            The data from which to construct the lines and discover
	 *            positions. Cannot be null, but may be empty.
	 */
	public TextPositions(String data) {
		Param.notNull(data, "data");

		this.data = data;
	}

	/**
	 * Returns the number of lines in the string.
	 *
	 * @return The number of lines. Will be &gt;= 1.
	 */
	public int getLineCount() {
		parseLines();
		return startPositions.size();
	}

	/**
	 * Returns the start position (an index from the beginning of the file) for
	 * the first character of the given line.
	 *
	 * @param lineIndex
	 *            The line number as a 0-based index into the lines of the file.
	 *            Must be &gt;= 0.
	 *
	 * @return The start position of the given line. Will be &gt;= 0.
	 */
	public int getStartPosition(int lineIndex) {
		parseLines();
		return startPositions.get(lineIndex);
	}

	/**
	 * Returns the end position of for the character past the last character on
	 * the given line, not including the line separator.
	 *
	 * @param lineIndex
	 *            The line number as a 0-based index into the lines of the file.
	 *            Must be &gt;= 0.
	 *
	 * @return The end position of one past the last character on the given
	 *         line. Will be &gt;= 0.
	 */
	public int getEndPosition(int lineIndex) {
		parseLines();

		String lineEnd = getLineSeparator(lineIndex);

		int end;
		if (lineEnd != null) {
			int nextStart = getStartPosition(lineIndex + 1);
			end = nextStart - lineEnd.length();
		} else {
			end = data.length();
		}

		return end;
	}

	/**
	 * Returns the number of characters in the given line.
	 *
	 * @param lineIndex
	 *            The line number as a 0-based index into the lines of the file.
	 *            Must be &gt;= 0.
	 *
	 * @return The number of characters for the line. Will be &gt;= 0.
	 */
	public int getLength(int lineIndex) {
		parseLines();

		String text = getText(lineIndex);
		return text.length();
	}

	/**
	 * Returns the text of the given line.
	 *
	 * @param lineIndex
	 *            The line number as a 0-based index into the lines of the file.
	 *            Must be &gt;= 0.
	 *
	 * @return The text for the given line. Will not be null, but may be empty.
	 */
	public String getText(int lineIndex) {
		parseLines();

		int start = getStartPosition(lineIndex);
		int end = getEndPosition(lineIndex);

		String text = data.substring(start, end);
		return text;
	}

	/**
	 * Returns the line separator at the end of the given line.
	 *
	 * @param lineIndex
	 *            The line number as a 0-based index into the lines of the file.
	 *            Must be &gt;= 0.
	 *
	 * @return The line separator at the end of the line, or null if it is the
	 *         last line in the file. Will not be empty.
	 */
	public String getLineSeparator(int lineIndex) {
		parseLines();

		String lineSeparator = null;
		if (lineIndex < lineEndings.size()) {
			lineSeparator = lineEndings.get(lineIndex);
		}

		return lineSeparator;
	}

	/**
	 * Returns the line index for the given position in the file.
	 *
	 * @param position
	 *            The 0-based index from the beginning of the file of a
	 *            particular character. This value must be &gt;= 0.
	 *
	 * @return The line number as a 0-based index into the lines of the file.
	 *         This value will be &gt;= 0.
	 */
	public int getLineIndex(int position) {
		parseLines();

		int lineIndex = Collections.binarySearch(startPositions, position);
		if (lineIndex <= 0) {
			lineIndex = -(lineIndex + 1);
		}

		if (lineIndex >= startPositions.size() || startPositions.get(lineIndex) > position) {
			lineIndex--;
		}

		return lineIndex;
	}

	private void parseLines() {
		if (startPositions == null) {
			List<Integer> starts = new ArrayList<Integer>();
			List<String> lineEnds = new ArrayList<String>();
			starts.add(0);
			for (int i = 0; i < data.length(); i++) {
				char current = data.charAt(i);
				if (current == '\r' || current == '\n') {
					int lineSepSize = 1;

					if (i + 1 < data.length()) {
						char next = data.charAt(i + 1);
						if (current == '\r' && next == '\n') {
							lineSepSize++;
						}
					}

					starts.add(i + lineSepSize);

					String lineEnd;
					if (current == '\n') {
						lineEnd = UNIX_LINE_SEPARATOR;
					} else if (lineSepSize == 1) {
						lineEnd = MAC_LINE_SEPARATOR;
					} else {
						lineEnd = DOS_LINE_SEPARATOR;
					}

					lineEnds.add(lineEnd);

					// make sure we pass the separator
					i += lineSepSize - 1;
				}
			}

			startPositions = starts;
			lineEndings = lineEnds;
		}
	}

}
