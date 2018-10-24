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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.spelling.WordCorrectionProposal;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.EclipseUtil;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ProblemAction;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.ValidationException;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.WatchedFile;
import com.ibm.commerce.qcheck.core.comment.Comment;
import com.ibm.commerce.qcheck.core.comment.CommentDescription;
import com.ibm.commerce.qcheck.core.comment.CommentFragment;
import com.ibm.commerce.qcheck.core.comment.HumanFragmentView;
import com.ibm.commerce.qcheck.core.comment.Tag;

/**
 * SpellingValidator is used to check spelling of words in Java doc comments.
 * Other comments will be ignored.
 * <p>
 * This uses the built-in eclipse spell-check feature.
 * 
 * @author Trent Hoeppner
 */
@SuppressWarnings("restriction")
public class SpellingValidator implements Validator {

	/**
	 * A single quote mark.
	 */
	private static final char SINGLEQUOTES = '\'';

	/**
	 * This object is used to ensure that only one thread at a time can run the
	 * spelling validator. This prevents one thread from enabling and disabling
	 * preferences while another thread is validating spelling and expecting the
	 * spelling to be enabled.
	 */
	private static final Semaphore SPELLING_SEMAPHORE = new Semaphore(1);

	private static final List<ModelEnum> REQUIRED_MODELS = Arrays.asList(ModelEnum.STRING, ModelEnum.COMP_UNIT);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ModelEnum> getRequiredModels() {
		return REQUIRED_MODELS;
	}

	/**
	 * The watched dictionary file in the user's installation area. The words in
	 * this file are added to the default dictionary. This value will never be
	 * null.
	 */
	private PersonalDictionaryFile userDictWatchedFile;

	private PersonalDictionaryFile baseDictWatchedFile;

