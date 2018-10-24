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
import java.util.ListIterator;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.comment.CommentFragment.CheckerState;

/**
 * CommentDescriptionBuilder is used to create a CommentDescription based on
 * some AST nodes from the Eclipse Java compilation unit.
 * <ul>
 * <li>Each HTML tag will be a single fragment.
 * <li>Each @link tag will have the link in its own fragment, followed by a
 * space fragment, followed by the linked method or class name. If the @link tag
 * has a human-friendly name, a space followed by the name will also be in their
 * own fragments. The { and } will also be in their own fragments.
 * <li>If a line starts with leading whitespace followed by a star ("*")
 * followed by whitespace, according to the JavaDoc convention, this section
 * will also be in its own fragment.
 * <li>Standard JavaDoc tags will be split into fragments as follows:
 * <ul>
 * <li>The tag name (including the @) will be in one fragment, with the
 * description following.
 * <li>If there is an identifier for the tag (such as with @param tags that have
 * the parameter name), the identifier will be in a fragment before the
 * description.
 * <li>Any whitespace separating these fragments will be in their own fragments
 * in the same order as they occur in the file.
 * <li>The description will be split into fragments using the same algorithm as
 * the main description for the JavaDoc.
 * <li>A {@link Tag} object will be created and associated with the first
 * fragment that belongs to the Tag.
 * </ul>
 * <li>Other normal text will be split into fragments, so that they do not
 * overlap other fragments, and do not cross a line.
 * </ul>
 * Each fragment will have appropriate grammar and spelling markers to make them
 * exempt (or not) as appropriate.
 * 
 * @author Trent Hoeppner
 */
public class CommentDescriptionBuilder {

	/**
	 * The list of fragments that make up this description. This list will never
	 * be null.
	 */
	public List<CommentFragment> fragments = new ArrayList<CommentFragment>();

	/**
	 * Constructor for CommentDescriptionBuilder. This will convert the given
	 * AST nodes into {@link CommentFragment CommentFragments} and store them
	 * internally.
	 *
	 * @param astFragments
	 *            The list of AST nodes from the JavaDoc. Cannot be null. If
	 *            empty, no fragments will be stored internally.
	 * @param comp
	 *            The compilation unit that the AST nodes come from. Cannot be
	 *            null.
	 * @param trimLeadingWhitespace
	 *            True indicates that leading whitespace on the first AST node
	 *            will be trimmed if it exists, false indicates that it will be
	 *            included.
	 * @param commentStartPos
	 *            The 0-based index, from the beginning of the file, that marks
	 *            the first character of the comment. Must be &gt;= 0.
	 * @param commentEndPos
	 *            The 0-based index, from the beginning of the file, that marks
	 *            the character after the last character of the comment. Must be
	 *            &gt;= 0.
	 */
	public CommentDescriptionBuilder(ValidatorResource resource, List astFragments, CompilationUnit comp,
			boolean trimLeadingWhitespace, int commentStartPos, int commentEndPos) {

		boolean isFirst = true;
		for (Object object : astFragments) {
			int sizeBeforeAddingNode = fragments.size();

			ASTNode node = (ASTNode) object;
			if (node instanceof TagElement) {
				addTag(resource, (TagElement) node, comp, isFirst && trimLeadingWhitespace);
			} else if (node instanceof TextElement) {
				addText(resource, (TextElement) node, comp, isFirst && trimLeadingWhitespace);
			} else if (node instanceof SimpleName) {
				addName((SimpleName) node, comp, isFirst && trimLeadingWhitespace);
			} else {
				add(resource, node, comp, isFirst && trimLeadingWhitespace);
			}

			if (sizeBeforeAddingNode > 0) {

				CommentFragment previousFragment = fragments.size() > 0 ? fragments.get(sizeBeforeAddingNode - 1)
						: null;
				List<CommentFragment> parsedFragments = fragments.subList(sizeBeforeAddingNode, fragments.size());

				if (previousFragment != null && node instanceof TagElement && parsedFragments.size() > 0) {
					// there is a special case where if the {@link ...} tag
					// splits
					// over multiple lines, and the actual text that is to be
					// grammar-checked starts on a line after the opening
					// {@link ...}, the space before the link will be followed
					// by a
					// \n, which results in an error. In this case, we remove
					// only
					// the last whitespace char from the previous fragment.
					int nodeLine = comp.getLineNumber(node.getStartPosition()) - 1;
					CommentFragment firstVisibleFragment = null;
					for (CommentFragment possibleVisible : parsedFragments) {
						if (!possibleVisible.isRelevantLineEnding()
								&& (possibleVisible.getGrammarState() == CheckerState.VISIBLE
										|| possibleVisible.getGrammarState() == CheckerState.APPLICABLE)) {
							firstVisibleFragment = possibleVisible;
							break;
						}
					}

					if (firstVisibleFragment != null
							&& previousFragment.getSourceLine() < firstVisibleFragment.getSourceLine()) {
						String previousFragmentText = previousFragment.getText();
						if (Character.isWhitespace(previousFragmentText.charAt(previousFragmentText.length() - 1))) {
							CommentFragment spaceToSkip = previousFragment.splitAt(previousFragmentText.length() - 1);
							spaceToSkip.setSpellingState(CheckerState.INVISIBLE);
							spaceToSkip.setGrammarState(CheckerState.INVISIBLE);
							spaceToSkip.setFormatting(false);
							fragments.add(sizeBeforeAddingNode, spaceToSkip);
						}
					}
				}
			}

			isFirst = false;
		}

		createHTMLTags();

		markExemptText();
		fillInMissingFragments(resource, commentStartPos, commentEndPos, comp, fragments, trimLeadingWhitespace);
	}

