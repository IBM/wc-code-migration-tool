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

/**
 * This class is used to generate a sequence of IDs. It can be seeded with an
 * initial ID to continue a sequence from before.
 * 
 * @author Trent Hoeppner
 */
public class IDGenerator {

	private int nextID;

	/**
	 * Constructor for this.
	 * 
	 * @param initial
	 *            The seed to start the sequencing from.
	 */
	public IDGenerator(int initial) {
		nextID = initial;
	}

	/**
	 * Constructor for this.
	 * 
	 * @param initial
	 *            The generator whose value will be used as the seed to start
	 *            the sequencing from. This value cannot be null.
	 */
	public IDGenerator(IDGenerator initial) {
		nextID = initial.nextID;
	}

	/**
	 * Returns the next ID in the sequence.
	 * 
	 * @return The next ID in the sequence.
	 */
	public synchronized int nextID() {
		return nextID++;
	}

}
