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

import com.ibm.commerce.qcheck.core.comment.CommentFragment.CheckerState;

/**
 * HumanFragmentView is a view of a CommentDescription that maps each character
 * in the human-readable string back to a position back in the description. This
 * class can record changes which do not update the original description, but
 * which can be used to create a string used for suggestions. Insert, update,
 * delete are supported on each character.
 * 
 * @author Trent Hoeppner
 */
public class HumanFragmentView {

	/**
	 * The original description that this view is based off of.
	 */
	private CommentDescription desc;

	/**
	 * The start position of this view within the human-readable string.
	 */
	private int commentStartPos;

	/**
	 * The end position (exclusive) of this view within the human-readable
	 * string.
	 */
	private int commentEndPos;

	/**
	 * All the characters that exist in this view, including modifications.
	 */
	private List<Char> allChars;

	/**
	 * Constructor for HumanFragmentView based on a start position and length.
	 * 
	 * @param newCommentStartPos
	 *            The 0-based index of the first character in this view relative
	 *            to the beginning of the human-readable string. Must be &gt;=
	 *            0.
	 * @param newCommentLength
	 *            The number of characters to include from the unmodified
	 *            human-readable string. Must be &gt;= 0.
	 * @param newDesc
	 *            The comment description that contains the original text from
	 *            the source file and corresponding human-readable string.
	 *            Cannot be null.
	 */
	public HumanFragmentView(int newCommentStartPos, int newCommentLength, CommentDescription newDesc) {
		this(newDesc, newCommentStartPos, newCommentStartPos + newCommentLength);
	}

	/**
	 * Constructor for HumanFragmentView based on a start position and length.
	 * 
	 * @param newDesc
	 *            The comment description that contains the original text from
	 *            the source file and corresponding human-readable string.
	 *            Cannot be null.
	 * @param newCommentStartPos
	 *            The 0-based index of the first character in this view relative
	 *            to the beginning of the human-readable string. Must be &gt;=
	 *            0.
	 * @param newCommentEndPos
	 *            The 0-based index of the last character (exclusive) in this
	 *            view relative to the beginning of the human-readable string.
	 *            Must be &gt;= 0.
	 */
	public HumanFragmentView(CommentDescription newDesc, int newCommentStartPos, int newCommentEndPos) {
		this.desc = newDesc;
		this.commentStartPos = newCommentStartPos;
		this.commentEndPos = newCommentEndPos;
		allChars = initSourceFragmentChars(commentStartPos, newCommentEndPos);
	}

	/**
	 * Returns the start position in the human-readable string.
	 * 
	 * @param modified
	 *            True if the start position should be based on the modified
	 *            version of the human-readable string, false if it should be
	 *            based on the original human-readable string.
	 * 
	 * @return The 0-based start position of this view within the human-readable
	 *         string. Will be &gt;= 0.
	 */
	public int getCommentStartPos(boolean modified) {
		return commentStartPos;
	}

	/**
	 * Returns the end position in the human-readable string.
	 * 
	 * @param modified
	 *            True if the end position should be based on the modified
	 *            version of the human-readable string, false if it should be
	 *            based on the original human-readable string.
	 * 
	 * @return The 0-based end position of this view within the human-readable
	 *         string, which is one character after the last character. Will be
	 *         &gt;= 0.
	 */
	public int getCommentEndPos(boolean modified) {
		return modified ? getCommentStartPos(modified) + getLength(modified) : commentEndPos;
	}

	/**
	 * Returns the length of the human-readable string.
	 * 
	 * @param modified
	 *            True if the length should be based on the modified version of
	 *            the human-readable string, false if it should be based on the
	 *            original human-readable string.
	 * 
	 * @return The number of characters in the human-readable string. Will be
	 *         &gt;= 0.
	 */
	public int getLength(boolean modified) {
		int length = 0;
		for (Char character : allChars) {
			if (character.isInHuman(modified)) {
				length++;
			}
		}

		return length;
	}

