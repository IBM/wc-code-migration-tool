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
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TagElement;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.comment.CommentFragment.CheckerState;

/**
 * CommentDescription represents a description which may have several sentences
 * and paragraphs, which is intended to be read as prose. This can
 * {@link #getHumanReadableString() create a new string} which is stripped of
 * HTML and Java doc tags (or renders them) in such a way that the sentences are
 * intact without any formatting elements. This is intended to be used for
 * grammar and spell-checking.
 * <p>
 * The parts of the sentence which make up the new string can be mapped back to
 * their original locations in the file. This lets the grammar checker examine
 * the comment without having to parse tags or spacing, while still allowing the
 * user to find out where the error is in the original file.
 * <p>
 * This description is made up of several {@link CommentFragment}s, which do not
 * span lines, and may include tags.
 * 
 * @author Trent Hoeppner
 */
public class CommentDescription extends CommentElement {

	/**
	 * The fragments which make up this description. The keys are starting
	 * positions for fragments, and the values are the fragments for each
	 * starting position. Will never be null.
	 */
	private Map<Integer, CommentFragment> startPositionToFragmentMap = new TreeMap();

	/**
	 * The list of all fragments in this. This will be null until lazy-loaded by
	 * the {@link CommentDescription#getFragments()} method. Therefore that
	 * method should always be used instead of accessing this field directly.
	 */
	private List<CommentFragment> allFragments;

	/**
	 * The comment that hosts this. If null, it will not be possible to get
	 * extra spaces between fragments.
	 */
	private Comment parent;

	/**
	 * The string that is created through the method
	 * {@link #createHumanReadableString()}. See {@link #getHumanReadableString}
	 * for details.
	 */
	private String humanReadableString = null;

	/**
	 * Constructor for CommentDescription based on tag fragments.
	 * 
	 * @param nodes
	 *            The list of {@link ASTNode}s that make up the description, as
	 *            returned by {@link TagElement#fragments()}. These will be used
	 *            by {@link CommentDescriptionBuilder} to construct the
	 *            CommentFragments that make up this description. Cannot be
	 *            null.
	 * @param comp
	 *            The compiled form of the file with all ASTNodes and line
	 *            numbers available. Cannot be null.
	 * @param trimLeadingWhiteSpace
	 *            True indicates that white space leading up to the first
	 *            non-white space character in the first fragment will be
	 *            stripped, false indicates that it will not be stripped.
	 * @param newParent
	 *            The object that contains this object. May be null if this has
	 *            no parent.
	 */
	public CommentDescription(ValidatorResource resource, List nodes, CompilationUnit comp,
			boolean trimLeadingWhiteSpace, Comment newParent) {

		this.parent = newParent;

		if (nodes.isEmpty()) {
			throw new IllegalArgumentException("Must contain at least one fragment.");
		}

		ASTNode first = (ASTNode) nodes.get(0);
		ASTNode last = (ASTNode) nodes.get(nodes.size() - 1);

		CommentDescriptionBuilder builder = new CommentDescriptionBuilder(resource, nodes, comp, true,
				first.getStartPosition(), last.getStartPosition() + last.getLength());

		builder.printFragments();

		for (CommentFragment fragment : builder.fragments) {
			startPositionToFragmentMap.put(fragment.getStartPosition(), fragment);
		}
	}

	/**
	 * Finds the starting position (relative to the beginning of the file) of
	 * the given line and column within the human-readable form of this
	 * description.
	 * 
	 * @param commentLine
	 *            The line number in the comment, as a 0-based index. Must be >=
	 *            0.
	 * @param commentColumn
	 *            The column number in the comment, as a 0-based index. Must be
	 *            >= 0.
	 * 
	 * @return The index of the starting position of the given line and column
	 *         as a 0-based index from the beginning of the file. Will be >= 0.
	 */
	public int findInComment(int commentLine, int commentColumn) {

		CommentFragment fragment = findFragment(commentLine, commentColumn);
		int realStart;
		if (fragment != null) {
			if (fragment.isInString()) {
				realStart = fragment.convertToStartingPosition(commentColumn);
			} else {
				realStart = fragment.getStartPosition();
			}
		} else {
			realStart = 1;
			if (Debug.COMMENT.isActive()) {
				Debug.COMMENT.log("Could not find fragment at comment location (line=", commentLine, ", col=",
						commentColumn, ").");
			}
		}

		return realStart;
	}

