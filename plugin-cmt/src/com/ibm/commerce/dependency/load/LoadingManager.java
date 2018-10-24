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

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.plan.IDGenerator;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemFactory;
import com.ibm.commerce.dependency.model.JavaItemIndex;
import com.ibm.commerce.dependency.model.JavaItemUtil;
import com.ibm.commerce.dependency.model.JavaItemUtil2;
import com.ibm.commerce.dependency.task.ChainTask;
import com.ibm.commerce.dependency.task.TaskList;

/**
 * This class creates the initial tasks to load data based on the available
 * inputs, starts those tasks, and waits for the results.
 * 
 * @author Trent Hoeppner
 */
public class LoadingManager {

	/**
	 * Loads the methods for a class.
	 * 
	 * @param factory
	 *            The factory used to create new methods. This value cannot be
	 *            null.
	 * @param javaClass
	 *            The specific class item to load the methods for. This value
	 *            cannot be null.
	 * @param sourceFile
	 *            The file that contains the source code of the class. This
	 *            value cannot be null.
	 */
	public void loadMethods(JavaItemFactory factory, JavaItem javaClass, File sourceFile) {
		JavaItemUtil2 util = new JavaItemUtil2();
		util.initialize(factory);

		TaskList taskList = new TaskList();

		LoadingContext context = new LoadingContext(taskList, factory, util);
		context.put(Name.CLASS_JAVAITEM, javaClass);
		context.put(Name.JAVA_SOURCE_FILE, sourceFile);

		ChainTask<LoadingContext> chain = new ChainTask<>("LoadMethods", context);
		chain.addTask(new LoadJavaCompilationUnitTask("LoadCompilationUnit", context));
		chain.addTask(new LoadJavaSourceMethodsTask("LoadMethods", context));

		taskList.addTask(chain, Priority.TOP_LEVEL);
		taskList.start();

		// wait for finish
		taskList.waitForCompletion();
	}

	/**
	 * Discovers and loads the projects and dependencies in a workspace and JAR
	 * files, but no child objects.
	 * 
	 * @param factory
	 *            The factory used to create new methods. This value cannot be
	 *            null.
	 * @param workspaceDir
	 *            The directory which contains the Eclipse projects. This value
	 *            cannot be null.
	 * @param thirdPartyDirs
	 *            The directories that contain third-party JARs to load. These
	 *            directories will have JARs found in them and in their
	 *            sub-directories, recursively. The class information will be
	 *            loaded but only the ones that the Commerce code depends on
	 *            will be kept in the final index. This value cannot be null,
	 *            but may be empty.
	 * @param filter
	 *            The filter that chooses which projects to load. If null, all
	 *            projects will be loaded.
	 * @param isExtractingAPI
	 *            True indicates that the third party JARs need to be extracted,
	 *            false indicates that only the workspace projects need to be
	 *            analyzed.
	 * 
	 * @return A new factory with the unnecessary third-party items removed.
	 *         This value will not be null.
	 */
	public JavaItemFactory loadProjects(JavaItemFactory factory, File workspaceDir, Set<File> thirdPartyDirs,
			FileFilter filter, boolean isExtractingAPI) {
		JavaItemUtil2 util = new JavaItemUtil2();
		util.initialize(factory);

		TaskList taskList = new TaskList();

		LoadingContext eclipseLoadingContext = new LoadingContext(taskList, factory, util);
		eclipseLoadingContext.put(Name.WORKSPACE_DIR, workspaceDir);
		eclipseLoadingContext.put(Name.FILTER, filter);
		eclipseLoadingContext.put(Name.IS_EXTRACTING_API, isExtractingAPI);

		ChainTask<LoadingContext> eclipseChain = new ChainTask<>("FindAndLoadEclipseProjects", eclipseLoadingContext);
		eclipseChain.addTask(new FindEclipseProjectDirsTask("FindEclipseProjectDirs", eclipseLoadingContext));
		eclipseChain.addTask(new LoadEclipseProjectsTask("LoadEclipseProjects", eclipseLoadingContext));

		taskList.addTask(eclipseChain, Priority.TOP_LEVEL);

		if (isExtractingAPI) {
			LoadingContext jarLoadingContext = new LoadingContext(taskList, factory, util);
			Set<File> jarDirectories = new HashSet<>();
			jarDirectories.add(new File(workspaceDir, "../lib"));
			jarDirectories.add(new File(workspaceDir, "WC"));
			jarDirectories.addAll(thirdPartyDirs);
			// jarDirectories.add(new File(workspaceDir, "WC/lib"));
			// jarDirectories.add(new File(workspaceDir,
			// "../../SDP/runtimes/base_v85_stub/plugins"));
			jarLoadingContext.put(Name.JAR_DIRECTORIES, jarDirectories);
			jarLoadingContext.put(Name.THIRD_PARTY_DIRECTORIES, thirdPartyDirs);
			jarLoadingContext.put(Name.FILTER, filter);
			jarLoadingContext.put(Name.IS_EXTRACTING_API, isExtractingAPI);

			ChainTask<LoadingContext> jarChain = new ChainTask<>("FindAndLoadJARProjects", jarLoadingContext);
			jarChain.addTask(new FindProjectJARsTask("FindProjectJARs", jarLoadingContext));
			jarChain.addTask(new LoadJARProjectsTask("LoadJARProjects", jarLoadingContext));

			taskList.addTask(jarChain, Priority.TOP_LEVEL);
		}

		taskList.start();

		// wait for finish
		taskList.waitForCompletion();

		// get rid of unnecessary third-party classes and methods, and projects
		JavaItemFactory smallFactory = factory;
		if (isExtractingAPI) {
			smallFactory = pruneUnusedThirdPartyItems(factory);
		}

		return smallFactory;
	}

