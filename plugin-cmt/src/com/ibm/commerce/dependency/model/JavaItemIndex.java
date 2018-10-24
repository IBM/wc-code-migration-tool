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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.commerce.cmt.ChangeType;
import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.DeltaList;
import com.ibm.commerce.cmt.plan.IDGenerator;
import com.ibm.commerce.dependency.load.Priority;
import com.ibm.commerce.dependency.task.Task;
import com.ibm.commerce.dependency.task.TaskContext;
import com.ibm.commerce.dependency.task.TaskList;

/**
 * This class represents all the objects in an environment for a single version
 * of the toolkit. It may also refer to a base version, in which case this will
 * store the differences with the base. If an index has a base index, and an
 * item is removed from the later index, all related items will have references
 * to that item removed, and the item will be set to null for its ID in the
 * index. This null value is used as a placeholder to signify that the item is
 * not in the current version, but is in some previous version.
 * 
 * @author Trent Hoeppner
 */
public class JavaItemIndex {

	/**
	 * The number of characters in a name that will be used for the key in an
	 * AlphaIndex. This groups together names that start with the same
	 * characters up to this length.
	 */
	private static final int NUM_CHARACTERS_FOR_ALPHA_INDEX = 18;

	/**
	 * Variable for the input JavaItems to copy into DeltaJavaItems.
	 */
	private static final String INPUT_ITEMS = "InputItems";

	/**
	 * Variable for the JavaItemIndex that will contain the new DeltaJavaItems.
	 */
	private static final String NEW_JAVA_ITEM_INDEX = "NewIndex";

	/**
	 * Variable for the output DeltaJavaItems which are based on other
	 * JavaItems.
	 */
	private static final String OUTPUT_ITEMS = "OutputItems";

	/**
	 * The version of a product that this index represents.
	 */
	private String version;

	/**
	 * The index which contains the items for the previous version. If this
	 * value is null, there is no previous version.
	 */
	private JavaItemIndex base;

	/**
	 * The items in this.
	 */
	private List<JavaItem> items;

	/**
	 * The number of non-null items in this.
	 */
	private int nonNullSize;

	/**
	 * An map to speed up searching by type and name.
	 */
	private Map<JavaItemType, AlphaIndex> typeToAlphaIndexMap;

	/**
	 * The generator which is used to create IDs for new items.
	 */
	private IDGenerator idGen;

	/**
	 * A read-write lock for the variables map.
	 */
	final private ReentrantReadWriteLock itemsLock = new ReentrantReadWriteLock();

	/**
	 * True if the items in this index are not in a stable state (still being
	 * copied in the constructor).
	 */
	private boolean inFlux;

	/**
	 * Constructor for this without a base index.
	 * 
	 * @param version
	 *            The version identifier. This value cannot be null or empty.
	 */
	public JavaItemIndex(String version) {
		Check.notNull(version, "version");

		this.version = version;
		items = new ArrayList<>();
		nonNullSize = 0;
	}

	/**
	 * Constructor for this with a base index. The base index represents a
	 * previous version of this, and this contains the differences with the
	 * base.
	 * 
	 * @param version
	 *            The version identifier. This value cannot be null or empty.
	 * @param base
	 *            The base index which represents a previous version of this.
	 *            This value cannot be null.
	 */
	public JavaItemIndex(String version, JavaItemIndex base) {
		Check.notNull(version, "version");
		Check.notNull(base, "base");

		this.version = version;
		this.base = base;

		setInFlux(true);

		List<JavaItem> deltaList = new ArrayList<>(base.getItems().size());

		List<TaskContext> contexts = new ArrayList<TaskContext>();

		// map
		TaskList taskList = new TaskList();
		Map<Integer, JavaItem> indexToInputMap = new HashMap<>();
		int i = 0;
		for (JavaItem item : base.getItems()) {
			if (item != null) {
				if (indexToInputMap.size() >= 1000) {
					indexToInputMap = addTask(taskList, indexToInputMap, contexts);
				}

				indexToInputMap.put(i, item);
			}

			deltaList.add(null);
			i++;
		}

		// add the remaining tasks
		addTask(taskList, indexToInputMap, contexts);

		// wait for finish
		taskList.start();
		taskList.waitForCompletion();

		// reduce
		for (TaskContext context : contexts) {
			Map<Integer, JavaItem> outputItems = context.get(OUTPUT_ITEMS);
			for (Map.Entry<Integer, JavaItem> entry : outputItems.entrySet()) {
				Integer index = entry.getKey();
				JavaItem deltaItem = entry.getValue();
				deltaList.set(index, deltaItem);
			}
		}

		items = new DeltaList<JavaItem>(deltaList);
		nonNullSize = base.size();

		setInFlux(false);
	}

