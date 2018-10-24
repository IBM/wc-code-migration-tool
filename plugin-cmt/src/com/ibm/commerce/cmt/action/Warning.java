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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;
import com.ibm.commerce.cmt.plan.LogStep;
import com.ibm.commerce.cmt.plan.Step;

/**
 * This class supports replacing class references in Java source code with
 * another reference.
 * 
 * @author Trent Hoeppner
 */
public class Warning implements Action {

	private String textContents;

	private List<Param> replaceParams;

	public Warning(String textContents, List<Param> replaceParams) {
		this.textContents = textContents;
		this.replaceParams = replaceParams;
	}

	@Override
	public String getName() {
		return "warning";
	}

	@Override
	public String getData() {
		return textContents;
	}

	@Override
	public List<? extends Param> getSubParams() {
		return replaceParams;
	}

	@Override
	public List<Step> getSteps(Context context) {
		List<Step> steps = new ArrayList<>();

		ReplaceParam<String> replaceParam;
		if (replaceParams.size() > 0) {
			replaceParam = (RegexReplaceParam) replaceParams.get(0);
		} else {
			replaceParam = new StringReplaceParam(getData());
		}

		// the source is always ignored, so set it to null
		String replacementText = replaceParam.findReplacement(context, null);

		LogStep logStep = new LogStep(replacementText);
		steps.add(logStep);

		return steps;
	}

	@Override
	public Node toXML(Document doc) {
		Element warningParam = doc.createElement("warning");
		if (!replaceParams.isEmpty()) {
			for (Param subParam : replaceParams) {
				Node subParamNode = subParam.toXML(doc);
				warningParam.appendChild(subParamNode);
			}
		} else {
			warningParam.setTextContent(textContents);
		}

		return warningParam;
	}

}
