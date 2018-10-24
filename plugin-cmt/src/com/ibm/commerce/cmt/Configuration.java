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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.commerce.cmt.action.Action;
import com.ibm.commerce.cmt.action.ClassDeclRemove;
import com.ibm.commerce.cmt.action.ClassDeclReplace;
import com.ibm.commerce.cmt.action.ClassRefRemove;
import com.ibm.commerce.cmt.action.ClassRefReplace;
import com.ibm.commerce.cmt.action.MethodDeclRemove;
import com.ibm.commerce.cmt.action.MethodDeclReplace;
import com.ibm.commerce.cmt.action.MethodRefRemove;
import com.ibm.commerce.cmt.action.MethodRefReplace;
import com.ibm.commerce.cmt.action.RegexReplaceParam;
import com.ibm.commerce.cmt.action.Warning;
import com.ibm.commerce.cmt.plan.IDGenerator;
import com.ibm.commerce.cmt.search.AndParam;
import com.ibm.commerce.cmt.search.ClassDeclParam;
import com.ibm.commerce.cmt.search.ClassRefParam;
import com.ibm.commerce.cmt.search.HasMethodParam;
import com.ibm.commerce.cmt.search.HasParamParam;
import com.ibm.commerce.cmt.search.HasSupertypeParam;
import com.ibm.commerce.cmt.search.IsInMethodParam;
import com.ibm.commerce.cmt.search.IsSupertypeParam;
import com.ibm.commerce.cmt.search.MethodDeclParam;
import com.ibm.commerce.cmt.search.MethodRefParam;
import com.ibm.commerce.cmt.search.NameParam;
import com.ibm.commerce.cmt.search.NotParam;
import com.ibm.commerce.cmt.search.RegexSearchParam;
import com.ibm.commerce.cmt.search.SearchParam;

/**
 * This class contains information to identify the source text to replace.
 * 
 * @author Trent Hoeppner
 */
public class Configuration {

	/**
	 * The list of files that contain patterns that define the search/action
	 * operations to perform.
	 */
	private List<File> patternFiles;

	/**
	 * The list of directories that contain source files to analyze.
	 */
	private List<File> sourceDirs;

	/**
	 * The specific Java file paths to analyze, which is used to focus on a few
	 * specific files. If this is null, all files that can be searched will be
	 * searched.
	 */
	private Set<String> javaFilePaths;

	/**
	 * The patterns that are loaded from the pattern files.
	 */
	private List<Pattern> patterns = new ArrayList<>();

	/**
	 * A file filter that accepts files that can be analyzed by at least one
	 * pattern.
	 */
	private SourceFileFilter filter = new SourceFileFilter();

	/**
	 * An object that generates ID numbers for issues found.
	 */
	private IDGenerator issueIDGenerator;

	/**
	 * Constructor for this.
	 * 
	 * @param commandFiles
	 *            The files that contain the commands to execute, in the order
	 *            that they should be executed. This value cannot be null or
	 *            empty.
	 * @param sourceDirs
	 *            The directories that contain the source files to change. This
	 *            value cannot be null or empty.
	 * @param javaFiles
	 *            The files to parse and generate a plan for. If null, all files
	 *            found will be analyzed.
	 */
	public Configuration(List<File> commandFiles, List<File> sourceDirs, Set<File> javaFiles) {
		Check.notNullOrEmpty(commandFiles, "commandFiles");
		Check.notNullOrEmpty(sourceDirs, "sourceDirs");

		for (File commandFile : commandFiles) {
			if (!commandFile.exists()) {
				throw new IllegalArgumentException(
						"Command file " + commandFile.getAbsolutePath() + " does not exist.");
			}

			if (!commandFile.isFile()) {
				throw new IllegalArgumentException("Command file " + commandFile.getAbsolutePath() + " is not a file.");
			}
		}

		for (File sourceDir : sourceDirs) {
			if (!sourceDir.exists()) {
				throw new IllegalArgumentException("Source dir " + sourceDir.getAbsolutePath() + " does not exist.");
			}

			if (!sourceDir.isDirectory()) {
				throw new IllegalArgumentException(
						"Source dir " + sourceDir.getAbsolutePath() + " is not a directory.");
			}
		}

		this.patternFiles = commandFiles;
		this.sourceDirs = sourceDirs;

		Set<String> javaFilePaths = null;
		if (javaFiles != null) {
			javaFilePaths = new LinkedHashSet<>();
			for (File javaFile : javaFiles) {
				try {
					javaFilePaths.add(javaFile.getCanonicalPath());
				} catch (IOException e) {
					System.out.println(
							"Trouble getting the canonical path in the filesystem for file: " + javaFile.toString());
				}
			}
		}

		this.javaFilePaths = javaFilePaths;
	}

