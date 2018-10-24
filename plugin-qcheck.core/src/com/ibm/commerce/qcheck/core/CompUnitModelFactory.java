package com.ibm.commerce.qcheck.core;

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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * This class is used to make compilation units for {@link ValidatorResource
 * resources}.
 * 
 * @author Trent Hoeppner
 */
public class CompUnitModelFactory implements ModelFactory<CompUnitModel> {

	@Override
	public CompUnitModel createModel(ValidatorResource resource) {
		CompilationUnit resourceCompUnit;
		if (resource instanceof WorkingValidatorResource) {
			WorkingValidatorResource workingResource = (WorkingValidatorResource) resource;
			resourceCompUnit = workingResource.getWorkingCompUnit();
		} else {
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			String stringForm = ModelEnum.STRING.getData(resource);
			parser.setSource(stringForm.toCharArray());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			resourceCompUnit = (CompilationUnit) parser.createAST(null);
		}

		CompUnitModel model = new CompUnitModel(resourceCompUnit);
		return model;
	}

}