	/**
	 * Adds a new task to the given task list which will create delta copies of
	 * the items in the given map.
	 * 
	 * @param taskList
	 *            The list to add the task to. This value cannot be null.
	 * @param indexToInputMap
	 *            A mapping from the index of the item in the JavaItemIndex to
	 *            the item that is to be copied. This value cannot be null, but
	 *            may be empty.
	 * @param contexts
	 *            The list of context objects for tasks that were previously
	 *            created, to which the new task's context should be added. This
	 *            value cannot be null, but may be empty.
	 * 
	 * @return A new, empty map which can be used in creating the next task for
	 *         other items to be copied.
	 */
	private Map<Integer, JavaItem> addTask(TaskList taskList, Map<Integer, JavaItem> indexToInputMap,
			List<TaskContext> contexts) {
		TaskContext context = new TaskContext(taskList);
		context.put(INPUT_ITEMS, indexToInputMap);
		context.put(NEW_JAVA_ITEM_INDEX, this);

		CreateDeltaJavaItemTask task = new CreateDeltaJavaItemTask("CreateDeltaJavaItem", context);

		taskList.addTask(task, Priority.TOP_LEVEL);
		contexts.add(context);

		indexToInputMap = new HashMap<>();
		return indexToInputMap;
	}

	/**
	 * Returns whether this index is stable and can be used.
	 * 
	 * @return True if this index is NOT stable, false if it is stable.
	 */
	public boolean isInFlux() {
		itemsLock.readLock().lock();
		try {
			return inFlux;
		} finally {
			itemsLock.readLock().unlock();
		}
	}

	private void setInFlux(boolean b) {
		itemsLock.writeLock().lock();
		try {
			inFlux = b;
		} finally {
			itemsLock.writeLock().unlock();
		}
	}

	/**
	 * Returns the version that this represents.
	 * 
	 * @return The version that this represents. This value will not be null.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Returns the index which contains the items for the previous version.
	 * 
	 * @return The index for the previous version, or null if there is no
	 *         previous version.
	 */
	public JavaItemIndex getBase() {
		return base;
	}

	/**
	 * Returns the items in this. This list may contain null values, indicating
	 * that the item has been removed.
	 * 
	 * @return The items in this. This value will not be null, but may be empty.
	 */
	public List<JavaItem> getItems() {
		itemsLock.readLock().lock();
		try {
			return Collections.unmodifiableList(new ArrayList<>(items));
		} finally {
			itemsLock.readLock().unlock();
		}
	}

	/**
	 * Returns the number of non-null items in this. If any items were removed
	 * in this index, this number will be smaller than
	 * <code>getItems().size()</code>.
	 * 
	 * @return The number of non-null items in this. This value will be &gt;= 0.
	 */
	public int size() {
		return nonNullSize;
	}

	/**
	 * Adds the given item to this and assigns an ID to it.
	 * 
	 * @param item
	 *            The item to add. This value cannot be null.
	 */
	public void addItem(JavaItem item) {
		itemsLock.writeLock().lock();
		try {
			if (!(item instanceof DeltaJavaItem)) {
				int newID = getIDGenerator().nextID();
				if (newID != items.size()) {
					throw new IllegalArgumentException(
							"item " + item.getName() + " was added in the wrong order, newID = " + newID
									+ ", expected ID = " + items.size() + ".");
				}

				item.setID(newID);
				items.add(item);
				nonNullSize++;
			} else {
				int id = item.getID();
				items.set(id, item);
			}

			if (typeToAlphaIndexMap != null) {
				AlphaIndex index = typeToAlphaIndexMap.get(item.getType());
				if (index != null) {
					index.add(item);
				}
			}
		} finally {
			itemsLock.writeLock().unlock();
		}
	}

