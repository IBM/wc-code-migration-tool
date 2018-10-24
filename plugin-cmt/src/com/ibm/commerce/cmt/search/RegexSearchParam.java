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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;

/**
 * This class is used to match regular expressions in text strings during the
 * search phase.
 * 
 * @author Trent Hoeppner
 */
public class RegexSearchParam implements FilterResultParam<String> {

	/**
	 * The regular expression to match text against.
	 */
	private String regex;

	/**
	 * Constructor for this.
	 * 
	 * @param regex
	 *            The regular expression to match text against. This value
	 *            cannot be null or empty.
	 */
	public RegexSearchParam(String regex) {
		this.regex = regex;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "regex";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getData() {
		return regex;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<? extends Param> getSubParams() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Node toXML(Document doc) {
		Element regexParam = doc.createElement("regex");
		regexParam.setTextContent(regex);

		return regexParam;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean accept(Context context, String result) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(result);
		boolean accept = matcher.matches();

		if (accept) {
			List<Matcher> matchers = context.get(Context.Prop.ALL_MATCHERS);
			matchers.add(matcher);
		}

		return accept;
	}

}
