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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemUtil;
import com.ibm.commerce.dependency.model.VariableVisitor;

/**
 * This class is used to find method references.
 * 
 * @author Trent Hoeppner
 */
public class MethodRefParam implements SearchParam {

	private List<Param> params;

	public MethodRefParam(List<Param> params) {
		Check.notNullOrEmpty(params, "params");

		this.params = params;
	}

	@Override
	public String getName() {
		return "methodref";
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
		NameParam classNameParam = null;
		HasSupertypeParam hasSupertypeParam = null;
		HasParamParam hasParamParam = null;
		IsInMethodParam isInMethodParam = null;
		NotParam notParam = null;
		for (Param param : params) {
			if (param instanceof NameParam) {
				NameParam p = (NameParam) param;
				if (p.getPurpose().equals("name")) {
					nameParam = p;
				} else if (p.getPurpose().equals("classname")) {
					classNameParam = p;
				}
			} else if (param instanceof HasParamParam) {
				hasParamParam = (HasParamParam) param;
			} else if (param instanceof IsInMethodParam) {
				isInMethodParam = (IsInMethodParam) param;
			} else if (param instanceof HasSupertypeParam) {
				hasSupertypeParam = (HasSupertypeParam) param;
			} else if (param instanceof NotParam) {
				notParam = (NotParam) param;
			}
		}

		CompilationUnit compUnit = context.get(Context.Prop.COMP_UNIT);
		JavaItem javaClass = JavaItemUtil.findJavaClass(context, (ASTNode) compUnit.types().get(0));

		List<ASTNodeIssueData> found = Collections.emptyList();
		if (javaClass != null) {
			MethodRefVisitor v = new MethodRefVisitor(context, nameParam, classNameParam, hasSupertypeParam,
					hasParamParam, isInMethodParam, notParam, javaClass);
			compUnit.accept(v);
			found = v.allFound;
		}

		return found;
	}

	@Override
	public Node toXML(Document doc) {
		Element searchParam = doc.createElement("methodref");
		for (Param subParam : params) {
			Node subParamNode = subParam.toXML(doc);
			searchParam.appendChild(subParamNode);
		}

		return searchParam;
	}

	private final class MethodRefVisitor extends VariableVisitor {
		private NameParam nameParam;

		private List<ASTNodeIssueData> allFound = new ArrayList<>();

		private Context context;

		private NameParam classNameParam;

		private HasSupertypeParam hasSupertypeParam;

		private HasParamParam hasParamParam;

		private IsInMethodParam isInMethodParam;

		private NotParam notParam;

		public MethodRefVisitor(Context context, NameParam nameParam, NameParam classNameParam,
				HasSupertypeParam hasSupertypeParam, HasParamParam hasParamParam, IsInMethodParam isInMethodParam,
				NotParam notParam, JavaItem javaClass) {
			super(context.get(Context.Prop.JAVA_ITEM_UTIL), javaClass, null, null);
			this.context = context;
			this.nameParam = nameParam;
			this.classNameParam = classNameParam;
			this.hasSupertypeParam = hasSupertypeParam;
			this.hasParamParam = hasParamParam;
			this.isInMethodParam = isInMethodParam;
			this.notParam = notParam;
		}

		@Override
		public boolean visit(MethodInvocation node) {
			List<Matcher> matchers = new ArrayList<>();
			context.set(Context.Prop.ALL_MATCHERS, matchers);

			boolean found = SearchUtil.matchMethodInvocation(node, context, nameParam, classNameParam,
					hasSupertypeParam, hasParamParam, scope);

			if (found) {
				boolean containerMethodOK = false;
				if (isInMethodParam != null) {
					containerMethodOK = isInMethodParam.accept(context, node);
				} else if (notParam != null) {
					containerMethodOK = notParam.accept(context, node);
				} else {
					containerMethodOK = true;
				}

				if (containerMethodOK) {
					allFound.add(new ASTNodeIssueData(node.getName(), matchers));
				}
			}

			// TODO need to check the parameters

			return super.visit(node);
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			List<Matcher> matchers = new ArrayList<>();
			context.set(Context.Prop.ALL_MATCHERS, matchers);

			boolean found = SearchUtil.matchMethodInvocation(node, context, nameParam, classNameParam,
					hasSupertypeParam, hasParamParam, scope);

			if (found) {
				boolean containerMethodOK = false;
				if (isInMethodParam != null) {
					containerMethodOK = isInMethodParam.accept(context, node);
				} else if (notParam != null) {
					containerMethodOK = notParam.accept(context, node);
				} else {
					containerMethodOK = true;
				}

				if (containerMethodOK) {
					allFound.add(new ASTNodeIssueData(node, matchers));
				}
			}

			// TODO need to check the parameters

			return super.visit(node);
		}

	}

}
