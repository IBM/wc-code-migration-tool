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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.commerce.cmt.CodeMigrationTool;
import com.ibm.commerce.cmt.plan.Issue;
import com.ibm.commerce.cmt.plan.LogStep;
import com.ibm.commerce.cmt.plan.Plan;
import com.ibm.commerce.cmt.plan.ReplaceInFileStep;
import com.ibm.commerce.cmt.plan.Step;
import com.ibm.commerce.dependency.load.APIFileManager;
import com.ibm.commerce.dependency.model.JavaItemIndex;
import com.ibm.commerce.qcheck.core.CompUnitModel;
import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ProblemAction;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.ValidationException;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.Level;
import com.ibm.commerce.qcheck.tools.config.LevelEnum;

/**
 * JavaDocValidator is used to check spacing and specific style points of Java
 * doc. The specific points addressed, and the {@link ConfigurationManager
 * configurable} levels are:
 * <ul>
 * <li>LOOSE
 * <ul>
 * <li>The <code>@throws</code> tag is not an <code>@exception</code> tag.
 * </ul>
 * <li>NORMAL
 * <ul>
 * <li>There is exactly one empty line after the description, but only if there
 * is at least one tag after the description.
 * <li>The name of the exception is not in the description of the
 * <code>@throws</code> tag.
 * <li>The <code>@throws</code> tag follows a convention of starting with "If"
 * followed by the circumstances in which the exception is thrown. This also
 * supports exceptions that are always thrown or never thrown.
 * <li>The <code>@return</code> tag does not have a reference to the return
 * type.
 * <li>Each <code>@param</code> tag does not have a reference to the parameter
 * type.
 * <li>If the return type is an object, the <code>@return</code> tag describes
 * the conditions when null will be returned, or declares that null will not be
 * returned. This will not be applied to "get" methods with no parameters and a
 * return value.
 * <li>If the return type is an array, the <code>@return</code> tag describes
 * the conditions when an empty array will be returned, or declares that an
 * empty array will not be returned. This will not be applied to "get" methods
 * with no parameters and a return value.
 * <li>If the return type is a String, the <code>@return</code> tag describes
 * the conditions when an empty String will be returned, or declares that an
 * empty String will not be returned. This will not be applied to "get" methods
 * with no parameters and a return value.
 * <li>If a parameter type is an object, each <code>@param</code> tag describes
 * what happens when the object is null, or declares that the object cannot be
 * null. This will not be applied to "set" methods with only one parameter and
 * no return values.
 * <li>If a parameter type is an array, each <code>@param</code> tag describes
 * what happens when the array is empty, or declares that the array cannot be
 * empty. This will not be applied to "set" methods with only one parameter and
 * no return values.
 * <li>If the parameter type is a String, each <code>@param</code> tag describes
 * what happens when the String is empty, or declares that the String cannot be
 * empty. This will not be applied to "set" methods with only one parameter and
 * no return values.
 * </ul>
 * <li>STRICT
 * <ul>
 * <li>For an attribute's field, "set" method, and "get" method trio, if the
 * attribute is an object, the entity with the highest visibility (private &lt;
 * package-private &lt; protected &lt; public) must state what happens when the
 * attribute is null (attribute description, <code>@param</code> description, or
 * <code>@return</code> description as appropriate). The others must have a
 * <code>@link</code> to that description. For same visibility, the attribute
 * description defers to the "get" and "set" methods, and the "get" method
 * defers to the "set" method.
 * </ul>
 * </ul>
 * 
 * @author Trent Hoeppner
 */
public class CMTValidator implements ConfigurableValidator {

	private CodeMigrationTool tool;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ModelEnum> getRequiredModels() {
		return Arrays.asList(ModelEnum.BATCH);
	}

	/**
	 * The detail level of validation. See {@link #getLevel} for details.
	 */
	private Level level;

	/**
	 * Constructor for RSARValidator.
	 *
	 * @param newLevel
	 *            The detail at which this should be configured. Cannot be null.
	 */
	public CMTValidator(Level newLevel) {
		this.level = newLevel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ValidationResult> analyze(List<ValidatorResource> resources, ProblemActionFactory actionFactory,
			IProgressMonitor monitor) throws ValidationException, IOException, OperationCanceledException {

		List<ValidationResult> results = new ArrayList<ValidationResult>();
		try {
			monitor.beginTask("Checking code migration issues", resources.size());
			if (level.getValue() == LevelEnum.NONE) {
				return results;
			}

			if (tool == null) {
				tool = new CodeMigrationTool("cmt.log");
				File serializedFile = new File(CodeMigrationTool.API_ZIP_FILENAME);
				System.out.println("serializedFile = " + serializedFile.getAbsolutePath());

				APIFileManager apiFileManager = new APIFileManager();
				JavaItemIndex index = apiFileManager.loadAPI(serializedFile);
				tool.initialize(index);
			}

			Set<File> filesToInvalidate = new LinkedHashSet<File>();
			for (ValidatorResource resource : resources) {
				File file = resource.getFileAsFile();
				filesToInvalidate.add(file);
			}

			Plan plan = tool.createPlan(Arrays.asList("v8-v9-wc-patterns.xml"), filesToInvalidate);

			for (ValidatorResource resource : resources) {
				if (Debug.VALIDATOR.isActive()) {
					Debug.VALIDATOR.log("Validating code migration tool issues for ", resource.getClassName());
				}

				// find the issues from the plan for this resource
				File file = resource.getFileAsFile();
				String filePath = file.getAbsolutePath();
				CompUnitModel compUnitModel = resource.getModelRegistry().getModel(ModelEnum.COMP_UNIT.getName(),
						resource);
				for (Issue issue : plan.getIssues()) {
					if (issue.getLocation().getFile().equals(filePath)) {
						int startingPosition = issue.getLocation().getRange().getStart();
						int length = issue.getLocation().getRange().getLength();

						CompilationUnit compilationUnit = compUnitModel.getModel();
						int column = compilationUnit.getColumnNumber(startingPosition);
						int lineNumber = compilationUnit.getLineNumber(startingPosition) - 1;

						// try to get a message, otherwise get a replacement
						// string, otherwise get nothing
						String message = "Error: Could not determine a message.";
						String replacement = null;
						for (Step step : issue.getSteps()) {
							if (step instanceof LogStep) {
								LogStep logStep = (LogStep) step;
								message = logStep.getMessage();
								break;
							} else if (step instanceof ReplaceInFileStep) {
								ReplaceInFileStep replaceStep = (ReplaceInFileStep) step;
								message = issue.getSource() + " should be replaced with "
										+ replaceStep.getReplacement();
								replacement = replaceStep.getReplacement();
								break;
							}
						}

						List<ProblemAction> actions = new ArrayList<ProblemAction>();
						if (replacement != null) {
							ProblemAction action = actionFactory.buildReplace(resource, startingPosition,
									issue.getLocation().getRange().getEnd(), replacement);
							actions.add(action);
						}

						ValidationResult result = new ValidationResult(message, resource, actions, lineNumber, column,
								length, startingPosition, "CMT");

						results.add(result);
					}
				}

				monitor.worked(1);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			monitor.done();
		}

		return results;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Level getLevel() {
		return level;
	}

}
