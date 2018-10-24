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
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ValidatorResource;

/**
 * Comment represents a Java doc comment in the compiled version of a file.
 * {@link CompilationUnit} represents compiled files, but provides inadequate
 * representation of spaces in comments. This class can be used to more
 * accurately detect formatting problems, and to create
 * {@link CommentDescription descriptions} which are appropriate to analyze the
 * grammar as an English sentence (HTML and Java doc tags are removed). The
 * locations of grammar problems can then be mapped back to the location in the
 * original file.
 * <p>
 * All text elements, including spaces, are represented by
 * {@link CommentFragment fragments}. Each fragment has information about its
 * location in the original file, and may contain information about its location
 * in a {@link CommentDescription#createHumanReadableString() human-readable
 * string}.
 * 
 * @author Trent Hoeppner
 */
public class Comment {

	/**
	 * The main description before any tags. See {@link #getDescription} for
	 * details.
	 */
	private CommentDescription description;

	/**
	 * The Java doc tags for this comment. This list may be empty, but will not
	 * be null after the constructor calls the
	 * {@link #createTags(Javadoc, CompilationUnit)} method.
	 */
	private List<Tag> tags;

	/**
	 * A complete list of spaces between the description and tags, as well as
	 * between the tag names and other parts of the tags, and between tags. The
	 * spaces are ordered according to their location in the file. This list may
	 * be empty, but will not be null after the constructor calls the
	 * {@link #createSpaces(Javadoc, CompilationUnit)} method.
	 */
	private List<CommentSpace> spaces;

	/**
	 * Constructor for Comment.
	 * 
	 * @param decl
	 *            The ASTNode which represents the comment to parse. Cannot be
	 *            null.
	 * @param comp
	 *            The compiled form of the file with all ASTNodes and line
	 *            numbers available. Cannot be null.
	 */
	public Comment(ValidatorResource resource, Javadoc decl, CompilationUnit comp) {
		description = createDescription(resource, decl, comp);
		tags = createTags(resource, decl, comp);
		spaces = createSpaces(decl, comp);
	}

	/**
	 * Returns the space that occurs after the given tag. This space occurs is
	 * after all fragments that belong to the tag.
	 * 
	 * @param tag
	 *            The tag to find the space after. Cannot be null.
	 * 
	 * @return The space that occurs after the given tag. Will be null if there
	 *         is no space after the given tag.
	 */
	public CommentSpace getSpaceAfterTag(Tag tag) {
		List<CommentFragment> fragments = tag.getFragments();
		CommentFragment lastFragment = fragments.get(fragments.size() - 1);
		return getSpaceAfterFragment(lastFragment);
	}

	/**
	 * Returns the space after the main description for this comment.
	 * 
	 * @return The space that occurs after the main description, or null if
	 *         there is no description or there is no space after the
	 *         description.
	 */
	public CommentSpace getSpaceAfterDescription() {
		if (description == null) {
			return null;
		}

		List<CommentFragment> fragments = description.getFragments();
		CommentFragment lastFragment = fragments.get(fragments.size() - 1);
		return getSpaceAfterFragment(lastFragment);
	}

	/**
	 * Returns the space before the given fragment.
	 * 
	 * @param fragment
	 *            The fragment to find the space before. Cannot be null.
	 * 
	 * @return The space that occurs before the given fragment, or null if there
	 *         is no such space.
	 */
	public CommentSpace getSpaceBeforeFragment(CommentFragment fragment) {
		CommentSpace found = null;

		for (CommentSpace space : spaces) {
			CommentFragment after = space.getAfter();
			if (after != null && after.equals(fragment)) {
				found = space;
				break;
			}
		}

		return found;
	}

	/**
	 * Returns the space after the given fragment.
	 * 
	 * @param fragment
	 *            The fragment to find the space after. Cannot be null.
	 * 
	 * @return The space that occurs after the given fragment, or null if there
	 *         is no such space.
	 */
	public CommentSpace getSpaceAfterFragment(CommentFragment fragment) {
		CommentSpace found = null;

		for (CommentSpace space : spaces) {
			CommentFragment before = space.getBefore();
			if (before != null && before.equals(fragment)) {
				found = space;
				break;
			}
		}

		return found;
	}