	/**
	 * Expands the human-readable string to include characters that exist either
	 * on the left or right of the current human-readable view. This affects the
	 * {@link #getSourceView(boolean) source view} by implicitly including any
	 * characters that are not visible in the human-readable string. In the
	 * special case, if the boundary is expanded to encompass a human-readable
	 * character, and the characters immediately following are only visible in
	 * the source view, those following characters will be included in the
	 * source view (but not the human-readable view).
	 * 
	 * @param commentChars
	 *            The number of characters to grow in the human-readable string.
	 *            Must be &gt;= 0.
	 * @param left
	 *            True indicates that the starting position will be decreased by
	 *            the given amount, false indicates that the end position will
	 *            be increased by the given amount.
	 */
	public void expand(int commentChars, boolean left) {
		// when expanding left or right, we overlap with the character that is
		// being expanded from. This ensures that "invisible" characters between
		// the human-readable characters (i.e. in the source string but not the
		// human-readable string) will appear. After that, we remove the
		// overlapped character and add the new chars appropriately.

		if (left) {
			int newCommentStartPos = commentStartPos - commentChars;
			List<Char> newChars = initSourceFragmentChars(newCommentStartPos, commentStartPos + 1);
			if (newChars.size() >= 2) {
				Char first = newChars.get(newChars.size() - 2);
				Char second = newChars.get(newChars.size() - 1);
				if (first.getChar() == '\r' && second.getChar() == '\n') {
					newChars.remove(newChars.size() - 2);
					newChars.remove(newChars.size() - 2);
				} else {
					newChars.remove(newChars.size() - 1);
				}
			} else {
				newChars.remove(newChars.size() - 1);
			}
			allChars.addAll(0, newChars);
			commentStartPos = newCommentStartPos;
		} else {
			int newCommentEndPos = commentEndPos + commentChars;
			List<Char> newChars = initSourceFragmentChars(commentEndPos - 1, newCommentEndPos);
			if (newChars.size() >= 2) {
				Char first = newChars.get(0);
				Char second = newChars.get(1);
				if (first.getChar() == '\r' && second.getChar() == '\n') {
					newChars.remove(0);
					newChars.remove(0);
				} else {
					newChars.remove(0);
				}
			} else {
				newChars.remove(0);
			}
			allChars.addAll(newChars);
			commentEndPos = newCommentEndPos;
		}

	}

	/**
	 * Deletes the given range of characters from the human-readable view.
	 * 
	 * @param commentPos
	 *            The 0-based index of the first character to delete, where 0
	 *            represents the first character in the human-readable string.
	 *            Must be &gt;= 0.
	 * @param length
	 *            The number of characters to delete. Must be &gt;= 0.
	 * @param deleteLeftWhiteSpace
	 *            True indicates that if there is whitespace to the left of the
	 *            indicated starting position then that whitespace will also be
	 *            deleted, false indicates that whitespace on the left will not
	 *            be deleted.
	 * @param deleteRightWhiteSpace
	 *            True indicates that if there is whitespace to the right of the
	 *            last character in the range then that whitespace will also be
	 *            deleted, false indicates that whitespace on the right will not
	 *            be deleted.
	 */
	public void deleteInHumanReadableString(int commentPos, int length, boolean deleteLeftWhiteSpace,
			boolean deleteRightWhiteSpace) {
		for (int i = 0; i < length; i++) {
			deleteInHumanReadableString(commentPos + i, deleteLeftWhiteSpace, deleteRightWhiteSpace);
		}
	}

	/**
	 * Deletes a single character in the human-readable view.
	 * 
	 * @param modifiedCommentPos
	 *            The 0-based index of the character to delete, where 0
	 *            represents the first character in the human-readable string.
	 *            Must be &gt;= 0.
	 * @param deleteLeftWhiteSpace
	 *            True indicates that if there is whitespace to the left of the
	 *            indicated position then that whitespace will also be deleted,
	 *            false indicates that whitespace on the left will not be
	 *            deleted.
	 * @param deleteRightWhiteSpace
	 *            True indicates that if there is whitespace to the right of the
	 *            indicated character then that whitespace will also be deleted,
	 *            false indicates that whitespace on the right will not be
	 *            deleted.
	 */
	public void deleteInHumanReadableString(int modifiedCommentPos, boolean deleteLeftWhiteSpace,
			boolean deleteRightWhiteSpace) {

		int charIndexToDelete = findIndexInAllChars(modifiedCommentPos);
		if (charIndexToDelete < 0) {
			throw new IllegalStateException(
					"Could not find the character for the modifiedCommentPos = " + modifiedCommentPos);
		}

		deleteInAllChars(deleteLeftWhiteSpace, deleteRightWhiteSpace, false, charIndexToDelete);
	}

