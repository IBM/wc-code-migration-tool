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

/**
 * FragmentPos represents a single character in a comment in the original file.
 * It is used to process characters in a way that does not depend on the
 * boundaries between fragments.
 * 
 * @author Trent Hoeppner
 */
public class FragmentPos implements Comparable<FragmentPos> {

	/**
	 * The fragment to which this belongs. See {@link #getFragment} for details.
	 */
	private CommentFragment fragment;

	/**
	 * The position of this character relative to the beginning of the fragment.
	 * See {@link #getPosWithinFragment} for details.
	 */
	private int posWithinFragment;

	/**
	 * Constructor for FragmentPos.
	 * 
	 * @param newFragment
	 *            The fragment to which this belongs. Cannot be null.
	 * @param newPosWithinFragment
	 *            The 0-based index of the character relative to the beginning
	 *            of the fragment. Must be &gt;= 0.
	 */
	public FragmentPos(CommentFragment newFragment, int newPosWithinFragment) {
		this.fragment = newFragment;
		this.posWithinFragment = newPosWithinFragment;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getChar();
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		FragmentPos other = (FragmentPos) obj;
		if (getChar() != other.getChar()) {
			return false;
		}

		return true;
	}

	/**
	 * Returns the starting position of this character relative to the beginning
	 * of the file.
	 * 
	 * @return The 0-based index from the beginning of the file where the
	 *         character represented by this occurs. Will be &gt;= 0.
	 */
	public int getStartPos() {
		return fragment.getStartPosition() + posWithinFragment;
	}

	/**
	 * Returns the character that this represents.
	 * 
	 * @return The character that this represents.
	 */
	public char getChar() {
		return fragment.getText().charAt(posWithinFragment);
	}

	/**
	 * Returns whether or not this character is whitespace. This is a
	 * convenience method that is equivalent to
	 * <code>Character.isWhitespace(getChar())</code>.
	 * 
	 * @return True if the character represented by this is whitespace, false
	 *         otherwise.
	 */
	public boolean isWhitespace() {
		return Character.isWhitespace(getChar());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(FragmentPos o) {
		return getStartPos() - o.getStartPos();
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return Character.toString(getChar());
	}

	/**
	 * Returns the fragment that this belongs to.
	 * 
	 * @return The fragment that contains the character that this represents.
	 *         Will not be null.
	 */
	public CommentFragment getFragment() {
		return fragment;
	}

	/**
	 * Returns the position of the character represented by this relative to the
	 * beginning of the file.
	 * 
	 * @return The 0-based index of the character represented by this, relative
	 *         to the beginning of the fragment. Will be &gt;= 0.
	 */
	public int getPosWithinFragment() {
		return posWithinFragment;
	}
}
