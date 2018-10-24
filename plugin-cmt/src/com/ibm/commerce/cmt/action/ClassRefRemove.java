package com.ibm.commerce.cmt.action;

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
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.FileContents;
import com.ibm.commerce.cmt.Param;
import com.ibm.commerce.cmt.plan.Issue;
import com.ibm.commerce.cmt.plan.LogStep;
import com.ibm.commerce.cmt.plan.Range;
import com.ibm.commerce.cmt.plan.ReplaceInFileStep;
import com.ibm.commerce.cmt.plan.Step;
import com.ibm.commerce.cmt.search.SearchResult;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemIndex;
import com.ibm.commerce.dependency.model.Workspace;

/**
 * This class supports removing class references in Java source code.
 * 
 * @author Trent Hoeppner
 */
public class ClassRefRemove implements Action {

	public ClassRefRemove() {
		// do nothing
	}

	@Override
	public String getName() {
		return "classRefRemove";
	}

	@Override
	public String getData() {
		return null;
	}

	@Override
	public List<? extends Param> getSubParams() {
		return null;
	}

	@Override
	public Node toXML(Document doc) {
		Element removeParam = doc.createElement("remove");
		return removeParam;
	}

	@Override
	public List<Step> getSteps(Context context) {
		SearchResult<ASTNode> current = context.get(Context.Prop.SEARCH_RESULT);
		Check.notNull(current, "context SEARCH_RESULT");

		JavaItemIndex index = context.get(Context.Prop.JAVA_ITEM_INDEX);

		ASTNode data = current.getDataObject();
		ChangeVisitor visitor = new ChangeVisitor(context, index);
		data.accept(visitor);
		String replacementText = visitor.outputReplacementText;

		List<Step> steps = new ArrayList<>();
		if (replacementText != null) {
			steps.add(new ReplaceInFileStep(replacementText));
		} else {
			steps.add(new LogStep("Warning: Could not safely remove this class reference."));
		}

		return steps;
	}

	private final class ChangeVisitor extends ASTVisitor {
		public String outputReplacementText;

		private Context context;

		private JavaItemIndex index;

		private Range range;

		public ChangeVisitor(Context context, JavaItemIndex index) {
			this.context = context;
			this.index = index;

			Issue issue = context.get(Context.Prop.ISSUE);
			range = issue.getLocation().getRange();
		}

		@Override
		public boolean visit(ImportDeclaration node) {
			int importStart = node.getStartPosition();
			int importEnd = importStart + node.getLength();

			range.setStart(importStart);
			range.setEnd(importEnd);

			outputReplacementText = "";

			return super.visit(node);
		}