	/**
	 * Deletes the given character from {@link #allChars}.
	 * 
	 * @param deleteLeftWhiteSpace
	 *            True indicates that if there is whitespace to the left of the
	 *            indicated position then that whitespace will also be deleted,
	 *            false indicates that whitespace on the left will not be
	 *            deleted.
	 * @param deleteRightWhiteSpace
	 *            True indicates that if there is whitespace to the right of the
	 *            indicated character then that whitespace will also be deleted,
	 *            false indicates that whitespace on the right will not be
	 *            deleted.
	 * @param removeFromAllChars
	 *            True indicates that the character will be removed from the
	 *            list, false indicates that it will only be marked for deletion
	 *            (invisible in both the source and human-readable views).
	 * @param charIndexToDelete
	 *            The 0-based index of the character to delete, where 0
	 *            represents the first character in {@link #allChars}. Must be
	 *            &gt;= 0.
	 */
	private void deleteInAllChars(boolean deleteLeftWhiteSpace, boolean deleteRightWhiteSpace,
			boolean removeFromAllChars, int charIndexToDelete) {
		Char charToDelete = allChars.get(charIndexToDelete);
		charToDelete.delete();
		if (charToDelete.isWhitespace()) {
			// delete all whitespace until the next and previous characters that
			// occur in the source string but not the human-readable string

			boolean deletedIsLineEnding = charToDelete.isRelevantLineEnding();

			// delete next whitespace
			if (deleteRightWhiteSpace) {
				for (int i = charIndexToDelete + 1; i < allChars.size(); i++) {
					Char nextCharacter = allChars.get(i);
					if (nextCharacter.isInvisibleWhitespace()
							|| isVisibleButDeleteableLineEnding(deletedIsLineEnding, charIndexToDelete, i)) {
						if (removeFromAllChars) {
							allChars.remove(i);
							i--;
						} else {
							nextCharacter.delete();
						}
					} else {
						break;
					}
				}
			}

			// delete previous whitespace
			if (deleteLeftWhiteSpace) {
				for (int i = charIndexToDelete - 1; i >= 0; i--) {
					Char prevCharacter = allChars.get(i);
					if (prevCharacter.isInvisibleWhitespace()
							|| isVisibleButDeleteableLineEnding(deletedIsLineEnding, charIndexToDelete, i)) {
						if (removeFromAllChars) {
							allChars.remove(i);
						} else {
							prevCharacter.delete();
						}
					} else {
						break;
					}
				}
			}
		}
	}

	/**
	 * Converts the given index of a character in the human-readable string to
	 * an index into {@link #allChars}.
	 * 
	 * @param modifiedCommentPos
	 *            The 0-based index into the modified human-readable string.
	 *            Must be &gt;= 0.
	 * 
	 * @return The 0-based index into {@link #allChars} which represents the
	 *         indicated character in the human-readable string. If no such
	 *         character exists, -1 will be returned.
	 */
	private int findIndexInAllChars(int modifiedCommentPos) {
		int index = -1;

		int realIndex = 0;
		int currentIndex = -1;
		for (Char character : allChars) {
			if (character.isInHuman(true)) {
				currentIndex++;
			}

			if (currentIndex == modifiedCommentPos) {
				index = realIndex;
				break;
			}

			realIndex++;
		}

		return index;
	}

