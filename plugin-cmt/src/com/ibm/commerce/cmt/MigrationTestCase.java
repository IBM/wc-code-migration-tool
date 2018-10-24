package com.ibm.commerce.cmt;

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import junit.framework.TestCase;

/**
 * This is a base class for migration tests.
 * 
 * @author Trent Hoeppner
 */
public abstract class MigrationTestCase extends TestCase {

	protected File parentDir = new File(".");

	protected List<File> filesToDelete;

	protected BufferedWriter logWriter;

	protected void setUp() throws Exception {
		filesToDelete = new ArrayList<>();
		logWriter = new BufferedWriter(new CharArrayWriter());
	}

	protected void tearDown() throws Exception {
		for (File file : filesToDelete) {
			if (file.exists()) {
				boolean deleted = file.delete();
				if (!deleted) {
					System.out.println(
							"Warning: file " + file.getAbsolutePath() + " could not be deleted on tearDown().");
				}
			}
		}

		logWriter.close();
	}

	public MigrationTestCase() {
		super();
	}

	public MigrationTestCase(String name) {
		super(name);
	}

	protected void checkNodeInstanceof(ASTNode node, Class<?> nodeType) {
		assertEquals("node class is wrong.", nodeType, node.getClass());
	}

	protected void checkNodeHasAncestorInstanceof(ASTNode node, Class<?> ancestorNodeType, int ancestorDepth) {
		ASTNode parent = node;
		for (int i = 0; i < ancestorDepth && parent != null; i++) {
			parent = node.getParent();
		}

		assertEquals("ancestor's class at depth " + ancestorDepth + " is wrong.", ancestorNodeType, parent.getClass());
	}

	protected void checkClassTypeParam(CompilationUnit compUnit, String declClassName,
			boolean expectClassTypeParamExists) {
		CheckClassTypeParamVisitor visitor = new CheckClassTypeParamVisitor(declClassName);
		compUnit.accept(visitor);
		assertEquals("Class type parameter with class " + declClassName + " does not exist correctly.",
				expectClassTypeParamExists, visitor.hasDecl);
	}

	protected void checkExtendedClass(CompilationUnit compUnit, String declClassName,
			boolean expectExtendedClassExists) {
		CheckExtendedClassVisitor visitor = new CheckExtendedClassVisitor(declClassName);
		compUnit.accept(visitor);
		assertEquals("ExtendedClass with class " + declClassName + " does not exist correctly.",
				expectExtendedClassExists, visitor.hasDecl);
	}

	protected void checkImplementedInterface(CompilationUnit compUnit, String declClassName,
			boolean expectExtendedClassExists) {
		CheckImplementedInterfaceVisitor visitor = new CheckImplementedInterfaceVisitor(declClassName);
		compUnit.accept(visitor);
		assertEquals("ExtendedClass with class " + declClassName + " does not exist correctly.",
				expectExtendedClassExists, visitor.hasDecl);
	}

	protected void checkTypeCast(CompilationUnit compUnit, String methodName, String declClassName,
			boolean expectTypeCastExists) {
		CheckTypeCastVisitor visitor = new CheckTypeCastVisitor(methodName, declClassName);
		compUnit.accept(visitor);
		assertEquals(
				"Type cast for method " + methodName + " with class " + declClassName + " does not exist correctly.",
				expectTypeCastExists, visitor.hasDecl);
	}

	protected void checkMethodTypeParam(CompilationUnit compUnit, String methodName, String declClassName,
			boolean expectMethodTypeParamExists) {
		CheckMethodTypeParamVisitor visitor = new CheckMethodTypeParamVisitor(declClassName);
		compUnit.accept(visitor);
		assertEquals("Method type parameter for method " + methodName + " with class " + declClassName
				+ " does not exist correctly.", expectMethodTypeParamExists, visitor.hasDecl);
	}

	protected void checkFieldDeclaration(CompilationUnit compUnit, String declClassName,
			boolean expectFieldDeclExists) {
		CheckFieldDeclarationVisitor visitor = new CheckFieldDeclarationVisitor(declClassName);
		compUnit.accept(visitor);
		assertEquals("Field declaration with declaring class " + declClassName + " does not exist correctly.",
				expectFieldDeclExists, visitor.hasDecl);
	}

	protected void checkCatchClause(CompilationUnit compUnit, String methodName, String declClassName,
			boolean expectCatchClauseExists) {
		CheckCatchClauseVisitor visitor = new CheckCatchClauseVisitor(methodName, declClassName);
		compUnit.accept(visitor);
		assertEquals("Catch clause exception declaration in method " + methodName + " with declaring class "
				+ declClassName + " does not exist correctly.", expectCatchClauseExists, visitor.hasDecl);
	}

	protected void checkThrowsClause(CompilationUnit compUnit, String methodName, String declClassName,
			boolean expectThrowsClauseExists) {
		CheckThrowsClauseVisitor visitor = new CheckThrowsClauseVisitor(methodName, declClassName);
		compUnit.accept(visitor);
		assertEquals("Throws exception declaration in method " + methodName + " with declaring class " + declClassName
				+ " does not exist correctly.", expectThrowsClauseExists, visitor.hasDecl);
	}