	/**
	 * Prints the fragments in this, one line at a time. Each line will have two
	 * additional lines underneath indicating the status of each character with
	 * respect to the grammar and spelling checkers. On these lines, the first
	 * character of each fragment will be marked with a start character, and the
	 * remaining characters will be marked with a continuation character, as
	 * follows:
	 * <table border="1">
	 * <tr>
	 * <th>Status</th>
	 * <th>Start Marker</th>
	 * <th>Continuation Marker</th>
	 * </tr>
	 * <tr>
	 * <td>APPLICABLE</td>
	 * <td>|</td>
	 * <td>&lt;empty space&gt;</td>
	 * </tr>
	 * <tr>
	 * <td>VISIBLE</td>
	 * <td>!</td>
	 * <td>+</td>
	 * </tr>
	 * <tr>
	 * <td>INVISIBLE</td>
	 * <td>x</td>
	 * <td>-</td>
	 * </tr>
	 * <tr>
	 * <td>null</td>
	 * <td>&</td>
	 * <td>@</td>
	 * </tr>
	 * </table>
	 * Note that having a null status is not a valid state and indicates an
	 * error in the algorithm.
	 */
	public void printFragments() {
		if (!Debug.COMMENT.isActive()) {
			return;
		}

		int previousFragmentLine = -1;
		StringBuffer textBuffer = new StringBuffer();
		StringBuffer spellingMarkerBuffer = new StringBuffer();
		StringBuffer grammarMarkerBuffer = new StringBuffer();
		for (CommentFragment fragment : fragments) {
			if (previousFragmentLine < fragment.getSourceLine()) {
				Debug.COMMENT.log(textBuffer.toString());
				Debug.COMMENT.log(spellingMarkerBuffer.toString());
				Debug.COMMENT.log(grammarMarkerBuffer.toString());
				textBuffer.setLength(0);
				spellingMarkerBuffer.setLength(0);
				grammarMarkerBuffer.setLength(0);
				previousFragmentLine = fragment.getSourceLine();
			}

			if (fragment.getText().length() == 0) {
				continue;
			}

			textBuffer.append(fragment.getText());
			String filler = CommentUtil.createSpaces(fragment.getText().length() - 1);
			if (fragment.getSpellingState() == CheckerState.APPLICABLE) {
				spellingMarkerBuffer.append('|').append(filler);
			} else if (fragment.getSpellingState() == CheckerState.VISIBLE) {
				filler = filler.replace(' ', '+');
				spellingMarkerBuffer.append('!').append(filler);
			} else if (fragment.getSpellingState() == CheckerState.INVISIBLE) {
				filler = filler.replace(' ', '-');
				spellingMarkerBuffer.append('x').append(filler);
			} else {
				filler = filler.replace(' ', '@');
				spellingMarkerBuffer.append('&').append(filler);
			}

			filler = CommentUtil.createSpaces(fragment.getText().length() - 1);
			if (fragment.getGrammarState() == CheckerState.APPLICABLE) {
				grammarMarkerBuffer.append('|').append(filler);
			} else if (fragment.getGrammarState() == CheckerState.VISIBLE) {
				filler = filler.replace(' ', '+');
				grammarMarkerBuffer.append('!').append(filler);
			} else if (fragment.getGrammarState() == CheckerState.INVISIBLE) {
				filler = filler.replace(' ', '-');
				grammarMarkerBuffer.append('x').append(filler);
			} else {
				filler = filler.replace(' ', '@');
				grammarMarkerBuffer.append('&').append(filler);
			}
		}

		Debug.COMMENT.log(textBuffer.toString());
		Debug.COMMENT.log(spellingMarkerBuffer.toString());
		Debug.COMMENT.log(grammarMarkerBuffer.toString());
	}

