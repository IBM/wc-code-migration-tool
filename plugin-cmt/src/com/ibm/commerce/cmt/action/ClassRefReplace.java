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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
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
public class ClassRefReplace implements Action {

	private String textContents;

	private List<Param> replaceParams;

	public ClassRefReplace(String textContents, List<Param> replaceParams) {
		this.textContents = textContents;
		this.replaceParams = replaceParams;
	}

	@Override
	public String getName() {
		return "classRefReplace";
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

		CompilationUnit compUnit = context.get(Context.Prop.COMP_UNIT);
		ImportVisitor importVisitor = new ImportVisitor();
		compUnit.accept(importVisitor);

		ASTNode data = current.getDataObject();
		ChangeVisitor visitor;
		if (replaceParams.size() > 0) {
			RegexReplaceParam replaceParam = (RegexReplaceParam) replaceParams.get(0);
			visitor = new ChangeVisitor(context, replaceParam, importVisitor.shortNameToImportedMap);
		} else {
			StringReplaceParam param = new StringReplaceParam(getData());
			visitor = new ChangeVisitor(context, param, importVisitor.shortNameToImportedMap);
		}
		data.accept(visitor);
		String replacementText = visitor.outputReplacementText;

		List<Step> steps = new ArrayList<>();
		ReplaceInFileStep replaceStep = new ReplaceInFileStep(replacementText);
		steps.add(replaceStep);

		return steps;
	}

	private String getSimpleClassName(String fullClassName) {
		String partialClassName = fullClassName;
		int lastDot = fullClassName.lastIndexOf('.');
		if (lastDot >= 0) {
			partialClassName = fullClassName.substring(lastDot + 1);
		}

		return partialClassName;
	}

	private final class ImportVisitor extends ASTVisitor {
		private Map<String, String> shortNameToImportedMap = new HashMap<>();

		@Override
		public boolean visit(ImportDeclaration node) {
			String fullClassName = node.getName().toString();
			String shortName = getSimpleClassName(fullClassName);
			shortNameToImportedMap.put(shortName, fullClassName);

			return super.visit(node);
		}

	}

	private final class ChangeVisitor extends ASTVisitor {
		private Context context;

		private ReplaceParam<String> replaceParam;

		private String outputReplacementText;

		private Map<String, String> shortNameToImportedMap;

		public ChangeVisitor(Context context, ReplaceParam<String> replaceParam,
				Map<String, String> shortNameToImportedMap) {
			this.context = context;
			this.replaceParam = replaceParam;
			this.shortNameToImportedMap = shortNameToImportedMap;
		}

		@Override
		public boolean visit(ImportDeclaration node) {
			String sourceName = node.getName().toString();
			String newClass = replaceParam.findReplacement(context, sourceName);
			outputReplacementText = "import " + newClass + ";";

			return super.visit(node);
		}

		@Override
		public boolean visit(SimpleType node) {
			changeType();
			return super.visit(node);
		}

		private void changeType() {
			String declared = context.get(Context.Prop.ORIGINAL_SOURCE);

			String replacement;
			int lastDot = declared.lastIndexOf('.');
			if (lastDot < 0) {
				String fullDeclared = shortNameToImportedMap.get(declared);
				replacement = replaceParam.findReplacement(context, fullDeclared);
				replacement = getSimpleClassName(replacement);
			} else {
				replacement = replaceParam.findReplacement(context, declared);
			}

			outputReplacementText = replacement;
		}

	}

}
