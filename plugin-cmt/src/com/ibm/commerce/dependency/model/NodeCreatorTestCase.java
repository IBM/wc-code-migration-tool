package com.ibm.commerce.dependency.model;

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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * CycleAnalyzerTest tests the {@link CycleAnalyzer} class.
 * 
 * @author Trent Hoeppner
 */
public class NodeCreatorTestCase extends TestCase {

	private Map<String, JavaItem> nameToItemMap;

	private JavaItemIndex index;

	/**
	 * Constructor for this.
	 *
	 * @param testName
	 *            The name of the test used by JUnit. Cannot be null or empty.
	 */
	public NodeCreatorTestCase(String testName) {
		super(testName);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void setUp() throws Exception {
		super.setUp();

		nameToItemMap = new HashMap<String, JavaItem>();
		index = new JavaItemIndex("v8");
	}

	/**
	 * Tests that if one node is setup then it exists.
	 */
	public void testSetupNodeIfOneNodeExpectNodeExists() {
		setupNode("project");
		assertNotNull("project node was not created.", get("project"));
	}

	/**
	 * Tests that if a node and child are setup they exist and their
	 * relationship is correct.
	 */
	public void testSetupNodeIfParentChildExpectNodesAndRelationshipExist() {
		setupNode("project:package");
		JavaItem projectNode = get("project");
		JavaItem packageNode = get("package");
		assertNotNull("project node was not created.", projectNode);
		assertNotNull("package node was not created.", packageNode);
		assertEquals("package parent is wrong.", projectNode, packageNode.getParent());
		assertEquals("parent does not have package as a child.", packageNode, projectNode.getChildren().get(0));
	}

	/**
	 * Tests that if a node and dependency are setup they exist and their
	 * relationship is correct.
	 */
	public void testSetupNodeIfNodeAndDependencyExpectNodesAndRelationshipExist() {
		setupNode("project>project2");
		JavaItem projectNode = get("project");
		JavaItem projectNode2 = get("project2");
		assertNotNull("project node was not created.", projectNode);
		assertNotNull("package node was not created.", projectNode2);
		assertEquals("project dependency is wrong.", projectNode2, projectNode.getDependencies().get(0));
	}

	/**
	 * Tests that if a node is created as a project, its type is correct.
	 */
	public void testSetupNodeIfNodeAsProjectExpectTypeIsProject() {
		setupNode("project_p");
		JavaItem projectNode = get("project");
		assertNotNull("project node was not created.", projectNode);
		assertEquals("project type is wrong.", JavaItemType.PROJECT, projectNode.getType());
	}

	/**
	 * Tests that if a node is created as a package, its type is correct.
	 */
	public void testSetupNodeIfNodeAsPackageExpectTypeIsPackage() {
		setupNode("node_k");
		JavaItem node = get("node");
		assertNotNull("node was not created.", node);
		assertEquals("Type is wrong.", JavaItemType.PACKAGE, node.getType());
	}

	/**
	 * Tests that if a node is created as a class, its type is correct.
	 */
	public void testSetupNodeIfNodeAsClassExpectTypeIsClass() {
		setupNode("node_c");
		JavaItem node = get("node");
		assertNotNull("node was not created.", node);
		assertEquals("Type is wrong.", JavaItemType.CLASS, node.getType());
	}

	/**
	 * Tests that if a node is created as a package under a project, and another
	 * package with the same name is created under a different project, they are
	 * different objects.
	 */
	public void testSetupNodeIfSamePackageDifferentProjectExpectDifferentPackageNodes() {
		setupNode("project1_p:package_k");
		setupNode("project2_p:package_k{project2}");
		JavaItem packageNode = get("package");
		JavaItem packageWithParentProject1Node = get("package{project1}");
		JavaItem packageWithParentProject2Node = get("package{project2}");
		assertSame("packageNode and packageWithParentProject1Node are different.", packageNode,
				packageWithParentProject1Node);
		assertNotSame("packageNode and packageWithParentProject2Node are the same.", packageNode,
				packageWithParentProject2Node);
	}

	/**
	 * Tests that if the same node name is added as a child twice, then there
	 * will be only one.
	 */
	public void testSetupNodeIfSameChildAddedTwiceExpectOneChild() {
		setupNode("project1_p:package1_k");
		setupNode("project1_p:package1_k");

		assertEquals("Number of children is wrong.", 1, get("project1").getChildren().size());
	}

	/**
	 * Tests that if the same node name is added as a dependency twice, then
	 * there will be only one.
	 */
	public void testSetupNodeIfSameDependencyAddedTwiceExpectOneChild() {
		setupNode("project1_p>project2_p");
		setupNode("project1_p>project2_p");

		assertEquals("Number of dependencies is wrong.", 1, get("project1").getDependencies().size());
	}

	private enum Rel {
		DEP, CHILD;
	}

	protected Collection<JavaItem> getAllItems() {
		return nameToItemMap.values();
	}

	protected void setupNode(String stringRep) {
		Pattern pattern = Pattern.compile("([\\w\\.&&[^_]]+)(_[pkc])?(\\{(\\w+)\\})?([>:])?");
		Matcher matcher = pattern.matcher(stringRep);
		JavaItem previous = null;
		Rel previousRel = null;
		while (matcher.find()) {
			String name = matcher.group(1);
			String type = matcher.group(2);
			String parent = matcher.group(4);
			String relName = matcher.group(5);
			Rel rel = null;
			if (relName != null) {
				if (relName.equals(">")) {
					rel = Rel.DEP;
				} else if (relName.equals(":")) {
					rel = Rel.CHILD;
				}
			}

			JavaItem parentItem = null;
			JavaItem item = null;
			if (parent != null) {
				parentItem = get(parent);
				for (JavaItem child : parentItem.getChildren()) {
					if (child.getName().equals(name)) {
						item = child;
						break;
					}
				}

				if (item == null) {
					item = new BaseJavaItem(name, index);
				}
			} else {
				item = nameToItemMap.get(name);
				if (item == null) {
					item = new BaseJavaItem(name, index);
					nameToItemMap.put(name, item);
				}
			}

			if (parentItem == null && previousRel == Rel.CHILD) {
				parentItem = previous;
			}

			if (type != null) {
				if (type.equals("_p")) {
					item.setType(JavaItemType.PROJECT);
				} else if (type.equals("_k")) {
					item.setType(JavaItemType.PACKAGE);
				} else if (type.equals("_c")) {
					item.setType(JavaItemType.CLASS);
				}
			}

			if (parentItem != null) {
				if (!previous.getChildren().contains(item)) {
					previous.getChildren().add(item);
					item.setParentID(previous.getID());
				}
			}

			if (previousRel == Rel.DEP) {
				if (!previous.getDependenciesIDs().contains(item.getID())) {
					previous.getDependenciesIDs().add(item.getID());
					item.getIncomingIDs().add(previous.getID());
				}
			}

			previous = item;
			previousRel = rel;
		}
	}

	protected JavaItem get(String name) {
		JavaItem item = null;

		Pattern pattern = Pattern.compile("([\\w\\.&&[^_]]+)(\\{(\\w+)\\})?");
		Matcher matcher = pattern.matcher(name);
		if (matcher.matches()) {
			String baseName = matcher.group(1);
			String parentName = matcher.group(3);
			if (parentName == null) {
				item = nameToItemMap.get(baseName);
			} else {
				JavaItem parent = nameToItemMap.get(parentName);
				if (parent == null) {
					throw new IllegalArgumentException("Parent item not found for " + name);
				}

				for (JavaItem child : parent.getChildren()) {
					if (child.getName().equals(baseName)) {
						item = child;
						break;
					}
				}
			}
		} else {
			throw new IllegalArgumentException("Not a valid format for the name: " + name);
		}

		return item;
	}
}
