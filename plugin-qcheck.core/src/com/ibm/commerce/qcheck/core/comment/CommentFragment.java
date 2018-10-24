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

import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * CommentFragment (commonly referred to as "fragment") represents a unique
 * piece of text within the comment. All fragments have a position and size in
 * the source file. A fragment may also have a position in a human-readable
 * string which is used for grammar- and spell-checking. See
 * {@link CommentDescription}.
 * 
 * @author Trent Hoeppner
 */
public class CommentFragment extends CommentElement {

	/**
	 * The 0-based line number in the original source file. See
	 * {@link #getSourceLine} for details.
	 */
	private int sourceLine;

	/**
	 * The 0-based column number in the original source file. See
	 * {@link #getSourceColumn} for details.
	 */
	private int sourceColumn;

	/**
	 * The 0-based starting position in the original source file. Each character
	 * in the file advances the position by one. See {@link #getStartPosition}
	 * for details.
	 */
	private int startPosition;

	/**
	 * The text represented by this fragment. See {@link #getText} for details.
	 */
	private String text;

	/**
	 * The 0-based line number in the human-readable string. See
	 * {@link #setStringLine} for details.
	 */
	private int stringLine = -1;

	/**
	 * The 0-based column number in the human-readable string. See
	 * {@link #setStringColumn} for details.
	 */
	private int stringColumn = -1;

	/**
	 * The in-line tag (such as "@link") that this fragment is connected to.
	 * This will only be non-null for the first fragment of the tag. See
	 * {@link #setTag} for details.
	 */
	private Tag tag;

	/**
	 * Indicates how the spell checker should treat this fragment. See
	 * {@link #setSpellingState} for details.
	 */
	private CheckerState spellingState;

	/**
	 * Indicates how the grammar checker should treat this fragment. See
	 * {@link #setGrammarState} for details.
	 */
	private CheckerState grammarState;

	/**
	 * True indicates this fragment performs some formatting, false indicates it
	 * does not.
	 */
	private boolean isFormatting = false;

	/**
	 * True indicates that this is a line ending that is visible in the
	 * human-readable string, false indicates it is not.
	 */
	private boolean isRelevantLineEnding = false;

	/**
	 * CheckerState represents how a fragment should be used for a validator.
	 */
	public static enum CheckerState {

		/**
		 * Indicates that the validator should completely ignore the fragment.
		 */
		INVISIBLE,

		/**
		 * Indicates that the validator can see the fragment, but should not do
		 * any validation on it.
		 */
		VISIBLE,

		/**
		 * Indicates that the validator should include the fragment in its
		 * validation process.
		 */
		APPLICABLE;
	}

	/**
	 * Constructor for CommentFragment.
	 * 
	 * @param newStartPosition
	 *            The 0-based index from the start of the file for the text.
	 *            Must be &gt;= 0.
	 * @param newText
	 *            The text represented by this fragment. Cannot be null, but may
	 *            be empty.
	 * @param newSourceLine
	 *            The 0-based line number in the original file for the text.
	 *            Must be &gt;= 0.
	 * @param newSourceColumn
	 *            The 0-based column number in the original file for the text.
	 *            Must be &gt;= 0.
	 */
	public CommentFragment(int newStartPosition, String newText, int newSourceLine, int newSourceColumn) {

		if (newText == null) {
			throw new IllegalArgumentException("text cannot be null");
		}

		if (newSourceLine < 0) {
			throw new IllegalArgumentException("sourceLine must be >= 0");
		}

		if (newSourceColumn < 0) {
			throw new IllegalArgumentException("sourceColumn must be >= 0");
		}

		this.startPosition = newStartPosition;
		this.text = newText;
		this.sourceLine = newSourceLine;
		this.sourceColumn = newSourceColumn;
	}

