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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;

/**
 * This class represents a meta-parameter that requires another filter parameter
 * to return false.
 * 
 * @author Trent Hoeppner
 */
public class NotParam implements FilterResultParam<Object> {

	private List<Param> params;

	public NotParam(List<Param> params) {
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
		Element param = doc.createElement("not");
		for (Param subParam : params) {
			Node subParamNode = subParam.toXML(doc);
			param.appendChild(subParamNode);
		}

		return param;
	}

	@Override
	public boolean accept(Context context, Object result) {
		Check.notNull(result, "result");

		boolean allAccept = true;
		for (Param param : params) {
			@SuppressWarnings("unchecked")
			FilterResultParam<Object> filter = (FilterResultParam<Object>) param;
			if (!filter.accept(context, result)) {
				allAccept = false;
				break;
			}
		}

		boolean not = !allAccept;

		return not;
	}

}
