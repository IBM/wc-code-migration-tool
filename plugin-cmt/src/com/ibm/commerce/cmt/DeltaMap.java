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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class records adds, updates, and removes compared to a base map. The
 * base map can also be a delta map. It is assumed that the base map does not
 * change.
 * 
 * @param <K>
 *            The type of the keys in the map.
 * @param <V>
 *            The type of the values in the map.
 * 
 * @author Trent Hoeppner
 */
public class DeltaMap<K, V> extends AbstractMap<K, V> {

	private Map<K, V> base;

	private Map<K, Change> changes;

	private int total;

	/**
	 * Constructor for this.
	 * 
	 * @param base
	 *            The base map which this modifies. This value cannot be null.
	 */
	public DeltaMap(Map<K, V> base) {
		Check.notNull(base, "base");

		this.base = base;
		total = base.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return new RemoveTrackingWrapperSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V get(Object key) {
		V value;
		if (changes != null && changes.containsKey(key)) {
			Change change = changes.get(key);
			if (change.type == ChangeType.REMOVE) {
				value = null;
			} else {
				value = change.object;
			}
		} else {
			value = base.get(key);
		}

		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V put(K key, V value) {
		V old;
		boolean existed;
		if (changes == null) {
			existed = base.containsKey(key);
			old = base.get(key);
		} else if (changes.containsKey(key)) {
			Change change = changes.get(key);
			if (change.type == ChangeType.REMOVE) {
				// in base but was removed in the delta
				// before putting, it didn't exist from the user's perspective
				existed = false;
				old = null;
			} else {
				existed = true;
				old = change.object;
			}
		} else {
			// was never in the base or the delta
			existed = false;
			old = null;
		}

		ChangeType type;
		if (existed) {
			type = ChangeType.UPDATE;
		} else {
			type = ChangeType.ADD;
			total++;
		}

		Change change = new Change(type, value);
		addChange(key, change);

		return old;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		V old;
		boolean removed = false;
		if (changes == null) {
			if (base.containsKey(key)) {
				// add a remove change
				removeKey((K) key);
				old = base.get(key);
				removed = true;
			} else {
				// it's not there, nothing to remove
				old = null;
			}
			old = base.get(key);
		} else if (changes.containsKey(key)) {
			Change change = changes.get(key);
			if (change.type == ChangeType.REMOVE) {
				// in base but was already removed in the delta
				// before removing, it didn't exist from the user's perspective
				old = null;
			} else {
				// it's an add or update change
				// remove the key, so it will no longer be added or updated
				changes.remove(key);
				old = change.object;
				removed = true;
			}
		} else {
			// was never in the base or the delta
			old = null;
		}

		if (removed) {
			// the key was removed - this will work whether the value is null or
			// not
			total--;
		}

		return old;
	}

	private void removeKey(K key) {
		Change change = new Change(ChangeType.REMOVE, null);
		addChange(key, change);
	}

	/**
	 * Adds the given change to this.
	 * 
	 * @param key
	 *            The key to set the value for. This value cannot be null.
	 * @param change
	 *            The change object to set. This value cannot be null.
	 */
	private void addChange(K key, Change change) {
		if (changes == null) {
			changes = new HashMap<>();
		}

		changes.put(key, change);
	}

	/**
	 * This map can track when its iterator removes something.
	 */
	private class RemoveTrackingWrapperSet implements Set<Map.Entry<K, V>> {

		private Set<Map.Entry<K, V>> cache;

		public RemoveTrackingWrapperSet() {
			Map<K, V> map = new LinkedHashMap<>(base);
			if (changes != null) {
				for (Map.Entry<K, Change> entry : changes.entrySet()) {
					entry.getValue().apply(map, entry.getKey());
				}
			}

			cache = map.entrySet();
		}

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new RemoveTrackingWrapperIterator(cache.iterator());
		}

		@Override
		public int size() {
			return cache.size();
		}

		@Override
		public boolean isEmpty() {
			return cache.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return cache.contains(o);
		}

		@Override
		public Object[] toArray() {
			return cache.toArray();
		}

		@Override
		public <T> T[] toArray(T[] array) {
			return cache.toArray(array);
		}

		@Override
		public boolean add(Map.Entry<K, V> e) {
			throw new UnsupportedOperationException("add() is not supported on a set backed by a Map.");
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException("remove() is not supported on a set backed by a Map.");
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return cache.containsAll(c);
		}

		@Override
		public boolean addAll(Collection<? extends Map.Entry<K, V>> c) {
			throw new UnsupportedOperationException("addAll() is not supported on a set backed by a Map.");
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException("retainAll() is not supported on a set backed by a Map.");
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException("removeAll() is not supported on a set backed by a Map.");
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("clear() is not supported on a set backed by a Map.");
		}

	}

	private class RemoveTrackingWrapperIterator implements Iterator<Map.Entry<K, V>> {

		private Iterator<Map.Entry<K, V>> wrapped;

		private Map.Entry<K, V> last;

		public RemoveTrackingWrapperIterator(Iterator<Map.Entry<K, V>> wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public boolean hasNext() {
			return wrapped.hasNext();
		}

		@Override
		public Map.Entry<K, V> next() {
			last = wrapped.next();
			return last;
		}

		@Override
		public void remove() {
			if (last != null) {
				DeltaMap.this.remove(last.getKey());
			}
			wrapped.remove();
		}
	}

	/**
	 * Change represents a change to one value in the map.
	 */
	private class Change {

		private ChangeType type;

		private V object;

		/**
		 * Constructor for this.
		 * 
		 * @param type
		 *            The type of change. This value cannot be null.
		 * @param object
		 *            The object to change. This value cannot be null for ADD
		 *            and UPDATE changes, and must be null for REMOVE changes.
		 */
		public Change(ChangeType type, V object) {
			this.type = type;
			this.object = object;
		}

		public void apply(Map<K, V> toModify, K key) {
			if (type == ChangeType.ADD) {
				toModify.put(key, object);
			} else if (type == ChangeType.UPDATE) {
				toModify.put(key, object);
			} else {
				toModify.remove(key);
			}
		}
	}

}