	/**
	 * Returns whether the character at the given index in {@link #allChars} can
	 * be deleted because it is a line ending and the character at the deleted
	 * index is being deleted and is part of the same line ending.
	 * 
	 * @param deletedIsLineEnding
	 *            True indicates that the character at deletedIndex is a line
	 *            ending character ("\r" or "\n"), false indicates that it is
	 *            not.
	 * @param deletedIndex
	 *            The 0-based index in allChars of the character being deleted.
	 *            Must be &gt;= 0.
	 * @param index
	 *            The 0-based index in allChars of the character that is being
	 *            considered as also being part of the same line ending. Must be
	 *            &gt;= 0.
	 * 
	 * @return True if the character at the index should be deleted, false
	 *         otherwise.
	 */
	// take care of \r\n - delete one and we have to delete the other
	private boolean isVisibleButDeleteableLineEnding(boolean deletedIsLineEnding, int deletedIndex, int index) {
		boolean isVisibleButDeletableLineEnding = false;
		if (deletedIsLineEnding) {
			boolean isAfterDeletedPos = index == deletedIndex + 1 && index < allChars.size();
			boolean isBeforeDeletedPos = index == deletedIndex - 1 && index >= 0;
			boolean nextIsLineEnding = (isAfterDeletedPos || isBeforeDeletedPos)
					&& allChars.get(index).isRelevantLineEnding();
			if (nextIsLineEnding) {
				isVisibleButDeletableLineEnding = true;
			}
		}
		return isVisibleButDeletableLineEnding;
	}

	/**
	 * Returns the human-readable string.
	 * 
	 * @param modified
	 *            True if the returned string should include modifications made
	 *            to this, false if it should not include any modifications.
	 * 
	 * @return The human-readable string. Will not be null, but may be empty.
	 */
	public String getHumanView(boolean modified) {
		StringBuffer buf = new StringBuffer();

		for (Char character : allChars) {
			if (character.isInHuman(modified)) {
				buf.append(character.getChar());
			}
		}

		return buf.toString();
	}

	/**
	 * Returns whether the given fragment character is in the human-readable
	 * string.
	 * 
	 * @param pos
	 *            The character to determine whether it is in the human-readable
	 *            string. Cannot be null.
	 * 
	 * @return True if the character is in the human-readable string, false
	 *         otherwise.
	 */
	private boolean inHumanReadableString(FragmentPos pos) {
		return pos.getFragment().getGrammarState() == CheckerState.APPLICABLE
				|| pos.getFragment().getGrammarState() == CheckerState.VISIBLE;
	}

	/**
	 * Returns the source code view that corresponds to the human-readable
	 * string. This might include some characters that would are excluded from
	 * the human-readable view, such as text between opening and closing
	 * <code>&lt;code&gt;</code> tags.
	 * 
	 * @param modified
	 *            True if the returned string should include modifications made
	 *            to this, false if it should not include any modifications.
	 * 
	 * @return The source code string. Will not be null, but may be empty.
	 */
	public String getSourceView(boolean modified) {
		StringBuffer buf = new StringBuffer();

		for (Char character : allChars) {
			if (character.isInSource(modified)) {
				buf.append(character.getChar());
			}
		}

		return buf.toString();
	}

	/**
	 * Returns all characters in this, but excluding special cases where a
	 * character is inserted and later deleted.
	 * 
	 * @return The characters in this as they would appear in the source code.
	 *         Will not be null, but may be empty.
	 */
	public List<Char> getSourceChars() {
		List<Char> sourceChars = new ArrayList();

		// add all the valid characters. Some characters that were inserted may
		// have been deleted later, so we filter these out.
		for (Char character : allChars) {
			if (!(character.isInserted() && character.isDeleted())) {
				sourceChars.add(character);
			}
		}
		return sourceChars;
	}

