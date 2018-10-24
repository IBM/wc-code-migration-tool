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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;

import com.ibm.commerce.qcheck.core.ExternalValidatorResource;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.comment.Comment;

import junit.framework.TestCase;

/**
 * ValidatorTestBase is a base class for testing validators. This class does
 * initial set-up and tear-down of tests for common structures needed for the
 * TestClass.java.
 * 
 * @author Trent Hoeppner
 */
public abstract class ValidatorTestBase extends TestCase {

	/**
	 * The comments available in TestClass.java.
	 */
	List<Comment> comments;

	/**
	 * The resource that represents TestClass.java.
	 */
	ValidatorResource resource;

	/**
	 * The validator that will be run. This should be initialized in an
	 * overridden version of the {@link #setUp()} method.
	 */
	Validator validator;

	/**
	 * {@inheritDoc}
	 */
	public void setUp() throws Exception {
		resource = new ExternalValidatorResource(new File("testData\\TestClass.java"));
		List<ASTNode> javadocs = resource.getTypedNodeList(ASTNode.JAVADOC);

		comments = new ArrayList<Comment>();
		for (ASTNode node : javadocs) {
			CompilationUnit resourceCompUnit = ModelEnum.COMP_UNIT.getData(resource);
			Comment comment = new Comment(resource, (Javadoc) node, resourceCompUnit);
			comments.add(comment);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void tearDown() {
		comments = null;
		resource = null;
	}

}
