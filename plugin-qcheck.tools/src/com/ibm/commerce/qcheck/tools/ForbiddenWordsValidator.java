package com.ibm.commerce.qcheck.tools;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.osgi.framework.Bundle;

import com.ibm.commerce.qcheck.core.EclipseUtil;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.ValidationException;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.comment.Comment;
import com.ibm.commerce.qcheck.core.comment.CommentDescription;
import com.ibm.commerce.qcheck.core.comment.Tag;

/**
 * ForbiddenWordsValidator is used to check that comments do not contain words
 * from a list of forbidden words and phrases, as required by IBM. There is also
 * a list of exceptions that overrides the forbidden phrases.
 * 
 * @author Trent Hoeppner
 */
public class ForbiddenWordsValidator implements Validator {

	private static final List<ModelEnum> REQUIRED_MODELS = Arrays.asList(ModelEnum.STRING, ModelEnum.COMP_UNIT);

	/**
	 * Constructor for this.
	 */
	public ForbiddenWordsValidator() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ModelEnum> getRequiredModels() {
		return REQUIRED_MODELS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ValidationResult> analyze(List<ValidatorResource> resources, ProblemActionFactory actionFactory,
			IProgressMonitor monitor) throws ValidationException, IOException, OperationCanceledException {

		List<ValidationResult> results = new ArrayList<ValidationResult>();

		try {
			monitor.beginTask("Checking for IBM forbidden words", resources.size());

			// load the forbidden words
			Terms terms = loadTerms("data/findTerms.txt");
			Terms exceptionTerms = loadTerms("data/exceptions.txt");

			// get the results
			for (ValidatorResource resource : resources) {

				EclipseUtil.getDefault().checkCanceled(monitor);

				CompilationUnit compilationUnit = ModelEnum.COMP_UNIT.getData(resource);
				List list = resource.getTypedNodeList(Javadoc.JAVADOC);
				for (Iterator it = list.iterator(); it.hasNext();) {

					EclipseUtil.getDefault().checkCanceled(monitor);

					Javadoc decl = (Javadoc) it.next();
					Comment comment = new Comment(resource, decl, compilationUnit);
					List<Tag> tags = comment.getAllTags();
					List<CommentDescription> descriptions = new ArrayList<CommentDescription>();
					if (comment.getDescription() != null) {
						descriptions.add(comment.getDescription());
					}
					for (Tag tag : tags) {
						if (tag.getComment() != null) {
							descriptions.add(tag.getComment());
						}
					}

					for (CommentDescription description : descriptions) {

						EclipseUtil.getDefault().checkCanceled(monitor);

						String text = description.getHumanReadableString().toLowerCase(Locale.ENGLISH);
						for (int i = 0; i < text.length(); i++) {

							if ((i == 0 || !isWordCharacter(text, i - 1)) && isWordCharacter(text, i)) {
								Term term = terms.findTerm(text, i);
								if (term != null) {
									Term exception = exceptionTerms.findTerm(text, i);
									if (exception == null) {
										// we have a forbidden term
										int startingPosition = description.findInComment(i);

										int column = compilationUnit.getColumnNumber(startingPosition);
										int lineNumber = compilationUnit.getLineNumber(startingPosition) - 1;

										ValidationResult result = new ValidationResult(
												"This word or phrase is on IBM's list of words "
														+ "that should never be used.",
												resource, Collections.EMPTY_LIST, lineNumber, column,
												term.phrase.length(), startingPosition, "Forbidden");

										results.add(result);
									}
								}
							}
						}
					}
				}

				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}

		return results;
	}

	/**
	 * Loads the terms from the given file.
	 *
	 * @param filename
	 *            The path name of the file with terms, relative to the root
	 *            folder. Cannot be null or empty.
	 *
	 * @return The object which contains all the terms in the given file. Will
	 *         not be null.
	 *
	 * @throws IOException
	 *             If the file does not exist or there was an error reading the
	 *             file.
	 */
	private Terms loadTerms(String filename) throws IOException {
		Bundle bundle = Activator.getDefault().getBundle();
		URL termsURL = FileLocator.find(bundle, new Path(filename), null);
		InputStream termsIn = termsURL.openStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(termsIn, "UTF-8"));

		Terms terms = new Terms();
		try {
			String line = reader.readLine();
			while (line != null) {
				String phrase = line.substring(2).toLowerCase(Locale.ENGLISH);
				Term term = new Term(phrase, null);
				terms.addTerm(term);
				line = reader.readLine();
			}
		} finally {
			reader.close();
		}
		return terms;
	}

	/**
	 * Returns whether the given character is part of a normal word.
	 *
	 * @param text
	 *            The text that contains the character. Cannot be null, but may
	 *            be empty.
	 * @param index
	 *            The 0-based index into <code>text</code> where the character
	 *            is located. Must be &gt;= 0.
	 *
	 * @return True if the character can be part of a word, false otherwise.
	 */
	private boolean isWordCharacter(String text, int index) {
		char c = text.charAt(index);
		boolean wordChar = Character.isJavaIdentifierPart(c);

		return wordChar;
	}

	/**
	 * Terms represents a phrase list which can be used for forbidden phrases or
	 * exceptions. It provides methods to find phrases in this within a piece of
	 * text.
	 */
	private class Terms {

		/**
		 * The list of words. This value will never be null.
		 */
		private List<Term> terms = new ArrayList<Term>();

		/**
		 * A mapping from the first word in a term to all the terms that have
		 * that first word. This value will never be null.
		 */
		private Map<String, List<Term>> firstWordToTermsMap = new TreeMap<String, List<Term>>();

		/**
		 * Constructor for Terms.
		 */
		private Terms() {
			// do nothing
		}

		/**
		 * Adds the given word or phrase to this.
		 *
		 * @param term
		 *            The word or phrase to add. Cannot be null.
		 */
		public void addTerm(Term term) {
			terms.add(term);

			// add it to the first word index for faster searching
			String currentPhrase = term.phrase;
			String firstWord = findFirstWord(currentPhrase, 0);

			List<Term> firstWordTerms = getTermsForFirstWord(firstWord);
			firstWordTerms.add(term);
		}

		/**
		 * Finds the first word at or after the given index in the given text.
		 *
		 * @param text
		 *            The text to search. Cannot be null, but may be empty.
		 * @param startIndex
		 *            The 0-based index into <code>text</code> to start
		 *            searching from. Must be >= 0.
		 *
		 * @return The value that was found. This value will be the same as
		 *         <code>text</code> if there is no word after the given index.
		 *         May be empty if the original text was empty. Will not be
		 *         null.
		 */
		private String findFirstWord(String text, int startIndex) {
			int phraseLength = text.length();

			int firstWhiteSpaceIndex = phraseLength;
			for (int i = startIndex; i < phraseLength; i++) {
				if (!isWordCharacter(text, i)) {
					firstWhiteSpaceIndex = i;
					break;
				}
			}

			String firstWord = text.substring(startIndex, firstWhiteSpaceIndex);

			return firstWord;
		}

		/**
		 * Returns the list of phrases that have the given string as the first
		 * word.
		 *
		 * @param firstWord
		 *            The word that should appear first in all phrases after.
		 *            All characters in the string should return true for
		 *            {@link #isWordCharacter(String, int)}. Cannot be null or
		 *            empty.
		 *
		 * @return The list of phrases. Will not be null, and every phrase will
		 *         contain the given string as a first word. This value will be
		 *         empty if no phrases from this could be found.
		 */
		private List<Term> getTermsForFirstWord(String firstWord) {
			List<Term> termsForFirstWord = firstWordToTermsMap.get(firstWord);
			if (termsForFirstWord == null) {
				termsForFirstWord = new ArrayList<Term>();
				firstWordToTermsMap.put(firstWord, termsForFirstWord);
			}

			return termsForFirstWord;
		}

		/**
		 * Finds the phrase for the word starting at the given index in the
		 * given text.
		 *
		 * @param text
		 *            The text that may have a phrase in this. Cannot be null,
		 *            but may be empty.
		 * @param firstWordIndex
		 *            The 0-based index into the given text. Cannot be null or
		 *            empty.
		 *
		 * @return The phrase from this that starts at the given index. Will be
		 *         null if no phrase could be found.
		 */
		public Term findTerm(String text, int firstWordIndex) {
			String firstWord = findFirstWord(text, firstWordIndex);
			List<Term> termsForFirstWord = firstWordToTermsMap.get(firstWord);

			Term term = null;
			if (termsForFirstWord != null) {
				for (Term possibleTerm : termsForFirstWord) {
					if (text.startsWith(possibleTerm.phrase, firstWordIndex)) {
						term = possibleTerm;
						break;
					}
				}
			}

			return term;
		}
	}

	/**
	 * Term represents a word or phrase, that may be forbidden or an exception
	 * to a forbidden phrase.
	 */
	private class Term implements Comparable<String> {

		/**
		 * The full phrase. This value will never be null or empty.
		 */
		private String phrase;

		/**
		 * The explanation of why the phrase is forbidden. Only used if this is
		 * a forbidden phrase. This value will never be empty. This value will
		 * be null when there is no explanation or the phrase is an exception.
		 */
		private String explanation;

		/**
		 * Any suggestions that can be substituted for the given phrase. Only
		 * used if this is a forbidden phrase. This value will never be null.
		 */
		private List<String> suggestions = new ArrayList<String>();

		/**
		 * Constructor for this.
		 *
		 * @param newPhrase
		 *            The word or phrase. Cannot be null or empty.
		 * @param newExplanation
		 *            The explanation for why the phrase is forbidden. Cannot be
		 *            empty. May be null if there is no explanation or the
		 *            phrase is an exception.
		 */
		private Term(String newPhrase, String newExplanation) {
			this.phrase = newPhrase;
			this.explanation = newExplanation;
		}

		/**
		 * Adds the given suggestion to this.
		 *
		 * @param suggestion
		 *            The suggestion to add. Cannot be null or empty.
		 */
		public void addSuggestion(String suggestion) {
			suggestions.add(suggestion);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compareTo(String o) {
			return phrase.compareTo(o);
		}
	}
}
