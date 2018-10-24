package com.ibm.commerce.qcheck.core.comment;

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

/**
 * ColLine represents a column and line number. This is a convenience class for
 * methods that need to return a line and column.
 * 
 * @author Trent Hoeppner
 */
public class ColLine {

	/**
	 * The column number.
	 */
	private int columnNum;

	/**
	 * The line number.
	 */
	private int lineNum;

	/**
	 * Constructor for ColLine.
	 * 
	 * @param columnNum
	 *            The column number.
	 * @param lineNum
	 *            The line number.
	 */
	public ColLine(int columnNum, int lineNum) {
		this.columnNum = columnNum;
		this.lineNum = lineNum;
	}

	/**
	 * Sets the column number.
	 * 
	 * @param columnNum
	 *            The 0-based column number. Must be &gt;= 0.
	 */
	public void setColumnNum(int columnNum) {
		this.columnNum = columnNum;
	}

	/**
	 * Returns the column number.
	 * 
	 * @return The 0-based column number.
	 */
	public int getColumnNum() {
		return columnNum;
	}

	/**
	 * Sets the line number.
	 * 
	 * @param lineNum
	 *            The line number to set.
	 */
	public void setLineNum(int lineNum) {
		this.lineNum = lineNum;
	}

	/**
	 * Returns the line number.
	 * 
	 * @return The line number.
	 */
	public int getLineNum() {
		return lineNum;
	}
}
