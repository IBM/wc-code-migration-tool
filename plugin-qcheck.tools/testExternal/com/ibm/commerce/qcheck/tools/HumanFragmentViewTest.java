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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.ibm.commerce.qcheck.core.ExternalValidatorResource;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.comment.Comment;
import com.ibm.commerce.qcheck.core.comment.CommentDescription;
import com.ibm.commerce.qcheck.core.comment.HumanFragmentView;

import junit.framework.TestCase;

/**
 * This class tests the {@link HumanFragmentView} class.
 * 
 * @author Trent Hoeppner
 */
public class HumanFragmentViewTest extends TestCase {

	/**
	 * The comments available in TestClass.java.
	 */
	private Map<String, CommentDescription> methodNameToMainDescriptionMap;

	/**
	 * {@inheritDoc}
	 */
	public void setUp() throws Exception {
		ValidatorResource resource = new ExternalValidatorResource(
				new File("testData\\HumanFragmentViewTestClass.java"));
		List<ASTNode> javadocs = resource.getTypedNodeList(ASTNode.JAVADOC);

		methodNameToMainDescriptionMap = new HashMap<String, CommentDescription>();
		for (ASTNode node : javadocs) {
			CompilationUnit resourceCompUnit = ModelEnum.COMP_UNIT.getData(resource);
			Comment comment = new Comment(resource, (Javadoc) node, resourceCompUnit);
			if (node.getParent() instanceof MethodDeclaration) {
				String methodName = ((MethodDeclaration) node.getParent()).getName().getFullyQualifiedName();
				methodNameToMainDescriptionMap.put(methodName, comment.getDescription());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void tearDown() {
		methodNameToMainDescriptionMap = null;
	}

	public void testGetHumanView1() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testGetHumanView1");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("The human view.", humanView);
	}

	public void testGetHumanView2() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testGetHumanView2");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("The human view with code.", humanView);
	}

	public void testGetHumanView3() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testGetHumanView3");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("The human view with a link.", humanView);
	}