	/**
	 * Finds the starting position (relative to the beginning of the file) of
	 * the given offset within the {@link #getHumanReadableString()
	 * human-readable string}. This method is similar to
	 * {@link #findInComment(int, int)}.
	 * 
	 * @param commentStartPos
	 *            The start index, as a 0-based index into the comment. Must be
	 *            >= 0.
	 * 
	 * @return The index of the starting position as a 0-based index from the
	 *         beginning of the file. Will be >= 0.
	 */
	public int findInComment(int commentStartPos) {

		String humanString = getHumanReadableString();

		if (commentStartPos >= humanString.length()) {
			throw new IllegalArgumentException("commentStartPos is out of bounds: " + commentStartPos
					+ ", human string length = " + humanString.length());
		}

		ColLine colLine = CommentUtil.calcColumnAndLine(humanString, commentStartPos);

		int realStart = findInComment(colLine.getLineNum(), colLine.getColumnNum());
		return realStart;
	}

	/**
	 * Finds the fragment that contains the text at the given start position
	 * within the human-readable string.
	 * 
	 * @param commentStartPos
	 *            The start index, as a 0-based index into the comment. Must be
	 *            >= 0.
	 * 
	 * @return The fragment that contains the given comment start position. Will
	 *         be null if no fragment was found.
	 */
	public CommentFragment findFragment(int commentStartPos) {
		ColLine colLine = CommentUtil.calcColumnAndLine(getHumanReadableString(), commentStartPos);

		CommentFragment fragment = findFragment(colLine.getLineNum(), colLine.getColumnNum());
		return fragment;
	}

	/**
	 * Finds the object which represents a single character at the given
	 * position in the human-readable string.
	 * 
	 * @param commentStartPos
	 *            The start index, as a 0-based index into the comment. Must be
	 *            >= 0.
	 * 
	 * @return The part of a fragment which is referred to by the given comment
	 *         start position. Will be null if no fragment part was found.
	 */
	public FragmentPos findFragmentPos(int commentStartPos) {
		ColLine colLine = CommentUtil.calcColumnAndLine(getHumanReadableString(), commentStartPos);

		CommentFragment fragment = findFragment(colLine.getLineNum(), colLine.getColumnNum());
		int realStart;
		int posWithinFragment;

		if (fragment != null) {
			if (fragment.isInString()) {
				realStart = fragment.convertToStartingPosition(colLine.getColumnNum());
			} else {
				realStart = fragment.getStartPosition();
			}
			posWithinFragment = realStart - fragment.getStartPosition();
		} else {
			realStart = 1;
			if (Debug.COMMENT.isActive()) {
				Debug.COMMENT.log("Could not find fragment at comment location (startpos=", commentStartPos, ").");
			}
			posWithinFragment = 0;
		}

		FragmentPos pos = new FragmentPos(fragment, posWithinFragment);
		return pos;
	}

	/**
	 * Finds the fragment that contains the given line and column.
	 * 
	 * @param commentLine
	 *            The line number within the comment, as a 0-based index. Must
	 *            be >= 0.
	 * @param commentColumn
	 *            The column number within the comment, as a 0-based index. Must
	 *            be >= 0.
	 * 
	 * @return The fragment that contains the given line and column from the
	 *         string. Will be null if no fragment was found.
	 */
	public CommentFragment findFragment(int commentLine, int commentColumn) {

		CommentFragment found = null;

		int lastSameLineMatchStartPos = -1;
		CommentFragment checkSpacesBeforeFragment = null;
		for (Integer startPosition : startPositionToFragmentMap.keySet()) {
			CommentFragment fragment = startPositionToFragmentMap.get(startPosition);

			if (fragment.getStringLine() == commentLine) {
				if (fragment.containsStringColumn(commentColumn)) {
					found = fragment;
					break;
				} else {
					lastSameLineMatchStartPos = fragment.getStartPosition();
				}
			} else if (fragment.getStringLine() > commentLine) {
				checkSpacesBeforeFragment = fragment;
				break;
			}
		}

		if (checkSpacesBeforeFragment != null && lastSameLineMatchStartPos >= 0) {
			// find the one after the lastSameLineMatch
			for (Integer startPosition : startPositionToFragmentMap.keySet()) {
				CommentFragment fragment = startPositionToFragmentMap.get(startPosition);
				if (fragment.getStartPosition() > lastSameLineMatchStartPos) {
					found = fragment;
					break;
				}
			}
		}

		return found;
	}

