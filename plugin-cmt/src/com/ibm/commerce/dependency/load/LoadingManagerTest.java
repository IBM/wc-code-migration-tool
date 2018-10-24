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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import com.ibm.commerce.cmt.plan.IDGenerator;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemFactory;
import com.ibm.commerce.dependency.model.JavaItemIndex;
import com.ibm.commerce.dependency.model.JavaItemType;
import com.ibm.commerce.dependency.model.JavaItemUtil;

import junit.framework.TestCase;

/**
 * This class tests the {@link LoadingManager} class.
 * 
 * @author Trent Hoeppner
 */
public class LoadingManagerTest extends TestCase {

	private JavaItemIndex index;

	/**
	 * This class tests that the proper data is loaded from sample projects and
	 * JARs in the testData directory.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void testLoadProjectsForExtract() throws Exception {
		index = new JavaItemIndex("8");
		index.setIDGenerator(new IDGenerator(0));
		JavaItemFactory factory = new JavaItemFactory(index);
		JavaItemUtil.initialize(factory);

		File workspaceDir = new File("testData/miniToolkit/WCDE_INT80/workspace");

		Set<File> thirdPartyDirs = new HashSet<>();
		thirdPartyDirs.add(new File(workspaceDir, "WC/lib"));
		thirdPartyDirs.add(new File(workspaceDir, "../../SDP/runtimes/base_v85_stub"));

		// "C:/dev/git/commerce-toolkit-plugins-external-wc/plugin-cmt/testData/miniToolkit/WCDE_INT80/workspace");
		FileFilter filter = null;

		// FileFilter filter = new FileFilter() {
		//
		// @Override
		// public boolean accept(File pathname) {
		// return pathname.getName().startsWith("Catalog-") ||
		// pathname.getName().startsWith("commons-") ;
		// }
		//
		// };

		boolean isExtractingAPI = true;
		LoadingManager manager = new LoadingManager();
		factory = manager.loadProjects(factory, workspaceDir, thirdPartyDirs, filter, isExtractingAPI);
		index = factory.getIndex();

		checkHierarchy("WebSphereCommerceServerExtensionsLogic", "com.ibm.commerce.copyright", "IBMCopyright",
				"LONG_COPYRIGHT");
		checkHierarchy("WebSphereCommerceServerExtensionsLogic", "com.ibm.commerce.test", "DataloadEnvDebug");
		checkHierarchy("WebSphereCommerceServerExtensionsLogic", "com.ibm.commerce.test", "TestLogging");
		checkDependencies("WebSphereCommerceServerExtensionsLogic", "WebSphereCommerceServerExtensionsData");
		checkDependencies("WebSphereCommerceServerExtensionsLogic", "Foundation-DataLoad");
		checkDependencies("WebSphereCommerceServerExtensionsLogic", "Enablement-BusinessContextEngineAdvancedLogic");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Catalog-ProductManagementData");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Catalog-ProductManagementLogic");
		checkDependencies("WebSphereCommerceServerExtensionsData", "ContentManagement-WorkspaceFlowData");
		checkDependencies("WebSphereCommerceServerExtensionsData", "ContentManagement-WorkspaceFlowLogic");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BaseComponentsData");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BaseComponentsLogic");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BaseComponentsLogicWAS6");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BusinessContextEngineAdvancedData");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BusinessContextEngineAdvancedInterface");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BusinessContextEngineAdvancedLogic");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BusinessContextEngineData");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BusinessContextEngineInterface");
		checkDependencies("WebSphereCommerceServerExtensionsData", "LMRecommender");
		checkDependencies("WebSphereCommerceServerExtensionsData", "feedbackilm");
		checkDependencies("WebSphereCommerceServerExtensionsData", "feedbacklm");
		checkDependencies("WebSphereCommerceServerExtensionsData", "jtopen");
		checkDependencies("WebSphereCommerceServerExtensionsData", "likeminds");
		checkDependencies("WebSphereCommerceServerExtensionsData", "wcslm");
		checkDependencies("WebSphereCommerceServerExtensionsData", "wpcpruntime");
		checkDependencies("WebSphereCommerceServerExtensionsData", "wpcpruntimecommon");
		checkDependencies("WebSphereCommerceServerExtensionsData", "xsdbeans");
		checkDependencies("WebSphereCommerceServerExtensionsLogic:com.ibm.commerce.test:DataloadEnvDebug",
				"null:com.ibm.commerce.foundation.dataload.database:DBManager");
		checkDependencies("WebSphereCommerceServerExtensionsLogic:com.ibm.commerce.test:DataloadEnvDebug:main()",
				"null:com.ibm.commerce.foundation.dataload.database:DBManager:getInstance()");
		checkDependencies("WebSphereCommerceServerExtensionsLogic:com.ibm.commerce.test:DataloadEnvDebug:main()",
				"null:com.ibm.commerce.foundation.dataload.database:DBManager:setTargetDatabaseProperties()");

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File("LoadingManagerTestOut.log")));
			for (JavaItem item : index.getItems()) {
				JavaItem current = item;
				Boolean binary = current.getAttribute(JavaItem.ATTR_BINARY);
				while (binary == null && current != null) {
					current = current.getParent();
					if (current != null) {
						binary = current.getAttribute(JavaItem.ATTR_BINARY);
					}
				}
				String line = "" + item.getID() + " (" + (binary == null || binary.booleanValue() ? "b" : "j") + ") "
						+ item;
				System.out.println(line);
				writer.write(line);
				writer.newLine();
			}
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// swallow to allow the main exception to escape
				}
			}
		}

		System.out.println("Done.");
	}

	/**
	 * This class tests that the proper data is loaded from sample projects in
	 * the testData directory, without loading JARs.
	 * 
	 * @throws Exception
	 *             If an error occurs during the test.
	 */
	public void dtestLoadProjectsFromBase() throws Exception {
		APIFileManager apiMmanager = new APIFileManager();
		JavaItemIndex base = apiMmanager.loadAPI(new File("testData/api-v8.zip"));

		index = new JavaItemIndex("9", base);
		JavaItemFactory factory = new JavaItemFactory(index);
		JavaItemUtil.initialize(factory);

		File workspaceDir = new File("testData/miniToolkit/WCDE_INT80/workspace");

		Set<File> thirdPartyDirs = new HashSet<>();
		thirdPartyDirs.add(new File(workspaceDir, "WC/lib"));
		thirdPartyDirs.add(new File(workspaceDir, "../../SDP/runtimes/base_v85_stub"));

		FileFilter filter = null;

		boolean isExtractingAPI = false;
		LoadingManager manager = new LoadingManager();
		manager.loadProjects(factory, workspaceDir, thirdPartyDirs, filter, isExtractingAPI);

		checkHierarchy("WebSphereCommerceServerExtensionsLogic", "com.ibm.commerce.copyright", "IBMCopyright",
				"LONG_COPYRIGHT");
		checkHierarchy("WebSphereCommerceServerExtensionsLogic", "com.ibm.commerce.test", "DataloadEnvDebug");
		checkHierarchy("WebSphereCommerceServerExtensionsLogic", "com.ibm.commerce.test", "TestLogging");
		checkDependencies("WebSphereCommerceServerExtensionsLogic", "WebSphereCommerceServerExtensionsData");
		checkDependencies("WebSphereCommerceServerExtensionsLogic", "Foundation-DataLoad");
		checkDependencies("WebSphereCommerceServerExtensionsLogic", "Enablement-BusinessContextEngineAdvancedLogic");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Catalog-ProductManagementData");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Catalog-ProductManagementLogic");
		checkDependencies("WebSphereCommerceServerExtensionsData", "ContentManagement-WorkspaceFlowData");
		checkDependencies("WebSphereCommerceServerExtensionsData", "ContentManagement-WorkspaceFlowLogic");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BaseComponentsData");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BaseComponentsLogic");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BaseComponentsLogicWAS6");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BusinessContextEngineAdvancedData");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BusinessContextEngineAdvancedInterface");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BusinessContextEngineAdvancedLogic");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BusinessContextEngineData");
		checkDependencies("WebSphereCommerceServerExtensionsData", "Enablement-BusinessContextEngineInterface");
		checkDependencies("WebSphereCommerceServerExtensionsData", "LMRecommender");
		checkDependencies("WebSphereCommerceServerExtensionsData", "feedbackilm");
		checkDependencies("WebSphereCommerceServerExtensionsData", "feedbacklm");
		checkDependencies("WebSphereCommerceServerExtensionsData", "jtopen");
		checkDependencies("WebSphereCommerceServerExtensionsData", "likeminds");
		checkDependencies("WebSphereCommerceServerExtensionsData", "wcslm");
		checkDependencies("WebSphereCommerceServerExtensionsData", "wpcpruntime");
		checkDependencies("WebSphereCommerceServerExtensionsData", "wpcpruntimecommon");
		checkDependencies("WebSphereCommerceServerExtensionsData", "xsdbeans");
		checkDependencies("WebSphereCommerceServerExtensionsLogic:com.ibm.commerce.test:DataloadEnvDebug",
				"null:com.ibm.commerce.foundation.dataload.database:DBManager");
		checkDependencies("WebSphereCommerceServerExtensionsLogic:com.ibm.commerce.test:DataloadEnvDebug:main()",
				"null:com.ibm.commerce.foundation.dataload.database:DBManager:getInstance()");
		checkDependencies("WebSphereCommerceServerExtensionsLogic:com.ibm.commerce.test:DataloadEnvDebug:main()",
				"null:com.ibm.commerce.foundation.dataload.database:DBManager:setTargetDatabaseProperties()");
	}

	private void checkDependencies(String... items) {
		JavaItem last = null;
		for (int i = 0; i < items.length; i++) {
			JavaItem current = parseItemString(items[i]);
			assertNotNull("Could not find " + items[i], current);

			if (last != null) {
				assertTrue("Dependency missing from " + last + " to " + current,
						last.getDependenciesIDs().contains(current.getID()));
				assertTrue("Incoming missing to " + current + " from " + last,
						current.getIncomingIDs().contains(last.getID()));
			}
			last = current;
		}
	}

	private JavaItem parseItemString(String itemString) {
		StringTokenizer tokenizer = new StringTokenizer(itemString, ":");
		int size = tokenizer.countTokens();
		String[] items = new String[size];
		int i = 0;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			items[i] = token;
			i++;
		}

		JavaItem foundItem = checkHierarchy(items);

		return foundItem;
	}

	private JavaItem checkHierarchy(String... items) {
		JavaItem lastParent = null;
		for (int i = 0; i < items.length; i++) {
			JavaItem current;
			if (items[i].equals("null")) {
				// only for null projects
				// we leave the lastParent as null, next time it will look for a
				// package with a null parent
				continue;
			} else {
				JavaItemType expectedType = JavaItemType.values()[i];
				String itemName = items[i];
				if (i == JavaItemType.FIELD.ordinal()) {
					// actually it could be a method or field
					if (itemName.endsWith("()")) {
						expectedType = JavaItemType.METHOD;
						itemName = itemName.substring(0, itemName.length() - 2);
					} else {
						expectedType = JavaItemType.FIELD;
					}
				}
				current = index.findItem(lastParent, itemName, expectedType);
			}

			assertNotNull("Could not find " + (lastParent == null ? "" : lastParent + ":") + items[i], current);
			lastParent = current;
		}

		return lastParent;
	}
}
