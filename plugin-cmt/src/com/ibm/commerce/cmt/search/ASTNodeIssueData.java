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

import org.eclipse.jdt.core.dom.ASTNode;

import com.ibm.commerce.cmt.plan.Range;

/**
 * This class represents a search result as an ASTNode.
 * 
 * @author Trent Hoeppner
 */
public class ASTNodeIssueData implements SearchResult<ASTNode> {

	private ASTNode node;

	private List<Matcher> matchers;

	public ASTNodeIssueData(ASTNode node, List<Matcher> matchers) {
		this.node = node;
		this.matchers = matchers;
	}

	@Override
	public Range getRange() {
		int start = node.getStartPosition();
		int end = start + node.getLength();

		Range range = new Range();
		range.setStart(start);
		range.setEnd(end);

		return range;
	}

	@Override
	public ASTNode getDataObject() {
		return node;
	}

	@Override
	public List<Matcher> getMatchers() {
		return matchers;
	}

}
