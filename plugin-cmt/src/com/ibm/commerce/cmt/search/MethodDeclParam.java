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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemUtil;

/**
 * This class is used to find method declarations.
 * 
 * @author Trent Hoeppner
 */
public class MethodDeclParam implements SearchParam {

	private List<Param> params;

	public MethodDeclParam(List<Param> params) {
		Check.notNullOrEmpty(params, "params");

		this.params = params;
	}

	@Override
	public String getName() {
		return "methodDecl";
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
		HasParamParam hasParamParam = null;
		HasMethodParam hasMethodParam = null;
		NotParam notParam = null;
		AndParam andParam = null;
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
			} else if (param instanceof HasMethodParam) {
				hasMethodParam = (HasMethodParam) param;
			} else if (param instanceof NotParam) {
				notParam = (NotParam) param;
			} else if (param instanceof AndParam) {
				andParam = (AndParam) param;
			}
		}

		CompilationUnit compUnit = context.get(Context.Prop.COMP_UNIT);
		JavaItem javaClass = JavaItemUtil.findJavaClass(context, (ASTNode) compUnit.types().get(0));

		List<ASTNodeIssueData> found = Collections.emptyList();
		if (javaClass != null) {
			MethodDeclVisitor v = new MethodDeclVisitor(context, nameParam, classNameParam, hasParamParam,
					hasMethodParam, notParam, andParam);
			compUnit.accept(v);
			found = v.allFound;
		}

		return found;

	}

	@Override
	public Node toXML(Document doc) {
		Element searchParam = doc.createElement("methoddecl");
		for (Param subParam : params) {
			Node subParamNode = subParam.toXML(doc);
			searchParam.appendChild(subParamNode);
		}

		return searchParam;
	}

	private final class MethodDeclVisitor extends ASTVisitor {
		private NameParam nameParam;

		private List<ASTNodeIssueData> allFound = new ArrayList<>();

		private Context context;

		private NameParam classNameParam;

		private HasParamParam hasParamParam;

		private HasMethodParam hasMethodParam;

		private NotParam notParam;

		private AndParam andParam;

		public MethodDeclVisitor(Context context, NameParam nameParam, NameParam classNameParam,
				HasParamParam hasParamParam, HasMethodParam hasMethodParam, NotParam notParam, AndParam andParam) {
			this.context = context;
			this.nameParam = nameParam;
			this.classNameParam = classNameParam;
			this.hasParamParam = hasParamParam;
			this.hasMethodParam = hasMethodParam;
			this.notParam = notParam;
			this.andParam = andParam;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			List<Matcher> matchers = new ArrayList<>();
			context.set(Context.Prop.ALL_MATCHERS, matchers);
			boolean found = SearchUtil.matchMethodDeclaration(node, context, nameParam, classNameParam, hasParamParam);
			if (found) {
				boolean containedMethodOK = true;
				if (hasMethodParam != null) {
					containedMethodOK = hasMethodParam.accept(context, node);
				} else if (notParam != null) {
					containedMethodOK = notParam.accept(context, node);
				} else if (andParam != null) {
					containedMethodOK = andParam.accept(context, node);
				}

				if (containedMethodOK) {
					allFound.add(new ASTNodeIssueData(node.getName(), matchers));
				}
			}

			return super.visit(node);
		}

	}

}
