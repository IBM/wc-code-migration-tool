package com.ibm.commerce.dependency.model;

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

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the variables that are available in a given scope. A
 * scope can have a parent scope and inherits the available variables from it.
 * 
 * @author Trent Hoeppner
 */
public class Scope {

	private Scope parent;

	private Map<String, JavaItem> varToTypeMap = new HashMap<>();

	private Map<String, JavaItem> importToTypeMap = new HashMap<>();

	public Scope() {
	}

	public Scope getParent() {
		return parent;
	}

	public Scope createSubScope() {
		Scope subScope = new Scope();
		subScope.parent = this;

		return subScope;
	}

	public void addVariable(String name, JavaItem type) {
		varToTypeMap.put(name, type);
	}

	public void addImportClass(String name, JavaItem type) {
		importToTypeMap.put(name, type);
	}

	public JavaItem getVariable(String name) {
		JavaItem type = varToTypeMap.get(name);
		if (type == null && parent != null) {
			type = parent.getVariable(name);
		}

		return type;
	}

	public JavaItem getStaticClassReference(String name) {
		JavaItem type = importToTypeMap.get(name);
		if (type == null && parent != null) {
			type = parent.getStaticClassReference(name);
		}

		return type;
	}
}