	/**
	 * Adds the given item to this without changing its ID. However, the item's
	 * ID will be checked to ensure that it is in the correct order.
	 * 
	 * @param item
	 *            The item to add. This value cannot be null.
	 */
	public void addItemPreserveID(JavaItem item) {
		itemsLock.writeLock().lock();

		if (items.size() != item.getID()) {
			throw new IllegalArgumentException("item " + item.getName() + " was added in the wrong order.");
		}

		try {
			items.add(item);
			nonNullSize++;

			if (typeToAlphaIndexMap != null) {
				AlphaIndex index = typeToAlphaIndexMap.get(item.getType());
				if (index != null) {
					index.add(item);
				}
			}
		} finally {
			itemsLock.writeLock().unlock();
		}
	}

	public void set(int index, DeltaJavaItem item) {
		itemsLock.writeLock().lock();
		try {
			if (!isInFlux()) {
				items.set(index, item);
			}
		} finally {
			itemsLock.writeLock().unlock();
		}
	}

	/**
	 * Returns the ID generator for new items. If this has a base index, that
	 * generator will be used instead.
	 * 
	 * @return The ID generator for new items. This value will be null if this
	 *         has no base and {@link #setIDGenerator(IDGenerator)} has not been
	 *         called.
	 */
	private IDGenerator getIDGenerator() {
		itemsLock.writeLock().lock();
		try {
			IDGenerator gen = idGen;
			if (gen == null && base != null) {
				gen = new IDGenerator(base.getIDGenerator());
				idGen = gen;
			}

			return gen;
		} finally {
			itemsLock.writeLock().unlock();
		}
	}

	/**
	 * Sets the given ID generator for this. It is an error to call this method
	 * if this index has a base index.
	 * 
	 * @param idGen
	 *            The ID generator to set. This value cannot be null.
	 */
	public void setIDGenerator(IDGenerator idGen) {
		if (base != null) {
			throw new IllegalStateException("Cannot set a new generator because this index is based on another index.");
		}

		this.idGen = idGen;
	}

	/**
	 * Returns the item with the given ID. If the ID is for an item that has
	 * been removed from this, null will be returned.
	 * 
	 * @param id
	 *            The ID of the item to get. This value must be &gt;= 0.
	 * 
	 * @return The item with the given ID. This value will be null if the item
	 *         was removed from this.
	 */
	public JavaItem getItem(int id) {
		itemsLock.readLock().lock();
		try {
			return items.get(id);
		} finally {
			itemsLock.readLock().unlock();
		}
	}

	/**
	 * Finds the class item with the given name.
	 * 
	 * @param packageName
	 *            The package to which the class belongs. This value cannot be
	 *            null.
	 * @param className
	 *            The name of the class to find. This value cannot be null or
	 *            empty.
	 * 
	 * @return The class item with the given name, or null if the class could
	 *         not be found.
	 */
	public JavaItem findClass(String packageName, String className) {
		AlphaIndex index = ensureIndexExists(JavaItemType.CLASS);
		itemsLock.readLock().lock();
		try {
			JavaItem found = null;
			List<JavaItem> matchingClasses = index.findAllWithSameName(className);
			for (JavaItem classItem : matchingClasses) {
				if (classItem.getParent() != null && classItem.getParent().getName().equals(packageName)) {
					found = classItem;
					break;
				}
			}

			return found;
		} finally {
			itemsLock.readLock().unlock();
		}
	}