	/**
	 * Discovers and loads the projects and dependencies in a workspace and JAR
	 * files, but no child objects.
	 * 
	 * @param factory
	 *            The factory used to create new methods. This value cannot be
	 *            null.
	 * @param workspaceDir
	 *            The directory which contains the Eclipse projects. This value
	 *            cannot be null.
	 * @param javaFiles
	 *            The files to be loaded using the given factory. The factory is
	 *            assumed to be associated with a delta index, so that the
	 *            references in the given files can be loaded properly. This
	 *            value cannot be null, but may be empty.
	 */
	public void loadFiles(JavaItemFactory factory, File workspaceDir, Set<File> javaFiles) {
		JavaItemUtil2 util = new JavaItemUtil2();
		util.initialize(factory);

		TaskList taskList = new TaskList();

		LoadingContext eclipseLoadingContext = new LoadingContext(taskList, factory, util);
		eclipseLoadingContext.put(Name.WORKSPACE_DIR, workspaceDir);
		eclipseLoadingContext.put(Name.FILTER, null);
		eclipseLoadingContext.put(Name.IS_EXTRACTING_API, false);
		eclipseLoadingContext.put(Name.JAVA_FILES, javaFiles);

		ChainTask<LoadingContext> eclipseChain = new ChainTask<>("FindAndLoadEclipseProjects", eclipseLoadingContext);
		eclipseChain.addTask(new FindEclipseProjectDirsTask("FindEclipseProjectDirs", eclipseLoadingContext));
		eclipseChain.addTask(new LoadEclipseProjectsTask("LoadEclipseProjects", eclipseLoadingContext));

		taskList.addTask(eclipseChain, Priority.TOP_LEVEL);

		taskList.start();

		// wait for finish
		taskList.waitForCompletion();
	}

