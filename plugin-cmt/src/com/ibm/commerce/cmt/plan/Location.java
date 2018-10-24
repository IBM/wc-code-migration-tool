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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.XMLConvertable;

/**
 * This class represents a location in a file for an issue, including the
 * changes of text in that file.
 * 
 * @author Trent Hoeppner
 */
public class Location implements XMLConvertable {

	private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS Z");

	private String filename;

	private Range range;

	private long timestamp;

	private String formattedRange;

	public String getFile() {
		return filename;
	}

	public void setFile(String filename) {
		this.filename = filename;
	}

	public Range getRange() {
		return range;
	}

	public void setRange(Range range) {
		this.range = range;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long time) {
		this.timestamp = time;
	}

	public String getFormattedRange() {
		return formattedRange;
	}

	public void setFormattedRange(String formattedRange) {
		this.formattedRange = formattedRange;
	}

	@Override
	public Node toXML(Document doc) {
		Element location = doc.createElement("location");
		location.setAttribute("file", filename);
		location.setAttribute("location", formattedRange);
		String value = TIMESTAMP_FORMAT.format(new Date(timestamp));
		location.setAttribute("timestamp", value);
		return location;
	}

}
