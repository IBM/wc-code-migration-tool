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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Configuration;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.XMLConvertable;

/**
 * This class represents an execution plan that contains all the steps in the
 * plan. If a file changes after the plan is formed, the steps may not work as
 * the locations will be different. In that case, a new plan should be created
 * to ensure it is current with the files to change.
 * 
 * @author Trent Hoeppner
 */
public class Plan implements XMLConvertable {

	private List<Issue> issues = new ArrayList<>();

	public void addIssue(Issue issue) {
		issues.add(issue);
		Collections.sort(issues, new IssueComparator());
	}

	public void execute(Configuration config, Context context) throws Exception {
		File currentFile = null;
		for (Issue issue : issues) {
			File newFile = new File(issue.getLocation().getFile());
			if (!newFile.equals(currentFile)) {
				// write the current contents to disk
				if (context.isDefined(Context.Prop.FILE_WRITE_BUFFER)) {
					writeToDisk(context);
				}

				// prepare the new file for modification
				context.set(Context.Prop.FILE, newFile);
				currentFile = newFile;
			}

			context.set(Context.Prop.ISSUE, issue);
			List<Step> steps = issue.getSteps();
			for (Step step : steps) {
				step.execute(context);
			}
		}

		if (context.isDefined(Context.Prop.FILE_WRITE_BUFFER)) {
			writeToDisk(context);
		}
	}

	private void writeToDisk(Context context) throws IOException {
		File file = context.get(Context.Prop.FILE);
		StringBuilder b = context.get(Context.Prop.FILE_WRITE_BUFFER);
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try {
			byte[] buf = new byte[4096];
			in = new BufferedInputStream(new ByteArrayInputStream(b.toString().getBytes()));
			out = new BufferedOutputStream(new FileOutputStream(file));
			int length = in.read(buf);
			while (length >= 0) {
				out.write(buf, 0, length);
				length = in.read(buf);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// swallow to allow main exception to escape
				}
			}

			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// swallow to allow main exception to escape
				}
			}
		}
	}

	@Override
	public Node toXML(Document doc) {
		Element plan = doc.createElement("plan");
		for (Issue issue : issues) {
			Node issueNode = issue.toXML(doc);
			plan.appendChild(issueNode);
		}

		return plan;
	}

	private static class IssueComparator implements Comparator<Issue> {

		@Override
		public int compare(Issue o1, Issue o2) {
			int result;

			Location location1 = o1.getLocation();
			Location location2 = o2.getLocation();
			result = location1.getFile().compareTo(location2.getFile());
			if (result != 0) {
				return result;
			}

			// want higher numbers to come first, so we process the file from
			// end to beginning
			result = location2.getRange().getStart() - location1.getRange().getStart();
			if (result != 0) {
				return result;
			}

			// want higher numbers to come first, so we process the file from
			// end to beginning
			result = location2.getRange().getEnd() - location1.getRange().getEnd();
			if (result != 0) {
				return result;
			}

			return 0;
		}

	}

	public List<Issue> getIssues() {
		return issues;
	}
}