	/**
	 * Creates the spaces for the given Java doc node.
	 * 
	 * @param decl
	 *            The ASTNode which represents the comment to parse. Cannot be
	 *            null.
	 * @param comp
	 *            The compiled form of the file with all ASTNodes and line
	 *            numbers available. Cannot be null.
	 * 
	 * @return The list of spaces that exist in the given comment. Will not be
	 *         null.
	 */
	private List<CommentSpace> createSpaces(Javadoc decl, CompilationUnit comp) {
		// first collect all fragments
		List<CommentFragment> fragments = new ArrayList<CommentFragment>();
		List<CommentFragment> descFragments = Collections.EMPTY_LIST;
		if (description != null) {
			descFragments = description.getFragments();
		}
		fragments.addAll(descFragments);
		for (Tag tag : tags) {
			List<CommentFragment> tagFragments = tag.getFragments();
			fragments.addAll(tagFragments);
		}

		// find the empty spaces between fragments, and add them
		List<CommentSpace> elements = new ArrayList<CommentSpace>();

		CommentFragment previous = null;
		for (CommentFragment fragment : fragments) {
			int fragmentStartPos = fragment.getStartPosition();
			createAndAddCommentSpace(elements, decl, comp, previous, fragmentStartPos, fragment);
			previous = fragment;
		}

		int fragmentStartPos = decl.getStartPosition() + decl.getLength();
		CommentFragment fragment = null;
		createAndAddCommentSpace(elements, decl, comp, previous, fragmentStartPos, fragment);

		return elements;
	}

	/**
	 * Creates a space that ends before the <code>next</code> fragment and after
	 * the <code>previous</code> fragment.
	 * 
	 * @param elements
	 *            The spaces that have been collected so far. Cannot be null.
	 * @param decl
	 *            The ASTNode which represents the comment to parse. Cannot be
	 *            null.
	 * @param comp
	 *            The compiled form of the file with all ASTNodes and line
	 *            numbers available. Cannot be null.
	 * @param previous
	 *            The previous fragment. The end of this fragment marks the
	 *            beginning of the space to create. If null, the start of the
	 *            declaration will be the beginning of this space.
	 * @param fragmentStartPos
	 *            The start position of the fragment after the space.
	 * @param next
	 *            The next fragment after the space, which the space points to.
	 *            If null, this means that there is no fragment after the space
	 *            to be created.
	 */
	private void createAndAddCommentSpace(List<CommentSpace> elements, Javadoc decl, CompilationUnit comp,
			CommentFragment previous, int fragmentStartPos, CommentFragment next) {
		int previousEndPos;
		if (previous == null) {
			// add a space for the beginning if there is a space there
			previousEndPos = decl.getStartPosition();
		} else {
			// add a space between previous and this if there is one
			previousEndPos = previous.getStartPosition() + previous.getText().length();
		}

		int length = fragmentStartPos - previousEndPos;
		if (length > 0) {
			CommentFragment spaceLine = createSpaceFragment(comp, previousEndPos, length);
			CommentSpace space = new CommentSpace(spaceLine, previous, next, comp);
			elements.add(space);
		}
	}

	/**
	 * Returns a new fragment to represent the text for a space. The space will
	 * contain only single space characters (" "), even though the actual
	 * characters may be different.
	 * 
	 * @param comp
	 *            The compiled form of the file with all ASTNodes and line
	 *            numbers available. Cannot be null.
	 * @param startPos
	 *            The start position of the fragment to create. Must be &gt;= 0.
	 * @param length
	 *            The number of characters in the fragment to create. Must be
	 *            &gt;= 0.
	 * 
	 * @return A new fragment to represent the indicated space. Will not be
	 *         null.
	 */
	private CommentFragment createSpaceFragment(CompilationUnit comp, int startPos, int length) {

		int sourceLine = comp.getLineNumber(startPos) - 1;
		int sourceColumn = comp.getColumnNumber(startPos);

		String newSpaces = CommentUtil.createSpaces(length);
		CommentFragment fragment = new CommentFragment(startPos, newSpaces, sourceLine, sourceColumn);

		return fragment;
	}

