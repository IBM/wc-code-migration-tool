package com.ibm.commerce.dependency.load;

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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.dependency.task.Task;

/**
 * This class parses a Java file in the form of a string and creates a
 * compilation unit from it.
 * 
 * @author Trent Hoeppner
 */
public class LoadJavaCompilationUnitTask extends Task<LoadingContext> {

	/**
	 * The required inputs for this task.
	 */
	private static final Set<String> INPUTS = new HashSet<>(Arrays.asList(Name.TEXT_CONTENT));

	/**
	 * The expected outputs for this task.
	 */
	private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(Name.JAVA_COMPILATION_UNIT));

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this task. This value cannot be null or empty.
	 * @param context
	 *            The context used for input and output during execution. This
	 *            value cannot be null.
	 */
	public LoadJavaCompilationUnitTask(String name, LoadingContext context) {
		super(name, context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getInputConstraints() {
		return INPUTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getOutputConstraints() {
		return OUTPUTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(LoadingContext context) throws Exception {
		String content = context.get(Name.TEXT_CONTENT);

		CompilationUnit compUnit = getCompilationUnit(content);

		context.put(Name.JAVA_COMPILATION_UNIT, compUnit);
	}

	/**
	 * Loads the compilation unit for the given Java source file.
	 * 
	 * @param sourceFile
	 *            The source file to load. This value cannot be null, and must
	 *            exist.
	 * 
	 * @return The compilation unit that represents the contents of the file.
	 *         This value will not be null.
	 */
	@SuppressWarnings("rawtypes")
	private CompilationUnit getCompilationUnit(String content) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(content.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
		parser.setCompilerOptions(options);
		CompilationUnit compUnit = (CompilationUnit) parser.createAST(null);

		if (compUnit.getProblems() != null && compUnit.getProblems().length > 0) {
			boolean broken = false;
			IProblem reason = null;
			for (IProblem problem : compUnit.getProblems()) {
				if (problem.isError()) {
					broken = true;
					reason = problem;
					break;
				}
			}

			if (broken) {
				List types = compUnit.types();
				if (types.isEmpty()) {
					System.out.println("Compile error: " + reason.getMessage() + ", context is " + getContext());
				} else {
					System.out.println("Compile error - " + ((TypeDeclaration) types.get(0)).getName() + ": "
							+ reason.getMessage());
				}
				return null;
			}
		}

		return compUnit;
	}

}