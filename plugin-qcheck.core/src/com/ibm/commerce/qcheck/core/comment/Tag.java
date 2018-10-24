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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TagElement;

import com.ibm.commerce.qcheck.core.ValidatorResource;

/**
 * Tag represents a tag in a Java doc comment, such as a <code>param</code> or
 * <code>return</code> tag.
 * 
 * @author Trent Hoeppner
 */
public class Tag extends CommentElement {

	/**
	 * The name of the tag. This always exists. See {@link #getName} for
	 * details.
	 */
	private CommentFragment name;

	/**
	 * A further qualifier for the tag. For <code>@param</code> tags, this will
	 * be the parameter name. For <code>@exception</code> tags, this will be the
	 * exception name. See {@link #getSecondName} for details.
	 */
	private List<CommentFragment> secondName;

	/**
	 * The description of the tag. See {@link #getComment} for details.
	 */
	private CommentDescription comment;

	/**
	 * A list of all the fragments in this. Will never be null.
	 */
	private List<CommentFragment> allFragments;

	/**
	 * Constructor for Tag with pre-processed values.
	 * 
	 * @param newName
	 *            The name of the tag, such as <code>@param</code>. Cannot be
	 *            null.
	 * @param newSecondName
	 *            A further qualifier for the tag. For <code>param</code> tags,
	 *            this will be the parameter name. For <code>exception</code>
	 *            tags, this will be the exception name. May be null.
	 * @param newComment
	 *            The description of the tag. May be null to indicate there is
	 *            no description.
	 * @param newFragments
	 *            The fragments of this tag. May be null to indicate that the
	 *            tag has no fragments.
	 */
	public Tag(CommentFragment newName, List<CommentFragment> newSecondName, CommentDescription newComment,
			List<CommentFragment> newFragments) {
		this.name = newName;
		this.secondName = newSecondName;
		this.comment = newComment;
		this.allFragments = newFragments != null ? newFragments : Collections.EMPTY_LIST;
		name.setTag(this);
	}

	/**
	 * Constructor for Tag with a TagElement that must be examined.
	 * 
	 * @param comp
	 *            The compiled form of the file with all ASTNodes and line
	 *            numbers available. Cannot be null.
	 * @param element
	 *            The ASTNode which represents a Java doc tag. Cannot be null.
	 */
	public Tag(ValidatorResource resource, CompilationUnit comp, TagElement element) {
		int currentStartPosition = element.getStartPosition();

		List<ASTNode> nodeList = new ArrayList<ASTNode>();
		nodeList.add(element);
		CommentDescriptionBuilder builder = new CommentDescriptionBuilder(resource, nodeList, comp, true,
				element.getStartPosition(), element.getStartPosition() + element.getLength());

		// find the tag created and copy the results
		boolean tagExists = false;
		for (CommentFragment fragment : builder.fragments) {
			Tag tag = fragment.getTag();
			if (tag != null) {
				name = tag.name;
				secondName = tag.secondName;
				comment = tag.comment;
				allFragments = tag.allFragments;
				fragment.setTag(this);

				tagExists = true;
				break;
			}
		}

		if (!tagExists) {
			throw new IllegalStateException("CommentDescriptionBuilder did not attach "
					+ "a Tag instance to the fragments created by a TagElement.");
		}
	}

	/**
	 * Returns all fragments in this tag (including fragments within the
	 * description).
	 * 
	 * @return The fragments in this tag. Will not be null.
	 */
	public List<CommentFragment> getFragments() {
		return allFragments;
	}

	/**
	 * Returns the name of this tag, such as <code>@param</code>.
	 * 
	 * @return The name of this tag. Will not be null.
	 */
	public CommentFragment getName() {
		return name;
	}

	/**
	 * Returns the qualifier for this tag. This will be null or non-null
	 * depending on the type of tag:
	 * <ul>
	 * <li><code>@param</code> - Non-null. The name of the parameter.
	 * <li><code>@return</code> - Null.
	 * <li><code>@throws</code>, <code>exception</code> - Non-null. The class
	 * name of the exception.
	 * <li><code>@see</code> - Non-null. The class name that is referenced.
	 * <li><code>@link</code> - Non-null. The class name that is referenced.
	 * </ul>
	 * 
	 * @return The qualifier for this tag. May be null depending on the tag
	 *         type.
	 */
	public List<CommentFragment> getSecondName() {
		return secondName;
	}

	/**
	 * Returns the description for this tag.
	 * 
	 * @return The description for this tag. May be null if there is no
	 *         description.
	 */
	public CommentDescription getComment() {
		return comment;
	}
}