	/**
	 * Creates and returns a list of characters, taken from the start and end
	 * range of {@link #desc the description}.
	 * 
	 * @param commentStartPos
	 *            The 0-based starting index into the human-readable string
	 *            returned by
	 *            {@link CommentDescription#getHumanReadableString()}. Will be
	 *            &gt;= 0.
	 * @param commentEndPos
	 *            The 0-based ending index (exclusive) into the human-readable
	 *            string returned by
	 *            {@link CommentDescription#getHumanReadableString()}. Will be
	 *            &gt;= 0.
	 * 
	 * @return A list of characters that represents their status and visibility
	 *         in the human-readable string and source view. Will not be null.
	 */
	private List<Char> initSourceFragmentChars(int commentStartPos, int commentEndPos) {
		// check for line breaks
		String humanString = desc.getHumanReadableString();

		// if the comment starts at \n inside \r\n, step it back to point to \r
		if (humanString.charAt(commentStartPos) == '\n' && commentStartPos > 0
				&& humanString.charAt(commentStartPos - 1) == '\r') {
			commentStartPos--;
		}

		// if the comment ends at \r inside \r\n, push it forward to point to \n
		if (humanString.charAt(commentEndPos - 1) == '\r' && commentEndPos < humanString.length()
				&& humanString.charAt(commentEndPos) == '\n') {
			commentEndPos++;
		}

		boolean endIsDoubleLineBreak = commentEndPos > 1 && humanString.charAt(commentEndPos - 2) == '\r'
				&& humanString.charAt(commentEndPos - 1) == '\n';

		int sourceStart = desc.findInComment(commentStartPos);
		int sourceEnd = desc.findInComment(commentEndPos - 1) + 1;
		if (endIsDoubleLineBreak) {
			// findInComment finds the '\r' in "\r\n", so we add one to point to
			// "\n"
			sourceEnd++;
		}

		List<Char> newChars = new ArrayList<Char>();
		for (CommentFragment fragment : desc.getFragments()) {
			if (sourceStart <= fragment.getEndPosition() && sourceEnd > fragment.getStartPosition()) {
				for (int i = 0; i < fragment.getLength(); i++) {
					if (sourceStart <= fragment.getStartPosition() + i && sourceEnd > fragment.getStartPosition() + i) {
						FragmentPos pos = new FragmentPos(fragment, i);
						Char originalChar = new OriginalChar(pos);
						newChars.add(originalChar);
					}
				}
			}

		}

		return newChars;
	}

	/**
	 * Takes the given string, finds all differences with the human-readable
	 * string in this, and makes corresponding updates in this. After this
	 * method is called, the modified versions of both the
	 * {@link #getHumanView(boolean) human-readable view} and the
	 * {@link #getSourceView(boolean) source view} will reflect the changes.
	 * 
	 * @param replacement
	 *            The string to replace the human-readable view of this. Cannot
	 *            be null, but may be empty.
	 */
	public void handleDiff(String replacement) {
		int[][] c = lcs(this, replacement);
		printDiff(c, this, getHumanView(false), replacement, getLength(false), replacement.length());

		// clean out the invisible whitespace that should be deleted. We can't
		// do this during the comparison because the algorithm is looking for
		// exact changes between strings.
		for (int i = 0; i < allChars.size(); i++) {
			HumanFragmentView.Char character = allChars.get(i);
			if (character.isDeleted() && character.isWhitespace()) {
				deleteInAllChars(false, true, false, i);
			}
		}

	}

	/**
	 * Runs the longest common subsequence algorithm as described in
	 * <samp>Wikipedia</samp>.
	 * 
	 * @param first
	 *            The view which represents the first string to compare. Cannot
	 *            be null.
	 * @param second
	 *            The second string to compare to. Cannot be null, but may be
	 *            empty.
	 * 
	 * @return A matrix that describes the differences between the two strings.
	 *         Will not be null, and both dimensions of the matrix will contain
	 *         at least 1 element.
	 */
	private int[][] lcs(HumanFragmentView first, String second) {
		int firstArrayLength = first.getLength(false) + 1;
		int secondArrayLength = second.length() + 1;
		int[][] c = new int[firstArrayLength][secondArrayLength];
		for (int i = 0; i < firstArrayLength; i++) {
			c[i][0] = 0;
		}
		for (int j = 0; j < secondArrayLength; j++) {
			c[0][j] = 0;
		}

		String firstString = first.getHumanView(false);

		if (firstArrayLength - 1 != firstString.length()) {
			throw new IllegalStateException("The length of the human view (" + firstString.length()
					+ ")is not the same as the calculated length (" + (firstArrayLength - 1) + ")");
		}

		for (int i = 1; i < firstArrayLength; i++) {
			for (int j = 1; j < secondArrayLength; j++) {
				if (firstString.charAt(i - 1) == second.charAt(j - 1)) {
					c[i][j] = c[i - 1][j - 1] + 1;
				} else {
					c[i][j] = Math.max(c[i][j - 1], c[i - 1][j]);
				}
			}
		}
		return c;
	}

