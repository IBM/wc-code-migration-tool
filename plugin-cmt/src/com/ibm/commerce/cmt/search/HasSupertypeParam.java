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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemUtil;

/**
 * This class is used to choose classes that have a certain ancestor type.
 * 
 * @author Trent Hoeppner
 */
public class HasSupertypeParam implements FilterResultParam<ASTNode> {

	private List<Param> params;

	public HasSupertypeParam(List<Param> params) {
		Check.notNullOrEmpty(params, "params");
		this.params = params;
	}

	@Override
	public String getName() {
		return "hassupertype";
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
		Element param = doc.createElement("hassupertype");
		for (Param subParam : params) {
			Node subParamNode = subParam.toXML(doc);
			param.appendChild(subParamNode);
		}

		return param;
	}

	@Override
	public boolean accept(Context context, ASTNode result) {
		Check.notNull(result, "result");

		NameParam nameParam = (NameParam) params.get(0);

		JavaItem javaClass = JavaItemUtil.findJavaClass(context, result);

		if (javaClass == null) {
			// TODO for JSP generated Java files, this can happen, should check
			// if they are ever analyzed
			return false;
		}

		boolean accept = SearchUtil.hasSupertype(context, nameParam, javaClass);

		return accept;
	}

}