	public void testGetHumanView4() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testGetHumanView4");
		HumanFragmentView view = new HumanFragmentView(25, 24, desc);
		String humanView = view.getHumanView(false);
		assertEquals("portion of a human view.", humanView);
	}

	public void testGetHumanView5() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testGetHumanView5");
		HumanFragmentView view = new HumanFragmentView(22, 2, desc);
		String humanView = view.getHumanView(false);
		assertEquals(" \r\n", humanView);
	}

	public void testGetSourceView1() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testGetSourceView1");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String sourceView = view.getSourceView(false);
		assertEquals("The source view.", sourceView);
	}

	public void testGetSourceView2() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testGetSourceView2");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String sourceView = view.getSourceView(false);
		assertEquals("The source view with <code>code</code>.", sourceView);
	}

	public void testGetSourceView3() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testGetSourceView3");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String sourceView = view.getSourceView(false);
		assertEquals("The source view with a {@link #testGetSourceView2 link}.", sourceView);
	}

	public void testGetSourceView4() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testGetSourceView4");
		HumanFragmentView view = new HumanFragmentView(25, 25, desc);
		String sourceView = view.getSourceView(false);
		assertEquals("portion of a source view.", sourceView);
	}

	public void testGetSourceView5() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testGetSourceView5");
		HumanFragmentView view = new HumanFragmentView(22, 2, desc);
		String sourceView = view.getSourceView(false);
		assertEquals("  {@link\r\n", sourceView);
	}

	public void testExpand1() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testExpand1");
		HumanFragmentView view = new HumanFragmentView(37, 9, desc);
		String humanView = view.getHumanView(false);
		assertEquals("this text", humanView);

		view.expand(2, true);

		String modifiedHumanView = view.getHumanView(false);
		assertEquals("->this text", modifiedHumanView);

		view.expand(2, false);

		modifiedHumanView = view.getHumanView(false);
		assertEquals("->this text<-", modifiedHumanView);
	}

	public void testExpand2() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testExpand2");
		HumanFragmentView view = new HumanFragmentView(38, 9, desc);
		String humanView = view.getHumanView(false);
		assertEquals("this text", humanView);

		view.expand(8, true);

		String modifiedHumanView = view.getHumanView(false);
		assertEquals("from\r\n->this text", modifiedHumanView);
	}

	public void testDeleteChar1() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testDeleteChar1");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests deleting one whitespace .", humanView);

		view.deleteInHumanReadableString(29, true, true);

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests deleting one whitespace.", modifiedHumanView);
	}

	public void testDeleteChar2() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testDeleteChar2");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests deleting one out of two whitespaces  .", humanView);

		view.deleteInHumanReadableString(41, true, true);

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests deleting one out of two whitespaces .", modifiedHumanView);
	}

	public void testDeleteChar3() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testDeleteChar3");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests deleting a whitespace in a link .", humanView);

		view.deleteInHumanReadableString(37, true, true);

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests deleting a whitespace in a link.", modifiedHumanView);

		String modifiedSourceView = view.getSourceView(true);
		assertEquals("Tests deleting a whitespace in a {@link #testDeleteChar2() link}.", modifiedSourceView);
	}

	public void testDeleteChar4() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testDeleteChar4");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests deleting a whitespace in a link  when\r\nthere is a whitespace after too.", humanView);

		view.deleteInHumanReadableString(37, true, true);

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests deleting a whitespace in a link when\r\nthere is a whitespace after too.",
				modifiedHumanView);

		String modifiedSourceView = view.getSourceView(true);
		assertEquals(
				"Tests deleting a whitespace in a {@link #testDeleteChar2() link} when\r\n     * there is a whitespace after too.",
				modifiedSourceView);
	}

	public void testDeleteChar5() {
		// try deleting the first char of \r\n
		CommentDescription desc = methodNameToMainDescriptionMap.get("testDeleteChar5");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests deleting an\r\nend-of-line.", humanView);

		view.deleteInHumanReadableString(17, true, true);

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests deleting anend-of-line.", modifiedHumanView);

		String modifiedSourceView = view.getSourceView(true);
		assertEquals("Tests deleting anend-of-line.", modifiedSourceView);

		// try deleting the second char of \r\n
		desc = methodNameToMainDescriptionMap.get("testDeleteChar5");
		view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		humanView = view.getHumanView(false);
		assertEquals("Tests deleting an\r\nend-of-line.", humanView);

		view.deleteInHumanReadableString(18, true, true);

		modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests deleting anend-of-line.", modifiedHumanView);

		modifiedSourceView = view.getSourceView(true);
		assertEquals("Tests deleting anend-of-line.", modifiedSourceView);
	}

	public void testHandleDiff1() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testHandleDiff1");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests replacing a normal char.", humanView);

		view.handleDiff("Tests replacing 1 normal char.");

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests replacing 1 normal char.", modifiedHumanView);

		String modifiedSourceView = view.getSourceView(true);
		assertEquals("Tests replacing 1 normal char.", modifiedSourceView);
	}

	public void testHandleDiff2() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testHandleDiff2");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests replacing the last char.", humanView);

		view.handleDiff("Tests replacing the last char!");

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests replacing the last char!", modifiedHumanView);

		String modifiedSourceView = view.getSourceView(true);
		assertEquals("Tests replacing the last char!", modifiedSourceView);
	}

	public void testHandleDiff3() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testHandleDiff3");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests replacing the first char.", humanView);

		view.handleDiff("Jests replacing the first char.");

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Jests replacing the first char.", modifiedHumanView);

		String modifiedSourceView = view.getSourceView(true);
		assertEquals("Jests replacing the first char.", modifiedSourceView);
	}

	public void testHandleDiff4() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testHandleDiff4");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests replacing a word.", humanView);

		view.handleDiff("Tests replacing two or three word.");

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests replacing two or three word.", modifiedHumanView);

		String modifiedSourceView = view.getSourceView(true);
		assertEquals("Tests replacing two or three word.", modifiedSourceView);
	}

	public void testHandleDiff5() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testHandleDiff5");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests replacing a word with some code.", humanView);

		view.handleDiff("Tests replacing some code.");

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests replacing some code.", modifiedHumanView);

		String modifiedSourceView = view.getSourceView(true);
		assertEquals("Tests replacing<code></code> some code.", modifiedSourceView);
	}

	public void testHandleDiff6() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testHandleDiff6");
		HumanFragmentView view = new HumanFragmentView(0, desc.getHumanReadableString().length(), desc);
		String humanView = view.getHumanView(false);
		assertEquals("Tests removing a\r\nline ending.", humanView);

		view.handleDiff("Tests removing aline ending.");

		String modifiedHumanView = view.getHumanView(true);
		assertEquals("Tests removing aline ending.", modifiedHumanView);

		String modifiedSourceView = view.getSourceView(true);
		assertEquals("Tests removing aline ending.", modifiedSourceView);
	}

	public void testHandleDiff7() {
		CommentDescription desc = methodNameToMainDescriptionMap.get("testHandleDiff7");
		HumanFragmentView view = new HumanFragmentView(59, 3, desc);
		String humanView = view.getHumanView(false);
		assertEquals("\r\n.", humanView);

		view.handleDiff(".");

		String modifiedHumanView = view.getHumanView(true);
		assertEquals(".", modifiedHumanView);

		String modifiedSourceView = view.getSourceView(true);
		assertEquals(".", modifiedSourceView);
	}

}
