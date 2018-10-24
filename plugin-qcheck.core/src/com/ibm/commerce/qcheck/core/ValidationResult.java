package com.ibm.commerce.qcheck.core;

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

import java.util.List;

/**
 * ValidationResult represents a validation check that failed. A message
 * describing the error and its location in the source file are included. One or
 * more suggestions may also be included, which can completely replace the text
 * in the original file.
 * 
 * @author Trent Hoeppner
 */
public class ValidationResult {

	/**
	 * The description of the error. See {@link #getMessage} for details.
	 */
	private String message;

	/**
	 * The original resource in which the error occurred. See
	 * {@link #getResource} for details.
	 */
	private ValidatorResource resource;

	/**
	 * The list of actions to take to resolve the problem. See
	 * {@link #getProblemActions} for details.
	 */
	private List<ProblemAction> problemActions;

	/**
	 * The 0-based starting line of the text which is in error. See
	 * {@link #getLine} for details.
	 */
	private int line;

	/**
	 * The 0-based starting column of the text which is in error. See
	 * {@link #getColumn} for details.
	 */
	private int column;

	/**
	 * The number of characters of the text which is in error. See
	 * {@link #getLength} for details.
	 */
	private int length;

	/**
	 * The 0-based index into the characters of the source file where the text
	 * which is in error begins. See {@link #getStartingPosition} for details.
	 */
	private int startingPosition;

	/**
	 * A prefix for the error message to help the user identify the type of rule
	 * which occurred. See {@link #getRuleType} for details.
	 */
	private String ruleType;

	/**
	 * Constructor for ValidationResult.
	 *
	 * @param newMessage
	 *            A description of the error. Cannot be null or empty.
	 * @param newResource
	 *            The file in which the error occurred. Cannot be null.
	 * @param newProblemActions
	 *            The list of actions to resolve the problem. Cannot be null,
	 *            but may be empty to indicate no actions.
	 * @param newLine
	 *            The 0-based starting line of the text which is in error. Must
	 *            be &gt;= 0.
	 * @param newColumn
	 *            The 0-based starting column of the text which is in error.
	 *            Must be &gt;= 0.
	 * @param newLength
	 *            The number of characters of the text which is in error. Note
	 *            that the error may span multiple lines. Must be &gt;= 0.
	 * @param newStartingPosition
	 *            The 0-based index into the characters of the source file where
	 *            the text which is in error begins.
	 * @param newRuleType
	 *            A prefix for the error message to help the user identify the
	 *            type of rule which occurred. Cannot be null or empty.
	 */
	public ValidationResult(String newMessage, ValidatorResource newResource, List<ProblemAction> newProblemActions,
			int newLine, int newColumn, int newLength, int newStartingPosition, String newRuleType) {

		this.message = newMessage;
		this.resource = newResource;
		this.problemActions = newProblemActions;
		this.line = newLine;
		this.column = newColumn;
		this.length = newLength;
		this.startingPosition = newStartingPosition;
		this.ruleType = newRuleType;
	}

	/**
	 * Returns the 0-based index into the characters of the source file where
	 * the text which is in error begins.
	 *
	 * @return The starting position of the text within the file. Will be &gt;=
	 *         0.
	 */
	public int getStartingPosition() {
		return startingPosition;
	}

	/**
	 * Returns the description of the error.
	 *
	 * @return The description of the error. Will not be null or empty.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the file in which the error occurred.
	 *
	 * @return The file in which the error occurred. Will not be null.
	 */
	public ValidatorResource getResource() {
		return resource;
	}

	/**
	 * Returns the list of actions to take to solve the problem.
	 *
	 * @return The list of actions. This value will not be null, but may be
	 *         empty to indicate no actions.
	 */
	public List<ProblemAction> getProblemActions() {
		return problemActions;
	}

	/**
	 * Returns the 0-based starting line of the text which is in error.
	 *
	 * @return The 0-based starting line of the text. Will be &gt;= 0.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Returns the 0-based starting column of the text which is in error.
	 *
	 * @return The 0-based starting column of the text. Will be &gt;= 0.
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * Returns the number of characters of the text which is in error. Note that
	 * the error may span multiple lines.
	 *
	 * @return The number of characters of the text. Will be &gt;= 0.
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Returns the prefix for the error message to help the user identify the
	 * type of rule which occurred.
	 *
	 * @return A prefix for the error message. Will not be null or empty.
	 */
	public String getRuleType() {
		return ruleType;
	}
}