	/**
	 * Finds HTML tags in existing fragments and, for each one that exists,
	 * splits the containing fragment so that a new HTML fragment is created.
	 */
	private void createHTMLTags() {
		// analyze all fragments - find html tags and create separate fragments
		// for them
		boolean inited = false;
		int lastLine = -1;

		for (int i = 0; i < fragments.size(); i++) {
			CommentFragment fragment = fragments.get(i);
			if (fragment.getText().isEmpty()) {
				continue;
			}

			if (!inited) {
				lastLine = fragment.getSourceLine();
				inited = true;
			}

			// split HTML tags into their own fragments
			int htmlTagStartIndex = fragment.getText().indexOf('<');
			while (htmlTagStartIndex >= 0) {
				// if there is a tag ending, we have an html tag
				int htmlTagEndIndex = fragment.getText().indexOf('>', htmlTagStartIndex);
				if (htmlTagEndIndex >= 0) {
					CommentFragment beforeHTMLTag = fragment;

					CommentFragment htmlTag = beforeHTMLTag.splitAt(htmlTagStartIndex);
					htmlTag.setFormatting(true);
					this.fragments.add(i + 1, htmlTag);
					i++;

					// need to find the tag ending again since it came from the
					// original fragment
					htmlTagEndIndex = htmlTag.getText().indexOf('>');
					CommentFragment afterHTMLTag = htmlTag.splitAt(htmlTagEndIndex + 1);
					this.fragments.add(i + 1, afterHTMLTag);
					i++;

					fragment = afterHTMLTag;

					htmlTagStartIndex = fragment.getText().indexOf('<');
				} else {
					htmlTagStartIndex = fragment.getText().indexOf('>', htmlTagStartIndex + 1);
				}
			}
		}
	}