	/**
	 * Constructor for SpellingValidator.
	 */
	public SpellingValidator() {
		File baseDir = null;

		String installDirURL = System.getProperty("osgi.install.area");
		if (installDirURL != null) {

			try {
				URI uri = new URL(installDirURL).toURI();
				baseDir = new File(uri);
			} catch (URISyntaxException e) {
				Debug.FRAMEWORK.log(e);
			} catch (MalformedURLException e) {
				Debug.FRAMEWORK.log(e);
			}
		}

		if (baseDir == null) {
			baseDir = new File(System.getProperty("user.dir"));
		}

		File userDictFile = new File(baseDir, "dropins\\wizard\\userconfig\\userdict.txt");
		userDictWatchedFile = new PersonalDictionaryFile(userDictFile);

		// TODO this should come from the plug-in
		File baseDictFile = new File(baseDir, "dropins\\wizard\\eclipse\\plugins\\userdict.txt");
		baseDictWatchedFile = new PersonalDictionaryFile(baseDictFile);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ValidationResult> analyze(List<ValidatorResource> resources, ProblemActionFactory actionFactory,
			IProgressMonitor monitor) throws ValidationException, IOException, OperationCanceledException {

		List<ValidationResult> results = new ArrayList<ValidationResult>();
		boolean preferencesSet = false;
		try {
			try {
				SPELLING_SEMAPHORE.acquire();
			} catch (InterruptedException e1) {
				if (Debug.FRAMEWORK.isActive()) {
					Debug.FRAMEWORK.log(e1);
				}

				throw new OperationCanceledException("Acquire interrupted.");
			}
			monitor.beginTask("Check spelling errors", 2 + resources.size());

			userDictWatchedFile.ensureLatestLoaded();
			if (userDictWatchedFile.changed()) {
				// copy the contents of the user dict into the base dict
				Set<String> words = new LinkedHashSet<String>();
				userDictWatchedFile.loadWords(words);
				baseDictWatchedFile.ensureLatestLoaded();
				if (baseDictWatchedFile.getLoadedObject().booleanValue()) {
					baseDictWatchedFile.loadWords(words);
				}

				baseDictWatchedFile.writeWords(words);
			}

			baseDictWatchedFile.ensureLatestLoaded();
			if (baseDictWatchedFile.getLoadedObject().booleanValue()) {
				setSpellCheckerPreferences(true, userDictWatchedFile.getFilename());
				preferencesSet = true;
			}

			SpellingContext context = new SpellingContext();
			SpellingService spellingService = EditorsUI.getSpellingService();

			IContentType textContentType = Platform.getContentTypeManager()
					.getContentType("org.eclipse.core.runtime.text");
			context.setContentType(textContentType);
			monitor.worked(1);

			// validate files
			for (ValidatorResource resource : resources) {
				EclipseUtil.getDefault().checkCanceled(monitor);

				CompilationUnit comp = ModelEnum.COMP_UNIT.getData(resource);
				List<ASTNode> javadocs = resource.getTypedNodeList(Javadoc.JAVADOC);
				for (ASTNode node : javadocs) {

					EclipseUtil.getDefault().checkCanceled(monitor);

					Comment bigComment = new Comment(resource, (Javadoc) node, comp);
					List<CommentDescription> descriptions = new ArrayList<CommentDescription>();
					if (bigComment.getDescription() != null) {
						descriptions.add(bigComment.getDescription());
					}
					List<Tag> tags = bigComment.getAllTags();
					for (Tag tag : tags) {
						if (tag.getComment() != null) {
							descriptions.add(tag.getComment());
						}
					}

					for (CommentDescription description : descriptions) {
						EclipseUtil.getDefault().checkCanceled(monitor);

						String comment = description.getHumanReadableString();
						Document document = new Document(comment);
						SpellingProblemCollector collector = new SpellingProblemCollector();
						spellingService.check(document, context, collector, null);
						for (SpellingProblem problem : collector.getProblems()) {
							reportSpellingProblem(results, resource, description, problem, actionFactory);
						}
					}
				}

				monitor.worked(1);
			}
		} finally {
			try {
				if (preferencesSet) {
					setSpellCheckerPreferences(false, null);
				}

				monitor.worked(1);
				monitor.done();
			} finally {
				SPELLING_SEMAPHORE.release();
			}
		}

		return results;
	}

	/**
	 * Adds a single spelling problem to the given results. If the spelling
	 * error is exempt because it is surrounded by <code>&lt;code&gt;</code>,
	 * <code>&lt;samp&gt;</code>, or <code>&lt;pre&gt;</code>, it will not be
	 * added.
	 *
	 * @param results
	 *            The results to add to. Cannot be null.
	 * @param resource
	 *            The file that contains the spelling error. Cannot be null.
	 * @param description
	 *            The description in which the error exists. Cannot be null.
	 * @param problem
	 *            The problem as reported by the Eclipse spell checker. Cannot
	 *            be null.
	 */
	private void reportSpellingProblem(List<ValidationResult> results, ValidatorResource resource,
			CommentDescription description, SpellingProblem problem, ProblemActionFactory actionFactory) {

		int offset = problem.getOffset();
		int length = problem.getLength();

		boolean exempt = true;
		HumanFragmentView view = new HumanFragmentView(offset, length, description);
		for (HumanFragmentView.Char character : view.getSourceChars()) {
			if (character.getSpellingState() == CommentFragment.CheckerState.APPLICABLE) {
				exempt = false;
				break;
			}
		}

		if (!exempt) {
			int startPositionOfErrorWord = description.findInComment(offset);
			int endPosition = startPositionOfErrorWord + length;
			CompilationUnit comp = ModelEnum.COMP_UNIT.getData(resource);

			List<ProblemAction> suggestions = new ArrayList<ProblemAction>();
			for (ICompletionProposal proposal : problem.getProposals()) {
				if (!(proposal instanceof WordCorrectionProposal)) {
					continue;
				}
				String replacement = proposal.getDisplayString();
				int i = replacement.indexOf(SINGLEQUOTES);
				if (i != -1) {
					replacement = replacement.substring(i + 1, replacement.length() - 1);
				}

				HumanFragmentView replaceView = new HumanFragmentView(offset, length, description);
				replaceView.handleDiff(replacement);

				String sourceView = replaceView.getSourceView(true);

				ProblemAction action = actionFactory.buildReplace(resource, startPositionOfErrorWord, endPosition,
						sourceView);
				suggestions.add(action);
			}

			HumanFragmentView surroundView = new HumanFragmentView(offset, length, description);
			String baseText = surroundView.getHumanView(false);
			baseText = "<code>" + baseText + "</code>";
			surroundView.handleDiff(baseText);
			String surroundSuggestion = surroundView.getSourceView(true);
			ProblemAction action = actionFactory.buildReplace(resource, startPositionOfErrorWord, endPosition,
					surroundSuggestion);
			suggestions.add(0, action);

			String textToReplace = view.getSourceView(false);
			int sourceLine = comp.getLineNumber(startPositionOfErrorWord) - 1;
			int sourceColumn = comp.getColumnNumber(startPositionOfErrorWord);

			ValidationResult result = new ValidationResult(problem.getMessage(), resource, suggestions, sourceLine,
					sourceColumn, textToReplace.length(), startPositionOfErrorWord, "Spelling");
			results.add(result);
		}
	}

	/**
	 * Set two preferences of the Eclipse spell-checker:
	 * <ul>
	 * <li>Turn the Eclipse spell-checker on or off.
	 * <li>Set the user dictionary. The words in this dictionary are added to
	 * the default Eclipse dictionary.
	 * </ul>
	 *
	 * @param active
	 *            True if the spell-checker should be turned on, false if it
	 *            should be turned off.
	 * @param userDictFile
	 *            The path of the user dictionary file. If null or empty, or
	 *            <code>active</code> is false, the dictionary will be turned
	 *            off.
	 */
	private void setSpellCheckerPreferences(final boolean active, final String userDictFile) {
		final CountDownLatch latch = new CountDownLatch(1);
		Runnable updatePreferencesRunnable = new Runnable() {

			public void run() {
				IPreferenceStore preferenceStore = EditorsUI.getPreferenceStore();
				IPreferenceStore store = JavaPlugin.getDefault().getPreferenceStore();
				String filenameToSet;
				if (active && userDictFile != null && !userDictFile.trim().isEmpty()) {
					filenameToSet = userDictFile;
				} else {
					filenameToSet = "";
				}
				store.setValue("spelling_user_dictionary", filenameToSet);
				preferenceStore.setValue("spellingEnabled", active);

				latch.countDown();
			}
		};

		if (Display.getDefault().getThread().equals(Thread.currentThread())) {
			// this thread is the GUI thread
			updatePreferencesRunnable.run();
		} else {
			// we need to change preferences on the GUI thread
			Display.getDefault().asyncExec(updatePreferencesRunnable);

			boolean stillHaveTime;
			try {
				stillHaveTime = latch.await(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new OperationCanceledException(
						"Thread interrupted on spelling validator, aborting this validation run.");
			}

			if (!stillHaveTime) {
				throw new OperationCanceledException("Timeout on spelling validator, aborting this validation run.");
			}
		}
	}

	/**
	 * SpellingProblemCollector collects spelling problems from the eclipse
	 * spell-checker.
	 */
	private class SpellingProblemCollector implements ISpellingProblemCollector {

		/**
		 * The list of problems collected so far. See {@link #getProblems} for
		 * details.
		 */
		private List<SpellingProblem> problems = new ArrayList<SpellingProblem>();

		/**
		 * Adds the given spelling problem to the list.
		 *
		 * @param spellingProblem
		 *            The problem to add. Cannot be null.
		 */
		@Override
		public void accept(SpellingProblem spellingProblem) {
			problems.add(spellingProblem);
		}

		/**
		 * Notifies this that spelling is about to begin.
		 */
		@Override
		public void beginCollecting() {
		}

		/**
		 * Notifies this that spelling has ended.
		 */
		@Override
		public void endCollecting() {
		}

		/**
		 * Returns the list of spelling problems collected. This method should
		 * be called after the spell-check has been completed.
		 *
		 * @return The list of spelling problems found. Will not be null.
		 */
		public List<SpellingProblem> getProblems() {
			return problems;
		}
	}

	/**
	 * PersonalDictionaryFile is used to watch the dictionary file. The loaded
	 * object is true if the file exists and can be opened, false otherwise.
	 */
	private class PersonalDictionaryFile extends WatchedFile<Boolean> {

		/**
		 * The name of the watched file. See {@link #getFilename} for details.
		 */
		private String filename;

		/**
		 * Constructor for PersonalDictionaryFile.
		 *
		 * @param newFile
		 *            The file to watch. Cannot be null.
		 */
		private PersonalDictionaryFile(File newFile) {
			super(newFile);
			this.filename = newFile.getAbsolutePath();
		}

		/**
		 * The name of the watched file.
		 *
		 * @return The name of the watched file. This value will not be null or
		 *         empty.
		 */
		public String getFilename() {
			return filename;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void syncWithSystem() {
			try {
				InputStream in = openInputStream();
				in.close();
				setLoadedObject(Boolean.TRUE);
			} catch (IOException e) {
				if (Debug.CONFIG.isActive()) {
					Debug.CONFIG.log(e, "Dictionary file " + getFilename() + " could not be opened.");
				}
				setLoadedObject(Boolean.FALSE);
			}
		}

		/**
		 * Loads the words in this file and adds them to the given set.
		 *
		 * @param words
		 *            The set of words to add to. Cannot be null, but may be
		 *            empty.
		 */
		private void loadWords(Set<String> words) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(openInputStream()));
				String line = reader.readLine();
				while (line != null) {
					if (!line.trim().isEmpty()) {
						words.add(line);
					}
					line = reader.readLine();
				}
			} catch (IOException e) {
				Debug.FRAMEWORK.log(e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Debug.FRAMEWORK.log(e);
					}
				}
			}
		}

		/**
		 * Writes the given words to the file represented by this. The existing
		 * file will be overwritten.
		 *
		 * @param words
		 *            The set of words to write to the file. Cannot be null, but
		 *            may be empty.
		 */
		private void writeWords(Set<String> words) {
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getFilename())));
				for (String word : words) {
					writer.write(word);
					writer.newLine();
				}
			} catch (IOException e) {
				Debug.FRAMEWORK.log(e);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						Debug.FRAMEWORK.log(e);
					}
				}
			}
		}
	}
}
