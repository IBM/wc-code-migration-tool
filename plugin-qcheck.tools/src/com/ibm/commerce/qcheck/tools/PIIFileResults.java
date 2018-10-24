package com.ibm.commerce.qcheck.tools;

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

/**
 * This class contains the results of a search from
 * {@link CheckPIIOutput#findSimilar(PIIFile)}.
 * 
 * @author Trent Hoeppner
 */
public class PIIFileResults {

	private List<PIIFile> results;

	private boolean exactMatch;

	public PIIFileResults(List<PIIFile> results, boolean exactMatch) {
		this.results = results;
		this.exactMatch = exactMatch;
	}

	public List<PIIFile> getResults() {
		return results;
	}

	public boolean isExactMatch() {
		return exactMatch;
	}
}