	/**
	 * Goes through all fragments and mark text that is surrounded by
	 * <code>&lt;code&gt;&lt;/code&gt;</code>,
	 * <code>&lt;samp&gt;&lt;/samp&gt;</code> or
	 * <code>&lt;pre&gt;&lt;/pre&gt;</code> as exempt from grammar and spell
	 * checking.
	 */
	private void markExemptText() {

		// we maintain the state to account for nested tags
		// this state never contains end tags, only start tags
		// if an end tag is encountered without a matching start tag, it is
		// ignored
		List<Style> styleState = new ArrayList<Style>();

		boolean inExemptArea = false;
		ListIterator<CommentFragment> fragmentIterator = this.fragments.listIterator();
		for (int i = 0; fragmentIterator.hasNext(); i++) {
			CommentFragment fragment = fragmentIterator.next();
			if (fragment.getText().isEmpty()) {
				continue;
			}

			Style style = Style.findStyle(fragment);
			if (style == null) {
				if (fragment.getSpellingState() == null) {
					fragment.setSpellingState(inExemptArea ? CommentFragment.CheckerState.INVISIBLE
							: CommentFragment.CheckerState.APPLICABLE);
					fragment.setGrammarState(inExemptArea ? CommentFragment.CheckerState.VISIBLE
							: CommentFragment.CheckerState.APPLICABLE);
				}
				continue;
			}

			// this is an html tag
			fragment.setSpellingState(CommentFragment.CheckerState.INVISIBLE);
			fragment.setGrammarState(CommentFragment.CheckerState.INVISIBLE);

			if (style.isStart()) {
				styleState.add(style);
			} else {
				// find the index of the start tag, and remove all states since
				// that point
				// If we have something like this
				// <code>text<samp>text</code>
				// then the start for the <samp> will also be removed.
				// As a result, if there is a </samp> tag later on, the text
				// between </code> and </samp> will not be exempt from spelling
				// and grammar checks.
				// <code>exempt<samp>exempt</code>not exempt</samp>
				int removeFromIndex = -1;
				for (int j = styleState.size() - 1; j >= 0; j--) {
					Style previous = styleState.get(j);
					if (style.getMatch() == previous) {
						// mark this tag as the first tag to start removing from

						// previous must be a start tag, since we don't add end
						// tags to the state
						removeFromIndex = j;
						break;
					}
				}

				if (removeFromIndex >= 0) {
					styleState = styleState.subList(0, removeFromIndex);
				}
			}

			inExemptArea = !styleState.isEmpty();
		}
	}

	/**
	 * Converts the text in the given AST node to a {@link CommentFragment} and
	 * adds it to this. The given node will be converted to multiple
	 * CommentFragments if the node spans more than one line.
	 *
	 * @param node
	 *            The AST node to convert. Cannot be null.
	 * @param comp
	 *            The compilation unit which contains the node. Cannot be null.
	 * @param trimLeadingWhitespace
	 *            True indicates that leading whitespace on the AST node will be
	 *            trimmed if it exists, false indicates that it will be
	 *            included.
	 */
	public void add(ValidatorResource resource, ASTNode node, CompilationUnit comp, boolean trimLeadingWhitespace) {
		int newStartPos = node.getStartPosition();
		int newEndPos = newStartPos + node.getLength();
		String string = CommentUtil.getOriginalText(resource, newStartPos, newEndPos);

		List<CommentFragment> newFragments = createFragment(comp, node, trimLeadingWhitespace, string);

		if (trimLeadingWhitespace) {
			// remove first whitespace chars
			if (newFragments.size() > 0 && newFragments.get(0).getText().trim().isEmpty()) {
				newFragments.remove(0);
			}
		}

		fragments.addAll(newFragments);
	}

