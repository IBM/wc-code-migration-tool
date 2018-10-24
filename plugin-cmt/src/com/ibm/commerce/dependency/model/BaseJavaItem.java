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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.commerce.cmt.Check;

/**
 * This class represents the original version of some part of a Java program.
 * 
 * @see DeltaJavaItem
 * @author Trent Hoeppner
 */
public class BaseJavaItem extends AbstractJavaItem {

	/**
	 * The parent of this JavaItem. A project JavaItem has a null parent.
	 */
	private Integer parent;

	/**
	 * The children of this JavaItem. This list is never null, but will always
	 * be empty for method JavaItems.
	 */
	private List<Integer> children = new CopyOnWriteArrayList<>();

	/**
	 * The outgoing dependencies of this JavaItem. For a project, this list will
	 * contain other projects that this project depends on. For a class, this
	 * list will contain other classes that are referenced by this class. For a
	 * method, this list will contain other methods called by this method.
	 */
	private List<Integer> dependencies = new CopyOnWriteArrayList<>();

	/**
	 * The incoming dependencies of this JavaItem. Every object in this list has
	 * an outgoing dependency to this JavaItem, so that with the
	 * {@link #dependencies} every JavaItem knows what it depends on, and what
	 * depends on it.
	 */
	private List<Integer> incoming = new CopyOnWriteArrayList<>();

	/**
	 * The name of this item. This value is never null.
	 */
	private String name;

	/**
	 * The type of this item. This value is never null.
	 */
	private JavaItemType type = JavaItemType.PROJECT;

	/**
	 * The attributes which contain type-specific properties of this. This value
	 * may be null if there are no attributes to save memory.
	 */
	private Map<String, Object> attributes;

	/**
	 * The identifier for this item.
	 */
	private int id = -1;

	/**
	 * Constructor for this.
	 * 
	 * @param name
	 *            The name of this item. This value cannot be null.
	 * @param index
	 *            The index that this is a part of. This value cannot be null.
	 */
	public BaseJavaItem(String name, JavaItemIndex index) {
		super(index);

		Check.notNullOrEmpty(name, "name");

		this.name = name;
	}

	/**
	 * Returns the children of this.
	 *
	 * @return The children of this. Will not be null, but may be empty.
	 */
	@Override
	public List<Integer> getChildrenIDs() {
		return children;
	}

	/**
	 * Returns the peers that this depends on.
	 *
	 * @return The dependencies of this. Will not be null, but may be empty.
	 */
	@Override
	public List<Integer> getDependenciesIDs() {
		return dependencies;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns the type of this.
	 * 
	 * @return The type of this. This value will not be null.
	 */
	@Override
	public JavaItemType getType() {
		return type;
	}

	/**
	 * Sets the type of this.
	 * 
	 * @param type
	 *            The type of this. This value cannot be null.
	 */
	@Override
	public void setType(JavaItemType type) {
		if (type == null) {
			throw new NullPointerException("type cannot be null.");
		}

		this.type = type;

		if (type == JavaItemType.METHOD) {
			children = Collections.emptyList();
		}
	}

	/**
	 * Returns the items that depend on this. This should be kept in sync with
	 * the dependencies. For example, if there are two items, A and B, and A
	 * depends on B, then B should be added to A's dependencies list, and A
	 * should be added to B's incoming list.
	 * 
	 * @return The items that depend on this. This value will not be null, but
	 *         may be empty.
	 */
	@Override
	public List<Integer> getIncomingIDs() {
		return incoming;
	}

	/**
	 * Returns the attributes of this. See the CONSTANTS of this class that
	 * start with "ATTR_" to see the attributes available for each type of item.
	 * 
	 * @return The attributes of this. This value may be null or empty if there
	 *         are no attributes.
	 */
	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * Returns the value for an attribute of this. See the CONSTANTS of this
	 * class that start with "ATTR_" to see the attributes available for each
	 * type of item.
	 * 
	 * @param name
	 *            The name of the attribute. This value cannot be null, but may
	 *            be empty.
	 * 
	 * @return The value of the attribute. This value may be null if the
	 *         attribute's value is null, or if the attribute has not been set.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAttribute(String name) {
		T value = null;
		if (attributes != null) {
			value = (T) attributes.get(name);
		}

		return value;
	}

	/**
	 * Returns the identifier of this. Every item in the system must have a
	 * unique identifier in order to work properly with {@link JavaItemIndex}.
	 * 
	 * @return The identifier of this.
	 */
	public int getID() {
		return id;
	}

	/**
	 * Sets the identifier of this. Every item in the system must have a unique
	 * identifier in order to work properly with {@link JavaItemIndex}.
	 * 
	 * @param id
	 *            The identifier of this.
	 */
	public void setID(int id) {
		this.id = id;
	}

	@Override
	public Integer getParentID() {
		return parent;
	}

	@Override
	public void setParentID(Integer parent) {
		this.parent = parent;
	}

	@Override
	public void setAttribute(String name, Object value) {
		if (attributes == null) {
			attributes = new HashMap<>();
		}

		attributes.put(name, value);
	}
}
