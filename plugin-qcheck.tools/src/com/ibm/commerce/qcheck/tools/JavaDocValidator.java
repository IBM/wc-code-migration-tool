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

import java.beans.Introspector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.EclipseUtil;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ProblemAction;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.Util;
import com.ibm.commerce.qcheck.core.ValidationException;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.comment.Comment;
import com.ibm.commerce.qcheck.core.comment.CommentDescription;
import com.ibm.commerce.qcheck.core.comment.CommentFragment;
import com.ibm.commerce.qcheck.core.comment.CommentSpace;
import com.ibm.commerce.qcheck.core.comment.HumanFragmentView;
import com.ibm.commerce.qcheck.core.comment.Tag;
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
public class JavaDocValidator implements ConfigurableValidator {

	/**
	 * The list of valid beginnings for <code>@exception</code> tags.
	 */
	private static final List<String> THROWS_BEGINNINGS = Arrays
			.asList(new String[] { "If", "Will always be thrown.", "Will never be thrown." });

	private static final List<ModelEnum> REQUIRED_MODELS = Arrays.asList(ModelEnum.STRING, ModelEnum.COMP_UNIT);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ModelEnum> getRequiredModels() {
		return REQUIRED_MODELS;
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
	public JavaDocValidator(Level newLevel) {
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
			monitor.beginTask("Checking JavaDoc errors", resources.size());
			if (level.getValue() == LevelEnum.NONE) {
				return results;
			}

			for (ValidatorResource resource : resources) {
				if (Debug.VALIDATOR.isActive()) {
					Debug.VALIDATOR.log("Validating JavaDoc for ", resource.getClassName());
				}

				EclipseUtil.getDefault().checkCanceled(monitor);

				JavaDocStructure struct = new JavaDocStructure(resource);
				struct.checkAttributes(results, actionFactory, monitor);
				struct.checkOthers(results, actionFactory, monitor);

				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}

		return results;
	}

	/**
	 * Checks that there is exactly one empty line after the description, but
	 * only if there are no tags after the description.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param comment
	 *            The comment which helps identify spacing between sections.
	 *            Cannot be null.
	 */
	private void checkAfterDescriptionSpace(List<ValidationResult> results, ValidatorResource resource,
			Comment comment) {
		CompilationUnit compilationUnit = ModelEnum.COMP_UNIT.getData(resource);
		CommentSpace afterDescriptionSpace = comment.getSpaceAfterDescription();
		if (afterDescriptionSpace == null) {
			return;
		}

		int numLines = afterDescriptionSpace.getNumLines();
		if (numLines != 2 && afterDescriptionSpace.getAfter() != null) {
			int startingPosition = afterDescriptionSpace.getStartPosition();

			int column = compilationUnit.getColumnNumber(startingPosition);
			int lineNumber = compilationUnit.getLineNumber(startingPosition) - 1;

			ValidationResult result = new ValidationResult(
					"There should be exactly one empty line between the description and tags.", resource,
					Collections.EMPTY_LIST, lineNumber, column, afterDescriptionSpace.length(), startingPosition,
					"JavaDoc");

			results.add(result);
		}
	}

	/**
	 * Checks that the name of the exception is not in the description of the
	 * <code>@throws</code> tag.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The throws tag to check for the description name. Cannot be
	 *            null.
	 */
	private void checkExceptionNameInDescription(List<ValidationResult> results, ValidatorResource resource, Tag tag) {
		String completeExceptionName = tag.getSecondName().get(0).getText();

		String baseExceptionName = convertTypeStringToBaseName(completeExceptionName);

		String validationFailedMessage = "Do not refer to the exception type within the description.";
		checkClassInComment(results, resource, tag, baseExceptionName, validationFailedMessage);
	}

	/**
	 * Converts the given type into a class name without the package, array
	 * delimiters, or parameterized type.
	 *
	 * @param completeTypeName
	 *            The type that may include a package, array delimiters, and a
	 *            parameterized type. Cannot be null or empty.
	 *
	 * @return The base type. Will not be null or empty.
	 */
	private String convertTypeStringToBaseName(String completeTypeName) {
		String baseTypeName;

		StringBuffer buf = new StringBuffer(completeTypeName);

		// remove leading package name
		int lastDotIndex = buf.lastIndexOf(".");
		if (lastDotIndex >= 0) {
			buf.delete(0, lastDotIndex + 1);
		}

		// remove array indices
		removeBetweenChars(buf, "[", "]");

		// remove type indices
		removeBetweenChars(buf, "<", ">");

		baseTypeName = buf.toString();
		return baseTypeName;
	}

	/**
	 * Removes values in the given buffer between the given opening string and
	 * closing string, if such strings exist. If there is a match for the
	 * opening string, but not the closing string, the string from the opening
	 * until the end of the buffer will be removed.
	 *
	 * @param baseTypeName
	 *            The buffer to check possibly remove text from. Cannot be null.
	 * @param openChar
	 *            The marker for the beginning of the text to remove. Cannot be
	 *            null or empty.
	 * @param closeChar
	 *            The marker for the end of the text to remove. Cannot be null
	 *            or empty.
	 */
	private void removeBetweenChars(StringBuffer baseTypeName, String openChar, String closeChar) {
		int openCharIndex = baseTypeName.indexOf(openChar);
		if (openCharIndex >= 0) {
			int closeCharIndex = baseTypeName.indexOf(closeChar, openCharIndex);
			if (closeCharIndex < 0) {
				closeCharIndex = baseTypeName.length() - 1;
			}

			baseTypeName.delete(openCharIndex, closeCharIndex + 1);
		}
	}

	/**
	 * Checks that the type of the return value for a method is not mentioned in
	 * the <code>@return</code> tag of the method's comment.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The return tag to check for the appropriate style. Cannot be
	 *            null.
	 * @param methodDecl
	 *            The method declaration which contains the return type to check
	 *            for. Cannot be null.
	 */
	private void checkReturnClassNameInDescription(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			MethodDeclaration methodDecl) {

		Type type = methodDecl.getReturnType2();
		if (type != null) {
			int startPosition = type.getStartPosition();
			int length = type.getLength();
			String file = ModelEnum.STRING.getData(resource);
			String fullTypeString = file.substring(startPosition, startPosition + length);
			String baseTypeString = convertTypeStringToBaseName(fullTypeString);
			checkClassInComment(results, resource, tag, baseTypeString,
					"Do not refer to the return type within the description.");
		}
	}

	/**
	 * Checks that the type of parameter for a method is not mentioned in the
	 * <code>@param</code> tag of the method's comment.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The <code>param</code> tag to check for the appropriate style.
	 *            Cannot be null.
	 * @param paramDeclaration
	 *            The declaration for the parameter which defines the type to
	 *            check for. Cannot be null.
	 */
	private void checkParamClassNameInDescription(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			SingleVariableDeclaration paramDeclaration) {

		Type type = paramDeclaration.getType();
		if (type != null) {
			int startPosition = type.getStartPosition();
			int length = type.getLength();
			String file = ModelEnum.STRING.getData(resource);
			String fullTypeString = file.substring(startPosition, startPosition + length);
			String baseTypeString = convertTypeStringToBaseName(fullTypeString);
			checkClassInComment(results, resource, tag, baseTypeString,
					"Do not refer to the param type within the description.");
		}
	}

	/**
	 * Finds the parameter declaration in the given method declaration which
	 * matches the given tag.
	 *
	 * @param tag
	 *            The tag which contains the name of the parameter. Cannot be
	 *            null.
	 * @param methodDecl
	 *            The method declaration which contains the parameter
	 *            declarations. Cannot be null.
	 *
	 * @return The declaration for the requested parameter, or null if there is
	 *         no matching parameter.
	 */
	private SingleVariableDeclaration findParamForTag(Tag tag, MethodDeclaration methodDecl) {
		Util.checkNotNull("tag", tag);
		Util.checkNotNull("methodDecl", methodDecl);

		// find the right parameter by name
		String desiredParamName = tag.getSecondName().get(0).getText();
		if (desiredParamName == null) {
			return null;
		}

		List params = methodDecl.parameters();
		SingleVariableDeclaration found = null;
		for (Object param : params) {
			SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
			if (parameter.getName().getIdentifier().equals(desiredParamName)) {
				found = parameter;
				break;
			}
		}
		return found;
	}

	/**
	 * Checks that the word "null" is referenced in the <code>@return</code> tag
	 * in the comment of the given method declaration. This check is only
	 * performed if null is a possible value.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The return tag to check for the appropriate style. Cannot be
	 *            null.
	 * @param methodDecl
	 *            The method declaration which is used to determine if null is a
	 *            possible value for the method. Cannot be null.
	 * @param comment
	 *            The comment that contains the tag, which is used to locate the
	 *            end of the tag. Cannot be null.
	 */
	private void checkNullReferredForObjectReturn(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			MethodDeclaration methodDecl, Comment comment, ProblemActionFactory actionFactory) {

		Type type = methodDecl.getReturnType2();
		if (isObject(type)) {
			DescriptionBounds bounds = new TagDescriptionBounds(tag, comment);
			List<String> suggestions = Arrays.asList("This value will not be null.", "This value will be null if ...");
			checkStringExistsInComment(results, resource, bounds, "Handle the null case. "
					+ "Can the return value be null?  " + "If so, under what conditions can it be null?", "null",
					suggestions, actionFactory);
		}
	}

	/**
	 * Checks that the word "empty" is referenced in the <code>@return</code>
	 * tag in the comment of the given method declaration. This check is only
	 * performed if the return type is an array.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The return tag to check for the appropriate style. Cannot be
	 *            null.
	 * @param methodDecl
	 *            The method declaration which is used to determine if an array
	 *            is being returned. Cannot be null.
	 * @param comment
	 *            The comment that contains the tag, which is used to locate the
	 *            end of the tag. Cannot be null.
	 */
	private void checkEmptyReferredForArrayReturn(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			MethodDeclaration methodDecl, Comment comment, ProblemActionFactory actionFactory) {

		Type type = methodDecl.getReturnType2();
		if (isArray(type)) {
			DescriptionBounds bounds = new TagDescriptionBounds(tag, comment);
			List<String> suggestions = Arrays.asList("This value will not be empty.",
					"This value will not be null or empty.", "This value will be empty if ...");
			checkStringExistsInComment(results,
					resource, bounds, "Handle the case when an empty array is returned. "
							+ "Can the returned array be empty?  " + "If so, under what conditions can it be empty?",
					"empty", suggestions, actionFactory);
		}
	}

	/**
	 * Checks that the word "empty" is referenced in the <code>@return</code>
	 * tag in the comment of the given method declaration. This check is only
	 * performed if the return base type is a <code>String</code>.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The return tag to check for the appropriate style. Cannot be
	 *            null.
	 * @param methodDecl
	 *            The method declaration which is used to determine if a String
	 *            is being returned. Cannot be null.
	 * @param comment
	 *            The comment that contains the tag, which is used to locate the
	 *            end of the tag. Cannot be null.
	 */
	private void checkEmptyReferredForStringReturn(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			MethodDeclaration methodDecl, Comment comment, ProblemActionFactory actionFactory) {

		Type type = methodDecl.getReturnType2();
		if (isString(resource, type)) {
			DescriptionBounds bounds = new TagDescriptionBounds(tag, comment);
			List<String> suggestions = Arrays.asList("This value will not be empty.",
					"This value will not be null or empty.", "This value will be empty if ...");
			checkStringExistsInComment(results,
					resource, bounds, "Handle the case when an empty String is returned. "
							+ "Can the returned String be empty?  " + "If so, under what conditions can it be empty?",
					"empty", suggestions, actionFactory);
		}
	}

	/**
	 * Checks that the word "null" is referenced in the given
	 * <code>@param</code> tag in the comment of the given method declaration.
	 * This check is only performed if null is a possible value.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The parameter tag to check for the appropriate style. Cannot
	 *            be null.
	 * @param paramDeclaration
	 *            The method declaration which is used to determine if null is a
	 *            possible value for the parameter. Cannot be null.
	 * @param comment
	 *            The comment that contains the tag, which is used to locate the
	 *            end of the tag. Cannot be null.
	 */
	private void checkNullReferredForObjectParam(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			SingleVariableDeclaration paramDeclaration, Comment comment, ProblemActionFactory actionFactory) {

		Type type = paramDeclaration.getType();
		if (isObject(type)) {
			DescriptionBounds bounds = new TagDescriptionBounds(tag, comment);
			List<String> suggestions = Arrays.asList("Cannot be null.", "If this value is null, ...",
					"A null value indicates that ...");
			checkStringExistsInComment(results, resource, bounds,
					"Handle the null case. " + "Can the parameter be null?  " + "If so, what happens if it is null?",
					"null", suggestions, actionFactory);
		}
	}

	/**
	 * Checks that the word "empty" is referenced in the given
	 * <code>@param</code> tag in the comment of the given method declaration.
	 * This check is only performed if the parameter type is an array.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The parameter tag to check for the appropriate style. Cannot
	 *            be null.
	 * @param paramDeclaration
	 *            The method declaration which is used to determine if the
	 *            parameter type is an array. Cannot be null.
	 * @param comment
	 *            The comment that contains the tag, which is used to locate the
	 *            end of the tag. Cannot be null.
	 */
	private void checkEmptyReferredForArrayParam(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			SingleVariableDeclaration paramDeclaration, Comment comment, ProblemActionFactory actionFactory) {

		Type type = paramDeclaration.getType();
		if (isArray(type)) {
			DescriptionBounds bounds = new TagDescriptionBounds(tag, comment);
			List<String> suggestions = Arrays.asList("Cannot be empty.", "Cannot be null or empty.",
					"If this array is empty, ...", "An empty array indicates that ...");
			checkStringExistsInComment(
					results, resource, bounds, "Handle the case when an empty array is given. "
							+ "Can the array be empty?  " + "If so, what happens if it is empty?",
					"empty", suggestions, actionFactory);
		}
	}

	/**
	 * Checks that the word "empty" is referenced in the given
	 * <code>@param</code> tag in the comment of the given method declaration.
	 * This check is only performed if the parameter base type is a
	 * <code>String</code>.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The parameter tag to check for the appropriate style. Cannot
	 *            be null.
	 * @param paramDeclaration
	 *            The method declaration which is used to determine if the
	 *            parameter's base type is String. Cannot be null.
	 * @param comment
	 *            The comment that contains the tag, which is used to locate the
	 *            end of the tag. Cannot be null.
	 */
	private void checkEmptyReferredForStringParam(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			SingleVariableDeclaration paramDeclaration, Comment comment, ProblemActionFactory actionFactory) {

		Type type = paramDeclaration.getType();
		if (isString(resource, type)) {
			DescriptionBounds bounds = new TagDescriptionBounds(tag, comment);
			List<String> suggestions = Arrays.asList("Cannot be empty.", "Cannot be null or empty.",
					"If this string is empty, ...", "An empty string indicates that ...");
			checkStringExistsInComment(
					results, resource, bounds, "Handle the case when an empty String is given. "
							+ "Can the String be empty?  " + "If so, what happens if it is empty?",
					"empty", suggestions, actionFactory);
		}
	}

	/**
	 * Returns whether the given Eclipse type object is an array.
	 *
	 * @param type
	 *            The parsed representation of a Java type declaration. If null,
	 *            false will be returned.
	 *
	 * @return True if the given type is an array, false otherwise.
	 */
	public boolean isArray(Type type) {
		return type != null && type.isArrayType();
	}

	/**
	 * Returns whether the given Eclipse type is an <code>Object</code>.
	 *
	 * @param type
	 *            The parsed representation of a Java type declaration. If null,
	 *            false will be returned.
	 *
	 * @return True if the given type is not null and is not primitive, false
	 *         otherwise.
	 */
	private boolean isObject(Type type) {
		return type != null && !type.isPrimitiveType();
	}

	/**
	 * Returns whether the given type is a String in the given file.
	 *
	 * @param resource
	 *            The file which contains the declaration. Cannot be null.
	 * @param type
	 *            The parsed representation of a Java type declaration. If null,
	 *            false will be returned.
	 *
	 * @return True if the type is a String, false otherwise.
	 */
	private boolean isString(ValidatorResource resource, Type type) {
		return isType(resource, type, "String", false);
	}

	/**
	 * Returns whether the given type matches the expected type name.
	 *
	 * @param resource
	 *            The file which contains the declaration. Cannot be null.
	 * @param type
	 *            The parsed representation of a Java type declaration. If null,
	 *            false will be returned.
	 * @param name
	 *            The expected text for the type declaration. Cannot be null or
	 *            empty.
	 * @param expectPrimitive
	 *            True indicates that the type is expected to be a primitive
	 *            type, false indicates that it is expected not to be a
	 *            primitive type.
	 *
	 * @return True if the declaration given by type matches the name and
	 *         <code>type.isPrimitiveType()</code> agrees with
	 *         <code>expectPrimitive</code>, false otherwise.
	 */
	private boolean isType(ValidatorResource resource, Type type, String name, boolean expectPrimitive) {
		boolean isString = false;
		if (type != null && type.isPrimitiveType() == expectPrimitive) {
			int startPosition = type.getStartPosition();
			int length = type.getLength();
			String file = ModelEnum.STRING.getData(resource);
			String fullTypeString = file.substring(startPosition, startPosition + length);
			String baseTypeString = convertTypeStringToBaseName(fullTypeString);
			if (baseTypeString.equals(name)) {
				isString = true;
			}
		}

		return isString;
	}

	/**
	 * Checks that a string occurs within the description of the given tag.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param validationFailedMessage
	 *            The message to include in the {@link ValidationResult} if the
	 *            text is not found. Cannot be null or empty.
	 * @param valueToFind
	 *            The value which should appear in the tag's description. Cannot
	 *            be null or empty.
	 * @param suggestions
	 *            The list of suggestions to append to the description to
	 *            resolve the error. Cannot be null, but may be empty.
	 * @param actionFactory
	 *            The factory to generate actions. This value cannot be null.
	 */
	private void checkStringExistsInComment(List<ValidationResult> results, ValidatorResource resource,
			DescriptionBounds bounds, String validationFailedMessage, String valueToFind, List<String> suggestions,
			ProblemActionFactory actionFactory) {

		CommentDescription commentDescription = bounds.getDescription();
		if (commentDescription == null) {
			return;
		}

		String description = commentDescription.getHumanReadableString().toLowerCase(Locale.ENGLISH);

		int nullIndex = description.indexOf(valueToFind);
		if (nullIndex < 0 && commentDescription.getFragments().size() > 0) {

			int startingPosition = bounds.getStartingPosition();
			int endingPosition = bounds.getEndingPosition();
			int length = endingPosition - startingPosition;
			CompilationUnit compilationUnit = ModelEnum.COMP_UNIT.getData(resource);
			int column = compilationUnit.getColumnNumber(startingPosition);
			int lineNumber = compilationUnit.getLineNumber(startingPosition) - 1;

			HumanFragmentView view = new HumanFragmentView(0, description.length(), commentDescription);
			String sourceDescription = view.getSourceView(false);
			List<ProblemAction> replacements = new ArrayList<ProblemAction>();
			for (String suggestion : suggestions) {
				String replacement = sourceDescription + " " + suggestion;
				ProblemAction action = actionFactory.buildReplace(resource, startingPosition, startingPosition + length,
						replacement);
				replacements.add(action);
			}

			ValidationResult result = new ValidationResult(validationFailedMessage, resource, replacements, lineNumber,
					column, length, startingPosition, "JavaDoc");

			results.add(result);
		}
	}

	/**
	 * Checks that the class with the given base type does not appear in the
	 * description of the given tag.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The tag to check for the appropriate style. Cannot be null.
	 * @param baseClassName
	 *            The base class name of the that should not appear in the tag's
	 *            description. Cannot be null or empty.
	 * @param validationFailedMessage
	 *            The message to include in the {@link ValidationResult} if the
	 *            text is not found. Cannot be null or empty.
	 */
	private void checkClassInComment(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			String baseClassName, String validationFailedMessage) {

		if (tag.getComment() == null) {
			return;
		}

		String description = tag.getComment().getHumanReadableString();

		int baseClassNameIndex = description.indexOf(baseClassName);
		while (baseClassNameIndex >= 0) {
			checkOneClassInDescription(results, resource, tag, baseClassName, validationFailedMessage, description,
					baseClassNameIndex);

			baseClassNameIndex = description.indexOf(baseClassName, baseClassNameIndex + 1);
		}
	}

	private void checkOneClassInDescription(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			String baseClassName, String validationFailedMessage, String description, int baseClassNameIndex) {
		int indexAfterBaseClassName = baseClassNameIndex + baseClassName.length();
		boolean nothingAfterBaseClassName = indexAfterBaseClassName == description.length()
				|| !Character.isLetterOrDigit(description.charAt(indexAfterBaseClassName));

		if (nothingAfterBaseClassName) {

			// find the first empty space before the class name, and the first
			// char of the identifier
			int firstCharIndex = baseClassNameIndex;
			int firstWholeWordCharIndex = baseClassNameIndex;
			boolean wholeWordStartFound = false;
			for (int i = baseClassNameIndex - 1; i >= 0; i--) {
				char currentChar = description.charAt(i);
				if (!wholeWordStartFound && !Character.isLetterOrDigit(currentChar)) {
					// we found the beginning of the word that contains the
					// class name
					firstWholeWordCharIndex = i + 1;
					wholeWordStartFound = true;
				}

				if (Character.isWhitespace(currentChar)) {
					firstCharIndex = i + 1;
					break;
				}
			}

			if (firstWholeWordCharIndex != baseClassNameIndex) {
				// the class name is only part of a larger word
				return;
			}

			Tag linkTag = null;
			CommentDescription desc = tag.getComment();
			CommentFragment fragment = desc.findFragment(baseClassNameIndex);
			for (int i = desc.getFragments().indexOf(fragment) - 1; i >= 0; i--) {
				CommentFragment currentFragment = desc.getFragments().get(i);
				if (currentFragment.hasTag()) {
					// we have the name fragment
					Tag currentTag = currentFragment.getTag();
					if (currentTag.getSecondName().contains(fragment)) {
						linkTag = currentTag;
						break;
					}
				}
			}

			if (linkTag != null) {
				List<CommentFragment> secondName = linkTag.getSecondName();
				if (secondName != null && secondName.size() >= 1) {
					String secondNameText = secondName.get(0).getText();
					if (secondName.size() > 1 || !secondNameText.equals(baseClassName)) {
						return;
					}
				}
			}

			String classText = description.substring(firstCharIndex, indexAfterBaseClassName);

			int startingPosition = tag.getComment().findInComment(firstCharIndex);
			CompilationUnit compilationUnit = ModelEnum.COMP_UNIT.getData(resource);
			int column = compilationUnit.getColumnNumber(startingPosition);
			int lineNumber = compilationUnit.getLineNumber(startingPosition) - 1;

			ValidationResult result = new ValidationResult(validationFailedMessage, resource, Collections.EMPTY_LIST,
					lineNumber, column, classText.length(), startingPosition, "JavaDoc");

			results.add(result);
		}
	}

	/**
	 * Checks that the <code>@throws</code> tag follows a convention of starting
	 * with "If" followed by the circumstances in which the exception is thrown.
	 * This also supports exceptions that are always thrown or never thrown.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The throws tag to check for the appropriate style. Cannot be
	 *            null.
	 */
	private void checkHasIf(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			ProblemActionFactory actionFactory) {
		CommentDescription tagDescription = tag.getComment();
		if (tagDescription == null) {
			return;
		}

		String commentText = tagDescription.getHumanReadableString();
		int i;
		for (i = 0; i < commentText.length(); i++) {
			if (!Character.isWhitespace(commentText.charAt(i))) {
				break;
			}
		}

		boolean found = false;
		for (String validBeginning : THROWS_BEGINNINGS) {
			int end = i + validBeginning.length();
			if (end <= commentText.length() && commentText.substring(i, end).equals(validBeginning)) {
				found = true;
				break;
			}
		}

		if (!found) {
			if (tagDescription.getFragments().size() == 0) {
				return;
			}

			int length = commentText.length();
			for (int j = 1; j < commentText.length(); j++) {
				if (Character.isWhitespace(commentText.charAt(j))) {
					length = j;
					break;
				}
			}

			CommentFragment firstFragment = tagDescription.findFirstHumanFragment();

			String firstLetters = commentText.substring(0, length);
			List<ProblemAction> suggestions = new ArrayList<ProblemAction>();
			for (String base : THROWS_BEGINNINGS) {
				String replacement = base + " " + firstLetters;
				ProblemAction action = actionFactory.buildReplace(resource, firstFragment.getStartPosition(),
						firstFragment.getStartPosition() + firstLetters.length(), replacement);
				suggestions.add(action);
			}

			ValidationResult result = new ValidationResult(
					"Start the @throws description with 'If' followed by the condition, "
							+ "or add a standard phrase if the exception is always or never thrown.",
					resource, suggestions, firstFragment.getSourceLine(), firstFragment.getSourceColumn(),
					firstLetters.length(), firstFragment.getStartPosition(), "JavaDoc");

			results.add(result);
		}

	}

	/**
	 * Checks that the <code>@throws</code> tag is not an
	 * <code>@exception</code> tag.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param tag
	 *            The throws tag to check for the tag name. Cannot be null.
	 */
	private void checkNotThrows(List<ValidationResult> results, ValidatorResource resource, Tag tag,
			ProblemActionFactory actionFactory) {

		// first find the problem
		String tagNameString = tag.getName().getText();
		if (tagNameString.equals("@exception")) {
			// it's wrong
			String message = "Use 'throws' instead of 'exception'.";

			int length = "@exception".length();
			int startingPosition = tag.getName().getStartPosition();
			int column = tag.getName().getSourceColumn();
			int lineNumber = tag.getName().getSourceLine();

			String suggestion = "@throws";
			ProblemAction action = actionFactory.buildReplace(resource, startingPosition, startingPosition + length,
					suggestion);
			List<ProblemAction> replacements = Arrays.asList(action);

			ValidationResult result = new ValidationResult(message, resource, replacements, lineNumber, column, length,
					startingPosition, "JavaDoc");
			results.add(result);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Level getLevel() {
		return level;
	}

	/**
	 * Visibility represents the different visibility modifiers in a Java file.
	 */
	private enum Visibility {

		/**
		 * This value represents a declaration which does not exist. This is
		 * used in {@link JavaDocValidator#AttributeTrio}, but is not fully
		 * supported.
		 * <p>
		 * Remove if possible.
		 */
		UNDECLARED,

		/**
		 * The modifier for private methods, fields and classes.
		 */
		PRIVATE,

		/**
		 * The modifier for methods, fields and classes that have no visibility
		 * modifier.
		 */
		PACKAGE_PRIVATE,

		/**
		 * The modifier for protected methods, fields and classes.
		 */
		PROTECTED,

		/**
		 * The modifier for public methods, fields and classes.
		 */
		PUBLIC;
	}

	/**
	 * Converts the given modifier bit mask into a Visibility value.
	 *
	 * @param modifiers
	 *            The modifier bit mask as returned by
	 *            {@link BodyDeclaration#getModifiers()}.
	 *
	 * @return A value that represents the given value. Will not be null, and
	 *         will not be {@link Visibility#UNDECLARED}.
	 */
	private Visibility calcVisibility(int modifiers) {
		Visibility visibility;
		if ((modifiers & Modifier.PUBLIC) != 0) {
			visibility = Visibility.PUBLIC;
		} else if ((modifiers & Modifier.PROTECTED) != 0) {
			visibility = Visibility.PROTECTED;
		} else if ((modifiers & Modifier.PRIVATE) != 0) {
			visibility = Visibility.PRIVATE;
		} else {
			visibility = Visibility.PACKAGE_PRIVATE;
		}

		return visibility;
	}

	/**
	 * Checks that the given description for an attribute references the most
	 * visible aspect of that attribute. Each attribute can have at most three
	 * aspects:
	 * <ol>
	 * <li>The field declaration
	 * <li>The set method
	 * <li>The get method
	 * </ol>
	 * The one with the highest visibility should have a thorough explanation of
	 * how the attribute affects, or is affected by, the class. This method
	 * checks that the description of a less visible aspect has a "@link" tag
	 * which points to the most visible aspect.
	 *
	 * @param results
	 *            The list to add to if any errors are found. Cannot be null.
	 * @param resource
	 *            The resource to check. Cannot be null.
	 * @param bounds
	 *            The description to check. Cannot be null.
	 * @param mostVisibleAspect
	 *            The most visible attribute aspect. Cannot be null.
	 * @param validationFailedMessage
	 *            The message to display to the user if the check fails. Cannot
	 *            be null or empty.
	 */
	private void checkVisibleIsReferenced(List<ValidationResult> results, ValidatorResource resource,
			DescriptionBounds bounds, AttributeAspect mostVisibleAspect, String validationFailedMessage,
			ProblemActionFactory actionFactory) {
		CommentDescription description = bounds.getDescription();
		boolean tagFound = false;
		String linkReference = "#" + mostVisibleAspect.getName();
		for (CommentFragment fragment : description.getFragments()) {
			if (fragment.hasTag()) {
				Tag tag = fragment.getTag();
				if (tag.getName().getText().equals("@link")) {
					CommentFragment secondName = tag.getSecondName().get(0);
					if (secondName != null && secondName.getText().contains(linkReference)) {
						tagFound = true;
						break;
					}
				}
			}
		}

		if (!tagFound) {
			int startingPosition = bounds.getStartingPosition();
			int endingPosition = bounds.getEndingPosition();
			int length = endingPosition - startingPosition;
			CompilationUnit compilationUnit = ModelEnum.COMP_UNIT.getData(resource);
			int column = compilationUnit.getColumnNumber(startingPosition);
			int lineNumber = compilationUnit.getLineNumber(startingPosition) - 1;

			String file = ModelEnum.STRING.getData(resource);
			String text = file.substring(startingPosition, endingPosition);

			String replacementText = text + "  See {@link #" + mostVisibleAspect.getName() + "} for details.";

			ProblemAction action = actionFactory.buildReplace(resource, startingPosition, startingPosition + length,
					replacementText);
			List<ProblemAction> replacements = Arrays.asList(action);

			ValidationResult result = new ValidationResult(validationFailedMessage, resource, replacements, lineNumber,
					column, length, startingPosition, "JavaDoc");

			results.add(result);
		}
	}

	/**
	 * DescriptionBounds represents a CommentDescription and its start and end
	 * position in the Java file.
	 */
	private abstract class DescriptionBounds {

		/**
		 * Returns the description represented by this.
		 *
		 * @return The description represented by this. A null value will be
		 *         returned if the description does not exist. If not null, will
		 *         have at least one {@link CommentFragment}.
		 */
		public abstract CommentDescription getDescription();

		/**
		 * Returns the start position of the description in the file.
		 *
		 * @return The 0-based index into the file of the first character of the
		 *         description, or -1 if the description does not exist.
		 */
		public int getStartingPosition() {
			int startPos = -1;
			if (getDescription() != null) {
				startPos = getDescription().getFragments().get(0).getStartPosition();
			}

			return startPos;
		}

		/**
		 * Returns the end position of the description in the file.
		 *
		 * @return The 0-based index into the file of the character after the
		 *         last character in the description, or -1 if the description
		 *         does not exist.
		 */
		public abstract int getEndingPosition();
	}

	/**
	 * TagDescriptionBounds represents the description after a {@link Tag}.
	 */
	private class TagDescriptionBounds extends DescriptionBounds {

		/**
		 * The tag that contains the description. Will be null if the required
		 * tag was not declared in the JavaDoc, in which case the starting and
		 * ending positions will be -1.
		 */
		private Tag tag;

		/**
		 * The comment that contains the tag. Will never be null.
		 */
		private Comment comment;

		/**
		 * Constructor for TagDescriptionBounds.
		 *
		 * @param newTag
		 *            The tag to check for the appropriate style. Null indicates
		 *            that the description does not exist.
		 * @param newComment
		 *            The comment that contains the tag, which is used to locate
		 *            the end of the tag. Cannot be null.
		 */
		private TagDescriptionBounds(Tag newTag, Comment newComment) {
			this.tag = newTag;
			this.comment = newComment;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public CommentDescription getDescription() {
			CommentDescription description = null;
			if (tag != null) {
				description = tag.getComment();
			}

			return description;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getEndingPosition() {
			int endPos = -1;
			if (getDescription() != null) {
				endPos = comment.getSpaceAfterTag(tag).getStartPosition();
			}

			return endPos;
		}
	}

	/**
	 * CommentDescription represents the main description within a Java doc
	 * comment.
	 */
	private class CommentDescriptionBounds extends DescriptionBounds {

		/**
		 * The comment that contains the description. Will never be null.
		 */
		private Comment comment;

		/**
		 * Constructor for CommentDescriptionBounds.
		 *
		 * @param newComment
		 *            The comment that contains the description. Cannot be null.
		 */
		private CommentDescriptionBounds(Comment newComment) {
			this.comment = newComment;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public CommentDescription getDescription() {
			CommentDescription description = null;
			if (comment != null) {
				description = comment.getDescription();
			}

			return description;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getEndingPosition() {
			int endPos = -1;
			if (getDescription() != null) {
				endPos = comment.getSpaceAfterDescription().getStartPosition();
			}
			return endPos;
		}
	}

	/**
	 * JavaDocStructure represents all the Java doc comments within one Java
	 * file and can check for errors in those comments. This structure
	 * recognizes {@link AttributeAspect attribute aspects} and treats them
	 * specially when validating.
	 */
	private class JavaDocStructure {

		/**
		 * A mapping from the base name of an attribute to the AttributeTrio
		 * which contains all the declared aspects for that attribute. Will
		 * never be null.
		 */
		private Map<String, AttributeTrio> attributeScopeIDToTrioMap = new HashMap<String, AttributeTrio>();

		/**
		 * All the Java doc comments which are not attributes. Will never be
		 * null.
		 */
		private List<JavaDocableUnit> others = new ArrayList<JavaDocableUnit>();

		/**
		 * Constructor for JavaDocStructure.
		 *
		 * @param resource
		 *            The resource to extract the structure from. Cannot be
		 *            null.
		 */
		private JavaDocStructure(ValidatorResource resource) {
			CompilationUnit compilationUnit = ModelEnum.COMP_UNIT.getData(resource);

			List javaDocs = resource.getTypedNodeList(Javadoc.JAVADOC);
			for (Iterator it = javaDocs.iterator(); it.hasNext();) {
				Javadoc decl = (Javadoc) it.next();
				ASTNode parent = decl.getParent();
				Comment comment = new Comment(resource, decl, compilationUnit);
				AttributeAspect aspect = getAttributeAspect(parent, decl, resource, comment, compilationUnit);
				if (aspect != null) {
					String scopeID = aspect.getScopeID();
					AttributeTrio trio = attributeScopeIDToTrioMap.get(scopeID);
					if (trio == null) {
						trio = new AttributeTrio();
						attributeScopeIDToTrioMap.put(scopeID, trio);
					}

					trio.add(aspect);
				} else {
					JavaDocableUnit unit = new JavaDocableUnit(resource, decl, compilationUnit, parent, comment);
					others.add(unit);
				}
			}

			// for each AttributeTrio, some aspects might not have JavaDoc
			// defined and the above algorithm will miss them. If we don't find
			// these missing methods, the aspects with JavaDoc will be
			// incorrectly labelled as high or low precedence.
			List fields = resource.getTypedNodeList(ASTNode.FIELD_DECLARATION);
			List methods = resource.getTypedNodeList(ASTNode.METHOD_DECLARATION);

			// we only create these aspects the first time we need to
			List<AttributeAspect> methodAspects = null;
			List<Attribute> fieldAspects = null;
			for (String attributeScopeID : attributeScopeIDToTrioMap.keySet()) {
				AttributeTrio trio = attributeScopeIDToTrioMap.get(attributeScopeID);
				if (trio.hasAllAspects()) {
					continue;
				}

				String scopeID = "";
				boolean hasSet = false;
				boolean hasGet = false;
				boolean hasField = false;
				for (AttributeAspect aspect : trio.getAspects()) {
					scopeID = aspect.getScopeID();
					if (aspect instanceof SetMethod) {
						hasSet = true;
					} else if (aspect instanceof GetMethod) {
						hasGet = true;
					} else if (aspect instanceof Attribute) {
						hasField = true;
					}
				}

				if (!hasGet || !hasSet) {
					if (methodAspects == null) {
						methodAspects = new ArrayList<AttributeAspect>();
						for (Object object : methods) {
							MethodDeclaration methodDecl = (MethodDeclaration) object;
							CompilationUnit compUnit = ModelEnum.COMP_UNIT.getData(resource);
							AttributeAspect possibleAspect = getAttributeAspect(methodDecl, null, resource, null,
									compUnit);
							if (possibleAspect != null) {
								methodAspects.add(possibleAspect);
							}
						}
					}

					if (!hasSet) {
						for (AttributeAspect possibleAspect : methodAspects) {
							if (possibleAspect.getScopeID().equals(scopeID)
									&& possibleAspect.getName().startsWith("set")) {
								trio.add(possibleAspect);
								break;
							}
						}
					}

					if (!hasGet) {
						for (AttributeAspect possibleAspect : methodAspects) {
							if (possibleAspect.getAttributeName().equals(scopeID)
									&& possibleAspect.getName().startsWith("get")) {
								trio.add(possibleAspect);
								break;
							}
						}
					}
				}

				if (!hasField) {
					if (fieldAspects == null) {
						fieldAspects = new ArrayList<Attribute>();
						for (Object object : fields) {
							FieldDeclaration fieldDecl = (FieldDeclaration) object;
							CompilationUnit resourceCompUnit = ModelEnum.COMP_UNIT.getData(resource);
							Attribute possibleAspect = (Attribute) getAttributeAspect(fieldDecl, null, resource, null,
									resourceCompUnit);
							if (possibleAspect != null) {
								fieldAspects.add(possibleAspect);
							}
						}
					}

					for (Attribute possibleAspect : fieldAspects) {
						if (possibleAspect.getScopeID().equals(scopeID)) {
							trio.add(possibleAspect);
							break;
						}
					}
				}
			}
		}

		/**
		 * Creates and returns a new attribute aspect for the given Java doc
		 * declaration.
		 *
		 * @param parent
		 *            The parent of the given <code>Javadoc</code> node, which
		 *            corresponds to the field or method declaration. Cannot be
		 *            null.
		 * @param decl
		 *            The Java doc to create the aspect for. Cannot be null.
		 * @param resource
		 *            The file which contains the declaration. Cannot be null.
		 * @param comment
		 *            The refined description of the <code>Javadoc</code>.
		 *            Cannot be null.
		 * @param comp
		 *            The compilation unit that comes from the resource. Cannot
		 *            be null.
		 *
		 * @return A new aspect. Will be null if the parent is not a valid
		 *         field, set method, or get method declaration.
		 */
		private AttributeAspect getAttributeAspect(ASTNode parent, Javadoc decl, ValidatorResource resource,
				Comment comment, CompilationUnit comp) {
			AttributeAspect aspect = null;
			if (parent instanceof MethodDeclaration) {
				MethodDeclaration method = (MethodDeclaration) parent;
				String methodName = method.getName().getFullyQualifiedName();
				boolean isSet = false;
				String attributeName = null;

				int numParams = method.parameters().size();
				Type returnType = method.getReturnType2();
				int numReturns = returnType != null && !isType(resource, returnType, "void", true) ? 1 : 0;

				if (numParams == 1 && numReturns == 0 && methodName.startsWith("set")) {
					isSet = true;
					attributeName = methodName.substring(3);
				} else if (numParams == 0 && numReturns == 1) {
					if (methodName.startsWith("get")) {
						attributeName = methodName.substring(3);
					} else if (methodName.startsWith("is")) {
						attributeName = methodName.substring(2);
					}
				}

				if (attributeName != null) {
					attributeName = Introspector.decapitalize(attributeName);

					try {
						if (isSet) {
							aspect = new SetMethod(method, decl, resource, comment, attributeName, comp);
						} else {
							aspect = new GetMethod(method, decl, resource, comment, attributeName, comp);
						}
					} catch (Exception e) {
						Debug.VALIDATOR.log(e);
					}

				}
			} else if (parent instanceof FieldDeclaration) {
				FieldDeclaration field = (FieldDeclaration) parent;

				// only allow non-final fields to be attributes
				int modifiers = field.getModifiers();
				if ((modifiers & Modifier.FINAL) == 0) {
					List fragments = field.fragments();

					// just take the name from the first variable name. We
					// ignore
					// additional variables declared in the same field.
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);

					String attributeName = fragment.getName().getFullyQualifiedName();
					aspect = new Attribute(field, decl, resource, comment, attributeName, comp);
				}
			}

			return aspect;
		}

		/**
		 * Performs all configured checks on the attributes in the file
		 * represented by this.
		 *
		 * @param results
		 *            The list to add to if any errors are found. Cannot be
		 *            null.
		 */
		public void checkAttributes(List<ValidationResult> results, ProblemActionFactory actionFactory,
				IProgressMonitor monitor) throws OperationCanceledException {
			for (String scopeID : attributeScopeIDToTrioMap.keySet()) {
				EclipseUtil.getDefault().checkCanceled(monitor);

				AttributeTrio trio = attributeScopeIDToTrioMap.get(scopeID);
				trio.check(results, actionFactory);
			}
		}

		/**
		 * Performs all configured checks on the Java doc that is not for
		 * attributes in the file represented by this.
		 *
		 * @param results
		 *            The list to add to if any errors are found. Cannot be
		 *            null.
		 */
		public void checkOthers(List<ValidationResult> results, ProblemActionFactory actionFactory,
				IProgressMonitor monitor) throws OperationCanceledException {
			for (JavaDocableUnit unit : others) {
				EclipseUtil.getDefault().checkCanceled(monitor);

				unit.checkNonAttributeProblems(results, true, actionFactory);
			}
		}

	}

	/**
	 * JavaDocableUnit represents any declaration that has some Java doc, and
	 * provides a method to {@link #checkNonAttributeProblems check} that the
	 * Java doc follows required guidelines.
	 */
	private class JavaDocableUnit {

		/**
		 * The file that contains the Java doc declaration. Will never be null.
		 */
		protected ValidatorResource resource;

		/**
		 * The Java doc to check. Will be null if there is no JavaDoc.
		 */
		protected Javadoc decl;

		/**
		 * The compilation unit used to find position of elements. Will never be
		 * null.
		 */
		protected CompilationUnit comp;

		/**
		 * The method, field or class declaration which is the parent of the
		 * Java doc AST node. Will never be null.
		 */
		private ASTNode parent;

		/**
		 * A refined representation of the Java doc. Will be null if there is no
		 * JavaDoc.
		 */
		protected Comment comment;

		/**
		 * Constructor for JavaDocableUnit.
		 *
		 * @param newResource
		 *            The file which contains the declaration. Cannot be null.
		 * @param newDecl
		 *            The Java doc that this represents. A null value indicates
		 *            that the method has no JavaDoc.
		 * @param newComp
		 *            The compilation unit that comes from the resource. Cannot
		 *            be null.
		 * @param newParent
		 *            The parent of the given <code>Javadoc</code> node, which
		 *            corresponds to the field method, or class declaration.
		 *            Cannot be null.
		 * @param newComment
		 *            The refined description of the <code>Javadoc</code>. A
		 *            null value indicates that there is no JavaDoc.
		 */
		private JavaDocableUnit(ValidatorResource newResource, Javadoc newDecl, CompilationUnit newComp,
				ASTNode newParent, Comment newComment) {
			this.resource = newResource;
			this.decl = newDecl;
			this.comp = newComp;
			this.parent = newParent;
			this.comment = newComment;
		}

		/**
		 * Checks all generic problems for Java doc, ignoring special treatment
		 * for attributes and the relationship between attribute aspects.
		 *
		 * @param results
		 *            The list to add to if any errors are found. Cannot be
		 *            null.
		 * @param checkNullsAndEmpties
		 *            True indicates that the existence of "null" and "empty"
		 *            should be checked for objects, Strings, and arrays
		 *            (provided that they are configured to be checked), false
		 *            indicates that they will not be checked even if
		 *            configured.
		 */
		public void checkNonAttributeProblems(List<ValidationResult> results, boolean checkNullsAndEmpties,
				ProblemActionFactory actionFactory) {

			if (comment == null) {
				return;
			}

			List<Tag> throwsTags = comment.getThrowsTags();
			for (Tag throwsTag : throwsTags) {
				if (level.getValue().ordinal() >= LevelEnum.LOOSE.ordinal()) {
					checkNotThrows(results, resource, throwsTag, actionFactory);
				}

				if (level.getValue().ordinal() >= LevelEnum.NORMAL.ordinal()) {
					checkHasIf(results, resource, throwsTag, actionFactory);
					checkExceptionNameInDescription(results, resource, throwsTag);
				}
			}

			if (level.getValue().ordinal() >= LevelEnum.NORMAL.ordinal()) {
				checkAfterDescriptionSpace(results, resource, comment);
			}

			if (parent instanceof MethodDeclaration) {
				MethodDeclaration methodDecl = (MethodDeclaration) parent;

				Tag returnTag = comment.getReturnTag();
				if (returnTag != null) {
					if (level.getValue().ordinal() >= LevelEnum.NORMAL.ordinal()) {
						checkReturnClassNameInDescription(results, resource, returnTag, methodDecl);
						if (checkNullsAndEmpties) {
							checkNullReferredForObjectReturn(results, resource, returnTag, methodDecl, comment,
									actionFactory);
							checkEmptyReferredForArrayReturn(results, resource, returnTag, methodDecl, comment,
									actionFactory);
							checkEmptyReferredForStringReturn(results, resource, returnTag, methodDecl, comment,
									actionFactory);
						}
					}
				}

				List<Tag> paramTags = comment.getParamTags();
				for (Tag paramTag : paramTags) {
					SingleVariableDeclaration paramDeclaration = findParamForTag(paramTag, methodDecl);

					if (paramDeclaration != null) {
						if (level.getValue().ordinal() >= LevelEnum.NORMAL.ordinal()) {
							checkParamClassNameInDescription(results, resource, paramTag, paramDeclaration);
							if (checkNullsAndEmpties) {
								checkNullReferredForObjectParam(results, resource, paramTag, paramDeclaration, comment,
										actionFactory);
								checkEmptyReferredForArrayParam(results, resource, paramTag, paramDeclaration, comment,
										actionFactory);
								checkEmptyReferredForStringParam(results, resource, paramTag, paramDeclaration, comment,
										actionFactory);
							}
						}
					}
				}
			}
		}

	}

	/**
	 * AttributeTrio represents an attribute, along with its "set" and "get"
	 * methods. If any of these does not exist, it will not be present in this.
	 * At least one of them must exist.
	 */
	private class AttributeTrio {

		/**
		 * All the aspects of this trio. See {@link #getAspects} for details.
		 */
		private List<AttributeAspect> aspects = new ArrayList<AttributeAspect>(3);

		/**
		 * Adds the given aspect to this trio.
		 *
		 * @param aspect
		 *            The aspect to add. Cannot be null.
		 */
		public void add(AttributeAspect aspect) {
			aspects.add(aspect);
		}

		/**
		 * Returns the list of aspects in this. The order of the aspects is not
		 * guaranteed. Contains at least one aspect and at most three aspects.
		 * This array will not be null. It will be empty when the class is
		 * created but is non-empty after {@link #add(AttributeAspect)} is
		 * called.
		 *
		 * @return The list of aspects. Will not be null.
		 */
		public List<AttributeAspect> getAspects() {
			return aspects;
		}

		/**
		 * Returns whether the field, and set and get method declarations, exist
		 * in this.
		 *
		 * @return True if all three declarations are in this, false otherwise.
		 */
		public boolean hasAllAspects() {
			return aspects.size() == 3;
		}

		/**
		 * Checks that the most visible aspect describes what happens to the
		 * class when the attribute is null or empty, and that the less visible
		 * aspects have a "@link" to the most visible aspect. Excluding the
		 * checks for null and empty, all other configured Java doc checks will
		 * also be performed on each aspect.
		 *
		 * @param results
		 *            The list to add to if any errors are found. Cannot be
		 *            null.
		 */
		public void check(List<ValidationResult> results, ProblemActionFactory actionFactory) {
			Collections.sort(aspects, new AttributeAspectComparator());

			if (level.getValue().ordinal() >= LevelEnum.STRICT.ordinal()) {
				// the last one in the list should explain null or empty cases
				AttributeAspect mostVisibleAspect = aspects.get(aspects.size() - 1);
				mostVisibleAspect.checkNull(results, mostVisibleAspect.resource, mostVisibleAspect.comment,
						actionFactory);
				mostVisibleAspect.checkEmpty(results, mostVisibleAspect.resource, mostVisibleAspect.comment,
						actionFactory);

				// the others should have a link to the first one
				for (int i = 0; i < aspects.size() - 1; i++) {
					AttributeAspect lessVisibleAspect = aspects.get(i);
					lessVisibleAspect.checkHasLinkTo(results, mostVisibleAspect, actionFactory);
				}
			}

			// check the other situations
			for (AttributeAspect aspect : aspects) {
				aspect.checkNonAttributeProblems(results, false, actionFactory);
			}
		}
	}

	/**
	 * AttributeAspectComparator is used to order {@link AttributeAspect
	 * AttributeAspects} in a list by their visibility, with the most visible
	 * aspects at the end, and the least visible at the beginning.
	 */
	private class AttributeAspectComparator implements Comparator<AttributeAspect> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(AttributeAspect object1, AttributeAspect object2) {
			int result = object1.getVisibility().ordinal() - object2.getVisibility().ordinal();
			if (result != 0) {
				return result;
			}

			int type1 = getTypeNumber(object1);
			int type2 = getTypeNumber(object2);
			result = type1 - type2;

			return result;
		}

		/**
		 * Returns a numeric representation of the aspect type, which can be
		 * used to compare them. Set methods have a higher ordinal number than
		 * get methods, which in turn have a higher ordinal number than field
		 * declarations.
		 *
		 * @param object1
		 *            The aspect to find an ordinal number for. If null, 0 will
		 *            be returned.
		 *
		 * @return An ordinal number that can be used to compare the type of the
		 *         attribute aspect.
		 */
		private int getTypeNumber(AttributeAspect object1) {
			int typeNumber;
			if (object1 instanceof SetMethod) {
				typeNumber = 2;
			} else if (object1 instanceof GetMethod) {
				typeNumber = 1;
			} else {
				typeNumber = 0;
			}

			return typeNumber;
		}

	}

	/**
	 * AttributeAspect represents one of three possible declarations that are
	 * related to attributes - the field declaration, set method, or get method.
	 * Note that this is a {@link JavaDocableUnit}, so it inherits the checks
	 * available in that class. However, it also supports checks that are
	 * special for the relationships between attribute-related declarations.
	 *
	 * @param <T>
	 *            The type of the <code>ASTNode</code> for this attribute
	 *            aspect.
	 */
	private abstract class AttributeAspect<T extends BodyDeclaration> extends JavaDocableUnit {

		/**
		 * The ASTNode that represents the declaration. See {@link #getNode} for
		 * details.
		 */
		private T node;

		/**
		 * A scope-sensitive string that is used to identify attributes within a
		 * file. See {@link #getScopeID} for details.
		 */
		private String scopeID;

		/**
		 * The visibility of the declaration. See {@link #getVisibility} for
		 * details.
		 */
		protected Visibility visibility;

		/**
		 * The base name of the attribute, common for all three aspects. This
		 * corresponds to the field name, or the part after "set" or "get" in
		 * the method names (with the first letter in lower case). See
		 * {@link #getAttributeName} for details.
		 */
		protected String attributeName;

		/**
		 * Constructor for AttributeAspect.
		 *
		 * @param newNode
		 *            The parent of the given <code>Javadoc</code> ASTNode,
		 *            which corresponds to the field or method declaration.
		 *            Cannot be null.
		 * @param newDecl
		 *            The Java doc that this represents. A null value indicates
		 *            that the method has no JavaDoc.
		 * @param resource
		 *            The file which contains the declaration. Cannot be null.
		 * @param comment
		 *            The refined description of the <code>Javadoc</code>. A
		 *            null value indicates that there is no JavaDoc.
		 * @param newAttributeName
		 *            The base name of the attribute, which corresponds to the
		 *            field name, or the part after "set" or "get" in the method
		 *            names (with the first letter in lower case). Cannot be
		 *            null or empty.
		 * @param comp
		 *            The compilation unit that comes from the resource. Cannot
		 *            be null.
		 */
		protected AttributeAspect(T newNode, Javadoc newDecl, ValidatorResource resource, Comment comment,
				String newAttributeName, CompilationUnit comp) {
			super(resource, newDecl, comp, newNode, comment);

			this.node = newNode;
			ASTNode current = newNode.getParent();
			StringBuffer scopeBuffer = new StringBuffer();
			scopeBuffer.append(newAttributeName);
			while (current != null && !(current instanceof CompilationUnit)) {
				String nodeName;
				if (current instanceof TypeDeclaration) {
					TypeDeclaration typeDecl = (TypeDeclaration) current;
					nodeName = typeDecl.getName().getFullyQualifiedName();
				} else {
					nodeName = new Integer(node.getStartPosition()).toString();
				}
				scopeBuffer.insert(0, '.').insert(0, nodeName);
				current = current.getParent();
			}
			this.scopeID = scopeBuffer.toString();

			this.attributeName = newAttributeName;

			if (newNode == null) {
				visibility = Visibility.UNDECLARED;
			} else {
				int modifiers = getNode().getModifiers();
				visibility = calcVisibility(modifiers);
			}
		}

		/**
		 * Returns a scope-sensitive string that is used to identify attributes
		 * within a file.
		 *
		 * @return The hierarchy of type declarations within the file, separated
		 *         by ".", and ending with the {@link #getAttributeName}. Will
		 *         not be null or empty.
		 */
		public String getScopeID() {
			return scopeID;
		}

		/**
		 * Returns the declared name of this aspect, which might not be the same
		 * as the {@link #getAttributeName() attribute name}.
		 *
		 * @return The name of this aspect. Will not be null or empty.
		 */
		public abstract String getName();

		/**
		 * Returns the base name of the attribute, common to all aspects.
		 *
		 * @return The base name of this attribute. Will not be null or empty.
		 */
		public String getAttributeName() {
			return attributeName;
		}

		/**
		 * Returns the ASTNode that represents the declaration of this aspect.
		 *
		 * @return The declaration node for this aspect. Will not be null.
		 */
		protected T getNode() {
			return node;
		}

		/**
		 * Returns the visibility of this aspect.
		 *
		 * @return The visibility of this aspect. Will not be null.
		 */
		public Visibility getVisibility() {
			return visibility;
		}

		/**
		 * Checks that this aspect has a link to the given other aspect. The
		 * details of where the link should appear depends on the aspect.
		 *
		 * @param results
		 *            The list to add to if any errors are found. Cannot be
		 *            null.
		 * @param mostVisibleAspect
		 *            The attribute aspect that is more visible than this.
		 *            Cannot be null.
		 */
		public abstract void checkHasLinkTo(List<ValidationResult> results, AttributeAspect mostVisibleAspect,
				ProblemActionFactory actionFactory);

		/**
		 * Checks that the conditions for a null object are described in this
		 * aspect. The precise location in which "null" should appear depends on
		 * the aspect. This method should only be called on the most visible
		 * attribute aspect.
		 *
		 * @param results
		 *            The list to add to if any errors are found. Cannot be
		 *            null.
		 * @param resource
		 *            The file that the declaration appears in. Cannot be null.
		 * @param comment
		 *            The refined form of Java doc to check for a reference to
		 *            null.
		 */
		public abstract void checkNull(List<ValidationResult> results, ValidatorResource resource, Comment comment,
				ProblemActionFactory actionFactory);

		/**
		 * Checks that the conditions for an empty String or array are described
		 * in this aspect. The precise location in which "empty" should appear
		 * depends on the aspect. This method should only be called on the most
		 * visible attribute aspect.
		 *
		 * @param results
		 *            The list to add to if any errors are found. Cannot be
		 *            null.
		 * @param resource
		 *            The file that the declaration appears in. Cannot be null.
		 * @param comment
		 *            The refined form of Java doc to check for a reference to
		 *            null.
		 */
		public abstract void checkEmpty(List<ValidationResult> results, ValidatorResource resource, Comment comment,
				ProblemActionFactory actionFactory);
	}

	/**
	 * Attribute represents the field declaration for an attribute, as opposed
	 * to the attributes set or get method.
	 */
	private class Attribute extends AttributeAspect<FieldDeclaration> {

		/**
		 * The description from the Java doc comment for the field. Will be null
		 * if the field declaration does not have JavaDoc.
		 */
		private DescriptionBounds bounds;

		/**
		 * Constructor for the Attribute class.
		 *
		 * @param newNode
		 *            The parent of the given <code>Javadoc</code> ASTNode,
		 *            which corresponds to the field declaration. Cannot be
		 *            null.
		 * @param newDecl
		 *            The Java doc that this represents. A null value indicates
		 *            that the method has no JavaDoc.
		 * @param resource
		 *            The file which contains the declaration. Cannot be null.
		 * @param comment
		 *            The refined description of the <code>Javadoc</code>. A
		 *            null value indicates that there is no JavaDoc.
		 * @param attributeName
		 *            The base name of the attribute, which corresponds to the
		 *            field name in this case. Cannot be null or empty.
		 * @param comp
		 *            The compilation unit that comes from the resource. Cannot
		 *            be null.
		 */
		private Attribute(FieldDeclaration newNode, Javadoc newDecl, ValidatorResource resource, Comment comment,
				String attributeName, CompilationUnit comp) {
			super(newNode, newDecl, resource, comment, attributeName, comp);
			bounds = new CommentDescriptionBounds(comment);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void checkEmpty(List<ValidationResult> results, ValidatorResource resource, Comment comment,
				ProblemActionFactory actionFactory) {

			Type type = getNode().getType();
			if (bounds != null && bounds.getDescription() != null && isArray(type) || isString(resource, type)) {
				List<String> suggestions = Arrays.asList("This value will never be empty.",
						"This value will never be null or empty.",
						"This value will be empty when ..., and indicates that ...");
				checkStringExistsInComment(results, resource,
						bounds, "As the highest visibility form of the attribute, "
								+ "this field description must describe " + "how an empty value affects the class.",
						"empty", suggestions, actionFactory);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void checkNull(List<ValidationResult> results, ValidatorResource resource, Comment comment,
				ProblemActionFactory actionFactory) {

			Type type = getNode().getType();
			if (bounds != null && bounds.getDescription() != null && isObject(type)) {
				List<String> suggestions = Arrays.asList("This value will never be null.",
						"This value will be null when ..., and indicates that ...");
				checkStringExistsInComment(results, resource,
						bounds, "As the highest visibility form of the attribute, "
								+ "this field description must describe " + "how a null value affects the class.",
						"null", suggestions, actionFactory);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void checkHasLinkTo(List<ValidationResult> results, AttributeAspect mostVisibleAspect,
				ProblemActionFactory actionFactory) {
			if (bounds != null && bounds.getDescription() != null) {
				checkVisibleIsReferenced(results, resource, bounds, mostVisibleAspect,
						"As a lower visibility form of the attribute, "
								+ "this field description must link to the higher visibility form.",
						actionFactory);
			}
		}

		/**
		 * Returns the field name, which also corresponds to the attribute name.
		 *
		 * @return The field name. Will not be null or empty.
		 */
		@Override
		public String getName() {
			return attributeName;
		}
	}

	/**
	 * SetMethod represents the set method declaration for an attribute, as
	 * opposed to the field declaration or get method declaration.
	 */
	private class SetMethod extends AttributeAspect<MethodDeclaration> {

		/**
		 * The description from the Java doc comment for the method. Will be
		 * null if the method declaration does not have JavaDoc.
		 */
		private DescriptionBounds bounds;

		/**
		 * The declaration for the only parameter. Will be null if there is no
		 * JavaDoc or the <code>@param</code> does not exist in the JavaDoc,
		 * which will cause all checks to be bypassed.
		 */
		private SingleVariableDeclaration paramDeclaration;

		/**
		 * Constructor for SetMethod.
		 *
		 * @param newNode
		 *            The parent of the given <code>Javadoc</code> ASTNode,
		 *            which corresponds to the set method declaration. Cannot be
		 *            null.
		 * @param newDecl
		 *            The Java doc that this represents. A null value indicates
		 *            that the method has no JavaDoc.
		 * @param resource
		 *            The file which contains the declaration. Cannot be null.
		 * @param comment
		 *            The refined description of the <code>Javadoc</code>. A
		 *            null value indicates that there is no JavaDoc.
		 * @param attributeName
		 *            The base name of the attribute. Cannot be null or empty.
		 * @param comp
		 *            The compilation unit that comes from the resource. Cannot
		 *            be null.
		 */
		private SetMethod(MethodDeclaration newNode, Javadoc newDecl, ValidatorResource resource, Comment comment,
				String attributeName, CompilationUnit comp) {
			super(newNode, newDecl, resource, comment, attributeName, comp);

			if (comment != null) {
				List<Tag> params = comment.getParamTags();
				Tag tag = null;
				if (!params.isEmpty()) {
					tag = params.get(0);
					paramDeclaration = findParamForTag(tag, getNode());
				} else {
					paramDeclaration = null;
				}

				bounds = new TagDescriptionBounds(tag, comment);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void checkEmpty(List<ValidationResult> results, ValidatorResource resource, Comment comment,
				ProblemActionFactory actionFactory) {
			Type type = null;
			if (paramDeclaration != null) {
				type = paramDeclaration.getType();
			}

			if (bounds != null && bounds.getDescription() != null && isArray(type) || isString(resource, type)) {
				List<String> suggestions = Arrays.asList("Cannot be empty.", "Cannot be null or empty.",
						"If this value is empty, ...", "An empty attribute indicates that ...");
				checkStringExistsInComment(results, resource,
						bounds, "As the highest visibility form of the attribute, "
								+ "this @param description must describe " + "how an empty value affects the class.",
						"empty", suggestions, actionFactory);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void checkNull(List<ValidationResult> results, ValidatorResource resource, Comment comment,
				ProblemActionFactory actionFactory) {
			Type type = null;
			if (paramDeclaration != null) {
				type = paramDeclaration.getType();
			}

			if (bounds != null && bounds.getDescription() != null && isObject(type)) {
				List<String> suggestions = Arrays.asList("Cannot be null.", "If this string is null, ...",
						"A null value indicates that ...");
				checkStringExistsInComment(results, resource,
						bounds, "As the highest visibility form of the attribute, "
								+ "this @param description must describe " + "how a null value affects the class.",
						"null", suggestions, actionFactory);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void checkHasLinkTo(List<ValidationResult> results, AttributeAspect mostVisibleAspect,
				ProblemActionFactory actionFactory) {
			if (bounds != null && bounds.getDescription() != null) {
				checkVisibleIsReferenced(results, resource, bounds, mostVisibleAspect,
						"As a lower visibility form of the attribute, "
								+ "this @param description must link to the higher visibility form.",
						actionFactory);
			}
		}

		/**
		 * Returns the set method name.
		 *
		 * @return The set method name. Will not be null or empty.
		 */
		@Override
		public String getName() {
			return getNode().getName().getFullyQualifiedName();
		}
	}

	/**
	 * GetMethod represents the get method declaration for an attribute, as
	 * opposed to the field declaration or set method declaration.
	 */
	private class GetMethod extends AttributeAspect<MethodDeclaration> {

		/**
		 * The description from the Java doc comment for the method. Will be
		 * null if the method declaration does not have JavaDoc.
		 */
		private DescriptionBounds bounds;

		/**
		 * Constructor for GetMethod.
		 *
		 * @param newNode
		 *            The parent of the given <code>Javadoc</code> ASTNode,
		 *            which corresponds to the get method declaration. Cannot be
		 *            null.
		 * @param newDecl
		 *            The Java doc that this represents. A null value indicates
		 *            that the method has no JavaDoc.
		 * @param resource
		 *            The file which contains the declaration. Cannot be null.
		 * @param comment
		 *            The refined description of the <code>Javadoc</code>. A
		 *            null value indicates that there is no JavaDoc.
		 * @param attributeName
		 *            The base name of the attribute. Cannot be null or empty.
		 * @param comp
		 *            The compilation unit that comes from the resource. Cannot
		 *            be null.
		 */
		private GetMethod(MethodDeclaration newNode, Javadoc newDecl, ValidatorResource resource, Comment comment,
				String attributeName, CompilationUnit comp) {
			super(newNode, newDecl, resource, comment, attributeName, comp);
			if (comment != null) {
				Tag tag = comment.getReturnTag();
				bounds = new TagDescriptionBounds(tag, comment);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void checkEmpty(List<ValidationResult> results, ValidatorResource resource, Comment comment,
				ProblemActionFactory actionFactory) {
			Type type = getNode().getReturnType2();
			if (bounds != null && bounds.getDescription() != null && isArray(type) || isString(resource, type)) {
				List<String> suggestions = Arrays.asList("This value will not be empty.",
						"This value will not be null or empty.", "This value will be empty if ...");
				checkStringExistsInComment(results, resource,
						bounds, "As the highest visibility form of the attribute, "
								+ "this @return description must describe " + "how an empty value affects the class.",
						"empty", suggestions, actionFactory);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void checkNull(List<ValidationResult> results, ValidatorResource resource, Comment comment,
				ProblemActionFactory actionFactory) {
			Type type = getNode().getReturnType2();
			if (bounds != null && bounds.getDescription() != null && isObject(type)) {
				List<String> suggestions = Arrays.asList("This value will not be null.",
						"This value will be null if ...");
				checkStringExistsInComment(results, resource,
						bounds, "As the highest visibility form of the attribute, "
								+ "this @return description must describe " + "how a null value affects the class.",
						"null", suggestions, actionFactory);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void checkHasLinkTo(List<ValidationResult> results, AttributeAspect mostVisibleAspect,
				ProblemActionFactory actionFactory) {
			if (bounds != null && bounds.getDescription() != null) {
				checkVisibleIsReferenced(results, resource, bounds, mostVisibleAspect,
						"As a lower visibility form of the attribute, "
								+ "this @return description must link to the higher visibility form.",
						actionFactory);
			}
		}

		/**
		 * Returns the get method name.
		 *
		 * @return The get method name. Will not be null or empty.
		 */
		@Override
		public String getName() {
			return getNode().getName().getFullyQualifiedName();
		}
	}

}
