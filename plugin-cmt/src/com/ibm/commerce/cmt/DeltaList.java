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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class records adds, updates, and removes compared to a base list. The
 * base list can also be a delta list. It is assumed that the base list does not
 * change.
 * 
 * @param <T>
 *            The type of items in the list.
 * 
 * @author Trent Hoeppner
 */
public class DeltaList<E> extends AbstractList<E> {

	private List<E> base;

	private List<Change<E>> changes;

	private List<E> cache;

	/**
	 * Constructor for this.
	 * 
	 * @param base
	 *            The base list which this modifies. This value cannot be null.
	 */
	public DeltaList(List<E> base) {
		Check.notNull(base, "base");

		this.base = base;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public E get(int index) {
		return getFinalList().get(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return getFinalList().size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public E set(int index, E element) {
		E old;
		if (cache == null) {
			if (changes == null) {
				old = base.get(index);
			} else {
				getFinalList();
				old = cache.get(index);
			}
		} else {
			old = cache.get(index);
		}

		Change<E> change = new Change<E>(ChangeType.UPDATE, index, element);
		addChange(change);

		if (cache != null) {
			change.apply(cache);
		}

		return old;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void add(int index, E element) {
		Change<E> change = new Change<E>(ChangeType.ADD, index, element);
		addChange(change);

		if (cache != null) {
			change.apply(cache);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public E remove(int index) {
		E old;
		if (cache == null) {
			if (changes == null) {
				old = base.get(index);
			} else {
				getFinalList();
				old = cache.get(index);
			}
		} else {
			old = cache.get(index);
		}

		Change<E> change = new Change<E>(ChangeType.REMOVE, index, null);
		addChange(change);

		if (cache != null) {
			change.apply(cache);
		}

		return old;
	}

	public List<Change<E>> getChanges() {
		List<Change<E>> list = changes;
		if (list == null) {
			list = Collections.emptyList();
		}

		return list;
	}

	private void addChange(Change<E> change) {
		if (changes == null) {
			changes = new ArrayList<>();
		}

		changes.add(change);
	}

	/**
	 * Returns the list that contains the base plus all modifications in this.
	 * 
	 * @return The base list plus modifications. This value will not be null.
	 */
	private List<E> getFinalList() {
		if (changes == null) {
			return base;
		} else if (cache == null) {
			cache = new ArrayList<E>(base);
			for (Change<E> change : changes) {
				change.apply(cache);
			}
		}

		return cache;
	}

	/**
	 * Change represents one change to the list. The meaning of the index
	 * depends on the type:
	 * <ul>
	 * <li>ADD - The index at which the value is inserted.
	 * <li>UPDATE - The index of the item which is updated (replaced by the
	 * object).
	 * <li>REMOVE - The index of the item which is removed.
	 * </ul>
	 *
	 * @param <E>
	 */
	public static class Change<E> {

		private ChangeType type;

		private int index;

		private E object;

		/**
		 * Constructor for this.
		 * 
		 * @param type
		 *            The type of change. This value cannot be null.
		 * @param index
		 *            The index at which the change happens. This value must be
		 *            >= 0.
		 * @param object
		 *            The object to change. This value cannot be null for ADD
		 *            and UPDATE changes, and must be null for REMOVE changes.
		 */
		public Change(ChangeType type, int index, E object) {
			this.type = type;
			this.index = index;
			this.object = object;
		}

		public ChangeType getType() {
			return type;
		}

		public int getIndex() {
			return index;
		}

		public E getObject() {
			return object;
		}

		public void apply(List<E> toModify) {
			if (type == ChangeType.ADD) {
				toModify.add(index, object);
			} else if (type == ChangeType.UPDATE) {
				toModify.set(index, object);
			} else {
				toModify.remove(index);
			}
		}
	}
}