	/**
	 * Converts the given Eclipse tag into multiple {@link CommentFragment
	 * CommentFragments}, as described in the class description.
	 *
	 * @param tagElement
	 *            The element to convert. This can be a @link tag, or other
	 *            standard tags such as @param or @return. Cannot be null.
	 * @param comp
	 *            The compilation unit which contains the node. Cannot be null.
	 * @param trimLeadingWhitespace
	 *            True indicates that leading whitespace on the AST node will be
	 *            trimmed if it exists, false indicates that it will be
	 *            included.
	 */
	public void addTag(ValidatorResource resource, TagElement tagElement, CompilationUnit comp,
			boolean trimLeadingWhitespace) {
		String originalTagString = CommentUtil.getOriginalText(resource, tagElement.getStartPosition(),
				tagElement.getStartPosition() + tagElement.getLength());
		int tagNameOffset = originalTagString.indexOf(tagElement.getTagName());
		CommentFragment name;
		if (tagNameOffset >= 0) {
			List<CommentFragment> newFragments = createFragment(comp, tagElement.getTagName(),
					tagElement.getStartPosition() + tagNameOffset);
			if (newFragments.size() == 1) {
				name = newFragments.get(0);
			} else if (newFragments.size() > 0) {
				// should not happen
				name = newFragments.get(0);
				if (Debug.COMMENT.isActive()) {
					Debug.COMMENT.log("The name ", name, " spans multiple lines.");
				}
			} else {
				// also should not happen
				name = null;
				if (Debug.COMMENT.isActive()) {
					Debug.COMMENT.log("The name is missing.");
				}
			}
		} else {
			// the @name is not there or is malformed, treat this as a normal
			// ASTNode
			add(resource, tagElement, comp, trimLeadingWhitespace);
			return;
		}

		List<CommentFragment> secondName;

		List astFragments = tagElement.fragments();
		List descriptionFragments;
		String text = name.getText();
		if (astFragments.size() > 0 && (text.equals("@param") || text.equals("@exception") || text.equals("@throws")
				|| text.equals("@see") || text.equals("@link"))) {
			ASTNode node = (ASTNode) astFragments.get(0);
			CommentDescriptionBuilder builder = new CommentDescriptionBuilder(resource, astFragments.subList(0, 1),
					comp, true, node.getStartPosition(), node.getStartPosition() + node.getLength());
			secondName = builder.fragments;
			descriptionFragments = astFragments.subList(1, astFragments.size());
		} else {
			secondName = null;
			descriptionFragments = astFragments;
		}

		CommentDescription comment;
		List<CommentFragment> commentFragments;
		if (!descriptionFragments.isEmpty()) {
			comment = new CommentDescription(resource, descriptionFragments, comp, true, null);
			commentFragments = comment.getFragments();
		} else {
			comment = null;
			commentFragments = Collections.EMPTY_LIST;
		}

		List<CommentFragment> allFragments = new ArrayList<CommentFragment>();
		allFragments.add(name);
		if (secondName != null) {
			allFragments.addAll(secondName);
		}
		allFragments.addAll(commentFragments);

		fillInMissingFragments(resource, tagElement.getStartPosition(),
				tagElement.getStartPosition() + tagElement.getLength(), comp, allFragments, trimLeadingWhitespace);

		// mark fragments
		boolean spellCheckToo = false;
		List<CommentFragment> visibleFragments;
		if (!descriptionFragments.isEmpty()) {
			visibleFragments = commentFragments;
			spellCheckToo = true;
		} else if (secondName != null && !secondName.isEmpty()) {
			visibleFragments = secondName;
		} else {
			visibleFragments = new ArrayList<CommentFragment>();
			visibleFragments.add(name);
		}

		for (CommentFragment fragment : allFragments) {
			if (visibleFragments.contains(fragment)) {
				if (fragment.getGrammarState() != null) {
					// we don't want to overwrite states because some fragments
					// in a description need to be invisible (e.g. JavaDoc
					// prefix on every line)
					continue;
				}
				fragment.setSpellingState(spellCheckToo ? CheckerState.APPLICABLE : CheckerState.VISIBLE);
				fragment.setGrammarState(CheckerState.APPLICABLE);
			} else {
				if (fragment.isRelevantLineEnding()) {
					continue;
				}
				fragment.setSpellingState(CheckerState.INVISIBLE);
				fragment.setGrammarState(CheckerState.INVISIBLE);
				fragment.setFormatting(true);
			}
		}

		// although we don't use this tag here, this will create the Tag and
		// associate it with the first element
		Tag tag = new Tag(name, secondName, comment, allFragments);

		this.fragments.addAll(allFragments);
	}