	/**
	 * Splits this fragment into two, modifying this to be the first part, and
	 * returning a new fragment that represents the second part.
	 * <p>
	 * This method assumes that this fragment exists on one line. Also, this
	 * method cannot be called after it is associated with a human-readable
	 * string, created by
	 * {@link CommentDescription#createHumanReadableString()}.
	 * 
	 * @param textPos
	 *            The 0-based index into the text of the fragment where the
	 *            split should occur. Text before this value will be included in
	 *            this, while text at or after this value will be included in
	 *            the returned fragment. Must be &gt;= 0.
	 * 
	 * @return The second fragment, which includes text at and after the given
	 *         text in the original fragment. Will not be null.
	 */
	public CommentFragment splitAt(int textPos) {
		if (isInString()) {
			throw new IllegalStateException(
					"Cannot split this fragment because it is associated with a human-readable string.");
		}

		CommentFragment after = new CommentFragment(startPosition + textPos, text.substring(textPos), sourceLine,
				sourceColumn + textPos);
		text = text.substring(0, textPos);

		return after;
	}

	/**
	 * Returns whether this fragment contains the given string column.
	 * 
	 * @param stringColumnToConvert
	 *            A 0-based column in a string created by
	 *            {@link CommentDescription#createHumanReadableString()}. Must
	 *            be &gt;= 0.
	 * 
	 * @return True if this fragment contains the given column, false otherwise.
	 */
	public boolean containsStringColumn(int stringColumnToConvert) {
		return stringColumnToConvert >= getStringColumn()
				&& stringColumnToConvert < getStringColumn() + getText().length();
	}

	/**
	 * Converts the given column within the string-form of the
	 * {@link CommentDescription} that contains this.
	 * 
	 * @param stringColumnToConvert
	 *            The column to convert. Must be a 0-based index into the
	 *            string. Must be >= 0.
	 * 
	 * @return The 0-based index from the start of the file that corresponds to
	 *         given position. Will be >= 0.
	 */
	public int convertToStartingPosition(int stringColumnToConvert) {
		if (!isInString()) {
			throw new IllegalArgumentException(
					"Cannot convert string column because this is not associated with a human-readable string.");
		}

		if (!containsStringColumn(stringColumnToConvert)) {
			throw new IllegalArgumentException("Cannot convert string column from a previous fragment.");
		}

		int difference = stringColumnToConvert - getStringColumn();
		int finalStartingPosition = getStartPosition() + difference;
		return finalStartingPosition;
	}

	/**
	 * Returns the 0-based line number in the human-readable string.
	 * 
	 * @return The 0-based line number in the human-readable string. See
	 *         {@link #setStringLine} for details.
	 */
	public int getStringLine() {
		return stringLine;
	}

	/**
	 * Sets the 0-based line number in the human-readable string.
	 * 
	 * @param newStringLine
	 *            The 0-based line number to set. Must be &gt;= 0.
	 */
	public void setStringLine(int newStringLine) {
		this.stringLine = newStringLine;
	}

	/**
	 * Returns the 0-based column number in the human-readable string.
	 * 
	 * @return The 0-based column number in the human-readable string. See
	 *         {@link #setStringColumn} for details.
	 */
	public int getStringColumn() {
		return stringColumn;
	}

	/**
	 * Sets the 0-based column number in the human-readable string.
	 * 
	 * @param newStringColumn
	 *            The 0-based column number to set. Must be &gt;= 0.
	 */
	public void setStringColumn(int newStringColumn) {
		this.stringColumn = newStringColumn;
	}

	/**
	 * Returns the 0-based line number in the source file.
	 * 
	 * @return The 0-based line number in the source file. Will be &gt;= 0.
	 */
	public int getSourceLine() {
		return sourceLine;
	}

	/**
	 * Returns the 0-based column number in the source file.
	 * 
	 * @return The 0-based column number in the source file. Will be &gt;= 0.
	 */
	public int getSourceColumn() {
		return sourceColumn;
	}

	/**
	 * Returns the 0-based start position from the beginning of the file of the
	 * first character in the text.
	 * 
	 * @return The 0-based start position of the first character in the text.
	 *         Will be &gt;= 0.
	 */
	public int getStartPosition() {
		return startPosition;
	}

	/**
	 * Returns the text for this fragment.
	 * 
	 * @return The text for this fragment. Will not be null, but may be empty.
	 */
	public String getText() {
		return text;
	}

	/**
	 * Returns whether this fragment has a {@link Tag}, where the description
	 * would have come from.
	 * 
	 * @return True if this has a Tag, false otherwise.
	 */
	public boolean hasTag() {
		return tag != null;
	}

	/**
	 * Returns the tag that this fragment corresponds to.
	 * 
	 * @return The tag where the text came from, or null if there is no tag. See
	 *         {@link #setTag} for details.
	 */
	public Tag getTag() {
		return tag;
	}