	/**
	 * Creates the main description for this comment.
	 * 
	 * @param decl
	 *            The ASTNode which represents the comment to parse. Cannot be
	 *            null.
	 * @param comp
	 *            The compiled form of the file with all ASTNodes and line
	 *            numbers available. Cannot be null.
	 * 
	 * @return The description, or null if there is no description.
	 */
	private CommentDescription createDescription(ValidatorResource resource, Javadoc decl, CompilationUnit comp) {
		CommentDescription comment = null;
		try {
			List astTags = decl.tags();
			if (astTags.size() > 0) {
				TagElement tag = (TagElement) astTags.get(0);
				if (tag.getTagName() == null) {
					// it's the description
					List fragments = tag.fragments();
					CommentDescription tagDescription = new CommentDescription(resource, fragments, comp, false, this);
					comment = tagDescription;
				}
			}
		} catch (Exception e) {
			Debug.COMMENT.log(e);
		}
		return comment;
	}

	/**
	 * Creates the tags that are in this comment. Each tag has different parts
	 * represented, not including spaces.
	 * 
	 * @param decl
	 *            The ASTNode which represents the comment to parse. Cannot be
	 *            null.
	 * @param comp
	 *            The compiled form of the file with all ASTNodes and line
	 *            numbers available. Cannot be null.
	 * 
	 * @return A new list of tags that exist for this comment. Will not be null.
	 */
	private List<Tag> createTags(ValidatorResource resource, Javadoc decl, CompilationUnit comp) {
		List<Tag> newTags = new ArrayList<Tag>();
		try {
			List jdtTags = decl.tags();
			for (Object object : jdtTags) {
				TagElement jdtTag = (TagElement) object;
				String tagNameString = jdtTag.getTagName();
				if (tagNameString != null) {
					// it's a tag, not a description
					Tag tag = new Tag(resource, comp, jdtTag);
					newTags.add(tag);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return newTags;
	}

	/**
	 * Returns the <code>@return</code> tag for this comment.
	 * 
	 * @return The <code>@return</code> tag. Will be null if there is no such
	 *         tag.
	 */
	public Tag getReturnTag() {
		Tag returnTag = null;
		for (Tag tag : tags) {
			String tagName = tag.getName().getText();
			if (tagName.equals("@return")) {
				returnTag = tag;
				break;
			}
		}

		return returnTag;
	}

	/**
	 * Returns the tags in this that are named <code>@throws</code> or
	 * <code>@exception</code>.
	 * 
	 * @return The list of throws tags in this. Will not be null, but will be
	 *         empty if there are no throws tags.
	 */
	public List<Tag> getThrowsTags() {
		List<Tag> throwsTags = new ArrayList<Tag>();
		for (Tag tag : tags) {
			String tagName = tag.getName().getText();
			if (tagName.equals("@throws") || tagName.equals("@exception")) {
				throwsTags.add(tag);
			}
		}

		return throwsTags;
	}

	/**
	 * Returns the main description for this comment.
	 * 
	 * @return The main description, or null if there is no description.
	 */
	public CommentDescription getDescription() {
		return description;
	}

	/**
	 * Returns all top-level tags in this comment. Tags in the description or in
	 * other tags will not be included.
	 * 
	 * @return The top-level tags in this. Will not be null.
	 */
	public List<Tag> getAllTags() {
		return tags;
	}

	/**
	 * Returns the <code>@param</code> tags in this comment.
	 * 
	 * @return The <code>@param</code> tags in this. Will not be null, but may
	 *         be empty if there are no such tags.
	 */
	public List<Tag> getParamTags() {
		List<Tag> paramTags = new ArrayList<Tag>();
		for (Tag tag : tags) {
			String tagName = tag.getName().getText();
			if (tagName.equals("@param")) {
				paramTags.add(tag);
			}
		}

		return paramTags;
	}

}