		@Override
		public boolean visit(SimpleType node) {
			ASTNode parent = node.getParent();
			if (parent.getNodeType() == ASTNode.METHOD_DECLARATION) {
				// check if it's in the thrown exceptions list
				int thrownIndex = -1;
				MethodDeclaration methodDeclaration = (MethodDeclaration) parent;
				@SuppressWarnings("rawtypes")
				List exceptions = methodDeclaration.thrownExceptionTypes();
				for (int i = 0; i < exceptions.size(); i++) {
					Object o = exceptions.get(i);
					if (o == node) {
						// yep, it's thrown
						thrownIndex = i;
						break;
					}
				}

				if (thrownIndex >= 0) {
					if (exceptions.size() > 1) {
						// there is more than one, just remove this one
						int removeStart = node.getStartPosition();
						int removeEnd = removeStart + node.getLength();
						if (thrownIndex < exceptions.size() - 1) {
							// there is one after this, remove everything up to
							// the beginning of the next one
							Type next = (Type) exceptions.get(thrownIndex + 1);
							removeEnd = next.getStartPosition();
						}

						range.setStart(removeStart);
						range.setEnd(removeEnd);
					} else {
						// there is only this exception, remove the whole throws
						// clause
						FileContents fileContents = context.get(Context.Prop.FILE_CONTENTS);
						int throwsIndex = fileContents.getContents().lastIndexOf("throws", node.getStartPosition());
						if (throwsIndex < 0) {
							throw new IllegalStateException(
									"Missing throws keyword for exception before " + node.getStartPosition());
						}

						int removeEnd = node.getStartPosition() + node.getLength();

						range.setStart(throwsIndex);
						range.setEnd(removeEnd);
					}
				}

				outputReplacementText = "";
			} else if (parent.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION
					&& parent.getParent().getNodeType() == ASTNode.CATCH_CLAUSE) {
				// we remove the whole catch block
				CatchClause catchClause = (CatchClause) parent.getParent();
				TryStatement tryStatement = (TryStatement) catchClause.getParent();
				Block tryBlock = tryStatement.getBody();

				CompilationUnit compUnit = context.get(Context.Prop.COMP_UNIT);
				String packageName = compUnit.getPackage().getName().getFullyQualifiedName();
				String className = ((TypeDeclaration) compUnit.types().get(0)).getName().getFullyQualifiedName();
				JavaItem javaClass = index.findClass(packageName, className);

				Workspace workspace = context.get(Context.Prop.DEPENDENCY_WORKSPACE);
				List<JavaItem> tryMethodCalls = workspace.findNodeDependencies(javaClass, tryBlock);

				// if any of the methods called in the try block throw the
				// exception (or a subclass of the exception), we cannot delete
				// the catch block
				String catchClassName = node.getName().getFullyQualifiedName();
				String partialName = catchClassName;
				int lastDot = catchClassName.lastIndexOf('.');
				if (lastDot >= 0) {
					partialName = catchClassName.substring(lastDot + 1);
				}
				JavaItem catchClass = null;
				for (JavaItem importedClass : javaClass.getDependencies()) {
					if (importedClass.getName().equals(partialName)) {
						catchClass = importedClass;
						break;
					}
				}

				// TODO handle Exception - it can catch RuntimeExceptions which
				// we cannot detect
				boolean canRemoveCatch = true;
				for (JavaItem method : tryMethodCalls) {
					List<Integer> throwsClassIDs = method.getAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES);
					for (Integer throwsClassID : throwsClassIDs) {
						// check this throws class or super class equals the
						// catch exception class
						JavaItem throwsClass = index.getItem(throwsClassID);
						JavaItem current = throwsClass;
						while (current != null && current != catchClass) {
							Integer currentID = method.getAttribute(JavaItem.ATTR_SUPERCLASS);
							current = index.getItem(currentID);
						}

						if (current != null) {
							// there was a match - this method throws an
							// exception that is caught by this catch clause
							canRemoveCatch = false;
						}
					}
				}

				if (canRemoveCatch) {
					// should we remove only this catch or the try as well?
					// if there is no other resources, catches, or finally, we
					// can remove the try as well
					boolean canRemoveTry = tryStatement.resources().isEmpty() && tryStatement.getFinally() == null
							&& tryStatement.catchClauses().size() == 1;

					int removeStart;
					int removeEnd;
					if (canRemoveTry) {
						// change the range to before the try
						removeStart = tryStatement.getStartPosition();
						removeEnd = removeStart + tryStatement.getLength();

						// find the text to replace
						List<?> statements = tryBlock.statements();
						int replaceStart;
						int replaceEnd;
						if (statements.isEmpty()) {
							// try block with no statements, use the block
							// itself
							replaceStart = tryBlock.getStartPosition();
							replaceEnd = replaceStart + tryBlock.getLength();
						} else {
							Statement first = (Statement) statements.get(0);
							replaceStart = first.getStartPosition();

							Statement last = (Statement) statements.get(statements.size() - 1);
							replaceEnd = last.getStartPosition() + last.getLength();
						}

						FileContents fileContents = context.get(Context.Prop.FILE_CONTENTS);
						outputReplacementText = fileContents.getContents().substring(replaceStart, replaceEnd);
					} else {
						// just remove the catch clause
						removeStart = catchClause.getStartPosition();
						removeEnd = removeStart + catchClause.getLength();

						outputReplacementText = "";
					}

					range.setStart(removeStart);
					range.setEnd(removeEnd);
				}
				// else we cannot remove the catch, no output text
			}

			return super.visit(node);
		}

	}

}
