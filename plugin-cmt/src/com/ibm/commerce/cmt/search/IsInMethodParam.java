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
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;

/**
 * This class is used to choose method references that are found inside a
 * certain method.
 * 
 * @author Trent Hoeppner
 */
public class IsInMethodParam implements FilterResultParam<ASTNode> {

	private List<Param> params;

	public IsInMethodParam(List<Param> params) {
		Check.notNullOrEmpty(params, "params");
		this.params = params;
	}

	@Override
	public String getName() {
		return "isinmethod";
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
		Element param = doc.createElement("isinmethod");
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

		MethodDeclaration parent = getContainingMethod(result);
		boolean accept = SearchUtil.matchMethodDeclaration(parent, context, nameParam, classNameParam, hasParamParam);

		return accept;
	}

	private MethodDeclaration getContainingMethod(ASTNode invocationNode) {
		ASTNode parent = invocationNode.getParent();
		while (!(parent instanceof MethodDeclaration)) {
			parent = parent.getParent();
		}

		return (MethodDeclaration) parent;
	}

}