	protected void checkMethodParamDeclaration(CompilationUnit compUnit, String methodName, String declClassName,
			boolean expectMethodParamExists) {
		CheckMethodParamDeclarationVisitor visitor = new CheckMethodParamDeclarationVisitor(methodName, declClassName);
		compUnit.accept(visitor);
		assertEquals("Method parameter declaration in method " + methodName + " with declaring class " + declClassName
				+ " does not exist correctly.", expectMethodParamExists, visitor.hasDecl);
	}

	protected void checkVariableDeclaration(CompilationUnit compUnit, String methodName, String declClassName,
			boolean expectDeclClassNameExists) {
		CheckVariableDeclarationVisitor visitor = new CheckVariableDeclarationVisitor(methodName, declClassName);
		compUnit.accept(visitor);
		assertEquals("Variable declaration in method " + methodName + " with declaring class " + declClassName
				+ " does not exist correctly.", expectDeclClassNameExists, visitor.hasDecl);
	}

	protected void checkImport(CompilationUnit compUnit, String importClass, boolean expectImportExists) {
		CheckImportVisitor visitor = new CheckImportVisitor(importClass);
		compUnit.accept(visitor);
		assertEquals("import " + importClass + " does not exist correctly.", expectImportExists, visitor.hasImport);
	}