	/**
	 * Looks for strings that are not included in any of the given fragments,
	 * and adds the strings to the list. These strings are extracted from
	 * corresponding text directly in the file.
	 *
	 * @param veryStartPos
	 *            The 0-based index from the beginning of the file that marks
	 *            the first character of the comment which should be included.
	 *            This character may be before the first character of the first
	 *            fragment, but it cannot be after. Must be &gt;= 0.
	 * @param veryEndPos
	 *            The 0-based index from the beginning of the file that marks
	 *            the character that comes after the last character of the
	 *            comment. This character cannot be included in the last
	 *            fragment. Must be &gt;= 0.
	 * @param comp
	 *            The compilation unit which contains the comment. Cannot be
	 *            null.
	 * @param allFragments
	 *            The list of fragments accumulated so far. Cannot be null, but
	 *            may be empty.
	 * @param trimLeadingWhitespace
	 *            True indicates that leading whitespace before the first
	 *            fragment will be trimmed if it exists, false indicates that it
	 *            will be included.
	 */
	private void fillInMissingFragments(ValidatorResource resource, int veryStartPos, int veryEndPos,
			CompilationUnit comp, List<CommentFragment> allFragments, boolean trimLeadingWhitespace) {
		// fill in the fragments that are not part of the AST tree
		int previousEnd = veryStartPos;
		for (int i = 0; i < allFragments.size(); i++) {
			CommentFragment fragment = allFragments.get(i);
			int startPos = fragment.getStartPosition();
			boolean added = addFillerFragment(resource, comp, allFragments, previousEnd, i, startPos,
					trimLeadingWhitespace);

			if (added) {
				i++;
			}

			previousEnd = fragment.getStartPosition() + fragment.getText().length();
		}

		int startPos = veryEndPos;
		addFillerFragment(resource, comp, allFragments, previousEnd, allFragments.size(), startPos,
				trimLeadingWhitespace);
	}

	/**
	 * Creates a fragment marked by the given range and adds it to the list of
	 * fragments at the given position.
	 *
	 * @param comp
	 *            The compilation unit which contains the comment. Cannot be
	 *            null.
	 * @param allFragments
	 *            The list of fragments accumulated so far. Cannot be null, but
	 *            may be empty.
	 * @param fillerStartPos
	 *            The 0-based index from the beginning of the file that marks
	 *            the first character of the filler fragment. Must be &gt;= 0.
	 * @param i
	 *            The 0-based index in the list of fragments where the filler
	 *            fragment should be inserted. Must be &gt;= 0.
	 * @param fillerEndPos
	 *            The 0-based index from the beginning of the file that marks
	 *            the character after the last character of the filler fragment.
	 *            Must be &gt;= 0.
	 * @param trimLeadingWhitespace
	 *            True indicates that if we are inserting a filler fragment at
	 *            the first position in the list and the fragment to insert is
	 *            all whitespace then the fragment will not be added, false
	 *            indicates that it will be added.
	 *
	 * @return True if a filler fragment was added, false otherwise.
	 */
	private boolean addFillerFragment(ValidatorResource resource, CompilationUnit comp,
			List<CommentFragment> allFragments, int fillerStartPos, int i, int fillerEndPos,
			boolean trimLeadingWhitespace) {
		boolean added = false;
		int newFragmentSize = fillerEndPos - fillerStartPos;
		if (newFragmentSize > 0) {
			int newStartPos = fillerStartPos;
			int newEndPos = newStartPos + newFragmentSize;
			String string = CommentUtil.getOriginalText(resource, newStartPos, newEndPos);
			if (!(trimLeadingWhitespace && i == 0 && string.trim().isEmpty())) {
				List<CommentFragment> newFragments = createFragment(comp, string, newStartPos);
				for (CommentFragment fragment : newFragments) {
					boolean lineHasRelevantText = false;
					for (int j = i - 1; j >= 0
							&& allFragments.get(j).getSourceLine() == fragment.getSourceLine(); j--) {
						if (allFragments.get(j).getGrammarState() != CheckerState.INVISIBLE
								|| allFragments.get(j).isFormatting()) {
							lineHasRelevantText = true;
							break;
						}
					}

					if (lineHasRelevantText) {
						String fragmentText = fragment.getText();
						if (fragmentText.equals("\n\r") || fragmentText.equals("\r\n") || fragmentText.equals("\n")
								|| fragmentText.equals("\r")) {
							// it's an end-of-line that will appear in the
							// human-readable text
							fragment.setSpellingState(CheckerState.VISIBLE);
							fragment.setGrammarState(CheckerState.APPLICABLE);
							fragment.setRelevantLineEnding(true);
						} else {
							fragment.setSpellingState(CheckerState.INVISIBLE);
							fragment.setGrammarState(CheckerState.INVISIBLE);
						}
					} else {
						fragment.setSpellingState(CheckerState.INVISIBLE);
						fragment.setGrammarState(CheckerState.INVISIBLE);
					}
				}
				allFragments.addAll(i, newFragments);
				added = true;
			}
		}
		return added;
	}

