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

import java.io.File;
import java.util.List;

import com.ibm.commerce.cmt.Param;

/**
 * This interface represents a kind of parameter that can be restricted to
 * certain file types.
 * 
 * @author Trent Hoeppner
 */
public interface FileFilterParam extends Param {

	default boolean allowFile(File file) {
		boolean allow = true;
		List<? extends Param> subParams = getSubParams();
		if (subParams != null) {
			for (Param param : subParams) {
				if (param instanceof FileFilterParam) {
					FileFilterParam searchParam = (FileFilterParam) param;
					if (!searchParam.allowFile(file)) {
						allow = false;
						break;
					}
				}
			}
		}

		return allow;
	}
}