	protected CompilationUnit loadTestFile(File file) throws IOException {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		String stringForm = loadFile(file);
		parser.setSource(stringForm.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		CompilationUnit resourceCompUnit = (CompilationUnit) parser.createAST(null);
		return resourceCompUnit;
	}

	private String loadFile(File file) throws IOException {
		String stringDoc = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			StringBuffer data = new StringBuffer();
			char[] buf = new char[2048];
			int charsRead = reader.read(buf);
			while (charsRead >= 0) {
				data.append(buf, 0, charsRead);
				charsRead = reader.read(buf);
			}
			stringDoc = data.toString();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// swallow to allow the exception to escape
				}
			}
		}

		return stringDoc;
	}

	protected File prepareTestFile(String filename) throws IOException {
		File originalFile = new File(parentDir, filename);
		File copyFile = new File(parentDir, filename + ".java");

		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try {
			byte[] buf = new byte[4096];
			in = new BufferedInputStream(new FileInputStream(originalFile));
			out = new BufferedOutputStream(new FileOutputStream(copyFile));
			int length = in.read(buf);
			while (length >= 0) {
				out.write(buf, 0, length);
				length = in.read(buf);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// swallow to allow main exception to escape
				}
			}

			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// swallow to allow main exception to escape
				}
			}
		}

		// add to files to delete, at the beginning of the list, so the last
		// file is deleted first
		filesToDelete.add(0, copyFile);

		return copyFile;
	}

	private boolean checkType(Type type, String targetDeclClassName) {
		boolean match = false;

		String declared = getDeclaredClassString(type);

		if (declared != null && declared.equals(targetDeclClassName)) {
			match = true;
		}

		return match;
	}

	@SuppressWarnings("unchecked")
	private boolean checkTypeParam(TypeParameter typeParam, String targetDeclClassName) {
		boolean match = false;

		List<Type> extendsTypes = typeParam.typeBounds();
		for (Type type : extendsTypes) {
			String declared = getDeclaredClassString(type);

			if (declared != null && declared.equals(targetDeclClassName)) {
				match = true;
				break;
			}
		}

		return match;
	}

	private String getDeclaredClassString(Type type) {
		String declared = null;
		if (type.isQualifiedType()) {
			QualifiedType t = (QualifiedType) type;
			declared = t.getName().getFullyQualifiedName();
		} else if (type.isNameQualifiedType()) {
			NameQualifiedType t = (NameQualifiedType) type;
			declared = t.getName().getFullyQualifiedName();
		} else if (type.isSimpleType()) {
			SimpleType t = (SimpleType) type;
			declared = t.getName().getFullyQualifiedName();
		}
		return declared;
	}

	private class CheckImportVisitor extends ASTVisitor {
		private String importClass;

		boolean hasImport;

		public CheckImportVisitor(String importClass) {
			this.importClass = importClass;
		}

		@Override
		public boolean visit(ImportDeclaration node) {
			String imported = node.getName().getFullyQualifiedName();
			if (imported.equals(importClass)) {
				hasImport = true;
			}

			return super.visit(node);
		}
	}

	private class CheckVariableDeclarationVisitor extends ASTVisitor {

		boolean hasDecl;

		private String methodName;

		private String declClassName;

		public CheckVariableDeclarationVisitor(String methodName, String declClassName) {
			this.methodName = methodName;
			this.declClassName = declClassName;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			if (node.getName().getFullyQualifiedName().equals(methodName)) {
				// this means visit the node's children
				return true;
			} else {
				// don't visit the node's children
				return false;
			}
		}

		@Override
		public boolean visit(VariableDeclarationStatement node) {
			hasDecl = checkType(node.getType(), declClassName);

			return super.visit(node);
		}

		@Override
		public boolean visit(VariableDeclarationExpression node) {
			hasDecl = checkType(node.getType(), declClassName);

			return super.visit(node);
		}

		@Override
		public boolean visit(SingleVariableDeclaration node) {
			hasDecl = checkType(node.getType(), declClassName);

			return super.visit(node);
		}
	}

	private class CheckFieldDeclarationVisitor extends ASTVisitor {

		boolean hasDecl;

		private String declClassName;

		public CheckFieldDeclarationVisitor(String declClassName) {
			this.declClassName = declClassName;
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			hasDecl = checkType(node.getType(), declClassName);

			return super.visit(node);
		}
	}

	private class CheckClassTypeParamVisitor extends ASTVisitor {

		boolean hasDecl;

		private String declClassName;

		public CheckClassTypeParamVisitor(String declClassName) {
			this.declClassName = declClassName;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean visit(TypeDeclaration node) {
			List<TypeParameter> thrownTypes = node.typeParameters();
			for (TypeParameter typeParam : thrownTypes) {
				if (checkTypeParam(typeParam, declClassName)) {
					hasDecl = true;
					break;
				}
			}

			return super.visit(node);
		}
	}

	private class CheckExtendedClassVisitor extends ASTVisitor {

		boolean hasDecl;

		private String declClassName;

		public CheckExtendedClassVisitor(String declClassName) {
			this.declClassName = declClassName;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			Type type = node.getSuperclassType();
			hasDecl = checkType(type, declClassName);

			return super.visit(node);
		}
	}

	private class CheckImplementedInterfaceVisitor extends ASTVisitor {

		boolean hasDecl;

		private String declClassName;

		public CheckImplementedInterfaceVisitor(String declClassName) {
			this.declClassName = declClassName;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean visit(TypeDeclaration node) {
			List<Type> types = node.superInterfaceTypes();
			for (Type type : types) {
				if (checkType(type, declClassName)) {
					hasDecl = true;
					break;
				}
			}

			return super.visit(node);
		}
	}

	private class CheckMethodTypeParamVisitor extends ASTVisitor {

		boolean hasDecl;

		private String declClassName;

		public CheckMethodTypeParamVisitor(String declClassName) {
			this.declClassName = declClassName;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean visit(MethodDeclaration node) {
			List<TypeParameter> thrownTypes = node.typeParameters();
			for (TypeParameter typeParam : thrownTypes) {
				if (checkTypeParam(typeParam, declClassName)) {
					hasDecl = true;
					break;
				}
			}

			return super.visit(node);
		}
	}

	private class CheckMethodParamDeclarationVisitor extends ASTVisitor {

		public boolean hasDecl;

		private String methodName;

		private String declClassName;

		public CheckMethodParamDeclarationVisitor(String methodName, String declClassName) {
			this.methodName = methodName;
			this.declClassName = declClassName;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean visit(MethodDeclaration node) {
			if (node.getName().getFullyQualifiedName().equals(methodName)) {
				List<SingleVariableDeclaration> parameters = node.parameters();
				for (SingleVariableDeclaration decl : parameters) {
					if (checkType(decl.getType(), declClassName)) {
						hasDecl = true;
						break;
					}
				}
			}

			return super.visit(node);
		}

	}

	private class CheckCatchClauseVisitor extends ASTVisitor {

		public boolean hasDecl;

		private String methodName;

		private String declClassName;

		public CheckCatchClauseVisitor(String methodName, String declClassName) {
			this.methodName = methodName;
			this.declClassName = declClassName;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			if (node.getName().getFullyQualifiedName().equals(methodName)) {
				// this means visit the node's children
				return true;
			} else {
				// don't visit the node's children
				return false;
			}
		}

		@Override
		public boolean visit(CatchClause node) {
			hasDecl = checkType(node.getException().getType(), declClassName);
			return super.visit(node);
		}

	}

	private class CheckTypeCastVisitor extends ASTVisitor {

		public boolean hasDecl;

		private String methodName;

		private String declClassName;

		public CheckTypeCastVisitor(String methodName, String declClassName) {
			this.methodName = methodName;
			this.declClassName = declClassName;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			if (node.getName().getFullyQualifiedName().equals(methodName)) {
				// this means visit the node's children
				return true;
			} else {
				// don't visit the node's children
				return false;
			}
		}

		@Override
		public boolean visit(CastExpression node) {
			hasDecl = checkType(node.getType(), declClassName);
			return super.visit(node);
		}

	}

	private class CheckThrowsClauseVisitor extends ASTVisitor {

		public boolean hasDecl;

		private String methodName;

		private String declClassName;

		public CheckThrowsClauseVisitor(String methodName, String declClassName) {
			this.methodName = methodName;
			this.declClassName = declClassName;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean visit(MethodDeclaration node) {
			if (node.getName().getFullyQualifiedName().equals(methodName)) {
				List<Type> thrownTypes = node.thrownExceptionTypes();
				for (Type type : thrownTypes) {
					if (checkType(type, declClassName)) {
						hasDecl = true;
						break;
					}
				}
			}

			return super.visit(node);
		}

	}

}