	/**
	 * Finds all package items with the same name across all projects.
	 * 
	 * @param packageName
	 *            The name of the packages to find. This value cannot be null.
	 * 
	 * @return The package items with the same name across all projects. This
	 *         value will not be null, but may be empty.
	 */
	public List<JavaItem> findPackages(String packageName) {
		AlphaIndex index = ensureIndexExists(JavaItemType.PACKAGE);
		itemsLock.readLock().lock();
		try {
			List<JavaItem> packagesForName = index.findAllWithSameName(packageName);

			return packagesForName;
		} finally {
			itemsLock.readLock().unlock();
		}
	}

	/**
	 * Removes the given item from this.
	 * 
	 * @param item
	 *            The item to remove. This value cannot be null.
	 */
	public void removeItem(JavaItem item) {
		itemsLock.writeLock().lock();
		try {
			int itemID = item.getID();
			items.set(itemID, null);
			nonNullSize--;

			removeFromDependencies(item);
			if (typeToAlphaIndexMap != null) {
				typeToAlphaIndexMap.put(item.getType(), null);
			}
		} finally {
			itemsLock.writeLock().unlock();
		}
	}

	/**
	 * Removes the given item from all the items that depend on it.
	 * 
	 * @param item
	 *            The item to remove from the items that depend on it. This
	 *            value cannot be null.
	 */
	private void removeFromDependencies(JavaItem item) {
		Integer itemID = item.getID();

		JavaItem parent = item.getParent();
		if (parent != null) {
			parent.getChildrenIDs().remove(itemID);
		}

		for (Integer childID : item.getChildrenIDs()) {
			JavaItem child = getItem(childID);
			child.setParentID(null);
		}

		for (Integer dependencyID : item.getDependenciesIDs()) {
			JavaItem dependency = getItem(dependencyID);
			dependency.getIncomingIDs().remove(itemID);
		}

		for (Integer incomingID : item.getIncomingIDs()) {
			JavaItem incoming = getItem(incomingID);
			incoming.getDependenciesIDs().remove(itemID);
		}

		Map<String, Object> attributes = item.getAttributes();
		if (attributes != null) {
			if (attributes.containsKey(JavaItem.ATTR_SUPERCLASS)) {
				Integer superClassID = item.getAttribute(JavaItem.ATTR_SUPERCLASS);
				JavaItem superClass = getItem(superClassID);
				Set<Integer> subClassIDs = superClass.getAttribute(JavaItem.ATTR_SUBCLASSES);
				subClassIDs.remove(itemID);
			}

			if (attributes.containsKey(JavaItem.ATTR_SUPERINTERFACES)) {
				Set<Integer> superInterfaceIDs = item.getAttribute(JavaItem.ATTR_SUPERINTERFACES);
				for (Integer superInterfaceID : superInterfaceIDs) {
					JavaItem superInterface = getItem(superInterfaceID);
					Set<Integer> subClassIDs = superInterface.getAttribute(JavaItem.ATTR_SUBCLASSES);
					subClassIDs.remove(itemID);
				}
			}

			if (attributes.containsKey(JavaItem.ATTR_SUBCLASSES)) {
				Set<Integer> subClassIDs = item.getAttribute(JavaItem.ATTR_SUBCLASSES);
				for (Integer subClassID : subClassIDs) {
					JavaItem subClass = getItem(subClassID);
					Integer superClassID = subClass.getAttribute(JavaItem.ATTR_SUPERCLASS);
					if (superClassID != null && superClassID.equals(itemID)) {
						subClass.setAttribute(JavaItem.ATTR_SUPERCLASS, null);
					}

					Set<Integer> superInterfaceIDs = subClass.getAttribute(JavaItem.ATTR_SUPERINTERFACES);
					if (superInterfaceIDs != null) {
						superInterfaceIDs.remove(itemID);
					}
				}
			}

			if (attributes.containsKey(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS)) {
				Set<Integer> methodOrFieldIDs = item.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS);
				for (Integer methodOrFieldID : methodOrFieldIDs) {
					JavaItem methodOrField = getItem(methodOrFieldID);
					Integer returnTypeID = methodOrField.getAttribute(JavaItem.ATTR_RETURN_TYPE);
					if (returnTypeID != null && returnTypeID.equals(itemID)) {
						methodOrField.setAttribute(JavaItem.ATTR_RETURN_TYPE, null);
					}

					List<Integer> methodParamTypeIDs = methodOrField.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
					if (methodParamTypeIDs != null) {
						methodParamTypeIDs.remove(itemID);
					}

					List<Integer> methodThrowsTypeIDs = methodOrField.getAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES);
					if (methodThrowsTypeIDs != null) {
						methodThrowsTypeIDs.remove(itemID);
					}

					Integer fieldTypeID = methodOrField.getAttribute(JavaItem.ATTR_FIELD_TYPE);
					if (fieldTypeID != null && fieldTypeID.equals(itemID)) {
						methodOrField.setAttribute(JavaItem.ATTR_FIELD_TYPE, null);
					}
				}
			}

