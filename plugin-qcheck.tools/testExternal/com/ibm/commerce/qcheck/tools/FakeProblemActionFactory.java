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

import java.io.IOException;
import java.net.URL;

import com.ibm.commerce.qcheck.core.ProblemAction;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.ValidatorResource;

/**
 * This class is used to test validators, and provides empty actions.
 * 
 * @author Trent Hoeppner
 */
public class FakeProblemActionFactory implements ProblemActionFactory {

	/**
	 * Constructor for this.
	 */
	public FakeProblemActionFactory() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProblemAction buildLink(URL url) {
		return new FakeProblemAction();

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProblemAction buildReplace(ValidatorResource resource, int startPosition, int endPosition,
			String replacement) {
		return new FakeProblemAction();
	}

	private static class FakeProblemAction implements ProblemAction {

		public FakeProblemAction() {
			// do nothing
		}

		@Override
		public void execute() throws IOException {
			// do nothing
		}

		@Override
		public String getDescription() {
			return "Fake action description";
		}

	}

}
