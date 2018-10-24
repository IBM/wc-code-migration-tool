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
import org.eclipse.jdt.core.dom.SimpleName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;
import com.ibm.commerce.cmt.plan.ReplaceInFileStep;
import com.ibm.commerce.cmt.plan.Step;
import com.ibm.commerce.cmt.search.SearchResult;

/**
 * This class supports replacing class references in Java source code with
 * another reference.
 * 
 * @author Trent Hoeppner
 */
public class MethodRefReplace implements Action {

	private String textContents;

	private List<Param> replaceParams;

	public MethodRefReplace(String textContents, List<Param> replaceParams) {
		this.textContents = textContents;
		this.replaceParams = replaceParams;
	}

	@Override
	public String getName() {
		return "methodRefReplace";
	}

	@Override
	public String getData() {
		return textContents;
	}

	@Override
	public List<? extends Param> getSubParams() {
		return replaceParams;
	}

	@Override
	public Node toXML(Document doc) {
		Element classRefParam = doc.createElement("replace");
		if (!replaceParams.isEmpty()) {
			for (Param subParam : replaceParams) {
				Node subParamNode = subParam.toXML(doc);
				classRefParam.appendChild(subParamNode);
			}
		} else {
			classRefParam.setTextContent(textContents);
		}

		return classRefParam;
	}

	@Override
	public List<Step> getSteps(Context context) {
		SearchResult<ASTNode> current = context.get(Context.Prop.SEARCH_RESULT);
		Check.notNull(current, "context SEARCH_RESULT");

		ASTNode data = current.getDataObject();
		ChangeVisitor visitor;
		if (replaceParams.size() > 0) {
			RegexReplaceParam replaceParam = (RegexReplaceParam) replaceParams.get(0);
			visitor = new ChangeVisitor(context, replaceParam);
		} else {
			StringReplaceParam param = new StringReplaceParam(getData());
			visitor = new ChangeVisitor(context, param);
		}
		data.accept(visitor);
		String replacementText = visitor.outputReplacementText;

		List<Step> steps = new ArrayList<>();
		ReplaceInFileStep replaceStep = new ReplaceInFileStep(replacementText);
		steps.add(replaceStep);

		return steps;
	}

	private final class ChangeVisitor extends ASTVisitor {
		private Context context;

		private ReplaceParam<String> replaceParam;

		private String outputReplacementText;

		public ChangeVisitor(Context context, ReplaceParam<String> replaceParam) {
			this.context = context;
			this.replaceParam = replaceParam;
		}

		@Override
		public boolean visit(SimpleName node) {
			String declared = context.get(Context.Prop.ORIGINAL_SOURCE);

			String replacement = replaceParam.findReplacement(context, declared);

			outputReplacementText = replacement;

			return super.visit(node);
		}

	}

}