	/**
	 * Returns a string that retains the paragraph and sentence structure, but
	 * has as little other formatting as possible. This string is suitable for
	 * grammar- and spell-checking.
	 * <p>
	 * Fragments in this will be updated so that they track their position in
	 * the created string, in addition to the position information for the
	 * original file.
	 * 
	 * @return The human-readable form of this description. Will not be null,
	 *         but may be empty if the fragments in this do not contain
	 *         human-readable text.
	 */
	public String getHumanReadableString() {
		if (humanReadableString == null) {
			humanReadableString = createHumanReadableString();
		}

		return humanReadableString;
	}

	/**
	 * Creates and returns a human-readable string, as described in
	 * {@link #getHumanReadableString()}.
	 * 
	 * @return A human-readable form of this description. Will not be null, but
	 *         may be empty if the fragments in this do not contain
	 *         human-readable text.
	 */
	private String createHumanReadableString() {
		if (Debug.COMMENT.isActive()) {
			CommentUtil.printColumns();
		}

		StringBuffer buf = new StringBuffer();
		int sourceLine = -1;
		int stringLine = 0;
		int stringColumn = 0;
		int lineStart = 0;
		StringBuffer line = new StringBuffer();
		for (Integer startPosition : startPositionToFragmentMap.keySet()) {
			CommentFragment fragment = startPositionToFragmentMap.get(startPosition);

			// filter out fragments not intended for humans
			if (fragment.getGrammarState() == CheckerState.INVISIBLE) {
				continue;
			}

			if (sourceLine < 0) {
				sourceLine = fragment.getSourceLine();

				if (Debug.COMMENT.isActive()) {
					lineStart = fragment.getSourceColumn();
				}
			}

			if (fragment.getSourceLine() > sourceLine) {
				int diff = fragment.getSourceLine() - sourceLine;
				for (int i = 0; i < diff; i++) {
					// buf.append("\n");
					sourceLine++;
				}

				// add at most one \n, language tool treats consecutive \n's as
				// the same paragraph

				stringLine++;
				stringColumn = 0;

				if (Debug.COMMENT.isActive()) {
					Debug.COMMENT.log(line.toString());
					line.setLength(0);
					lineStart = fragment.getSourceColumn();
				}
			}

			fragment.setStringLine(stringLine);

			String fragmentText = fragment.getText();
			if (fragmentText.equals("\r")) {
				// normalize it because the grammar checker doesn't think \r
				// alone is a line ending
				fragmentText = "\r\n";
			}
			buf.append(fragmentText);
			if (Debug.COMMENT.isActive()) {
				line.append(CommentUtil.createSpaces(fragment.getSourceColumn() - line.length()));
				line.append(fragmentText);
			}
			fragment.setStringColumn(stringColumn);
			stringColumn += fragmentText.length();
		}

		if (Debug.COMMENT.isActive()) {
			Debug.COMMENT.log(line.toString());
		}

		String string = buf.toString();
		if (Debug.COMMENT.isActive()) {
			Debug.COMMENT.log(string);
		}
		return string;
	}

	/**
	 * Returns a list of all fragments in this, in the order that they occur in
	 * the original file.
	 * 
	 * @return A new list with the fragments in this. Will not be null.
	 */
	public List<CommentFragment> getFragments() {
		if (allFragments == null) {
			allFragments = new ArrayList<CommentFragment>();
			for (Integer key : startPositionToFragmentMap.keySet()) {
				allFragments.add(startPositionToFragmentMap.get(key));
			}
		}

		return allFragments;
	}

	/**
	 * Finds the first fragment in this that contains human-readable text.
	 * 
	 * @return The first fragment in this that contains human-readable text.
	 *         Will be null if there are no fragments with human-readable text.
	 */
	public CommentFragment findFirstHumanFragment() {
		CommentFragment first = null;
		for (CommentFragment fragment : getFragments()) {
			if (fragment.isInString()) {
				first = fragment;
				break;
			}
		}

		return first;
	}
}