	/**
	 * Performs the modifications on first that make it match second.
	 * 
	 * @param c
	 *            The difference matrix as returned by
	 *            {@link #lcs(HumanFragmentView, String) the LCS algorithm}.
	 * @param first
	 *            The view that was used as input to the <code>LCS</code>
	 *            algorithm.
	 * @param firstString
	 *            A string representation of the original string before any
	 *            modifications.
	 * @param second
	 *            The string used as input to the <code>LCS</code> algorithm.
	 * @param i
	 *            The current 0-based index into the first dimension of the
	 *            difference matrix (a recursive parameter). The initial value
	 *            should be the length of the unmodified human-readable string.
	 *            Must be &gt;= 0.
	 * @param j
	 *            The current 0-based index into the second dimension of the
	 *            difference matrix (a recursive parameter). The initial value
	 *            should be the length of the second string. Must be &gt;= 0.
	 * 
	 * @return The change in length after this call, which is cumulative with
	 *         previous changes. Will be &gt;= 0.
	 */
	private int printDiff(int[][] c, HumanFragmentView first, String firstString, String second, int i, int j) {
		int change = 0;
		if (i > 0 && j > 0 && firstString.charAt(i - 1) == second.charAt(j - 1)) {
			change = printDiff(c, first, firstString, second, i - 1, j - 1);
			// it's a common char - do nothing
		} else {
			if (j > 0 && (i == 0 || c[i][j - 1] >= c[i - 1][j])) {
				change = printDiff(c, first, firstString, second, i, j - 1);
				// y[j] was added
				first.insertInHumanReadableString(i + change, second.charAt(j - 1));
				change++;
			} else if (i > 0 && (j == 0 || c[i][j - 1] < c[i - 1][j])) {
				change = printDiff(c, first, firstString, second, i - 1, j);
				// x[i] was deleted
				first.deleteInHumanReadableString(i + change - 1, false, false);
				change--;
			} else {
				// invalid condition
			}
		}

		return change;
	}

	/**
	 * Inserts the given character in {@link #allChars}. The new character will
	 * have the same visibility as the character before it (or APPLICABLE if
	 * there is no previous character).
	 * 
	 * @param i
	 *            The 0-based index into {@link #allChars} where the new
	 *            character should be inserted. Must be &gt;= 0.
	 * @param charAt
	 *            The character to insert.
	 */
	private void insertInHumanReadableString(int i, char charAt) {
		CommentFragment.CheckerState spellingState;
		if (i > 0 && i <= allChars.size()) {
			Char previousChar = allChars.get(i - 1);
			spellingState = previousChar.getSpellingState();
		} else {
			spellingState = CheckerState.APPLICABLE;
		}
		InsertedChar insertedChar = new InsertedChar(charAt, spellingState);
		allChars.add(i, insertedChar);
	}

	/**
	 * Returns the source view of this.
	 * 
	 * @return The modified source view of this. Will not be null, but may be
	 *         empty.
	 */
	public String toString() {
		return getSourceView(true);
	}

	/**
	 * Char represents a single character in the source and/or human-readable
	 * view of this. Various states of the character can be queried.
	 */
	public interface Char {

		/**
		 * Returns whether this is in the source view.
		 * 
		 * @param modified
		 *            True indicates that the we want to consider if this
		 *            character is part of the modified source view, false
		 *            indicates we want to consider if this character is part of
		 *            the original source view.
		 * 
		 * @return True if this is in the source view, false otherwise.
		 */
		boolean isInSource(boolean modified);

		/**
		 * Returns whether this is invisible whitespace.
		 * 
		 * @return True if this is whitespace that is not in the human-readable
		 *         view, false otherwise.
		 */
		boolean isInvisibleWhitespace();

		/**
		 * Returns whether this is a line ending that will show in the
		 * human-readable view.
		 * 
		 * @return True if this is a line ending that will show in the
		 *         human-readable view, false otherwise.
		 */
		boolean isRelevantLineEnding();

		/**
		 * Returns whether this is whitespace.
		 * 
		 * @return True if this is whitespace, false otherwise.
		 */
		boolean isWhitespace();

