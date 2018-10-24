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

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.Param;

/**
 * This class is used to choose class references that are declared in an extends
 * clause.
 * 
 * @author Trent Hoeppner
 */
public class IsSupertypeParam implements FilterResultParam<ASTNode> {

	public IsSupertypeParam() {
		// do nothing
	}

	@Override
	public String getName() {
		return "issupertype";
	}

	@Override
	public String getData() {
		return null;
	}

	@Override
	public List<Param> getSubParams() {
		return Collections.emptyList();
	}

	@Override
	public Node toXML(Document doc) {
		Element param = doc.createElement("issupertype");
		return param;
	}

	@Override
	public boolean accept(Context context, ASTNode result) {
		Check.notNull(result, "result");

		boolean found = false;
		if (result instanceof SimpleType) {
			if (result.getParent() instanceof TypeDeclaration) {
				TypeDeclaration typeDecl = (TypeDeclaration) result.getParent();

				// check the superclass
				found = typeDecl.getSuperclassType() == result;

				if (!found) {
					// check the interfaces
					for (Object o : typeDecl.superInterfaceTypes()) {
						if (o == result) {
							found = true;
							break;
						}
					}
				}
			}
		}

		return found;
	}

}