			if (attributes.containsKey(JavaItem.ATTR_RETURN_TYPE)) {
				Integer classID = item.getAttribute(JavaItem.ATTR_RETURN_TYPE);
				JavaItem classItem = getItem(classID);
				Set<Integer> methodIDs = classItem.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS);
				methodIDs.remove(itemID);
			}

			if (attributes.containsKey(JavaItem.ATTR_METHOD_PARAM_TYPES)) {
				List<Integer> paramTypeIDs = item.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
				for (Integer paramTypeID : paramTypeIDs) {
					JavaItem paramType = getItem(paramTypeID);
					Set<Integer> methodIDs = paramType.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS);
					methodIDs.remove(itemID);
				}
			}

			if (attributes.containsKey(JavaItem.ATTR_METHOD_THROWS_TYPES)) {
				List<Integer> throwsTypeIDs = item.getAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES);
				for (Integer throwsTypeID : throwsTypeIDs) {
					JavaItem paramType = getItem(throwsTypeID);
					Set<Integer> methodIDs = paramType.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS);
					methodIDs.remove(itemID);
				}
			}

			if (attributes.containsKey(JavaItem.ATTR_USED_BY_ARRAY_CLASS)) {
				Integer arrayClassID = item.getAttribute(JavaItem.ATTR_USED_BY_ARRAY_CLASS);
				JavaItem arrayClass = getItem(arrayClassID);
				arrayClass.setAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS, null);
			}

			if (attributes.containsKey(JavaItem.ATTR_ARRAY_BASE_CLASS)) {
				Integer arrayBaseClassID = item.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS);
				JavaItem arrayBaseClass = getItem(arrayBaseClassID);
				arrayBaseClass.setAttribute(JavaItem.ATTR_USED_BY_ARRAY_CLASS, null);
			}

			if (attributes.containsKey(JavaItem.ATTR_FIELD_TYPE)) {
				Integer classID = item.getAttribute(JavaItem.ATTR_FIELD_TYPE);
				JavaItem classItem = getItem(classID);
				Set<Integer> methodIDs = classItem.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS);
				methodIDs.remove(itemID);
			}

			if (attributes.containsKey(JavaItem.ATTR_OUTER_CLASS)) {
				Integer outerClassID = item.getAttribute(JavaItem.ATTR_OUTER_CLASS);
				JavaItem outerClass = getItem(outerClassID);
				Set<Integer> innerClassIDs = outerClass.getAttribute(JavaItem.ATTR_INNER_CLASSES);
				innerClassIDs.remove(itemID);
			}

			if (attributes.containsKey(JavaItem.ATTR_INNER_CLASSES)) {
				Set<Integer> innerClassIDs = item.getAttribute(JavaItem.ATTR_INNER_CLASSES);
				for (Integer innerClassID : innerClassIDs) {
					JavaItem innerClass = getItem(innerClassID);
					innerClass.setAttribute(JavaItem.ATTR_OUTER_CLASS, null);
				}
			}
		}
	}

	/**
	 * Removes the given item with the given ID from this.
	 * 
	 * @param itemID
	 *            The ID of the item to remove. This value cannot be null.
	 */
	public void removeItem(int itemID) {
		itemsLock.writeLock().lock();
		try {
			JavaItem item = getItem(itemID);
			removeItem(item);
		} finally {
			itemsLock.writeLock().unlock();
		}
	}

	/**
	 * Changes all item IDs to that they are consecutive. They may become
	 * non-consecutive after calling {@link #removeItem(JavaItem)}.
	 */
	public void consolidateIDs() {
		itemsLock.writeLock().lock();
		try {
			idGen = new IDGenerator(0);
			for (JavaItem item : items) {
				int id = idGen.nextID();
				item.setID(id);
			}
		} finally {
			itemsLock.writeLock().unlock();
		}
	}

	/**
	 * Finds the method with the given parent (matched by name, including all
	 * ancestors), name, and parameter list.
	 * 
	 * @param parentItem
	 *            The parent item that contains the names of parents of the
	 *            target item. If null, the target object will not have a
	 *            parent.
	 * @param name
	 *            The name of the method to find. This value cannot be null.
	 * @param parameterTypeIDs
	 *            The IDs of the parameters for the method. This value cannot be
	 *            null, but may be empty.
	 * 
	 * @return The item that was found, or null if there is no such item.
	 */
	public JavaItem findMethod(JavaItem parentItem, String name, List<Integer> parameterTypeIDs) {
		Check.notNull(parameterTypeIDs, "parameterTypeIDs");
		long startTime = System.currentTimeMillis();
		AlphaIndex index = ensureIndexExists(JavaItemType.METHOD);
		itemsLock.readLock().lock();
		try {
			List<JavaItem> matchingItems = index.findAllWithSameName(name);
			JavaItem found = null;
			for (JavaItem item : matchingItems) {
				if (item.getName().equals(name) && item.getType() == JavaItemType.METHOD) {
					List<Integer> existingParameterTypeIDs = item.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
					if (existingParameterTypeIDs == null) {
						existingParameterTypeIDs = Collections.emptyList();
					}

					if (existingParameterTypeIDs.equals(parameterTypeIDs)) {
						if (isNamesMatch(parentItem, item.getParent())) {
							found = item;
							break;
						}
					}
				}
			}

			return found;
		} finally {
			itemsLock.readLock().unlock();
			long endTime = System.currentTimeMillis();
			long diff = endTime - startTime;
			if (diff > 100) {
				System.out.println("Took " + diff + " ms to find " + parentItem + ":" + name);
			}
		}
	}

	/**
	 * Finds the item with the given parent (matched by name, including all
	 * ancestors), name, and type.
	 * <p>
	 * Note: Do not use this to find methods, as it will not find all overloaded
	 * versions with the same method name, only the first one. Instead, use
	 * {@link #findMethod(JavaItem, String, List)} to do a pure find, or
	 * {@link JavaItemFactory#createMethod(JavaItem, String, List)} to create a
	 * method if it does not yet exist.
	 * 
	 * @param parentItem
	 *            The parent item that contains the names of parents of the
	 *            target item. If null, the target object will not have a
	 *            parent.
	 * @param name
	 *            The name of the object to find. This value cannot be null.
	 * @param type
	 *            The type of the object to find. This value cannot be null.
	 * 
	 * @return The item that was found, or null if there is no such item.
	 */
	public JavaItem findItem(JavaItem parentItem, String name, JavaItemType type) {
		long startTime = System.currentTimeMillis();
		AlphaIndex index = ensureIndexExists(type);
		itemsLock.readLock().lock();
		try {
			List<JavaItem> matchingItems = index.findAllWithSameName(name);
			JavaItem found = null;
			for (JavaItem item : matchingItems) {
				if (item.getName().equals(name) && item.getType() == type) {
					if (isNamesMatch(parentItem, item.getParent())) {
						found = item;
						break;
					}
				}
			}

			return found;
		} finally {
			itemsLock.readLock().unlock();
			long endTime = System.currentTimeMillis();
			long diff = endTime - startTime;
			if (diff > 100) {
				System.out.println("Took " + diff + " ms to find " + parentItem + ":" + name);
			}
		}
	}

	public void mergeToBase() {
		if (base == null) {
			return;
		}

		base.setInFlux(true);

		JavaItemFactory baseFactory = new JavaItemFactory(base);

		DeltaList<JavaItem> deltaList = (DeltaList<JavaItem>) items;
		List<DeltaList.Change<JavaItem>> changes = deltaList.getChanges();
		for (DeltaList.Change<JavaItem> change : changes) {
			JavaItem deltaItem = change.getObject();
			int index = change.getIndex();
			if (change.getType() == ChangeType.ADD) {
				JavaItem parent;
				if (deltaItem.getParentID() != null) {
					parent = base.getItem(deltaItem.getParentID());
				} else {
					parent = null;
				}

				JavaItem newBaseItem = baseFactory.createItem(parent, deltaItem.getName(), deltaItem.getType(), false);

				if (newBaseItem.getID() != deltaItem.getID()) {
					throw new IllegalStateException("The change contained a new item with ID " + deltaItem.getID()
							+ " but when added to the base, the ID was " + newBaseItem.getID());
				}

				OverwriteJavaItemVisitor visitor = new OverwriteJavaItemVisitor(newBaseItem);
				deltaItem.accept(visitor);
			} else if (change.getType() == ChangeType.UPDATE) {
				JavaItem baseItem = base.getItem(deltaItem.getID());
				OverwriteJavaItemVisitor visitor = new OverwriteJavaItemVisitor(baseItem);
				deltaItem.accept(visitor);
			} else {
				base.removeItem(index);
			}
		}

		base.setInFlux(false);
	}

	/**
	 * Ensures that items of the given type are indexed by name and returns that
	 * index.
	 * 
	 * @param type
	 *            The type to find the index for. If null, all items will be
	 *            indexed by their own types and null will be returned.
	 * 
	 * @return The index for items of the given type, or null if the type was
	 *         null.
	 */
	private AlphaIndex ensureIndexExists(JavaItemType type) {
		AlphaIndex index = null;
		itemsLock.readLock().lock();
		try {
			if (typeToAlphaIndexMap == null) {
				typeToAlphaIndexMap = new HashMap<>();
			}

			if (type != null) {
				index = typeToAlphaIndexMap.get(type);
			}
		} finally {
			itemsLock.readLock().unlock();
		}

		if (index == null) {
			itemsLock.writeLock().lock();
			try {
				index = new AlphaIndex();
				typeToAlphaIndexMap.put(type, index);

				for (JavaItem item : items) {
					if (item == null) {
						continue;
					}

					AlphaIndex currentIndex;
					if (type == item.getType()) {
						currentIndex = index;
					} else if (type == null) {
						currentIndex = typeToAlphaIndexMap.get(item.getType());
						if (currentIndex == null) {
							currentIndex = new AlphaIndex();
							typeToAlphaIndexMap.put(item.getType(), currentIndex);
						}
					} else {
						continue;
					}

					currentIndex.add(item);
				}
			} finally {
				itemsLock.writeLock().unlock();
			}
		}

		return index;
	}

	/**
	 * Returns whether the given items match names, along with all their
	 * ancestors' names.
	 * 
	 * @param item1
	 *            The first item to compare. This value may be null.
	 * @param item2
	 *            The first item to compare. This value may be null.
	 * 
	 * @return True if both items are null or if their names and all ancestors'
	 *         names are the same, false otherwise.
	 */
	private boolean isNamesMatch(JavaItem item1, JavaItem item2) {
		boolean match;
		if (item1 == null) {
			if (item2 == null) {
				// both null
				match = true;
			} else {
				// one null, one not null
				match = false;
			}
		} else {
			if (item2 == null) {
				// one not null, one null
				match = false;
			} else {
				if (item1.getName().equals(item2.getName())) {
					match = isNamesMatch(item1.getParent(), item2.getParent());
				} else {
					// names are different
					match = false;
				}
			}
		}

		return match;
	}

	/**
	 * This task is used to create delta copies of several JavaItems.
	 */
	public static class CreateDeltaJavaItemTask extends Task<TaskContext> {

		/**
		 * The required inputs for this task.
		 */
		private static final Set<String> INPUTS = new HashSet<>(Arrays.asList(INPUT_ITEMS, NEW_JAVA_ITEM_INDEX));

		/**
		 * The expected outputs for this task.
		 */
		private static final Set<String> OUTPUTS = new HashSet<>(Arrays.asList(OUTPUT_ITEMS));

		/**
		 * Constructor for this.
		 * 
		 * @param name
		 *            The name of the task. This value cannot be null or empty.
		 * @param context
		 *            The context which contains variables for the task. This
		 *            value cannot be null.
		 */
		public CreateDeltaJavaItemTask(String name, TaskContext context) {
			super(name, context);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Set<String> getInputConstraints() {
			return INPUTS;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Set<String> getOutputConstraints() {
			return OUTPUTS;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void execute(TaskContext context) throws Exception {
			Map<Integer, JavaItem> baseItems = context.get(INPUT_ITEMS);
			JavaItemIndex newIndex = context.get(NEW_JAVA_ITEM_INDEX);

			Map<Integer, JavaItem> deltaItems = new HashMap<>(baseItems.size());
			for (Map.Entry<Integer, JavaItem> entry : baseItems.entrySet()) {
				Integer index = entry.getKey();
				JavaItem baseItem = entry.getValue();
				JavaItem deltaItem = new DeltaJavaItem(baseItem, newIndex);
				deltaItems.put(index, deltaItem);
			}

			context.put(OUTPUT_ITEMS, deltaItems);
		}

	}

	/**
	 * This class partially indexes items by their name. The first few
	 * characters of the name form the key, and all items with the same name are
	 * stored with that key. Since JavaItem names are not unique, a map with the
	 * name as the key will not work, so this class allows quickly narrowing
	 * down the search for items with the same name without having a list for
	 * every name and wasting memory.
	 */
	private class AlphaIndex {
		private Map<String, List<JavaItem>> firstFewLettersToItemsMap = new HashMap<>();

		/**
		 * Constructor for this.
		 */
		public AlphaIndex() {
			// do nothing
		}

		/**
		 * Adds the given item to this.
		 * 
		 * @param item
		 *            The item to add. This value cannot be null.
		 */
		public void add(JavaItem item) {
			Check.notNull(item, "item");

			String firstFew = getFirstFewLetters(item.getName());
			List<JavaItem> matchingList = firstFewLettersToItemsMap.get(firstFew);
			if (matchingList == null) {
				matchingList = new ArrayList<>();
				firstFewLettersToItemsMap.put(firstFew, matchingList);
			}

			matchingList.add(item);
		}

		/**
		 * Finds all items that have the given name.
		 * 
		 * @param name
		 *            The name of the items to find. This value cannot be null.
		 * 
		 * @return The items that have the given name. This value will not be
		 *         null, but may be empty.
		 */
		public List<JavaItem> findAllWithSameName(String name) {
			Check.notNull(name, "name");

			List<JavaItem> found = new ArrayList<>();
			String firstFew = getFirstFewLetters(name);
			List<JavaItem> matchingList = firstFewLettersToItemsMap.get(firstFew);
			if (matchingList != null) {
				for (JavaItem item : matchingList) {
					if (item.getName().equals(name)) {
						found.add(item);
					}
				}
			}

			return found;
		}

		/**
		 * Finds the first few characters of the given name, or all characters
		 * if the name contains less than few characters.
		 * 
		 * @param name
		 *            The name to get the letters from. This value cannot be
		 *            null.
		 * 
		 * @return The first few characters of the name. This value will not be
		 *         null, but may be empty if the name is empty.
		 */
		private String getFirstFewLetters(String name) {
			Check.notNull(name, "name");

			int actualSize = Math.min(NUM_CHARACTERS_FOR_ALPHA_INDEX, name.length());
			return name.substring(0, actualSize);
		}
	}
}
