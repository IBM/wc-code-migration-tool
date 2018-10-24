package com.ibm.commerce.qcheck.ui.builder;

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

import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ProblemAction;
import com.ibm.commerce.qcheck.ui.internal.UIActionFactory;
import com.ibm.commerce.qcheck.ui.internal.UIValidationRunner;

/**
 * ValidationMarkerResolutionGenerator is used to create
 * {@link IMarkerResolution marker resolutions} for errors that are created as a
 * result of validation. Markers are added to files and show up in the eclipse
 * UI as small colored lines with comments and possible suggestions (on the
 * right hand side of the editor). Each marker resolution represents one of
 * those suggestions, which the user can select to perform the suggestion
 * automatically.
 * 
 * @author Trent Hoeppner
 */
public class ValidationMarkerResolutionGenerator implements IMarkerResolutionGenerator {

	/**
	 * Constructor for this.
	 */
	public ValidationMarkerResolutionGenerator() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		IMarkerResolution[] results;
		try {
			Integer numSuggestionsInteger = (Integer) marker
					.getAttribute(UIValidationRunner.MARKER_ATT_NUM_SUGGESTIONS);
			if (numSuggestionsInteger != null) {
				int numSuggestions = numSuggestionsInteger;
				results = new IMarkerResolution[numSuggestions];
				for (int i = 0; i < numSuggestions; i++) {
					String xml = (String) marker.getAttribute(UIValidationRunner.MARKER_ATT_SUGGESTION_PREFIX + i);
					try {
						ProblemAction currSuggestion = new UIActionFactory().unmarshal(xml);
						results[i] = new SuggestionMarkerResolution(currSuggestion);
					} catch (IOException e) {
						Debug.VALIDATOR.log(e);
					}
				}
			} else {
				results = new IMarkerResolution[0];
			}
		} catch (CoreException e) {
			Debug.FRAMEWORK.log(e);
			results = new IMarkerResolution[0];
		} catch (NullPointerException e) {
			Debug.FRAMEWORK.log(e);
			results = new IMarkerResolution[0];
		}

		return results;
	}

	/**
	 * SuggestionMarkerResolution is a simple marker resolution that supports
	 * replacement of text highlighted by the marker.
	 */
	private static final class SuggestionMarkerResolution implements IMarkerResolution {

		/**
		 * The string to replace the text highlighted by the marker. This value
		 * will never be null, but may be empty.
		 */
		private ProblemAction action;

		/**
		 * Constructor for SuggestionMarkerResolution.
		 *
		 * @param newSuggestion
		 *            The string to replace the text highlighted by the marker.
		 *            Cannot be null, but may be empty.
		 */
		private SuggestionMarkerResolution(ProblemAction newSuggestion) {
			this.action = newSuggestion;
		}

		/**
		 * Returns the label that describes this marker resolution.
		 *
		 * @return The label that describes this marker resolution. Will not be
		 *         null or empty.
		 */
		@Override
		public String getLabel() {
			return action.getDescription();
		}

		/**
		 * Executes the action in this.
		 *
		 * @param marker
		 *            The marker that is associated with the action. This value
		 *            cannot be null.
		 */
		@Override
		public void run(IMarker marker) {
			try {
				action.execute();
			} catch (Throwable e) {
				Debug.FRAMEWORK.log(e);
			}
		}

	}
}
