package com.ibm.commerce.qcheck.core.comment;

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

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.Param;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.ValidatorResource;

/**
 * CommentUtil provides some utility methods for handling strings within
 * comments and debugging.
 * 
 * @author Trent Hoeppner
 */
public final class CommentUtil {

	/**
	 * The maximum number of text columns to print out.
	 */
	private static final int MAX_TEXT_COLUMNS = 80;

	/**
	 * A string which shows all the columns, marked with a number every 5
	 * columns. This is used to make finding errors in pointing to code errors
	 * easier.
	 */
	private static String columnGuide = null;

	/**
	 * Constructor for CommentUtil. Private to prevent instantiation.
	 */
	private CommentUtil() {
		// do nothing
	}

	/**
	 * Prints the given result to standard out. This method is used for
	 * debugging purposes.
	 *
	 * @param result
	 *            The result to print out. Cannot be null.
	 */
	public static void printError(ValidationResult result) {
		if (Debug.COMMENT.isActive()) {
			printColumns();
			StringBuffer buf = new StringBuffer();
			buf.append(createSpaces(result.getColumn()));
			for (int i = 0; i < result.getLength(); i++) {
				buf.append("^");
			}
			Debug.COMMENT.log(buf.toString());

			Debug.COMMENT.log(result.getResource().getFilename(), ", line ", result.getLine(), ", column ",
					result.getColumn(), ", length ", result.getLength(), ": ", result.getMessage());
		}
	}

	/**
	 * Prints 0-based column numbers from 1-80 to standard out. This method is
	 * used for debugging purposes, to make visual comparisons between positions
	 * within strings.
	 */
	public static void printColumns() {
		if (columnGuide == null) {
			StringBuffer buf = new StringBuffer();
			int pos = 0;
			while (pos < MAX_TEXT_COLUMNS) {
				if (pos % 5 == 0) {
					String number = Integer.toString(pos);
					buf.append(number);
					pos += number.length() - 1;
				} else {
					buf.append(" ");
				}
				pos++;
			}
			buf.append("\n");
			for (int column = 0; column < MAX_TEXT_COLUMNS; column++) {
				if (column % 5 == 0) {
					buf.append("|");
				} else {
					buf.append("-");
				}
			}
			columnGuide = buf.toString();
		}
		Debug.COMMENT.log(columnGuide);
	}

	/**
	 * Creates a String with the given number of spaces.
	 *
	 * @param numSpaces
	 *            The number of spaces to create. Must be &gt;= 0.
	 *
	 * @return A new string with the indicated number of characters. Will not be
	 *         null.
	 */
	public static String createSpaces(int numSpaces) {
		StringBuffer spaces = new StringBuffer();
		for (int i = 0; i < numSpaces; i++) {
			spaces.append(' ');
		}

		return spaces.toString();
	}

	/**
	 * Returns the number of characters for the line break at the given
	 * position. This computation is performed based on the characters in the
	 * given description, and will identify '/r', '/n' and combinations of them.
	 *
	 * @param description
	 *            The text in which the line breaks may occur. Cannot be null or
	 *            empty.
	 * @param maxPos
	 *            The maximum position in the text that is available to search.
	 *            This is used when it is known that the line break must occur
	 *            before a certain position. Must be &gt;= 0 and &lt;= the
	 *            length of the string.
	 * @param pos
	 *            The position at which to detect the line break. Must be &gt;=
	 *            0 and &lt;= the length of the string.
	 *
	 * @return The number of characters for the line break at the given
	 *         position, or 0 if there is no line break at the given position.
	 */
	public static int findLineBreakLength(String description, int maxPos, int pos) {
		int lineBreakLength = 0;
		if (description.charAt(pos) == '\n' && pos + 1 < maxPos && description.charAt(pos + 1) == '\r'
				|| description.charAt(pos) == '\r' && pos + 1 < maxPos && description.charAt(pos + 1) == '\n') {
			lineBreakLength = 2;
		} else if (description.charAt(pos) == '\n' || description.charAt(pos) == '\r') {
			lineBreakLength = 1;
		}
		return lineBreakLength;
	}

	/**
	 * Returns the number of characters for the line break at the given
	 * position. This computation is based on how the given
	 * {@link CompilationUnit} calculates line breaks.
	 *
	 * @param position
	 *            The 0-based index into the file to check for a line break.
	 *            Must be &gt;= 0.
	 * @param comp
	 *            The compiled form of the file with all ASTNodes and line
	 *            numbers available. Cannot be null.
	 *
	 * @return The number of characters for the line break starting at position.
	 *         Will be 0 if there is no line break.
	 */
	public static int findLineBreakLength(int position, CompilationUnit comp) {
		int lineBreakLength;

		int previousPosition = position - 1;
		if (previousPosition >= 0) {
			int previousLine = comp.getLineNumber(previousPosition) - 1;
			int currentLine = comp.getLineNumber(position) - 1;
			if (previousLine == currentLine) {
				lineBreakLength = 0;
			} else if (previousLine == currentLine - 1) {
				lineBreakLength = 1;
			} else {
				throw new IllegalArgumentException("previousLine (" + previousLine + ") and currentLine (" + currentLine
						+ ") are more than one line apart.");
			}
		} else {
			lineBreakLength = 0;
		}

		return lineBreakLength;
	}

	public static ColLine calcColumnAndLine(String string, int commentStartPos) {
		int lineNum = 0;
		int columnNum = 0;
		boolean previousWasLineBreak = false;
		int previousCharSize = 0;
		for (int charIndex = 0; charIndex <= commentStartPos; charIndex++) {
			if (previousWasLineBreak) {
				lineNum++;
				columnNum = 0;
			} else {
				columnNum += previousCharSize;
			}

			int linebreakLength = findLineBreakLength(string, string.length(), charIndex);

			previousWasLineBreak = linebreakLength > 0;
			previousCharSize = previousWasLineBreak ? linebreakLength : 1;

			if (previousCharSize == 2) {
				charIndex += 1;
			}
		}
		ColLine colLine = new ColLine(columnNum, lineNum);
		return colLine;
	}

	public static String getOriginalText(ValidatorResource resource, int newStartPos, int newEndPos) {
		Param.notNull(resource, "resource");

		String string = ModelEnum.STRING.getData(resource);

		Param.boundsValid(newStartPos, "newStartPos", newEndPos, "newEndPos", string, "StringModel(resource)");

		String subString = string.substring(newStartPos, newEndPos);

		return subString;
	}
}
