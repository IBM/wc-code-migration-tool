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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * CommentSpace represents space within the comment. CommentSpace includes a
 * list of fragments which represent consecutive lines. Each line ends with a
 * line break, except the last one which may or may not have a line break at the
 * end.
 * 
 * @author Trent Hoeppner
 */
public class CommentSpace extends CommentElement {

	/**
	 * The list of lines for this space.
	 */
	private List<CommentFragment> spaces = new ArrayList<CommentFragment>();

	/**
	 * True if the last line has a line break, false otherwise.
	 */
	private boolean lastHasBreak;

	/**
	 * The fragment that exists before this space. Will be null if there is no
	 * such fragment.
	 */
	private CommentFragment before;

	/**
	 * The fragment that exists after this space. Will be null if there is no
	 * such fragment.
	 */
	private CommentFragment after;

	/**
	 * Constructor for CommentSpace.
	 * 
	 * @param multiLineSpace
	 *            The fragment which represents the space between
	 *            <code>before</code> and <code>after</code>. This space may
	 *            span multiple lines. Cannot be null.
	 * @param newBefore
	 *            The fragment which occurs before this space. May be null if
	 *            there is no such fragment.
	 * @param newAfter
	 *            The fragment which occurs after this space. May be null if
	 *            there is no such fragment.
	 * @param comp
	 *            The compiled form of the file with all ASTNodes and line
	 *            numbers available. Cannot be null.
	 */
	public CommentSpace(CommentFragment multiLineSpace, CommentFragment newBefore, CommentFragment newAfter,
			CompilationUnit comp) {
		String text = multiLineSpace.getText();
		int originalStartPosInFile = multiLineSpace.getStartPosition();
		int lastStartPosInFile = multiLineSpace.getStartPosition();
		int lastStartPosInText = 0;
		int offset = 0;
		while (offset < text.length()) {
			int lineBreakLength = CommentUtil.findLineBreakLength(originalStartPosInFile + offset, comp);
			if (lineBreakLength > 0) {
				String subText = text.substring(lastStartPosInText, offset + lineBreakLength - 1);
				int lineNumber = comp.getLineNumber(lastStartPosInFile) - 1;
				int columnNumber = comp.getColumnNumber(lastStartPosInFile);
				CommentFragment space = new CommentFragment(lastStartPosInFile, subText, lineNumber, columnNumber);
				spaces.add(space);

				lastStartPosInFile += subText.length();
				lastStartPosInText += subText.length();

				// prepare for next loop iteration
				offset += lineBreakLength - 1;
			}
			offset++;
		}

		if (lastStartPosInText < text.length()) {
			String subText = text.substring(lastStartPosInText);
			int lineNumber = comp.getLineNumber(lastStartPosInFile) - 1;
			int columnNumber = comp.getColumnNumber(lastStartPosInFile);
			CommentFragment space = new CommentFragment(lastStartPosInFile, subText, lineNumber, columnNumber);
			spaces.add(space);
		}

		lastHasBreak = CommentUtil.findLineBreakLength(multiLineSpace.getStartPosition() + text.length(), comp) > 0;

		this.before = newBefore;
		this.after = newAfter;
	}

	/**
	 * Returns the number of lines in this space.
	 * 
	 * @return The number of lines in this space. Will be &gt;= 0.
	 */
	public int getNumLines() {
		int lines = spaces.size();
		if (!lastHasBreak) {
			lines--;
		}

		return lines;
	}

	/**
	 * Returns the start position of this in the file.
	 * 
	 * @return The 0-based index into the file which marks the first character
	 *         of this space. Will be &gt;= 0.
	 */
	public int getStartPosition() {
		return spaces.get(0).getStartPosition();
	}

	/**
	 * Returns the number of characters in this space.
	 * 
	 * @return The number of characters in this space. Will be &gt;= 0.
	 */
	public int length() {
		int length = 0;
		for (CommentFragment fragment : spaces) {
			length += fragment.getText().length();
		}
		return length;
	}

	/**
	 * Returns the lines which make up this space.
	 * 
	 * @return The lines which make up this space. Will not be null, but may be
	 *         empty if this space is empty.
	 */
	public List<CommentFragment> getSpaces() {
		return spaces;
	}

	/**
	 * Returns the fragment which occurs before this space.
	 * 
	 * @return The fragment which occurs before this space. Will be null if
	 *         there is no fragment.
	 */
	public CommentFragment getBefore() {
		return before;
	}

	/**
	 * Returns the fragment which occurs after this space.
	 * 
	 * @return The fragment which occurs after this space. Will be null if there
	 *         is no fragment.
	 */
	public CommentFragment getAfter() {
		return after;
	}
}
