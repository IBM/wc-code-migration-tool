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
 * This class is used to choose names that match a string or regular expression.
 * 
 * @author Trent Hoeppner
 */
public class NameParam implements FilterResultParam<String> {

	private String data;

	private List<Param> params;

	private String purpose;

	public NameParam(String purpose, String data, List<Param> params) {
		Check.notNull(params, "params");
		if (params.isEmpty()) {
			Check.notNullOrEmpty(data, "data");
		}

		this.purpose = purpose;
		this.data = data;
		this.params = params;
	}

	@Override
	public String getName() {
		return "name";
	}

	@Override
	public String getData() {
		return data;
	}

	@Override
	public List<Param> getSubParams() {
		return params;
	}

	public String getPurpose() {
		return purpose;
	}

	@Override
	public Node toXML(Document doc) {
		Element nameParam = doc.createElement(purpose);
		if (!params.isEmpty()) {
			for (Param subParam : params) {
				Node subParamNode = subParam.toXML(doc);
				nameParam.appendChild(subParamNode);
			}
		} else {
			nameParam.setTextContent(data);
		}

		return nameParam;
	}

	@Override
	public boolean accept(Context context, String result) {
		Check.notNull(result, "result");

		boolean accept = false;
		if (!params.isEmpty()) {
			for (Param param : params) {
				@SuppressWarnings("unchecked")
				FilterResultParam<String> fparam = (FilterResultParam<String>) param;
				accept = fparam.accept(context, result);
				if (!accept) {
					break;
				}
			}
		} else {
			accept = result.equals(data);
		}

		return accept;
	}

}
