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

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Context;

/**
 * This class is an execution step that replaces some text in a file.
 * 
 * @author Trent Hoeppner
 */
public class ReplaceInFileStep implements Step {

	private String replacement;

	public ReplaceInFileStep(String replacement) {
		this.replacement = replacement;
	}

	@Override
	public void execute(Context context) throws Exception {
		StringBuilder b = context.get(Context.Prop.FILE_WRITE_BUFFER);
		Range range = context.get(Context.Prop.RANGE);
		String originalSource = context.get(Context.Prop.ORIGINAL_SOURCE);

		String sourceValue = b.substring(range.getStart(), range.getEnd());
		if (!sourceValue.equals(originalSource)) {
			File file = context.get(Context.Prop.FILE);
			String formattedRange = context.get(Context.Prop.FORMATTED_RANGE);
			throw new Exception("Expected source in " + file.getAbsolutePath() + " at " + formattedRange + " to be "
					+ originalSource + ", but was " + sourceValue);
		}

		b.replace(range.getStart(), range.getEnd(), replacement);
	}

	@Override
	public Node toXML(Document doc) {
		Element replaceInFile = doc.createElement("replaceinfile");

		Element replacementNode = doc.createElement("replacement");
		replacementNode.setTextContent(replacement);
		replaceInFile.appendChild(replacementNode);

		return replaceInFile;
	}

	public String getReplacement() {
		return replacement;
	}

}
