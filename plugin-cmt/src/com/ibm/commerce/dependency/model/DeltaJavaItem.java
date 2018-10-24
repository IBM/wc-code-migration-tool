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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.commerce.cmt.ChangeListener;
import com.ibm.commerce.cmt.Check;
import com.ibm.commerce.cmt.NotifierList;
import com.ibm.commerce.cmt.NotifierSet;

/**
 * This class represents changes in an item from a base item.
 * 
 * @see BaseJavaItem
 * @author Trent Hoeppner
 */
public class DeltaJavaItem extends AbstractJavaItem implements ChangeListener {

	/**
	 * The item that this is based off of.
	 */
	private JavaItem base;

	/**
	 * The parent ID of this item, which may be different from the base.
	 */
	private Integer parent;

	/**
	 * The children of this item, which may have items added or removed from the
	 * base.
	 */
	private List<Integer> children;

	/**
	 * This dependencies of this item, which may have items added or removed
	 * from the base.
	 */
	private List<Integer> dependencies;

	/**
	 * The incoming dependencies of this item, which may have items added or
	 * removed from the base.
	 */
	private List<Integer> incoming;

	/**
	 * The attributes of this item, which may be different from the base.
	 */
	private Map<String, Object> attributes;

	/**
	 * Constructor for this.
	 * 
	 * @param base
	 *            The item that is the previous version of this. This value
	 *            cannot be null.
	 * @param index
	 *            The index that this is a part of. This value cannot be null.
	 */
	public DeltaJavaItem(JavaItem base, JavaItemIndex index) {
		super(index);

		Check.notNull(base, "base");

		this.base = base;

		parent = base.getParentID();

		children = new NotifierList<Integer>(new ArrayList<>(), this);
		dependencies = new NotifierList<Integer>(new ArrayList<>(), this);
		incoming = new NotifierList<Integer>(new ArrayList<>(), this);

		// copy data from base to this
		OverwriteJavaItemVisitor visitor = new OverwriteJavaItemVisitor(this);
		base.accept(visitor);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return base.getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JavaItemType getType() {
		return base.getType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setType(JavaItemType type) {
		throw new UnsupportedOperationException("setType() cannot be called on a delta item.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void setAttribute(String name, Object value) {
		if (attributes == null) {
			attributes = new HashMap<>();
		}

		if (value instanceof List) {
			value = new NotifierList((List) value, this);
		} else if (value instanceof Set) {
			value = new NotifierSet((Set) value, this);
		}

		attributes.put(name, value);
		changed();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getID() {
		return base.getID();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setID(int id) {
		throw new UnsupportedOperationException("setID() cannot be called on a delta item.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Integer> getChildrenIDs() {
		return children;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Integer> getDependenciesIDs() {
		return dependencies;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getParentID() {
		return parent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setParentID(Integer parent) {
		this.parent = parent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Integer> getIncomingIDs() {
		return incoming;
	}

	@Override
	public void changed() {
		// let the index know that this item changed by calling set
		getIndex().set(getID(), this);
	}

}
