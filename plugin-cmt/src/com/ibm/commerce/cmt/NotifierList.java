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
import java.util.List;
import java.util.ListIterator;

/**
 * This class wraps a list object and notifies a listener that the list changed.
 * 
 * @param <E>
 *            The type of elements in the list.
 * 
 * @author Trent Hoeppner
 */
public class NotifierList<E> implements List<E> {

	/**
	 * The list that is wrapped and which is being listened to for changes.
	 */
	private List<E> wrappedList;

	/**
	 * The listener to notify with the list changes.
	 */
	private ChangeListener listener;

	/**
	 * Constructor for this.
	 * 
	 * @param wrappedList
	 *            The list that is wrapped and which is being listened to for
	 *            changes. This value cannot be null.
	 * @param listener
	 *            The listener to notify when the list changes. This value
	 *            cannot be null.
	 */
	public NotifierList(List<E> wrappedList, ChangeListener listener) {
		this.wrappedList = wrappedList;
		this.listener = listener;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return wrappedList.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return wrappedList.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		return wrappedList.contains(o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<E> iterator() {
		return wrappedList.iterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] toArray() {
		return wrappedList.toArray();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T[] toArray(T[] a) {
		return wrappedList.toArray(a);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(E e) {
		listener.changed();
		return wrappedList.add(e);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object o) {
		listener.changed();
		return wrappedList.remove(o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		return wrappedList.containsAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		listener.changed();
		return wrappedList.addAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		listener.changed();
		return wrappedList.addAll(index, c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		listener.changed();
		return wrappedList.removeAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		listener.changed();
		return wrappedList.retainAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		listener.changed();
		wrappedList.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public E get(int index) {
		return wrappedList.get(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public E set(int index, E element) {
		listener.changed();
		return wrappedList.set(index, element);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void add(int index, E element) {
		listener.changed();
		wrappedList.add(index, element);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public E remove(int index) {
		listener.changed();
		return wrappedList.remove(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int indexOf(Object o) {
		return wrappedList.indexOf(o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int lastIndexOf(Object o) {
		return wrappedList.lastIndexOf(o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListIterator<E> listIterator() {
		return wrappedList.listIterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListIterator<E> listIterator(int index) {
		return wrappedList.listIterator(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return new NotifierList<>(wrappedList.subList(fromIndex, toIndex), listener);
	}

}
