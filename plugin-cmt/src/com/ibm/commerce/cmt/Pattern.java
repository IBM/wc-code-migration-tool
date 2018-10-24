package com.ibm.commerce.cmt;

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

import java.io.File;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.action.Action;
import com.ibm.commerce.cmt.plan.Issue;
import com.ibm.commerce.cmt.plan.Location;
import com.ibm.commerce.cmt.plan.Plan;
import com.ibm.commerce.cmt.plan.Range;
import com.ibm.commerce.cmt.plan.Step;
import com.ibm.commerce.cmt.search.SearchParam;
import com.ibm.commerce.cmt.search.SearchResult;

/**
 * This class represents a single pattern in a pattern file, and contains a search parameter and an action parameter.
 * 
 * @author Trent Hoeppner
 */
public class Pattern implements XMLConvertable {

	private SearchParam searchParam;

	private Action action;

	public SearchParam getSearchParam() {
		return searchParam;
	}

	public void setSearchParam(SearchParam searchParam) {
		this.searchParam = searchParam;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public void findInCurrentFileForPlan(Context context, Plan plan) {
		FileContents fileContents = context.get(Context.Prop.FILE_CONTENTS);
		File file = context.get(Context.Prop.FILE);
		if (!searchParam.allowFile(file)) {
			return;
		}

		List<? extends SearchResult<?>> results = searchParam.findAll(context);
		Check.notNull(results, "results");
		for (SearchResult<?> current : results) {
			// create an issue
			Range range = current.getRange();

			Location location = new Location();
			location.setFile(file.getAbsolutePath());
			location.setTimestamp(file.lastModified());
			location.setRange(range);
			String formattedRange = fileContents.format(range);
			location.setFormattedRange(formattedRange);

			int id = context.getIssueIDGenerator().nextID();
			Issue issue = new Issue(id);
			issue.setPattern(this);
			issue.setLocation(location);

			String sourceText = fileContents.getSubstring(range);
			issue.setSource(sourceText);

			context.set(Context.Prop.SEARCH_RESULT, current);
			context.set(Context.Prop.ISSUE, issue);
			context.set(Context.Prop.ALL_MATCHERS, current.getMatchers());
			List<Step> steps = action.getSteps(context);
			issue.setSteps(steps);

			// the range might have changed, reformat the range and set the
			// source
			formattedRange = fileContents.format(range);
			location.setFormattedRange(formattedRange);
			sourceText = fileContents.getSubstring(range);
			issue.setSource(sourceText);

			plan.addIssue(issue);
		}
	}

	@Override
	public Node toXML(Document doc) {
		Element pattern = doc.createElement("pattern");
		Node searchNode = searchParam.toXML(doc);
		Node actionNode = action.toXML(doc);
		pattern.appendChild(searchNode);
		pattern.appendChild(actionNode);
		return pattern;
	}

}
