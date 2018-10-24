package com.ibm.commerce.qcheck.ui.internal;

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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ProblemAction;
import com.ibm.commerce.qcheck.core.ValidationException;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.comment.CommentUtil;
import com.ibm.commerce.qcheck.tools.Options;
import com.ibm.commerce.qcheck.tools.ValidatorRunner;
import com.ibm.commerce.qcheck.tools.config.TimeEnum;

/**
 * UIValidationRunner is a utility class to run the validation tools for the
 * purpose of adding markers. This class is used by the builder mechanism and
 * the as-you-type mechanism.
 * 
 * @author Trent Hoeppner
 */
public class UIValidationRunner {

	/**
	 * The singleton instance of UIValidationRunner.
	 */
	public static final UIValidationRunner INSTANCE = new UIValidationRunner();

	/**
	 * The name of a marker attribute, used to store the start position for the
	 * marker.
	 */
	public static final String MARKER_ATT_START_POSITION = "start position";

	/**
	 * The name of a marker attribute, used to store the number of suggestions
	 * for the marker.
	 */
	public static final String MARKER_ATT_NUM_SUGGESTIONS = "numSuggestions";

	/**
	 * The prefix for marker attributes that define the suggestions used for a
	 * marker. The index of the suggestion is added on to this prefix to make
	 * the full marker attribute name.
	 */
	public static final String MARKER_ATT_SUGGESTION_PREFIX = "suggestion.";

	/**
	 * The name for the type of marker produced by this builder.
	 */
	public static final String MARKER_TYPE = "com.ibm.commerce.qcheck.ui.javaProblem";

	/**
	 * Constructor for UIValidationRunner. Package-private to prevent external
	 * instantiation.
	 */
	UIValidationRunner() {
		// do nothing
	}

	/**
	 * Runs all configured validators on the given files and adds markers to
	 * each file for any errors that are found.
	 *
	 * @param resources
	 *            The files to check. Cannot be null.
	 * @param time
	 *            The circumstances under which the validation occurs. Cannot be
	 *            null.
	 * @param monitor
	 *            The monitor used to report the progress of the validators and
	 *            detect cancellation. May be null.
	 */
	public void validate(List<ValidatorResource> resources, TimeEnum time, IProgressMonitor monitor) {
		try {
			monitor.beginTask("Validate and add markers", 2);

			Options.ensureLoaded();
			Boolean asYouTypeLoaded = Options.Attributes.AS_YOU_TYPE_BUTTON_ON.getValue();
			Boolean buildOn = Options.Attributes.BUILDER_BUTTON_ON.getValue();
			boolean asYouTypeRequestedButDisabled = time == TimeEnum.ASYOUTYPE && !asYouTypeLoaded.booleanValue();
			boolean buildRequestedButDisabled = (time == TimeEnum.FULLTOOLKITBUILD
					|| time == TimeEnum.INCREMENTALTOOLKITBUILD) && !buildOn.booleanValue();
			if (asYouTypeRequestedButDisabled || buildRequestedButDisabled) {
				return;
			}

			deleteMarkers(resources);

			List<ValidationResult> allResults = ValidatorRunner.runValidators(resources, time, new UIActionFactory(),
					new SubProgressMonitor(monitor, 1));

			// add markers to all files
			for (ValidationResult result : allResults) {
				if (Debug.COMMENT.isActive()) {
					StringBuffer buf = new StringBuffer();
					CommentUtil.printError(result);
					buf.append(CommentUtil.createSpaces(result.getColumn()));
					int matchLength = result.getLength();
					for (int i = 0; i < matchLength; i++) {
						buf.append("^");
					}
					Debug.COMMENT.log(buf.toString());
				}

				ValidatorResource possibleResource = result.getResource();
				addMarker(possibleResource.getFileAsResource(), result, IMarker.SEVERITY_WARNING);
			}
			monitor.worked(1);
		} catch (IOException e) {
			Debug.FRAMEWORK.log(e);
		} catch (ValidationException e) {
			Debug.FRAMEWORK.log(e);
		} catch (OperationCanceledException e) {
			if (Debug.FRAMEWORK.isActive()) {
				Debug.FRAMEWORK.log(e);
			}
		} catch (Exception e) {
			Debug.FRAMEWORK.log(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Adds the given marker to the given file.
	 *
	 * @param resource
	 *            The file to add the marker to. Cannot be null.
	 * @param result
	 *            The error to convert into a marker. Cannot be null.
	 * @param severity
	 *            The seriousness of the error. Must be a valid severity
	 *            constant defined in {@link IMarker}.
	 */
	public void addMarker(IResource resource, ValidationResult result, int severity) {
		try {
			IMarker marker = resource.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, result.getRuleType() + ": " + result.getMessage());
			marker.setAttribute(IMarker.SEVERITY, severity);

			int lineNumber = result.getLine() + 1;
			if (lineNumber <= 0) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			marker.setAttribute(IMarker.CHAR_START, result.getStartingPosition());
			marker.setAttribute(IMarker.CHAR_END, result.getStartingPosition() + result.getLength());
			marker.setAttribute(MARKER_ATT_START_POSITION, result.getStartingPosition());
			List<ProblemAction> suggestions = result.getProblemActions();
			marker.setAttribute(MARKER_ATT_NUM_SUGGESTIONS, suggestions.size());

			UIActionFactory actionFactory = new UIActionFactory();
			for (int i = 0; i < suggestions.size(); i++) {
				String xml = actionFactory.marshal(suggestions.get(i));
				marker.setAttribute(MARKER_ATT_SUGGESTION_PREFIX + i, xml);
			}
		} catch (CoreException e) {
			Debug.FRAMEWORK.log(e);
		} catch (Exception e) {
			Debug.FRAMEWORK.log(e);
		}
	}

	/**
	 * Removes all markers defined by this from the given file. If the resource
	 * does not exist, no attempt will be made to delete markers.
	 *
	 * @param resource
	 *            The file to remove markers from. Cannot be null.
	 */
	public void deleteMarkers(IResource resource) {
		try {
			if (resource != null && resource.exists()) {
				resource.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
				if (resource instanceof IContainer) {
					IContainer container = (IContainer) resource;
					IResource[] members = container.members();
					for (IResource member : members) {
						deleteMarkers(member);
					}
				}
			}
		} catch (CoreException e) {
			Debug.FRAMEWORK.log(e);
		}
	}

	/**
	 * Remove all markers in the area which belongs to the given files. It is a
	 * overloading method of {@link deleteMarkers(IResource resource)}.
	 *
	 * @param resources
	 *            The files to remove markers form. Cannot be null.
	 */
	public void deleteMarkers(List<ValidatorResource> resources) {
		// remove all the existing markers in the area
		Iterator<ValidatorResource> resourceIterator = resources.iterator();
		while (resourceIterator.hasNext()) {
			ValidatorResource resource = resourceIterator.next();
			deleteMarkers(resource.getFileAsResource());
		}
	}
}
