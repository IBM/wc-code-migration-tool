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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;

/**
 * This class is used to choose methods with certain parameters or a certain
 * number of parameters.
 * 
 * @author Trent Hoeppner
 */
public class HasParamParam implements FilterResultParam<ASTNode> {

	public enum Operator {
		EQUAL("=") {
			@Override
			public boolean evaluate(int numArgs, int number) {
				return numArgs == number;
			}
		},

		LESS_THAN_EQUAL("<=") {
			@Override
			public boolean evaluate(int numArgs, int number) {
				return numArgs <= number;
			}
		},

		GREATER_THAN_EQUAL(">=") {
			@Override
			public boolean evaluate(int numArgs, int number) {
				return numArgs >= number;
			}
		};

		private String value;

		private Operator(String value) {
			this.value = value;
		}

		public static Operator parse(String operatorString) {
			Operator found = null;
			for (Operator operator : Operator.values()) {
				if (operatorString.equals(operator.value)) {
					found = operator;
					break;
				}
			}

			if (found == null) {
				throw new IllegalArgumentException("Invalid operator: " + operatorString);
			}

			return found;
		}

		abstract public boolean evaluate(int numArgs, int number);
	}

	private Map<String, String> attributes;

	public HasParamParam(Map<String, String> attributes) {
		Check.notNullOrEmpty(attributes, "attributes");
		this.attributes = attributes;
	}

	@Override
	public String getName() {
		return "hasparam";
	}

	@Override
	public String getData() {
		return null;
	}

	@Override
	public List<Param> getSubParams() {
		return Collections.emptyList();
	}

	@Override
	public Node toXML(Document doc) {
		Element param = doc.createElement("hasparam");
		for (String name : attributes.keySet()) {
			String value = attributes.get(name);
			param.setAttribute(name, value);
		}

		return param;
	}

	@Override
	public boolean accept(Context context, ASTNode result) {
		Check.notNull(result, "result");

		String operatorAndNumber = attributes.get("num");
		Pattern pattern = Pattern.compile("((=)|(\\>=)|(\\<=))(\\d+)");
		Matcher matcher = pattern.matcher(operatorAndNumber);
		if (!matcher.matches()) {
			throw new IllegalStateException("num attribute in <hasparam> cannot be parsed: " + operatorAndNumber);
		}

		String operatorString = matcher.group(1);
		int number = Integer.parseInt(matcher.group(5));
		Operator operator = Operator.parse(operatorString);

		int numArgs;
		if (result instanceof MethodDeclaration) {
			MethodDeclaration decl = (MethodDeclaration) result;
			numArgs = decl.parameters().size();
		} else if (result instanceof MethodInvocation) {
			MethodInvocation inv = (MethodInvocation) result;
			numArgs = inv.arguments().size();
		} else if (result instanceof ClassInstanceCreation) {
			ClassInstanceCreation inv = (ClassInstanceCreation) result;
			numArgs = inv.arguments().size();
		} else {
			throw new IllegalArgumentException("result must be either MethodDeclaration or MethodInvocation but was "
					+ result.getClass().getCanonicalName());
		}

		boolean accept = operator.evaluate(numArgs, number);

		return accept;
	}

}
