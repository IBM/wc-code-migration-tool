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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;

import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.MigrationTestCase;
import com.ibm.commerce.cmt.plan.IDGenerator;

/**
 * This class tests the {@link ClassRefParam} class.
 * 
 * @author Trent Hoeppner
 */
public class ClassRefParamTest extends MigrationTestCase {

	private Context context;

	protected void setUp() throws Exception {
		super.setUp();
		parentDir = new File("testData\\classRefCmd");
		context = new Context(new IDGenerator(1));
	}

	/**
	 * Tests that if the sub parameters list is null, an exception will be
	 * thrown.
	 */
	public void testConstructorIfSubParamsNullExpectException() {
		try {
			new ClassRefParam(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the sub parameters list is empty, an exception will be
	 * thrown.
	 */
	public void testConstructorIfSubParamsEmptyExpectException() {
		try {
			new ClassRefParam(Collections.emptyList());
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests that if a full class name is given, it can be retrieved.
	 */
	public void testConstructorIfSubParamsOkExpectCanBeRetrieved() {
		NameParam nameParam = new NameParam("name", "javax.ejb.FinderException", Collections.emptyList());
		ClassRefParam r = new ClassRefParam(Arrays.asList(nameParam));
		assertEquals("name is wrong.", "classRef", r.getName());
		assertEquals("data is wrong.", null, r.getData());
		assertEquals("sub params size is wrong.", 1, r.getSubParams().size());
		assertEquals("param 0 is wrong.", nameParam, r.getSubParams().get(0));
	}

	/**
	 * Tests that if the class name is found in the imports of a target file it
	 * will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfClassInImportsFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test1Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		ASTNodeIssueData issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the full class name is found when defining a variable of a
	 * target file it will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInVariableDeclFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test2Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkVariableDeclaration(compUnit, "run", "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the partial class name is found when defining a variable of
	 * a target file it will be found with the partial target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInVariableDeclFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test3Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkVariableDeclaration(compUnit, "run", "FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the full class name is found when defining a method
	 * parameter of a target file it will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInMethodParamDeclFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test4Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkMethodParamDeclaration(compUnit, "run", "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the partial class name is found when defining a method
	 * parameter of a target file it will be found with the partial target class
	 * name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInMethodParamDeclFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test5Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkMethodParamDeclaration(compUnit, "run", "FinderException", true);
		checkMethodParamDeclaration(compUnit, "run", "HelloException", false);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the full class name is found when defining a catch clause
	 * of a target file it will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInCatchClauseFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test6Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkCatchClause(compUnit, "run", "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the partial class name is found when defining a catch
	 * clause of a target file it will be found with the partial target class
	 * name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInCatchClauseFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test7Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkCatchClause(compUnit, "run", "FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the full class name is found when defining a field of a
	 * target file it will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInFieldDeclFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test8Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkFieldDeclaration(compUnit, "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the partial class name is found when defining a field of a
	 * target file it will be found with the partial target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInFieldDeclFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test9Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkFieldDeclaration(compUnit, "FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the full class name is found when defining a throws clause
	 * of a target file it will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInThrowsClauseFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test10Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkThrowsClause(compUnit, "run", "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the partial class name is found when defining a throws
	 * clause of a target file it will be found with the partial target class
	 * name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInThrowsClauseFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test11Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkThrowsClause(compUnit, "run", "FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the full class name is found when defining a type parameter
	 * for a class of a target file it will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInClassTypeParameterFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test12Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkClassTypeParam(compUnit, "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the partial class name is found when defining a type
	 * parameter of a class of a target file it will be found with the partial
	 * target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInClassTypeParameterFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test13Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkClassTypeParam(compUnit, "FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the full class name is found when defining a type parameter
	 * for a method of a target file it will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInMethodTypeParameterFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test14Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkMethodTypeParam(compUnit, "run", "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the partial class name is found when defining a type
	 * parameter of a method of a target file it will be found with the partial
	 * target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInMethodTypeParameterFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test15Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkMethodTypeParam(compUnit, "run", "FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the full class name is found when extending a superclass in
	 * a target file it will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInExtendedClassFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test16Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkExtendedClass(compUnit, "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the partial class name is found when extending a superclass
	 * in a target file it will be found with the partial target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInExtendedClassFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test17Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkExtendedClass(compUnit, "FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the full class name is found when implementing an interface
	 * in a target file it will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInImplementedInterfaceFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test18Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImplementedInterface(compUnit, "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the partial class name is found when implementing an
	 * interface in a target file it will be found with the partial target class
	 * name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInImplementedInterfaceFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test19Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImplementedInterface(compUnit, "FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the full class name is found when type casting in a target
	 * file it will be found.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInTypeCastFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test20Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkTypeCast(compUnit, "run", "javax.ejb.FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	/**
	 * Tests that if the partial class name is found when type casting in a
	 * target file it will be found with the partial target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInTypeCastFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test21Java.txt");
		CompilationUnit compUnit = loadTestFile(file);
		context.set(Context.Prop.FILE, file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkTypeCast(compUnit, "run", "FinderException", true);

		ClassRefParam r = createSimpleClassRefParam();
		List<ASTNodeIssueData> issueDatas = r.findAll(context);
		Iterator<ASTNodeIssueData> iterator = issueDatas.iterator();
		SearchResult<ASTNode> issueData;
		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), ImportDeclaration.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Missing at least one SearchResult", true, iterator.hasNext());
		issueData = iterator.next();
		checkNodeInstanceof(issueData.getDataObject(), SimpleType.class);

		assertEquals("Have an extra least one SearchResult", false, iterator.hasNext());
	}

	private ClassRefParam createSimpleClassRefParam() {
		return new ClassRefParam(
				Arrays.asList(new NameParam("name", "javax.ejb.FinderException", Collections.emptyList())));
	}

}
