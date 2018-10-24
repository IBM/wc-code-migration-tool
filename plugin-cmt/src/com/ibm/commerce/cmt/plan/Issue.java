package com.ibm.commerce.cmt.plan;

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

import com.ibm.commerce.cmt.Pattern;
import com.ibm.commerce.cmt.XMLConvertable;

/**
 * This class represents a single search result from doing a scan.
 * 
 * @author Trent Hoeppner
 */
public class Issue implements XMLConvertable {

	private int id;

	private Pattern pattern;

	private Location location;

	private String description;

	private String source;

	private List<Step> steps;

	public Issue(int id) {
		this.id = id;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public Node toXML(Document doc) {
		Element issue = doc.createElement("issue");
		issue.setAttribute("id", String.valueOf(id));

		Node patternNode = pattern.toXML(doc);
		issue.appendChild(patternNode);

		Node locationNode = location.toXML(doc);
		issue.appendChild(locationNode);

		Element descriptionNode = doc.createElement("description");
		descriptionNode.setTextContent(description);
		issue.appendChild(descriptionNode);

		Element sourceNode = doc.createElement("source");
		sourceNode.setTextContent(source);
		issue.appendChild(sourceNode);

		Element stepsNode = doc.createElement("steps");
		issue.appendChild(stepsNode);

		for (Step step : steps) {
			Node stepNode = step.toXML(doc);
			stepsNode.appendChild(stepNode);
		}

		return issue;
	}

	public List<Step> getSteps() {
		return steps;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}
}
