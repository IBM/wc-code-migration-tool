package com.ibm.commerce.cmt;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.commerce.cmt.plan.Position;
import com.ibm.commerce.cmt.plan.Range;

/**
 * This class represents the contents of a text file, indexed by line number.
 * 
 * @author Trent Hoeppner
 */
public class FileContents {

	private File file;

	private String contents;

	private List<Integer> lineNumIndexes;

	public FileContents(File file) {
		Check.notNull(file, "file");

		if (!file.exists()) {
			throw new IllegalArgumentException("file does not exist: " + file.getAbsolutePath());
		}

		if (!file.isFile()) {
			throw new IllegalArgumentException("file is not a file: " + file.isFile());
		}

		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public String getContents() {
		return contents;
	}

	public void load() throws IOException {
		String stringDoc = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			StringBuffer data = new StringBuffer();
			char[] buf = new char[2048];
			int charsRead = reader.read(buf);
			while (charsRead >= 0) {
				data.append(buf, 0, charsRead);
				charsRead = reader.read(buf);
			}
			stringDoc = data.toString();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// swallow to allow the exception to escape
				}
			}
		}

		contents = stringDoc;
	}

	public Position toPosition(int contentIndex) {
		calcLineNumIndexes();

		int foundIndex = Collections.binarySearch(lineNumIndexes, contentIndex);
		int line;
		int column;
		if (foundIndex >= 0) {
			// it's the first column of a line
			line = foundIndex + 1;
			column = 1;
		} else {
			// it's in the middle of a line
			int lineIndex = -(foundIndex + 1) - 1;
			int lineStartContentIndex = lineNumIndexes.get(lineIndex);
			line = lineIndex + 1;
			column = contentIndex - lineStartContentIndex + 1;
		}

		Position p = new Position(line, column);

		return p;
	}

	public int toContentIndex(Position position) {
		Check.notNull(position, "position");

		calcLineNumIndexes();

		int lineStartContentIndex = lineNumIndexes.get(position.getLine() - 1);
		int contentIndex = lineStartContentIndex + position.getColumn() - 1;

		return contentIndex;
	}

	public String format(Range range) {
		Check.notNull(range, "range");

		Position startPosition = toPosition(range.getStart());
		Position endPosition = toPosition(range.getEnd());

		StringBuilder b = new StringBuilder();
		b.append(startPosition.getLine()).append(":").append(startPosition.getColumn());
		b.append("-");
		b.append(endPosition.getLine()).append(":").append(endPosition.getColumn());

		return b.toString();
	}

	public String getSubstring(Range range) {
		Check.notNull(range, "range");

		String substring = getContents().substring(range.getStart(), range.getEnd());

		return substring;
	}

	private void calcLineNumIndexes() {
		if (lineNumIndexes == null) {
			lineNumIndexes = new ArrayList<>();
			lineNumIndexes.add(0);
			for (int i = 0; i < contents.length(); i++) {
				char c = contents.charAt(i);
				if (c == '\n') {
					lineNumIndexes.add(i + 1);
				} else if (c == '\r') {
					// mac is \r
					// windows is \r\n
					if (i + 1 < contents.length()) {
						char nextC = contents.charAt(i + 1);
						if (nextC == '\n') {
							// windows, we just wait until the next iteration
							// and it will be handled properly
						} else {
							// mac, this is the whole line ending, need to add
							// it
							lineNumIndexes.add(i + 1);
						}
					} else {
						// this is the last character in the file, treat it as a
						// line ending
						lineNumIndexes.add(i + 1);
					}
				}
			}
		}
	}

}