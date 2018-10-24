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

import java.io.Writer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Context;

/**
 * This class is an execution step that logs a message in the output.
 * 
 * @author Trent Hoeppner
 */
public class LogStep implements Step {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private String message;

	public LogStep(String message) {
		this.message = message;
	}

	@Override
	public void execute(Context context) throws Exception {
		Writer logWriter = context.get(Context.Prop.LOG_WRITER);
		logWriter.append(message);
		logWriter.append(LINE_SEPARATOR);
		logWriter.flush();
	}

	@Override
	public Node toXML(Document doc) {
		Element classRefParam = doc.createElement("log");
		classRefParam.setTextContent(message);

		return classRefParam;
	}

	public String getMessage() {
		return message;
	}

}