	/**
	 * Returns the list of pattern files, which define what will be searched for
	 * and the actions to take.
	 * 
	 * @return The pattern files. This value will not be null.
	 */
	public List<File> getCommandFileList() {
		return patternFiles;
	}

	/**
	 * Returns the list of source directories which contain the files to find
	 * issues in.
	 * 
	 * @return The source directories. This value will not be null.
	 */
	public List<File> getSourceDirList() {
		return sourceDirs;
	}

	/**
	 * Initializes this by loading the pattern files.
	 * 
	 * @throws IOException
	 *             If an error occurs while reading the pattern files.
	 */
	public void load() throws IOException {
		// tokenize the patterns in the pattern files
		for (File patternFile : patternFiles) {
			loadPatternFile(patternFile);
		}

		// TODO load other files to get the last issue id
		issueIDGenerator = new IDGenerator(1);
	}

	/**
	 * Returns the ID generator that is used for new issues.
	 * 
	 * @return The ID generator for issues. This value will be null if
	 *         {@link #load()} has not yet been called.
	 */
	public IDGenerator getIssueIDGenerator() {
		return issueIDGenerator;
	}

	/**
	 * Loads the given pattern file and parses it.
	 * 
	 * @param commandFile
	 *            The pattern file to parse. This value cannot be null.
	 * 
	 * @throws IOException
	 *             If an error occurs loading or parsing the pattern file.
	 */
	private void loadPatternFile(File commandFile) throws IOException {
		StringBuilder b = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(commandFile));
			String line = reader.readLine();
			while (line != null) {
				b.append(line);
				line = reader.readLine();
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// swallow to allow main exception to escape
				}
			}
		}

		String contents = b.toString();
		if (contents.trim().isEmpty()) {
			// TODO handle this error better
			throw new RuntimeException("File " + commandFile.getAbsolutePath()
					+ " must contain a <patterns> element as the root element.");
		}

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new ByteArrayInputStream(contents.getBytes("UTF-8")));

			// convert document into a set of commands
			NodeList nodeList = doc.getElementsByTagName("pattern");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node patternNode = nodeList.item(i);
				List<Node> searchAndActionNodes = getNonTextChildren(patternNode);
				if (searchAndActionNodes.size() != 2) {
					// TODO handle this problem better
					throw new RuntimeException("Pattern must have exactly 2 elements (1 search and 1 action) but had "
							+ searchAndActionNodes.size() + ":\n" + patternNode);
				}

				Node searchNode = searchAndActionNodes.get(0);
				Node actiohNode = searchAndActionNodes.get(1);

				Pattern pattern = getPatternForTokens(searchNode, actiohNode);
				patterns.add(pattern);
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Could not parse file " + commandFile.getAbsolutePath());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Could not parse file " + commandFile.getAbsolutePath());
		}

		// // convert tokens into commands
		// for (int i = 0; i < topToken.subTokens.size(); i = i + 2) {
		// Token searchToken = topToken.subTokens.get(i);
		// Token actionToken = topToken.subTokens.get(i + 1);
		// Action command = getCommandForToken(searchToken, actionToken);
		// commands.add(command);
		// }
	}

	/**
	 * Returns the children of the given node that are not text nodes.
	 * 
	 * @param node
	 *            The node to get the children of. This value cannot be null.
	 * 
	 * @return The children of the given node that are not text nodes. This
	 *         value will not be null, but may be empty.
	 */
	private List<Node> getNonTextChildren(Node node) {
		List<Node> elements = new ArrayList<>();
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				elements.add(child);
			}
		}

		return elements;
	}

	/**
	 * Get a pattern for the given XML nodes. The nodes will be parsed into
	 * {@link SearchParam} and {@link Action} objects respectively, and a
	 * pattern will be returned for them.
	 * 
	 * @param searchToken
	 *            The search node to parse. This value cannot be null.
	 * @param actionToken
	 *            The action node to parse. This value cannot be null.
	 * 
	 * @return The pattern which contains the parsed search parameter and
	 *         action. This value will not be null.
	 */
	private Pattern getPatternForTokens(Node searchToken, Node actionToken) {
		Pattern pattern = null;
		SearchParam searchParam = (SearchParam) getParamForToken(null, searchToken, null);
		Action action = (Action) getParamForToken(null, actionToken, searchParam);

		if (searchParam == null) {
			throw new IllegalStateException(
					"Pattern was defined without a search parameter: " + searchToken.getNodeName());
		}

		if (action == null) {
			throw new IllegalStateException(
					"Pattern was defined without an action parameter: " + actionToken.getNodeName());
		}

		pattern = new Pattern();
		pattern.setSearchParam(searchParam);
		pattern.setAction(action);

		return pattern;
	}

	/**
	 * Converts the given node, and all sub-nodes (recursively), into a Param of
	 * the correct type.
	 * 
	 * @param parent
	 *            The parent of the node, which in some cases is used to
	 *            determine context for a node. If null, the node will be
	 *            considered to not have any special context.
	 * @param token
	 *            The node to parse. This value cannot be null.
	 * @param searchParam
	 *            The search parameter that is used for context. For example, a
	 *            replace action will correspond to a different action depending
	 *            on what the top-level SearchParam is for this action. May not
	 *            be null if the node is an action.
	 * 
	 * @return The parameter that was parsed. This value will be null if the
	 *         node could not be parsed.
	 */
	private Param getParamForToken(Node parent, Node token, SearchParam searchParam) {
		Param param = null;
		String tokenName = token.getNodeName();
		String parentName = "";
		if (parent != null) {
			parentName = parent.getNodeName();
		}

		if (tokenName.equals("classref")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			param = new ClassRefParam(params);
		} else if (tokenName.equals("methodref")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			param = new MethodRefParam(params);
		} else if (tokenName.equals("classdecl")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			param = new ClassDeclParam(params);
		} else if (tokenName.equals("methoddecl")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			param = new MethodDeclParam(params);
		} else if (tokenName.equals("name")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			String textContents = getTextContents(token, params);
			param = new NameParam("name", textContents, params);
		} else if (tokenName.equals("classname")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			String textContents = getTextContents(token, params);
			param = new NameParam("classname", textContents, params);
		} else if (tokenName.equals("issupertype")) {
			param = new IsSupertypeParam();
		} else if (tokenName.equals("hassupertype")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			param = new HasSupertypeParam(params);
		} else if (tokenName.equals("isinmethod")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			param = new IsInMethodParam(params);
		} else if (tokenName.equals("hasparam")) {
			Map<String, String> attributes = getAttributes(token);
			param = new HasParamParam(attributes);
		} else if (tokenName.equals("hasmethod")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			param = new HasMethodParam(params);
		} else if (tokenName.equals("not")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			param = new NotParam(params);
		} else if (tokenName.equals("and")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			param = new AndParam(params);
		} else if (tokenName.equals("warning")) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			String textContents = getTextContents(token, params);
			param = new Warning(textContents, params);
		} else if (tokenName.equals("regex") && (parentName.equals("name") || parentName.equals("classname"))) {
			param = new RegexSearchParam(token.getTextContent());
		} else if (tokenName.equals("regex") && (parentName.equals("replace") || parentName.equals("warning"))) {
			param = new RegexReplaceParam(token.getTextContent());
		} else if (tokenName.equals("replace") && searchParam instanceof ClassRefParam) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			String textContents = getTextContents(token, params);
			param = new ClassRefReplace(textContents, params);
		} else if (tokenName.equals("replace") && searchParam instanceof MethodRefParam) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			String textContents = getTextContents(token, params);
			param = new MethodRefReplace(textContents, params);
		} else if (tokenName.equals("replace") && searchParam instanceof ClassDeclParam) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			String textContents = getTextContents(token, params);
			param = new ClassDeclReplace(textContents, params);
		} else if (tokenName.equals("replace") && searchParam instanceof MethodDeclParam) {
			List<Param> params = convertChildrenToParams(token, searchParam);
			String textContents = getTextContents(token, params);
			param = new MethodDeclReplace(textContents, params);
		} else if (tokenName.equals("remove") && searchParam instanceof ClassRefParam) {
			param = new ClassRefRemove();
		} else if (tokenName.equals("remove") && searchParam instanceof MethodRefParam) {
			param = new MethodRefRemove();
		} else if (tokenName.equals("remove") && searchParam instanceof ClassDeclParam) {
			param = new ClassDeclRemove();
		} else if (tokenName.equals("remove") && searchParam instanceof MethodDeclParam) {
			param = new MethodDeclRemove();
		}

		return param;
	}

	/**
	 * Returns the XML attributes for the given node.
	 * 
	 * @param token
	 *            The node to get the attributes of. This value cannot be null.
	 * 
	 * @return The name-value pairs for the attributes from the given node. This
	 *         value will not be null, but may be empty.
	 */
	private Map<String, String> getAttributes(Node token) {
		NamedNodeMap nodeMap = token.getAttributes();
		Map<String, String> attributes = new LinkedHashMap<>();
		for (int i = 0; i < nodeMap.getLength(); i++) {
			Attr a = (Attr) nodeMap.item(i);
			String name = a.getName();
			String value = a.getValue();
			attributes.put(name, value);
		}

		return attributes;
	}

	/**
	 * Returns the text contents of the given node.
	 * 
	 * @param token
	 *            The node to get the text contents of. This value cannot be
	 *            null.
	 * @param params
	 *            The parsed sub-nodes of this. This value cannot be null. If
	 *            empty, the text contents will be ignored.
	 * 
	 * @return The text contents of the node, or null if there are any
	 *         sub-nodes.
	 */
	private String getTextContents(Node token, List<Param> params) {
		String textContents = null;
		if (params.isEmpty()) {
			textContents = token.getTextContent();
		}
		return textContents;
	}

	/**
	 * Converts the children of the given node to Param subclasses, by calling
	 * {@link #getParamForToken(Node, Node, SearchParam)} on each child.
	 * 
	 * @param token
	 *            The node whose children are to be converted. This value cannot
	 *            be null.
	 * @param searchParam
	 *            The search parameter that is used for context. For example, a
	 *            replace action will correspond to a different action depending
	 *            on what the top-level SearchParam is for this action. May not
	 *            be null if the node is an action.
	 * 
	 * @return The child Param objects of the given node. This value will not be
	 *         null, but may be empty.
	 */
	private List<Param> convertChildrenToParams(Node token, SearchParam searchParam) {
		List<Param> params = new ArrayList<>();
		NodeList childNodes = token.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node subToken = childNodes.item(i);
			Param subParam = getParamForToken(token, subToken, searchParam);
			if (subParam != null) {
				params.add(subParam);
			}
		}
		return params;
	}

	/**
	 * Returns the patterns that were parsed by the {@link #load()} method.
	 * 
	 * @return The patterns that were parsed. This value will not be null, but
	 *         may be empty.
	 */
	public List<Pattern> getPatterns() {
		return patterns;
	}

	/**
	 * Returns the source files in all of the input source directories, filtered
	 * by the types of files that the existing patterns can analyze, and by the
	 * java files given in the constructor.
	 * 
	 * @return The source files in all the directories. This value will not be
	 *         null, but may be empty.
	 */
	public List<File> getFiles() {
		List<File> allFiles = new ArrayList<>();
		for (File dir : sourceDirs) {
			addInDirectory(dir, allFiles);
		}

		return allFiles;
	}

	/**
	 * Adds all the files, recursively, from the given directory to the given
	 * list. The files are filtered by the input patterns - if any pattern can
	 * analyze the file, it is included.
	 * 
	 * @param dir
	 *            The directory to find files in. This value cannot be null.
	 * @param addedSoFar
	 *            The list to add files to. This value cannot be null.
	 */
	private void addInDirectory(File dir, List<File> addedSoFar) {
		File[] subFiles = dir.listFiles(filter);
		if (subFiles != null) {
			for (File subFile : subFiles) {
				if (subFile.isFile()) {
					if (javaFilePaths == null) {
						addedSoFar.add(subFile);
					} else {
						try {
							String path = subFile.getCanonicalPath();
							if (javaFilePaths.contains(path)) {
								addedSoFar.add(subFile);
							}
						} catch (IOException e) {
							System.out.println("Trouble getting the canonical path in the filesystem for file: "
									+ subFile.toString());
						}
					}
				} else if (subFile.isDirectory()) {
					addInDirectory(subFile, addedSoFar);
				}
			}
		}
	}

	/**
	 * This class filters files that at least one pattern is able to analyze. If
	 * no pattern can analyze it, it will be rejected. Directories are always
	 * accepted.
	 */
	private class SourceFileFilter implements FileFilter {

		/**
		 * Filters files that at least one pattern is able to analyze. If no
		 * pattern can analyze it, it will be rejected. Directories are always
		 * accepted.
		 */
		@Override
		public boolean accept(File pathname) {
			boolean accept = false;
			if (pathname.isDirectory()) {
				accept = true;
			} else {
				for (Pattern pattern : patterns) {
					if (pattern.getSearchParam().allowFile(pathname)) {
						accept = true;
						break;
					}
				}
			}

			return accept;
		}

	}
}