	/**
	 * Converts the given text element to a {@link CommentFragment} and adds it
	 * to this. The given node will be converted to multiple CommentFragments if
	 * the node spans more than one line.
	 *
	 * @param textElement
	 *            The AST node to convert. Cannot be null.
	 * @param comp
	 *            The compilation unit which contains the node. Cannot be null.
	 * @param trimLeadingWhitespace
	 *            True indicates that leading whitespace on the AST node will be
	 *            trimmed if it exists, false indicates that it will be
	 *            included.
	 */
	public void addText(ValidatorResource resource, TextElement textElement, CompilationUnit comp,
			boolean trimLeadingWhitespace) {
		add(resource, textElement, comp, trimLeadingWhitespace);
	}

	/**
	 * Creates a {@link CommentFragment} for the given name using its fully
	 * qualified name, and adds it to this.
	 *
	 * @param simpleName
	 *            The AST node to convert. Cannot be null.
	 * @param comp
	 *            The compilation unit which contains the node. Cannot be null.
	 * @param trimLeadingWhitespace
	 *            True indicates that leading whitespace on the AST node will be
	 *            trimmed if it exists, false indicates that it will be
	 *            included.
	 */
	public void addName(SimpleName simpleName, CompilationUnit comp, boolean trimLeadingWhitespace) {
		String string = simpleName.getFullyQualifiedName();
		List<CommentFragment> newFragments = createFragment(comp, simpleName, trimLeadingWhitespace, string);
		fragments.addAll(newFragments);
	}

	/**
	 * Creates fragments to represent the given node, using the given string to
	 * represent the text in the node.
	 *
	 * @param comp
	 *            The compilation unit which contains the node. Cannot be null.
	 * @param node
	 *            The AST node that the new fragments will represent. Cannot be
	 *            null.
	 * @param trimLeadingWhitespace
	 *            True indicates that leading whitespace on the string will be
	 *            trimmed if it exists, false indicates that it will be
	 *            included.
	 * @param string
	 *            The text to represent the node. Cannot be null, but may be
	 *            empty.
	 *
	 * @return The list of fragments which represent the given node. Will not be
	 *         null, but may be empty.
	 */
	private List<CommentFragment> createFragment(CompilationUnit comp, ASTNode node, boolean trimLeadingWhitespace,
			String string) {
		int startPos = node.getStartPosition();
		if (trimLeadingWhitespace) {
			int i;
			for (i = 0; i < string.length(); i++) {
				if (!Character.isWhitespace(string.charAt(i))) {
					break;
				}
			}

			string = string.substring(i);
			startPos += i;
		}

		return createFragment(comp, string, startPos);
	}

	/**
	 * Creates fragments using the given string to represent the text.
	 *
	 * @param comp
	 *            The compilation unit which contains the node. Cannot be null.
	 * @param string
	 *            The text to represent the node. Cannot be null, but may be
	 *            empty.
	 * @param startPos
	 *            The 0-based index from the beginning of the file that marks
	 *            the position of the first character in the string.
	 *
	 * @return The list of fragments which represent the given node. Will not be
	 *         null, but may be empty.
	 */
	private List<CommentFragment> createFragment(CompilationUnit comp, String string, int startPos) {

		// if a fragment spans a line, break it up into two

		List<CommentFragment> newFragments = new ArrayList<CommentFragment>();

		String currentString = string;
		int currentStartPos = startPos;
		int endPos = currentStartPos + string.length();

		int currentStartPosLine = comp.getLineNumber(currentStartPos) - 1;
		int endPosLine = comp.getLineNumber(endPos - 1) - 1;
		while (currentStartPosLine < endPosLine) {
			int newLinePos = -1;
			int newLine = -1;
			for (int i = currentStartPos + 1; i < endPos; i++) {
				int iLine = comp.getLineNumber(i) - 1;
				if (iLine > currentStartPosLine) {
					newLinePos = i;
					newLine = iLine;
					break;
				}
			}

			if (newLinePos < 0) {
				break;
			}

			int length = newLinePos - currentStartPos;
			String fragmentString = currentString.substring(0, length);
			currentString = currentString.substring(length);
			CommentFragment fragment = new CommentFragment(currentStartPos, fragmentString,
					comp.getLineNumber(currentStartPos) - 1, comp.getColumnNumber(currentStartPos));
			newFragments.add(fragment);

			currentStartPos = newLinePos;
			currentStartPosLine = newLine;
		}

		CommentFragment fragment = new CommentFragment(currentStartPos, currentString,
				comp.getLineNumber(currentStartPos) - 1, comp.getColumnNumber(currentStartPos));
		newFragments.add(fragment);

		return newFragments;
	}

