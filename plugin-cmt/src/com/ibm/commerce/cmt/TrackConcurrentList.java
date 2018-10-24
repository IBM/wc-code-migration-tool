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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This class is a list implementation that can help detect
 * ConcurrentModificationException issues. When an item is modified, a stack
 * trace string is generated and stored in a set. If the iterator.next() is
 * called and results in a ConcurrentModificationException, all the stack traces
 * will be printed to the standard error stream.
 * 
 * @param <E>
 *            The type of elements in the list.
 * 
 * @author Trent Hoeppner
 */
public class TrackConcurrentList<E> extends ArrayList<E> {

	/**
	 * Generated for serialization.
	 */
	private static final long serialVersionUID = -7720122804459779490L;

	/**
	 * The stack traces to store.
	 */
	private ConcurrentSkipListSet<String> stackTraces = new ConcurrentSkipListSet<>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(E e) {
		addException();
		return super.add(e);
	}

	/**
	 * Adds a new exception stack trace to the list, so that the callers can be
	 * tracked later.
	 */
	private void addException() {
		try {
			throw new Exception();
		} catch (Exception e1) {
			stackTraces.add(toString(e1));
		}
	}

	/**
	 * Converts the given exception into a string.
	 * 
	 * @param e
	 *            The exception to convert. This value cannot be null.
	 * 
	 * @return The stack trace as a string. This value will not be null or
	 *         empty.
	 */
	private String toString(Exception e) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(b);
		e.printStackTrace(writer);
		writer.flush();
		return b.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public E remove(int index) {
		addException();
		return super.remove(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object o) {
		addException();
		return super.remove(o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return new InternalIterator(super.iterator());
	}

	/**
	 * This is an iterator wrapper that can catch
	 * ConcurrentModificationException on next() and print out the users of this
	 * from stack traces for analysis.
	 */
	public class InternalIterator implements Iterator<E> {

		/**
		 * The wrapped iterator.
		 */
		private Iterator<E> i;

		/**
		 * Constructor for this.
		 * 
		 * @param i
		 *            The iterator to wrap. This value cannot be null.
		 */
		public InternalIterator(Iterator<E> i) {
			this.i = i;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return i.hasNext();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public E next() {
			try {
				addException();
				return i.next();
			} catch (ConcurrentModificationException e) {
				System.err.println("------------------------ConcurrentModifications!! --------------------------");
				e.printStackTrace();
				for (String e1 : stackTraces) {
					System.err.println(e1);
				}
				System.err.println("--------------------------------------------------");

				throw new ConcurrentModificationException(e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			addException();
			i.remove();
		}

	}

}
