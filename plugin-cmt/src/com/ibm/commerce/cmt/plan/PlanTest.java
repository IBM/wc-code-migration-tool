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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.commerce.cmt.Configuration;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.FileContents;
import com.ibm.commerce.cmt.MigrationTestCase;

/**
 * This class tests the {@link Plan} class.
 * 
 * @author Trent Hoeppner
 */
public class PlanTest extends MigrationTestCase {

	protected void setUp() throws Exception {
		super.setUp();
		parentDir = new File("testData\\classRefCmd");
	}

	/**
	 * Tests that if the class name is found in the imports of a target file it
	 * will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfClassInImportsFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test1Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
	}

	/**
	 * Tests that if the full class name is found when defining a variable of a
	 * target file it will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInVariableDeclFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test2Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkVariableDeclaration(compUnit, "run", "javax.ejb.FinderException", true);
		checkVariableDeclaration(compUnit, "run", "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkVariableDeclaration(afterCompUnit, "run", "javax.ejb.FinderException", false);
		checkVariableDeclaration(afterCompUnit, "run", "java.net.HelloException", true);
	}

	/**
	 * Tests that if the partial class name is found when defining a variable of
	 * a target file it will be replaced with the partial target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInVariableDeclFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test3Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkVariableDeclaration(compUnit, "run", "FinderException", true);
		checkVariableDeclaration(compUnit, "run", "HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkVariableDeclaration(afterCompUnit, "run", "FinderException", false);
		checkVariableDeclaration(afterCompUnit, "run", "HelloException", true);
	}

	/**
	 * Tests that if the full class name is found when defining a method
	 * parameter of a target file it will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInMethodParamDeclFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test4Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkMethodParamDeclaration(compUnit, "run", "javax.ejb.FinderException", true);
		checkMethodParamDeclaration(compUnit, "run", "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkMethodParamDeclaration(afterCompUnit, "run", "javax.ejb.FinderException", false);
		checkMethodParamDeclaration(afterCompUnit, "run", "java.net.HelloException", true);
	}

	/**
	 * Tests that if the partial class name is found when defining a method
	 * parameter of a target file it will be replaced with the partial target
	 * class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInMethodParamDeclFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test5Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkMethodParamDeclaration(compUnit, "run", "FinderException", true);
		checkMethodParamDeclaration(compUnit, "run", "HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkMethodParamDeclaration(afterCompUnit, "run", "FinderException", false);
		checkMethodParamDeclaration(afterCompUnit, "run", "HelloException", true);
	}

	/**
	 * Tests that if the full class name is found when defining a catch clause
	 * of a target file it will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInCatchClauseFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test6Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkCatchClause(compUnit, "run", "javax.ejb.FinderException", true);
		checkCatchClause(compUnit, "run", "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkCatchClause(afterCompUnit, "run", "javax.ejb.FinderException", false);
		checkCatchClause(afterCompUnit, "run", "java.net.HelloException", true);
	}

	/**
	 * Tests that if the partial class name is found when defining a catch
	 * clause of a target file it will be replaced with the partial target class
	 * name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInCatchClauseFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test7Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkCatchClause(compUnit, "run", "FinderException", true);
		checkCatchClause(compUnit, "run", "HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkCatchClause(afterCompUnit, "run", "FinderException", false);
		checkCatchClause(afterCompUnit, "run", "HelloException", true);
	}

	/**
	 * Tests that if the full class name is found when defining a field of a
	 * target file it will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInFieldDeclFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test8Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkFieldDeclaration(compUnit, "javax.ejb.FinderException", true);
		checkFieldDeclaration(compUnit, "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkFieldDeclaration(afterCompUnit, "javax.ejb.FinderException", false);
		checkFieldDeclaration(afterCompUnit, "java.net.HelloException", true);
	}

	/**
	 * Tests that if the partial class name is found when defining a field of a
	 * target file it will be replaced with the partial target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInFieldDeclFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test9Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkFieldDeclaration(compUnit, "FinderException", true);
		checkFieldDeclaration(compUnit, "HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkFieldDeclaration(afterCompUnit, "FinderException", false);
		checkFieldDeclaration(afterCompUnit, "HelloException", true);
	}

	/**
	 * Tests that if the full class name is found when defining a throws clause
	 * of a target file it will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInThrowsClauseFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test10Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkThrowsClause(compUnit, "run", "javax.ejb.FinderException", true);
		checkThrowsClause(compUnit, "run", "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkThrowsClause(afterCompUnit, "run", "javax.ejb.FinderException", false);
		checkThrowsClause(afterCompUnit, "run", "java.net.HelloException", true);
	}

	/**
	 * Tests that if the partial class name is found when defining a throws
	 * clause of a target file it will be replaced with the partial target class
	 * name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInThrowsClauseFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test11Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkThrowsClause(compUnit, "run", "FinderException", true);
		checkThrowsClause(compUnit, "run", "HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkThrowsClause(afterCompUnit, "run", "FinderException", false);
		checkThrowsClause(afterCompUnit, "run", "HelloException", true);
	}

	/**
	 * Tests that if the full class name is found when defining a type parameter
	 * for a class of a target file it will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInClassTypeParameterFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test12Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkClassTypeParam(compUnit, "javax.ejb.FinderException", true);
		checkClassTypeParam(compUnit, "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkClassTypeParam(afterCompUnit, "javax.ejb.FinderException", false);
		checkClassTypeParam(afterCompUnit, "java.net.HelloException", true);
	}

	/**
	 * Tests that if the partial class name is found when defining a type
	 * parameter of a class of a target file it will be replaced with the
	 * partial target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInClassTypeParameterFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test13Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkClassTypeParam(compUnit, "FinderException", true);
		checkClassTypeParam(compUnit, "HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkClassTypeParam(afterCompUnit, "FinderException", false);
		checkClassTypeParam(afterCompUnit, "HelloException", true);
	}

	/**
	 * Tests that if the full class name is found when defining a type parameter
	 * for a method of a target file it will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInMethodTypeParameterFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test14Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkMethodTypeParam(compUnit, "run", "javax.ejb.FinderException", true);
		checkMethodTypeParam(compUnit, "run", "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkMethodTypeParam(afterCompUnit, "run", "javax.ejb.FinderException", false);
		checkMethodTypeParam(afterCompUnit, "run", "java.net.HelloException", true);
	}

	/**
	 * Tests that if the partial class name is found when defining a type
	 * parameter of a method of a target file it will be replaced with the
	 * partial target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInMethodTypeParameterFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test15Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkMethodTypeParam(compUnit, "run", "FinderException", true);
		checkMethodTypeParam(compUnit, "run", "HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkMethodTypeParam(afterCompUnit, "run", "FinderException", false);
		checkMethodTypeParam(afterCompUnit, "run", "HelloException", true);
	}

	/**
	 * Tests that if the full class name is found when extending a superclass in
	 * a target file it will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInExtendedClassFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test16Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkExtendedClass(compUnit, "javax.ejb.FinderException", true);
		checkExtendedClass(compUnit, "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkExtendedClass(afterCompUnit, "javax.ejb.FinderException", false);
		checkExtendedClass(afterCompUnit, "java.net.HelloException", true);
	}

	/**
	 * Tests that if the partial class name is found when extending a superclass
	 * in a target file it will be replaced with the partial target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInExtendedClassFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test17Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkExtendedClass(compUnit, "FinderException", true);
		checkExtendedClass(compUnit, "HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkExtendedClass(afterCompUnit, "FinderException", false);
		checkExtendedClass(afterCompUnit, "HelloException", true);
	}

	/**
	 * Tests that if the full class name is found when implementing an interface
	 * in a target file it will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInImplementedInterfaceFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test18Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkImplementedInterface(compUnit, "javax.ejb.FinderException", true);
		checkImplementedInterface(compUnit, "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkImplementedInterface(afterCompUnit, "javax.ejb.FinderException", false);
		checkImplementedInterface(afterCompUnit, "java.net.HelloException", true);
	}

	/**
	 * Tests that if the partial class name is found when implementing an
	 * interface in a target file it will be replaced with the partial target
	 * class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInImplementedInterfaceFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test19Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkImplementedInterface(compUnit, "FinderException", true);
		checkImplementedInterface(compUnit, "HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkImplementedInterface(afterCompUnit, "FinderException", false);
		checkImplementedInterface(afterCompUnit, "HelloException", true);
	}

	/**
	 * Tests that if the full class name is found when type casting in a target
	 * file it will be replaced.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfFullClassInTypeCastFoundExpectReplaced() throws Exception {
		File file = prepareTestFile("Test20Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkTypeCast(compUnit, "run", "javax.ejb.FinderException", true);
		checkTypeCast(compUnit, "run", "java.net.HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkTypeCast(afterCompUnit, "run", "javax.ejb.FinderException", false);
		checkTypeCast(afterCompUnit, "run", "java.net.HelloException", true);
	}

	/**
	 * Tests that if the partial class name is found when type casting in a
	 * target file it will be replaced with the partial target class name.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testExecuteIfPartialClassInTypeCastFoundExpectReplacedWithPartial() throws Exception {
		File file = prepareTestFile("Test21Java.txt");
		FileContents contents = new FileContents(file);
		contents.load();
		CompilationUnit compUnit = loadTestFile(file);

		checkImport(compUnit, "javax.ejb.FinderException", true);
		checkImport(compUnit, "java.net.HelloException", false);
		checkTypeCast(compUnit, "run", "FinderException", true);
		checkTypeCast(compUnit, "run", "HelloException", false);

		List<File> commandFileList = Arrays.asList(new File("testData\\commandClassRef.txt"));
		List<File> sourceDirList = Arrays.asList(new File("testData\\classRefCmd"));
		Configuration config = new Configuration(commandFileList, sourceDirList, null);
		config.load();
		Context context = new Context(new IDGenerator(1));
		context.set(Context.Prop.FILE, file);
		context.set(Context.Prop.LOG_WRITER, logWriter);

		Plan plan = new Plan();
		config.getPatterns().get(0).findInCurrentFileForPlan(context, plan);
		plan.execute(config, context);

		CompilationUnit afterCompUnit = loadTestFile(file);
		checkImport(afterCompUnit, "javax.ejb.FinderException", false);
		checkImport(afterCompUnit, "java.net.HelloException", true);
		checkTypeCast(afterCompUnit, "run", "FinderException", false);
		checkTypeCast(afterCompUnit, "run", "HelloException", true);
	}
}
