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

import java.util.List;

import com.ibm.commerce.cmt.Check;

/**
 * This class can create items.
 * 
 * @author Trent Hoeppner
 */
public class JavaItemFactory {

	/**
	 * The index to which new items will be added.
	 */
	private JavaItemIndex current;

	/**
	 * Constructor for this.
	 * 
	 * @param current
	 *            The index to which new items will be added. This value cannot
	 *            be null.
	 */
	public JavaItemFactory(JavaItemIndex current) {
		Check.notNull(current, "current");

		this.current = current;
	}

	/**
	 * Creates a project item with the given name.
	 * 
	 * @param name
	 *            The name of the project. This value cannot be null or empty.
	 * 
	 * @return The newly created item. This value will not be null.
	 */
	public JavaItem createProject(String name) {
		return createItem(null, name, JavaItemType.PROJECT, true);
	}

	/**
	 * Creates a package item with the given name.
	 * 
	 * @param parent
	 *            The parent project of this. If null, the package will not be
	 *            in a specific project.
	 * @param name
	 *            The name of the package. This value cannot be null.
	 * 
	 * @return The newly created item. This value will not be null.
	 */
	public JavaItem createPackage(JavaItem parent, String name) {
		return createItem(parent, name, JavaItemType.PACKAGE, true);
	}

	/**
	 * Creates a class item with the given name.
	 * 
	 * @param parent
	 *            The parent package of this. This value can be null for the
	 *            primitive types.
	 * @param name
	 *            The name of the class. This value cannot be null or empty.
	 * 
	 * @return The newly created item. This value will not be null.
	 */
	public JavaItem createClass(JavaItem parent, String name) {
		return createItem(parent, name, JavaItemType.CLASS, true);
	}

	/**
	 * Creates a field item with the given name.
	 * 
	 * @param parent
	 *            The parent class of this. This value can be null for an array
	 *            wrapper.
	 * @param name
	 *            The name of the field. This value cannot be null or empty.
	 * 
	 * @return The newly created item. This value will not be null.
	 */
	public JavaItem createField(JavaItem parent, String name) {
		return createItem(parent, name, JavaItemType.FIELD, true);
	}

	/**
	 * Creates a method item with the given name.
	 * 
	 * @param parent
	 *            The parent class of this. This value cannot be null.
	 * @param name
	 *            The name of the project. This value cannot be null or empty.
	 * 
	 * @return The newly created item. This value will not be null.
	 */
	public JavaItem createMethod(JavaItem parent, String name, List<Integer> parameterTypeIDs) {
		Check.notNull(parent, "parent");
		JavaItem method = current.findMethod(parent, name, parameterTypeIDs);

		if (method == null) {
			method = createItem(parent, name, JavaItemType.METHOD, false);
			method.setAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES, parameterTypeIDs);
			parent.getChildrenIDs().add(method.getID());
		}

		return method;
	}

	/**
	 * Creates an item with the given name and type, which is not added to the
	 * index.
	 * 
	 * @param name
	 *            The name of the item. This value cannot be null or empty.
	 * @param type
	 *            The type of the item. This value cannot be null.
	 * 
	 * @return The newly created item. This value will not be null.
	 */
	public JavaItem createUntracked(String name, JavaItemType type) {
		JavaItem item = new BaseJavaItem(name, current);
		item.setType(type);
		return item;
	}

	/**
	 * Creates and returns an item if it does not exist, otherwise just returns
	 * it.
	 * 
	 * @param parent
	 *            The parent of the item to be created. This can be null for
	 *            projects and packages which cannot be found in any project.
	 * @param name
	 *            The name of the item. This value cannot be null or empty.
	 * @param type
	 *            The type of the item to create. This value cannot be null.
	 * @param checkExists
	 *            True indicates that the item will be checked if it exists and
	 *            reused if possible, false indicates that this check will not
	 *            be done (it is assumed that the item does not already exist).
	 * 
	 * @return The newly created item. This value will not be null.
	 */
	public JavaItem createItem(JavaItem parent, String name, JavaItemType type, boolean checkExists) {
		if (parent != null && !parent.getVersion().equals(current.getVersion())) {
			throw new IllegalArgumentException("parent (" + parent.getVersion()
					+ ") must have the same version as this index (" + current.getVersion() + ").");
		}

		boolean exists = false;
		JavaItem item = null;
		if (checkExists) {
			item = current.findItem(parent, name, type);
			if (item != null) {
				exists = true;
			}
		}

		if (item == null) {
			item = new BaseJavaItem(name, current);
			item.setType(type);
		}

		if (!exists) {
			current.addItem(item);
		}

		// the item's ID is definitely now valid

		if (exists) {
			JavaItem oldParent = item.getParent();
			if (oldParent != null) {
				if (oldParent.getChildrenIDs().contains(item.getID())) {
					oldParent.getChildrenIDs().remove((Object) item.getID());
				}
			}
		}

		if (parent != null) {
			item.setParentID(parent.getID());
			if (checkExists) {
				// when JavaItem.copyTo() is used, the child will be from the
				// new index, but the parent is from the old index. we don't
				// want to add the id of the child to the parent in this case.
				if (!parent.getChildrenIDs().contains(item.getID())) {
					parent.getChildrenIDs().add(item.getID());
				}
			}
		}

		return item;
	}

	/**
	 * Returns the index to which items will be added.
	 * 
	 * @return The index to which items will be added. This value will not be
	 *         null.
	 */
	public JavaItemIndex getIndex() {
		return current;
	}
}