	/**
	 * Sets the tag that this fragment corresponds to.
	 * 
	 * @param linkTag
	 *            The tag for this fragment. Cannot be null.
	 */
	public void setTag(Tag linkTag) {
		this.tag = linkTag;
	}

	/**
	 * Returns whether this fragment has been created in a human-readable
	 * string.
	 * 
	 * @return True if this fragment is associated with a human-readable string,
	 *         false otherwise.
	 */
	public boolean isInString() {
		return stringLine >= 0 && stringColumn >= 0;
	}

	/**
	 * Removes the last white space character from this fragment. This is to
	 * handle a special case for grammar checking involving links. See internal
	 * comments of
	 * {@link CommentDescription#CommentDescription(List, CompilationUnit, boolean, Comment)}.
	 * <p>
	 * If there is no white space character at the end of this fragment, this
	 * method will have no effect.
	 */
	public void removeFinalWhitespace() {
		if (Character.isWhitespace(text.charAt(text.length() - 1))) {
			text = text.substring(0, text.length() - 1);
		}
	}

	/**
	 * Returns the end position of this relative to the start of the file.
	 * 
	 * @return The position of the last character in this fragment (exclusive).
	 *         Will be &gt;= 0.
	 */
	public int getEndPosition() {
		return getStartPosition() + getLength();
	}

	/**
	 * Returns the number of characters in this.
	 * 
	 * @return The number of characters in this fragment. Will be &gt;= 0.
	 */
	public int getLength() {
		return text.length();
	}

	/**
	 * Sets how the spelling validator will interpret this fragment.
	 * 
	 * @param newSpellingState
	 *            The hint to the spelling validator that indicates how this
	 *            fragment should be treated. Cannot be null.
	 */
	public void setSpellingState(CheckerState newSpellingState) {
		this.spellingState = newSpellingState;
	}

	/**
	 * Returns the hint that the spelling validator uses to interpret this
	 * fragment.
	 * 
	 * @return The hint to the spelling validator that indicates how this
	 *         fragment should be treated. May be null if
	 *         {@link #setSpellingState(CheckerState)} has not been called yet.
	 */
	public CheckerState getSpellingState() {
		return spellingState;
	}

	/**
	 * Sets how the grammar validator will interpret this fragment.
	 * 
	 * @param newGrammarState
	 *            The hint to the grammar validator that indicates how this
	 *            fragment should be treated. Cannot be null.
	 */
	public void setGrammarState(CheckerState newGrammarState) {
		this.grammarState = newGrammarState;
	}

	/**
	 * Returns the hint that the grammar validator uses to interpret this
	 * fragment.
	 * 
	 * @return The hint to the grammar validator that indicates how this
	 *         fragment should be treated. May be null if
	 *         {@link #setGrammarState(CheckerState)} has not been called yet.
	 */
	public CheckerState getGrammarState() {
		return grammarState;
	}

	/**
	 * Sets whether this fragment indicates how to format text.
	 * 
	 * @param formatting
	 *            True indicates that this fragment formats text, false
	 *            indicates it does not.
	 */
	public void setFormatting(boolean formatting) {
		this.isFormatting = formatting;
	}

	/**
	 * Returns whether this fragment indicates how to format text.
	 * 
	 * @return True if this fragment formats text, false otherwise. See
	 *         {@link #setFormatting} for details.
	 */
	public boolean isFormatting() {
		return isFormatting;
	}

	/**
	 * Sets whether this fragment is a line ending that appears in the
	 * human-readable string.
	 * 
	 * @param relevantLineEnding
	 *            True indicates that this is a line ending that appears in the
	 *            human-readable string, false indicates that is either not a
	 *            line ending or does not appear in the human-readable string.
	 */
	public void setRelevantLineEnding(boolean relevantLineEnding) {
		this.isRelevantLineEnding = relevantLineEnding;
	}

	/**
	 * Returns whether this fragment is a line ending that appears in the
	 * human-readable string.
	 * 
	 * @return True if this fragment is a line ending that appears in the
	 *         human-readable string, false otherwise. See
	 *         {@link #setRelevantLineEnding} for details.
	 */
	public boolean isRelevantLineEnding() {
		return isRelevantLineEnding;
	}
}
