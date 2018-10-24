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
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;
import com.ibm.commerce.cmt.plan.Issue;
import com.ibm.commerce.cmt.plan.LogStep;
import com.ibm.commerce.cmt.plan.Range;
import com.ibm.commerce.cmt.plan.ReplaceInFileStep;
import com.ibm.commerce.cmt.plan.Step;
import com.ibm.commerce.cmt.search.SearchResult;

/**
 * This class supports removing class references in Java source code.
 * 
 * @author Trent Hoeppner
 */
public class MethodRefRemove implements Action {

	public MethodRefRemove() {
		// do nothing
	}

	@Override
	public String getName() {
		return "methodRefRemove";
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

		ASTNode data = current.getDataObject();
		ChangeVisitor visitor = new ChangeVisitor(context);
		data.accept(visitor);
		String replacementText = visitor.outputReplacementText;

		List<Step> steps = new ArrayList<>();
		if (replacementText != null) {
			steps.add(new ReplaceInFileStep(replacementText));
		} else {
			steps.add(new LogStep("Warning: Could not safely remove this method reference."));
		}

		return steps;
	}

	private final class ChangeVisitor extends ASTVisitor {
		public String outputReplacementText;

		private Range range;

		public ChangeVisitor(Context context) {
			Issue issue = context.get(Context.Prop.ISSUE);
			range = issue.getLocation().getRange();
		}

		@Override
		public boolean visit(SimpleName node) {
			ASTNode methodInvocation = findInvocation(node);
			ASTNode parent = methodInvocation.getParent();
			if (parent.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
				// we remove the whole statement
				int removeStart = parent.getStartPosition();
				int removeEnd = removeStart + parent.getLength();

				range.setStart(removeStart);
				range.setEnd(removeEnd);

				outputReplacementText = "";
			}

			return super.visit(node);
		}

		public ASTNode findInvocation(SimpleName node) {
			ASTNode parent = node.getParent();
			while (!(parent instanceof MethodInvocation) && !(parent instanceof ClassInstanceCreation)) {
				parent = parent.getParent();
			}

			return parent;
		}

	}

}