	/**
	 * Removes the unused third-party items from the given factory and index by
	 * copying the relevant items to the new index, and remapping the integer
	 * references.
	 * 
	 * @param factory
	 *            The factory and index to prune. This value cannot be null.
	 * 
	 * @return The factory and index with the irrelevant items removed. This
	 *         value will not be null.
	 */
	private JavaItemFactory pruneUnusedThirdPartyItems(JavaItemFactory factory) {
		try {
			// 1. copy all non-third-party projects to a new index
			JavaItemIndex oldIndex = factory.getIndex();
			JavaItemIndex smallIndex;
			if (oldIndex.getBase() == null) {
				smallIndex = new JavaItemIndex(oldIndex.getVersion());
				smallIndex.setIDGenerator(new IDGenerator(0));
			} else {
				smallIndex = new JavaItemIndex(oldIndex.getVersion(), oldIndex.getBase());
			}
			JavaItemFactory smallFactory = new JavaItemFactory(smallIndex);
			Map<Integer, Integer> oldToNewIDMap = new HashMap<>();
			for (JavaItem oldItem : oldIndex.getItems()) {
				if (!isThirdParty(oldItem)) {
					JavaItem newItem = oldItem.copyTo(smallFactory);
					oldToNewIDMap.put(oldItem.getID(), newItem.getID());
				}
			}

			// 2. for all copied items in the new index, find if they directly
			// depend on a third-party object in the old index
			Set<Integer> allDependentIDs = new HashSet<>();
			for (JavaItem newItem : smallIndex.getItems()) {
				Set<Integer> dependentIDs = findDependentIDs(newItem);
				allDependentIDs.addAll(dependentIDs);
			}

			// 3. find all parents of dependent IDs
			Set<Integer> allDependentIDsWithParents = new HashSet<>();
			for (Integer dependentID : allDependentIDs) {
				JavaItem current = oldIndex.getItem(dependentID);
				while (current != null) {
					allDependentIDsWithParents.add(current.getID());
					current = current.getParent();
				}
			}

			// 4. copy all dependent IDs to the new index
			for (Integer dependentID : allDependentIDsWithParents) {
				if (!oldToNewIDMap.containsKey(dependentID)) {
					JavaItem oldDependentItem = oldIndex.getItem(dependentID);
					JavaItem newDependentItem = oldDependentItem.copyTo(smallFactory);
					oldToNewIDMap.put(oldDependentItem.getID(), newDependentItem.getID());
				}
				// else it was already copied
			}

			// 5. remap all the item's referenced IDs in the new index
			for (JavaItem newItem : smallIndex.getItems()) {
				remapDependencies(oldToNewIDMap, newItem);
			}
			return smallFactory;
		} catch (Exception e) {
			System.out.println("Error during pruning");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Finds all IDs in the given item, looks up the ID in the map to find the
	 * new ID, and sets the new ID in its place. If an ID has no corresponding
	 * new ID in the map, that dependency will be removed.
	 * 
	 * @param oldToNewIDMap
	 *            The mapping from IDs from the old index to IDs in the new
	 *            index. This value cannot be null.
	 * @param newItem
	 *            The item to remap IDs for. This value cannot be null.
	 */
	private void remapDependencies(Map<Integer, Integer> oldToNewIDMap, JavaItem newItem) {
		Integer parentID = newItem.getParentID();
		if (parentID != null) {
			Integer newParentID = oldToNewIDMap.get(parentID);
			Check.notNull(newParentID, "newParentID");
			newItem.setParentID(newParentID);
		}

		remapIDsInList(oldToNewIDMap, newItem.getChildrenIDs());
		remapIDsInList(oldToNewIDMap, newItem.getDependenciesIDs());
		remapIDsInList(oldToNewIDMap, newItem.getIncomingIDs());

		if (newItem.getAttributes() != null) {
			remapSingleAttributeID(oldToNewIDMap, newItem, JavaItem.ATTR_SUPERCLASS);
			remapCollectionAttribute(oldToNewIDMap, newItem, JavaItem.ATTR_SUPERINTERFACES);
			remapSingleAttributeID(oldToNewIDMap, newItem, JavaItem.ATTR_RETURN_TYPE);
			remapCollectionAttribute(oldToNewIDMap, newItem, JavaItem.ATTR_METHOD_PARAM_TYPES);
			remapSingleAttributeID(oldToNewIDMap, newItem, JavaItem.ATTR_ARRAY_BASE_CLASS);
			remapSingleAttributeID(oldToNewIDMap, newItem, JavaItem.ATTR_FIELD_TYPE);
			remapCollectionAttribute(oldToNewIDMap, newItem, JavaItem.ATTR_METHOD_THROWS_TYPES);
			remapCollectionAttribute(oldToNewIDMap, newItem, JavaItem.ATTR_INNER_CLASSES);
			remapSingleAttributeID(oldToNewIDMap, newItem, JavaItem.ATTR_OUTER_CLASS);
		}
	}

	/**
	 * Changes an attribute which has an ID reference to another JavaItem to the
	 * corresponding ID in the new index. If the new ID does not exist, the
	 * attribute will be removed.
	 * 
	 * @param oldToNewIDMap
	 *            The mapping from IDs from the old index to IDs in the new
	 *            index. This value cannot be null.
	 * @param newItem
	 *            The item to remap the attribute ID for. This value cannot be
	 *            null.
	 * @param attributeName
	 *            The name of the attribute to remap. This value cannot be null
	 *            or empty.
	 */
	private void remapSingleAttributeID(Map<Integer, Integer> oldToNewIDMap, JavaItem newItem, String attributeName) {
		Map<String, Object> attributes = newItem.getAttributes();
		if (attributes.containsKey(attributeName)) {
			Integer oldValue = newItem.getAttribute(attributeName);
			Integer newValue = oldToNewIDMap.get(oldValue);
			if (newValue != null) {
				newItem.setAttribute(attributeName, newValue);
			} else {
				attributes.remove(attributeName);
			}
		}
	}

	/**
	 * Changes an attribute which has a collection of IDs (List or Set) to other
	 * JavaItems to the corresponding IDs in the new index. If a new ID does not
	 * exist, it will be removed from the collection.
	 * 
	 * @param oldToNewIDMap
	 *            The mapping from IDs from the old index to IDs in the new
	 *            index. This value cannot be null.
	 * @param newItem
	 *            The item to remap the attribute ID collection for. This value
	 *            cannot be null.
	 * @param attributeName
	 *            The name of the attribute to remap. This value cannot be null
	 *            or empty.
	 */
	@SuppressWarnings("unchecked")
	private void remapCollectionAttribute(Map<Integer, Integer> oldToNewIDMap, JavaItem newItem, String attributeName) {
		Map<String, Object> attributes = newItem.getAttributes();
		if (attributes.containsKey(attributeName)) {
			Object oldValue = newItem.getAttribute(attributeName);
			if (oldValue instanceof List) {
				List<Integer> list = (List<Integer>) oldValue;
				remapIDsInList(oldToNewIDMap, list);
			} else if (oldValue instanceof Set) {
				Set<Integer> list = (Set<Integer>) oldValue;
				remapIDsInSet(oldToNewIDMap, list);
			} else {
				throw new IllegalArgumentException("attributeName must be a List or Set type: " + attributeName);
			}
		}
	}

	/**
	 * Changes the JavaItemIDs in the given list by replacing them with the
	 * corresponding IDs in the map. If the map does not contain the original
	 * ID, the ID will be removed from the list.
	 * 
	 * @param oldToNewIDMap
	 *            The mapping from IDs from the old index to IDs in the new
	 *            index. This value cannot be null.
	 * @param list
	 *            The list of IDs to remap. This value cannot be null.
	 */
	private void remapIDsInList(Map<Integer, Integer> oldToNewIDMap, List<Integer> list) {
		for (int i = 0; i < list.size(); i++) {
			Integer childID = list.get(i);
			Integer newChildID = oldToNewIDMap.get(childID);
			if (newChildID != null) {
				list.set(i, newChildID);
			} else {
				list.remove(i);

				// rollback one index to compensate for removing one from the
				// list
				i--;
			}
		}
	}

	/**
	 * Changes the JavaItemIDs in the given set by replacing them with the
	 * corresponding IDs in the map. If the map does not contain the original
	 * ID, the ID will be removed from the set.
	 * 
	 * @param oldToNewIDMap
	 *            The mapping from IDs from the old index to IDs in the new
	 *            index. This value cannot be null.
	 * @param set
	 *            The set of IDs to remap. This value cannot be null.
	 */
	private void remapIDsInSet(Map<Integer, Integer> oldToNewIDMap, Set<Integer> set) {
		Set<Integer> tempSet = new LinkedHashSet<>();
		for (Integer id : set) {
			Integer newID = oldToNewIDMap.get(id);
			if (newID != null) {
				tempSet.add(newID);
			}
			// else it will be removed
		}

		set.clear();
		set.addAll(tempSet);
	}

	/**
	 * Finds all the IDs for JavaItems that are referenced by the given item and
	 * returns them. This includes parent, children, dependencies and incoming,
	 * as well as all IDs in attributes. It does NOT include the item's own ID.
	 * 
	 * @param item
	 *            The item to get IDs from. This value cannot be null.
	 * 
	 * @return The JavaItem IDs that are referenced by the given item. This
	 *         value will not be null, but may be empty.
	 */
	private Set<Integer> findDependentIDs(JavaItem item) {

		Set<Integer> dependentIDs = new HashSet<>();

		if (item.getParentID() != null) {
			dependentIDs.add(item.getParentID());
		}
		dependentIDs.addAll(item.getChildrenIDs());
		dependentIDs.addAll(item.getDependenciesIDs());
		dependentIDs.addAll(item.getIncomingIDs());

		Map<String, Object> attributes = item.getAttributes();
		if (attributes != null) {
			if (attributes.containsKey(JavaItem.ATTR_SUPERCLASS)) {
				Integer superClassID = item.getAttribute(JavaItem.ATTR_SUPERCLASS);
				if (superClassID != null) {
					dependentIDs.add(superClassID);
				}
			}

			if (attributes.containsKey(JavaItem.ATTR_SUPERINTERFACES)) {
				Set<Integer> superInterfaceIDs = item.getAttribute(JavaItem.ATTR_SUPERINTERFACES);
				dependentIDs.addAll(superInterfaceIDs);
			}

			if (attributes.containsKey(JavaItem.ATTR_RETURN_TYPE)) {
				Integer returnTypeID = item.getAttribute(JavaItem.ATTR_RETURN_TYPE);
				if (returnTypeID != null) {
					dependentIDs.add(returnTypeID);
				}
			}

			if (attributes.containsKey(JavaItem.ATTR_METHOD_PARAM_TYPES)) {
				List<Integer> methodParamTypes = item.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
				dependentIDs.addAll(methodParamTypes);
			}

			if (attributes.containsKey(JavaItem.ATTR_ARRAY_BASE_CLASS)) {
				Integer arrayBaseClassID = item.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS);
				if (arrayBaseClassID != null) {
					dependentIDs.add(arrayBaseClassID);
				}
			}

			if (attributes.containsKey(JavaItem.ATTR_FIELD_TYPE)) {
				Integer fieldTypeID = item.getAttribute(JavaItem.ATTR_FIELD_TYPE);
				if (fieldTypeID != null) {
					dependentIDs.add(fieldTypeID);
				}
			}

			if (attributes.containsKey(JavaItem.ATTR_METHOD_THROWS_TYPES)) {
				List<Integer> methodThrowsTypes = item.getAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES);
				dependentIDs.addAll(methodThrowsTypes);
			}

			if (attributes.containsKey(JavaItem.ATTR_METHOD_THROWS_TYPES)) {
				List<Integer> methodThrowsTypes = item.getAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES);
				dependentIDs.addAll(methodThrowsTypes);
			}

			if (attributes.containsKey(JavaItem.ATTR_INNER_CLASSES)) {
				Set<Integer> innerClassIDs = item.getAttribute(JavaItem.ATTR_INNER_CLASSES);
				dependentIDs.addAll(innerClassIDs);
			}

			if (attributes.containsKey(JavaItem.ATTR_OUTER_CLASS)) {
				Integer outerClassID = item.getAttribute(JavaItem.ATTR_OUTER_CLASS);
				if (outerClassID != null) {
					dependentIDs.add(outerClassID);
				}
			}
		}

