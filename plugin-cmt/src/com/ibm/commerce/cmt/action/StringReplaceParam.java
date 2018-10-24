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

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;

/**
 * This class is used to replace a string with another string.
 * 
 * @author Trent Hoeppner
 */
public class StringReplaceParam implements ReplaceParam<String> {

	private String replacement;

	public StringReplaceParam(String replacement) {
		this.replacement = replacement;
	}

	@Override
	public String getName() {
		return "string";
	}

	@Override
	public String getData() {
		return replacement;
	}

	@Override
	public List<? extends Param> getSubParams() {
		return null;
	}

	@Override
	public Node toXML(Document doc) {
		Element stringParam = doc.createElement("string");
		stringParam.setTextContent(replacement);

		return stringParam;
	}

	@Override
	public String findReplacement(Context context, String source) {
		return replacement;
	}

}