		/**
		 * Returns whether this is in the human-readable view.
		 * 
		 * @param modified
		 *            True indicates that the we want to consider if this
		 *            character is part of the modified human-readable view,
		 *            false indicates we want to consider if this character is
		 *            part of the original human-readable view.
		 * 
		 * @return True if this is in the source view, false otherwise.
		 */
		boolean isInHuman(boolean modified);

		/**
		 * Returns whether this has been deleted.
		 * 
		 * @return True if this has been deleted, false otherwise.
		 */
		boolean isDeleted();

		/**
		 * Returns whether this has been inserted. Note that a character can be
		 * inserted first, then deleted, so that both this method and
		 * {@link #isDeleted()} can return true at the same time.
		 * 
		 * @return True if this has been inserted, false otherwise.
		 */
		boolean isInserted();

		/**
		 * Returns the character value for this.
		 * 
		 * @return The character value.
		 */
		char getChar();

		/**
		 * Deletes this, so that it is ignored in all views. This method will
		 * not remove this from {@link HumanFragmentView#allChars}.
		 */
		void delete();

		/**
		 * Restores this from deleted status. This will only have an effect if
		 * this was first deleted.
		 */
		void undelete();

		/**
		 * Returns the spelling state of this.
		 * 
		 * @return The spelling state. Will not be null.
		 */
		CheckerState getSpellingState();
	}

	private class OriginalChar implements Char {

		/**
		 * The character that this represents. Will never be null.
		 */
		private FragmentPos pos;

		/**
		 * True after calling {@link #delete()}, false by default or after
		 * calling {@link #undelete()}.
		 */
		private boolean isDeleted;

		/**
		 * Constructor for OriginalChar.
		 * 
		 * @param pos
		 *            The character that this will represent. Cannot be null.
		 */
		public OriginalChar(FragmentPos pos) {
			this.pos = pos;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public char getChar() {
			return pos.getChar();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isInHuman(boolean modified) {
			boolean humanReadable = inHumanReadableString(pos);
			return modified ? !isDeleted && humanReadable : humanReadable;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isInSource(boolean modified) {
			return modified ? !isDeleted : true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void delete() {
			isDeleted = true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isWhitespace() {
			return pos.isWhitespace();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isRelevantLineEnding() {
			return pos.getFragment().isRelevantLineEnding();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isInvisibleWhitespace() {
			return !inHumanReadableString(pos) && (pos.isWhitespace() || pos.getChar() == '*');
		}

		/**
		 * {@inheritDoc}
		 */
		public String toString() {
			return pos.toString();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isDeleted() {
			return isDeleted;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isInserted() {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void undelete() {
			isDeleted = false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public CheckerState getSpellingState() {
			return pos.getFragment().getSpellingState();
		}

	}

	// we always assume an inserted char is in the human-readable string
	private class InsertedChar implements Char {

		/**
		 * The character that this represents. Will never be null.
		 */
		private char character;

		/**
		 * True after calling {@link #delete()}, false by default or after
		 * calling {@link #undelete()}.
		 */
		private boolean isDeleted;

		/**
		 * The visibility of this to the spell checker. Can never be null.
		 */
		private CommentFragment.CheckerState spellingState;

		/**
		 * Constructor for InsertedChar.
		 * 
		 * @param character
		 *            The character that this will represent. Cannot be null.
		 * @param spellingState
		 *            The visibility of this to the spell checker. Cannot be
		 *            null.
		 */
		public InsertedChar(char character, CommentFragment.CheckerState spellingState) {
			this.character = character;
			this.spellingState = spellingState;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void delete() {
			isDeleted = true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public char getChar() {
			return character;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isInHuman(boolean modified) {
			return modified ? !isDeleted : false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isInSource(boolean modified) {
			return modified ? !isDeleted : false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isWhitespace() {
			return Character.isWhitespace(character);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isRelevantLineEnding() {
			return character == '\r' || character == '\n';
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isInvisibleWhitespace() {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public String toString() {
			return Character.toString(character);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isDeleted() {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isInserted() {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void undelete() {
			isDeleted = false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public CheckerState getSpellingState() {
			return spellingState;
		}
	}
}
