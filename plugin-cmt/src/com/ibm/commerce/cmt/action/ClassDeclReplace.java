package com.ibm.commerce.cmt.action;

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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.Context;
import com.ibm.commerce.cmt.FileContents;
import com.ibm.commerce.cmt.Param;
import com.ibm.commerce.cmt.plan.Issue;
import com.ibm.commerce.cmt.plan.LogStep;
import com.ibm.commerce.cmt.plan.Range;
import com.ibm.commerce.cmt.plan.RenameFileStep;
import com.ibm.commerce.cmt.plan.ReplaceInFileStep;
import com.ibm.commerce.cmt.plan.Step;
import com.ibm.commerce.cmt.search.SearchResult;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemUtil;

/**
 * This class supports removing class references in Java source code.
 * 
 * @author Trent Hoeppner
 */
public class ClassDeclReplace implements Action {

	private String textContents;

	private List<Param> replaceParams;

	public ClassDeclReplace(String textContents, List<Param> replaceParams) {
		this.textContents = textContents;
		this.replaceParams = replaceParams;
	}

	@Override
	public String getName() {
		return "classDeclReplace";
	}

	@Override
	public String getData() {
		return textContents;
	}

	@Override
	public List<? extends Param> getSubParams() {
		return replaceParams;
	}

	@Override
	public Node toXML(Document doc) {
		Element classRefParam = doc.createElement("replace");
		if (!replaceParams.isEmpty()) {
			for (Param subParam : replaceParams) {
				Node subParamNode = subParam.toXML(doc);
				classRefParam.appendChild(subParamNode);
			}
		} else {
			classRefParam.setTextContent(textContents);
		}

		return classRefParam;
	}

	@Override
	public List<Step> getSteps(Context context) {
		SearchResult<ASTNode> current = context.get(Context.Prop.SEARCH_RESULT);
		Check.notNull(current, "context SEARCH_RESULT");

		ASTNode data = current.getDataObject();
		ChangeVisitor visitor;
		if (replaceParams.size() > 0) {
			RegexReplaceParam replaceParam = (RegexReplaceParam) replaceParams.get(0);
			visitor = new ChangeVisitor(context, replaceParam);
		} else {
			StringReplaceParam param = new StringReplaceParam(getData());
			visitor = new ChangeVisitor(context, param);
		}
		data.accept(visitor);

		List<Step> steps = new ArrayList<>();
		if (visitor.renameFile) {
			// it's a top-level class
			steps.add(new ReplaceInFileStep(visitor.classTextReplacement));
			steps.add(new RenameFileStep(visitor.file, visitor.targetFile));
		} else {
			steps.add(new LogStep("Warning: Could not safely rename this class declaration."));
		}

		return steps;
	}

	private final class ChangeVisitor extends ASTVisitor {
		private Context context;

		private ReplaceParam<String> replaceParam;

		private Range range;

		private File file;

		private boolean renameFile;

		private File targetFile;

		private String classTextReplacement;

		public ChangeVisitor(Context context, ReplaceParam<String> replaceParam) {
			this.context = context;
			this.replaceParam = replaceParam;

			Issue issue = context.get(Context.Prop.ISSUE);
			file = new File(issue.getLocation().getFile());
			range = issue.getLocation().getRange();
		}

		@Override
		public boolean visit(SimpleName node) {
			TypeDeclaration typeDeclaration = (TypeDeclaration) node.getParent();

			JavaItem javaClass = JavaItemUtil.findJavaClass(context, typeDeclaration);
			if (javaClass == null) {
				// TODO this happens for JSP generated classes
				return super.visit(node);
			}

			String fullDeclared = JavaItemUtil.getFullClassNameForType(javaClass);

			renameFile = typeDeclaration.isPackageMemberTypeDeclaration();
			if (renameFile) {
				String replacement = replaceParam.findReplacement(context, fullDeclared);
				String newPackage;
				String newName;
				int newLastDot = replacement.lastIndexOf('.');
				if (newLastDot >= 0) {
					newPackage = replacement.substring(0, newLastDot);
					newName = replacement.substring(newLastDot + 1);
				} else {
					newPackage = "";
					newName = replacement;
				}

				String oldPackage = javaClass.getParent().getName();
				if (!oldPackage.equals(newPackage)) {
					// change the package as well as the class name
					CompilationUnit compUnit = context.get(Context.Prop.COMP_UNIT);
					FileContents fileContents = context.get(Context.Prop.FILE_CONTENTS);
					PackageDeclaration packageDecl = compUnit.getPackage();
					String baseString;
					if (packageDecl == null) {
						range.setStart(0);
						baseString = fileContents.getSubstring(range);
						baseString = "package " + newPackage + ";" + System.getProperty("line.separator") + baseString;
					} else {
						range.setStart(packageDecl.getName().getStartPosition());
						baseString = fileContents.getSubstring(range);
						int endOfPackageNameIndex = oldPackage.length();
						baseString = newPackage + baseString.substring(endOfPackageNameIndex);
					}

					// replace the class name at the end
					int classNameStartIndex = baseString.length() - javaClass.getName().length();
					classTextReplacement = baseString.substring(0, classNameStartIndex) + newName;
				} else {
					// just change the class name
					classTextReplacement = newName;
				}

				String fullFilePath = file.getParentFile().getAbsolutePath().replace('\\', '/');
				String packageAsRelativePath = oldPackage.replace('.', '/');
				String packageParentDirName = findPackageParentDir(fullFilePath, packageAsRelativePath);

				String newPackageAsRelativePath;
				if (!newPackage.isEmpty()) {
					newPackageAsRelativePath = '/' + newPackage.replace('.', '/') + '/';
				} else {
					newPackageAsRelativePath = "/";
				}

				String classParentDir = packageParentDirName + newPackageAsRelativePath;
				targetFile = new File(classParentDir, newName + ".java");
			}

			return super.visit(node);
		}

		private String findPackageParentDir(String fullFilePath, String packageAsRelativePath) {
			if (packageAsRelativePath.isEmpty()) {
				return fullFilePath;
			}

			int lastFullPathSeparatorIndex = fullFilePath.lastIndexOf('/');
			int lastPackageSeparatorIndex = packageAsRelativePath.lastIndexOf('/');

			String packagePathPart;
			boolean isLastPackagePart = lastPackageSeparatorIndex < 0;
			if (isLastPackagePart) {
				// this is the last part of the package
				packagePathPart = packageAsRelativePath;
			} else {
				packagePathPart = packageAsRelativePath.substring(lastPackageSeparatorIndex + 1);
			}

			String fullPathPart = fullFilePath.substring(lastFullPathSeparatorIndex + 1);

			if (!fullPathPart.equals(packagePathPart)) {
				// something is wrong, the package name doesn't match the
				// directory name
				throw new IllegalStateException(
						"package " + packageAsRelativePath + " does not align with the directory: " + fullFilePath);
			}

			String fullFilePathParent = fullFilePath.substring(0, lastFullPathSeparatorIndex);
			if (isLastPackagePart) {
				return fullFilePathParent;
			} else {
				String packageAsRelativePathParent = packageAsRelativePath.substring(0, lastPackageSeparatorIndex);
				return findPackageParentDir(fullFilePathParent, packageAsRelativePathParent);
			}
		}

	}

}