		return dependentIDs;
	}

	/**
	 * Returns whether the given item is defined in a third-party project.
	 * 
	 * @param item
	 *            The item to check. This value cannot be null, and the index it
	 *            belongs to must have consistent IDs.
	 * 
	 * @return True if the item was defined in a third-party project, false
	 *         otherwise.
	 */
	private boolean isThirdParty(JavaItem item) {
		JavaItem current = item;
		Boolean currentThirdParty = current.getAttribute(JavaItem.ATTR_THIRD_PARTY);
		while (currentThirdParty == null && current != null) {
			current = current.getParent();
			if (current != null) {
				currentThirdParty = current.getAttribute(JavaItem.ATTR_THIRD_PARTY);
			}
		}

		boolean thirdParty = false;
		if (currentThirdParty != null) {
			thirdParty = currentThirdParty;
		}

		return thirdParty;
	}

	/**
	 * Main method used for testing.
	 * 
	 * @param args
	 *            Ignored.
	 */
	public static void main(String[] args) {
		JavaItemIndex index = new JavaItemIndex("v8");
		index.setIDGenerator(new IDGenerator(0));
		JavaItemFactory factory = new JavaItemFactory(index);
		JavaItemUtil.initialize(factory);

		File workspaceDir = new File("H:/WCDE_INT80/workspace");

		Set<File> thirdPartyDirs = new HashSet<>();
		thirdPartyDirs.add(new File(workspaceDir, "WC/lib"));
		thirdPartyDirs.add(new File(workspaceDir, "../../SDP/runtimes/base_v85_stub"));

		FileFilter filter = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				// TODO Auto-generated method stub
				return pathname.getName().startsWith("Catalog-") || pathname.getName().startsWith("commons-");
			}

		};
		boolean isExtractingAPI = true;
		LoadingManager manager = new LoadingManager();
		manager.loadProjects(factory, workspaceDir, thirdPartyDirs, filter, isExtractingAPI);

		for (JavaItem item : index.getItems()) {
			Boolean binary = item.getAttribute(JavaItem.ATTR_BINARY);
			System.out.println(
					"" + item.getID() + " (" + (binary == null || binary.booleanValue() ? "b" : "j") + ") " + item);
		}
	}
}
