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

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;
import com.ibm.commerce.cmt.XMLUtil;
import com.ibm.commerce.cmt.plan.Issue;

/**
 * This class is used to replace some text in a string using a regular
 * expression.
 * 
 * @author Trent Hoeppner
 */
public class RegexReplaceParam implements ReplaceParam<String> {

	private String regex;

	public RegexReplaceParam(String regex) {
		this.regex = regex;
	}

	@Override
	public String getName() {
		return "regex";
	}

	@Override
	public String getData() {
		return regex;
	}

	@Override
	public List<? extends Param> getSubParams() {
		return null;
	}

	@Override
	public Node toXML(Document doc) {
		Element regexParam = doc.createElement("regex");
		regexParam.setTextContent(regex);

		return regexParam;
	}

	@Override
	public String findReplacement(Context context, String source) {
		return findReplacementInternal(context, regex);
	}

	private String findReplacementInternal(Context context, String regex) {
		List<String> groups = context.get(Context.Prop.ALL_GROUPS);
		Pattern replaceStartPattern = Pattern.compile("(?<!\\\\)\\$");
		Matcher replaceStartMatcher = replaceStartPattern.matcher(regex);
		Pattern functionPattern = Pattern.compile("(\\w+)\\{([^}]*)\\}");

		StringBuilder b = new StringBuilder();
		int lastEndMatchIndex = 0;
		while (replaceStartMatcher.find(lastEndMatchIndex)) {
			int startIndex = replaceStartMatcher.start();
			int endIndex;
			String group;

			// determine if it's a single digit next, or something else
			char nextChar = regex.charAt(startIndex + 1);
			if (!Character.isDigit(nextChar)) {
				Matcher functionMatcher = functionPattern.matcher(regex);
				functionMatcher.region(startIndex + 1, regex.length());
				if (!functionMatcher.find()) {
					throw new IllegalStateException("regular expression does not define a group or a function at "
							+ (startIndex + 1) + ": " + regex);
				}

				endIndex = functionMatcher.end();

				String functionName = functionMatcher.group(1);
				String functionParameter = functionMatcher.group(2);
				String evaluatedParameter = null;
				if (functionParameter != null) {
					evaluatedParameter = findReplacementInternal(context, functionParameter);
				}

				if ("tolower".equals(functionName)) {
					group = evaluatedParameter.toLowerCase();
				} else {
					throw new IllegalStateException("regular expression specifies an undefined function name at "
							+ (startIndex + 1) + ": " + regex);
				}
			} else {
				endIndex = replaceStartMatcher.end() + 1;
				int groupIndex = Integer.parseInt("" + nextChar);
				if (groupIndex >= groups.size()) {
					Issue issue = context.get(Context.Prop.ISSUE);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					XMLUtil.writeXML(issue.getPattern(), out);
					String patternXML = out.toString();
					throw new IllegalStateException(
							"No such group index: " + groupIndex + " for pattern\n" + patternXML);
				}

				group = groups.get(groupIndex);
			}

			try {
				String lastNormalText = regex.substring(lastEndMatchIndex, startIndex);
				b.append(lastNormalText);
			} catch (StringIndexOutOfBoundsException e) {
				e.printStackTrace();
			}

			b.append(group);

			lastEndMatchIndex = endIndex;
		}

		String lastNormalText = regex.substring(lastEndMatchIndex, regex.length());
		b.append(lastNormalText);

		String replacement = b.toString();

		return replacement;
	}

}
