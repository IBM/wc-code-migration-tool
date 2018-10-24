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
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemUtil;

/**
 * This class represents a search parameter that can find Java class
 * declarations.
 * 
 * @author Trent Hoeppner
 */
public class ClassDeclParam implements SearchParam {

	private List<Param> params;

	public ClassDeclParam(List<Param> params) {
		Check.notNullOrEmpty(params, "params");

		this.params = params;
	}

	@Override
	public String getName() {
		return "classDecl";
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
		for (Param param : params) {
			if (param instanceof NameParam) {
				nameParam = (NameParam) params.get(0);
			}
		}

		ClassDeclVisitor v = new ClassDeclVisitor(context, nameParam);
		CompilationUnit compUnit = context.get(Context.Prop.COMP_UNIT);
		compUnit.accept(v);
		List<ASTNodeIssueData> found = v.allFound;

		return found;
	}

	@Override
	public Node toXML(Document doc) {
		Element searchParam = doc.createElement("classdecl");
		for (Param subParam : params) {
			Node subParamNode = subParam.toXML(doc);
			searchParam.appendChild(subParamNode);
		}

		return searchParam;
	}

	private final class ClassDeclVisitor extends ASTVisitor {
		private NameParam nameParam;

		private List<ASTNodeIssueData> allFound = new ArrayList<>();

		private Context context;

		public ClassDeclVisitor(Context context, NameParam nameParam) {
			this.context = context;
			this.nameParam = nameParam;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			JavaItem javaClass = JavaItemUtil.findJavaClass(context, node);
			if (javaClass == null) {
				// TODO find out why some JSP files are not found
				return super.visit(node);
			}

			String fullName = JavaItemUtil.getFullClassNameForType(javaClass);
			List<Matcher> matchers = new ArrayList<>();
			context.set(Context.Prop.ALL_MATCHERS, matchers);
			if (nameParam.accept(context, fullName)) {
				allFound.add(new ASTNodeIssueData(node.getName(), matchers));
			}

			return super.visit(node);
		}

	}

}
