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

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
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
 * This parameter is used to choose only classes or class references that have a
 * certain method.
 * 
 * @author Trent Hoeppner
 */
public class HasMethodParam implements FilterResultParam<ASTNode> {

	private List<Param> params;

	public HasMethodParam(List<Param> params) {
		Check.notNullOrEmpty(params, "params");
		this.params = params;
	}

	@Override
	public String getName() {
		return "not";
	}

	@Override
	public String getData() {
		return null;
	}

	@Override
	public List<Param> getSubParams() {
		return params;
	}

	@Override
	public Node toXML(Document doc) {
		Element param = doc.createElement("hasmethod");
		for (Param subParam : params) {
			Node subParamNode = subParam.toXML(doc);
			param.appendChild(subParamNode);
		}

		return param;
	}

	@Override
	public boolean accept(Context context, ASTNode result) {
		Check.notNull(result, "result");

		// result input can be MethodInvocation or ClassInstanceCreation

		NameParam nameParam = null;
		NameParam classNameParam = null;
		HasParamParam hasParamParam = null;
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
			}
		}

		JavaItem javaClass = JavaItemUtil.findJavaClass(context, result);

		boolean found = false;
		if (javaClass != null) {
			MethodRefVisitor v = new MethodRefVisitor(context, nameParam, classNameParam, hasParamParam, javaClass);
			result.accept(v);
			found = v.found;
		}

		return found;
	}

	private final class MethodRefVisitor extends VariableVisitor {
		private NameParam nameParam;

		private Context context;

		private NameParam classNameParam;

		private HasParamParam hasParamParam;

		private boolean found;

		public MethodRefVisitor(Context context, NameParam nameParam, NameParam classNameParam,
				HasParamParam hasParamParam, JavaItem javaClass) {
			super(context.get(Context.Prop.JAVA_ITEM_UTIL), javaClass, null, null);
			this.context = context;
			this.nameParam = nameParam;
			this.classNameParam = classNameParam;
			this.hasParamParam = hasParamParam;
		}

		@Override
		public boolean visit(MethodInvocation node) {
			if (found) {
				return false;
			}

			found = SearchUtil.matchMethodInvocation(node, context, nameParam, classNameParam, null, hasParamParam,
					scope);

			// TODO need to check the parameters

			return super.visit(node);
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			if (found) {
				return false;
			}

			found = SearchUtil.matchMethodInvocation(node, context, nameParam, classNameParam, null, hasParamParam,
					scope);

			// TODO need to check the parameters

			return super.visit(node);

		}
	}

}
