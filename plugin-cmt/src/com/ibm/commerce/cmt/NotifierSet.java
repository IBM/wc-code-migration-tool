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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * This class wraps a set object and notifies a listener that the set changed.
 * 
 * @param <E>
 *            The type of elements in the set.
 * 
 * @author Trent Hoeppner
 */
public class NotifierSet<E> implements Set<E> {

	/**
	 * The set that is wrapped and which is being listened to for changes.
	 */
	private Set<E> wrappedSet;

	/**
	 * The listener to notify with the set changes.
	 */
	private ChangeListener listener;

	/**
	 * Constructor for this.
	 * 
	 * @param wrappedSet
	 *            The set that is wrapped and which is being listened to for
	 *            changes. This value cannot be null.
	 * @param listener
	 *            The listener to notify when the set changes. This value cannot
	 *            be null.
	 */
	public NotifierSet(Set<E> wrappedSet, ChangeListener listener) {
		this.wrappedSet = wrappedSet;
		this.listener = listener;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return wrappedSet.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return wrappedSet.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		return wrappedSet.contains(o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<E> iterator() {
		return wrappedSet.iterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] toArray() {
		return wrappedSet.toArray();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T[] toArray(T[] a) {
		return wrappedSet.toArray(a);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(E e) {
		listener.changed();
		return wrappedSet.add(e);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object o) {
		listener.changed();
		return wrappedSet.remove(o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		return wrappedSet.containsAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		listener.changed();
		return wrappedSet.addAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		listener.changed();
		return wrappedSet.removeAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		listener.changed();
		return wrappedSet.retainAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		listener.changed();
		wrappedSet.clear();
	}

}