	/**
	 * Style represents the different HTML tags and the @link tag that can
	 * result in exempting text from grammar- and/or spell-checking
	 */
	private enum Style {

		/**
		 * The opening <code>code</code> tag, represented by
		 * <code>&lt;code&gt;</code>.
		 */
		START_CODE(true, null),

		/**
		 * The closing <code>code</code> tag, represented by
		 * <code>&lt;/code&gt;</code>.
		 */
		END_CODE(false, START_CODE),

		/**
		 * The opening <code>samp</code> tag, represented by
		 * <code>&lt;samp&gt;</code>.
		 */
		START_SAMP(true, null),

		/**
		 * The closing <code>samp</code> tag, represented by
		 * <code>&lt;/samp&gt;</code>.
		 */
		END_SAMP(false, START_SAMP),

		/**
		 * The opening <code>pre</code> tag, represented by
		 * <code>&lt;pre&gt;</code>.
		 */
		START_PRE(true, null),

		/**
		 * The closing <code>pre</code> tag, represented by
		 * <code>&lt;/pre&gt;</code>.
		 */
		END_PRE(false, START_PRE),

		/**
		 * The opening <code>link</code> tag, represented by <code>@link</code>.
		 */
		START_LINK(true, null),

		/**
		 * The opening <code>link</code> tag, represented by <code>}</code>.
		 */
		END_LINK(false, START_LINK);

		/**
		 * Returns the value that matches the given fragment.
		 *
		 * @param fragment
		 *            The fragment that may match an opening or closing tag.
		 *            Cannot be null.
		 *
		 * @return The value that matches the fragment, or null if the fragment
		 *         is not a closing or opening tag.
		 */
		public static Style findStyle(CommentFragment fragment) {
			String text = fragment.getText().toLowerCase();

			Style style = null;
			if (text.equals("<code>")) {
				style = START_CODE;
			} else if (text.equals("</code>")) {
				style = END_CODE;
			} else if (text.equals("<samp>")) {
				style = START_SAMP;
			} else if (text.equals("</samp>")) {
				style = END_SAMP;
			} else if (text.equals("<pre>")) {
				style = START_PRE;
			} else if (text.equals("</pre>")) {
				style = END_PRE;
			} else if (text.equals("@link")) {
				style = START_LINK;
			} else if (text.equals("}")) {
				style = END_LINK;
			}

			return style;
		}

		/**
		 * True if this is an opening tag, false if this is a closing tag.
		 */
		private boolean start;

		/**
		 * Represents the opening tag, but only if this is a closing tag.
		 * Otherwise this value will be null.
		 */
		private Style match;

		/**
		 * Constructor for Style.
		 *
		 * @param start
		 *            True if this is an opening tag, false otherwise.
		 * @param match
		 *            The opening tag if this is a closing tag. If this is an
		 *            opening tag the value must be null.
		 */
		private Style(boolean start, Style match) {
			this.start = start;
			this.match = match;
			if (match != null) {
				match.match = this;
			}
		}

		/**
		 * Returns whether this is an opening tag.
		 *
		 * @return True if this is an opening tag, false otherwise.
		 */
		public boolean isStart() {
			return start;
		}

		/**
		 * Returns the opening tag if this is a closing tag.
		 *
		 * @return The opening tag if this is a closing tag, null otherwise.
		 */
		public Style getMatch() {
			return match;
		}
	};

}
