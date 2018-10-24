package com.ibm.commerce.dependency.load;

/**
 * This interface represents code that can be run on the elements of a
 * Collection.
 * 
 * @author Trent Hoeppner
 */
public interface CollectionRunnable<T> {

	/**
	 * Processes the given element.
	 * 
	 * @param entry
	 *            The element to process.
	 * 
	 * @return True to stop execution on other elements, false to continue
	 *         processing other elements.
	 */
	boolean run(T entry);

	/**
	 * Adds the properties set by this to the given context.
	 * 
	 * @param context
	 *            The context to add the properties to. This value will not be
	 *            null.
	 */
	void addProperties(LoadingContext context);
}
