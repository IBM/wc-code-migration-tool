package com.ibm.commerce.cmt.search;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

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

/**
 * This class represents a search parameter that can find class references (but
 * not declarations).
 * 
 * @author Trent Hoeppner
 */
public class ClassRefParam implements SearchParam {

	private List<Param> params;

	public ClassRefParam(List<Param> params) {
		Check.notNullOrEmpty(params, "params");

		this.params = params;
	}

	@Override
	public String getName() {
		return "classRef";
	}

	@Override
	public String getData() {
		return null;
	}

	@Override
	public List<? extends Param> getSubParams() {
		return params;
	}

	public boolean allowFile(File file) {
		boolean allow;
		if (file.getName().toLowerCase().endsWith(".java")) {
			// probably yes, but check subparams
			allow = SearchParam.super.allowFile(file);
		} else {
			// this search param cannot work if it's not a java file
			allow = false;
		}

		return allow;
	}

	@Override
	public List<ASTNodeIssueData> findAll(Context context) {
		NameParam nameParam = null;
		IsSupertypeParam isSupertypeParam = null;
		HasSupertypeParam hasSupertypeParam = null;
		for (Param param : params) {
			if (param instanceof NameParam) {
				nameParam = (NameParam) params.get(0);
			} else if (param instanceof IsSupertypeParam) {
				isSupertypeParam = (IsSupertypeParam) param;
			} else if (param instanceof HasSupertypeParam) {
				hasSupertypeParam = (HasSupertypeParam) param;
			}
		}

		ClassRefVisitor v = new ClassRefVisitor(context, nameParam, isSupertypeParam, hasSupertypeParam);
		CompilationUnit compUnit = context.get(Context.Prop.COMP_UNIT);
		compUnit.accept(v);
		List<ASTNodeIssueData> found = v.allFound;
		//
		// List<ASTNodeIssueData> list = new ArrayList<>();
		// for (ASTNode node : found) {
		// ASTNodeIssueData data = new ASTNodeIssueData(node);
		// list.add(data);
		// }

		return found;
	}

	@Override
	public Node toXML(Document doc) {
		Element searchParam = doc.createElement("classref");
		for (Param subParam : params) {
			Node subParamNode = subParam.toXML(doc);
			searchParam.appendChild(subParamNode);
		}

		return searchParam;
	}

	private final class ClassRefVisitor extends ASTVisitor {
		private NameParam nameParam;

		private IsSupertypeParam isSupertypeParam;

		private List<ASTNodeIssueData> allFound = new ArrayList<>();

		private Context context;

		private Map<String, String> shortNameToImportedMap = new HashMap<>();

		private HasSupertypeParam hasSupertypeParam;

		public ClassRefVisitor(Context context, NameParam nameParam, IsSupertypeParam isSupertypeParam,
				HasSupertypeParam hasSupertypeParam) {
			this.context = context;
			this.nameParam = nameParam;
			this.isSupertypeParam = isSupertypeParam;
			this.hasSupertypeParam = hasSupertypeParam;
		}

		@Override
		public boolean visit(ImportDeclaration node) {
			String imported = node.getName().getFullyQualifiedName();
			List<Matcher> matchers = new ArrayList<>();
			context.set(Context.Prop.ALL_MATCHERS, matchers);
			if (isSupertypeParam == null && nameParam.accept(context, imported)) {
				if (hasSupertypeParam == null || hasSupertypeParam.accept(context, node)) {
					allFound.add(new ASTNodeIssueData(node, matchers));
					String shortName = getSimpleClassName(imported);
					shortNameToImportedMap.put(shortName, imported);
				}
			}

			return super.visit(node);
		}

		@Override
		public boolean visit(SimpleType node) {
			List<Matcher> matchers = new ArrayList<>();
			context.set(Context.Prop.ALL_MATCHERS, matchers);
			if (matches(node)) {
				allFound.add(new ASTNodeIssueData(node, matchers));
			}
			return super.visit(node);
		}

		private boolean matches(SimpleType node) {
			boolean match = false;
			if (isSupertypeParam == null || isSupertypeParam.accept(context, node)) {
				if (hasSupertypeParam == null || hasSupertypeParam.accept(context, node)) {
					String declared = node.getName().getFullyQualifiedName();
					int lastDot = declared.lastIndexOf('.');
					if (lastDot < 0) {
						// no package name, find in imports
						String fullDeclared = shortNameToImportedMap.get(declared);
						if (fullDeclared == null) {
							// TODO not imported directly, it was * imported...
							// for now do not match this
						} else {
							match = nameParam.accept(context, fullDeclared);
						}
					} else {
						match = nameParam.accept(context, declared);
					}
				}
			}

			return match;
		}

		private String getSimpleClassName(String fullClassName) {
			String partialClassName = fullClassName;
			int lastDot = fullClassName.lastIndexOf('.');
			if (lastDot >= 0) {
				partialClassName = fullClassName.substring(lastDot + 1);
			}

			return partialClassName;
		}

	}

}
