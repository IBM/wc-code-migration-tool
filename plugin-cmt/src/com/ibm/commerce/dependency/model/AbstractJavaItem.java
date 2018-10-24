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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.commerce.cmt.Check;

/**
 * This class is the base class for JavaItems.
 * 
 * @author Trent Hoeppner
 */
public abstract class AbstractJavaItem implements JavaItem {

	/**
	 * Returns the index that this belongs to. This value may be null if it is
	 * not part of an index.
	 */
	private JavaItemIndex index;

	protected ReentrantReadWriteLock idLock = new ReentrantReadWriteLock();

	public AbstractJavaItem(JavaItemIndex index) {
		Check.notNull(index, "index");

		this.index = index;
	}

	@Override
	public JavaItemIndex getIndex() {
		return index;
	}

	@Override
	public List<JavaItem> getChildren() {
		try {
			idLock.readLock().lock();
			List<JavaItem> children = new ArrayList<>();
			for (Integer childID : getChildrenIDs()) {
				JavaItem child = index.getItem(childID);
				children.add(child);
			}

			return Collections.unmodifiableList(children);
		} finally {
			idLock.readLock().unlock();
		}
	}

	@Override
	public List<JavaItem> getChildren(JavaItemType type) {
		Check.notNull(type, "type");

		List<JavaItem> filtered = new ArrayList<>();
		for (JavaItem child : getChildren()) {
			if (child.getType() == type) {
				filtered.add(child);
			}
		}

		return Collections.unmodifiableList(filtered);
	}

	@Override
	public List<JavaItem> getDependencies() {
		try {
			idLock.readLock().lock();
			List<JavaItem> dependencies = new ArrayList<>();
			for (Integer dependencyID : getDependenciesIDs()) {
				JavaItem dependency = index.getItem(dependencyID);
				dependencies.add(dependency);
			}

			return Collections.unmodifiableList(dependencies);
		} finally {
			idLock.readLock().unlock();
		}
	}

	@Override
	public String getVersion() {
		return index.getVersion();
	}

	@Override
	public JavaItem getParent() {
		Integer parentID = getParentID();

		JavaItem parent = null;
		if (parentID != null) {
			parent = index.getItem(parentID);
		}

		return parent;
	}

	@Override
	public List<JavaItem> getIncoming() {
		try {
			idLock.readLock().lock();
			List<JavaItem> incomings = new ArrayList<>();
			for (Integer incomingID : getIncomingIDs()) {
				JavaItem incoming = index.getItem(incomingID);
				incomings.add(incoming);
			}

			return Collections.unmodifiableList(incomings);
		} finally {
			idLock.readLock().unlock();
		}
	}

	/**
	 * Returns a human-friendly string representation of this by concatenating
	 * the name of this with all parent names.
	 * 
	 * @return A string representation of this. This value will not be null, but
	 *         may be empty.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		JavaItem current = this;
		while (current != null) {
			buf.insert(0, current.getName());

			if (getIndex().isInFlux()) {
				break;
			}

			if (current.getParent() != null) {
				buf.insert(0, ":");
			}
			current = current.getParent();
		}

		if (!getIndex().isInFlux()) {
			if (getType() == JavaItemType.FIELD) {
				Integer fieldTypeID = getAttribute(JavaItem.ATTR_FIELD_TYPE);
				if (fieldTypeID == null) {
					buf.append(" FType(null)");
				} else {
					JavaItem fieldType = getIndex().getItem(fieldTypeID);
					buf.append(" FType(" + fieldType + ")");
				}
			}

			if (getType() == JavaItemType.METHOD) {
				Integer returnTypeID = getAttribute(JavaItem.ATTR_RETURN_TYPE);
				if (returnTypeID == null) {
					buf.append(" RType(null)");
				} else {
					JavaItem returnType = getIndex().getItem(returnTypeID);
					buf.append(" RType(" + returnType + ")");
				}

				List<Integer> paramTypeIDs = getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
				if (paramTypeIDs == null) {
					buf.append(" Params()");
				} else {
					buf.append(" Params(");
					boolean first = true;
					for (Integer paramTypeID : paramTypeIDs) {
						if (first) {
							first = false;
						} else {
							buf.append(", ");
						}

						JavaItem paramType = getIndex().getItem(paramTypeID);
						buf.append(paramType.toString());
					}
					buf.append(")");
				}

				List<Integer> throwsTypeIDs = getAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES);
				if (throwsTypeIDs == null) {
					buf.append(" Throws()");
				} else {
					buf.append(" Throws(");
					boolean first = true;
					for (Integer throwsTypeID : throwsTypeIDs) {
						if (first) {
							first = false;
						} else {
							buf.append(", ");
						}

						JavaItem throwsType = getIndex().getItem(throwsTypeID);
						buf.append(throwsType.toString());
					}
					buf.append(")");
				}
			}
		}

		return buf.toString();
	}
}
