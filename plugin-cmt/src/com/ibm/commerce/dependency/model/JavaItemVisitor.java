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
import java.util.Set;

/**
 * This interface represents code that can navigate a JavaItem's data similar to
 * the Visitor design pattern. The base values such as parent and dependencies
 * are visited, and the attributes are visited only if they exist. This pattern
 * is used to ensure that no attributes are forgotten if a new one is added.
 * 
 * @author Trent Hoeppner
 */
public interface JavaItemVisitor {

	/**
	 * Returns whether a null, missing, or empty value or attribute will be
	 * included in the visitation algorithm.
	 * 
	 * @return True if null, missing or empty values should be visited, false if
	 *         they should be skipped.
	 */
	default boolean isIncludingEmpty() {
		return false;
	}

	/**
	 * Visits the parent ID of the given item. This method will not be called if
	 * the item has no parent, unless {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param item
	 *            The item that is being visited. This cannot not be null.
	 * @param parentID
	 *            The ID of the parent item.
	 * 
	 * @return True to visit the parent of the item, false otherwise.
	 */
	boolean visitParent(JavaItem item, Integer parentID);

	/**
	 * Visits the child IDs of the given item. This method will not be called if
	 * the item has no children, unless {@link #isIncludingEmpty()} returns
	 * true.
	 * 
	 * @param item
	 *            The item that is being visited. This cannot not be null.
	 * @param childrenIDs
	 *            The IDs of child items. This value will not be null.
	 * 
	 * @return True to visit the children of the item, false otherwise.
	 */
	boolean visitChildren(JavaItem item, List<Integer> childrenIDs);

	/**
	 * Visits the dependency IDs of the given item. This method will not be
	 * called if the item has no dependencies, unless
	 * {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param item
	 *            The item that is being visited. This cannot not be null.
	 * @param dependencyIDs
	 *            The IDs of dependent items. This value will not be null.
	 * 
	 * @return True to visit the dependencies of the item, false otherwise.
	 */
	boolean visitDependencies(JavaItem item, List<Integer> dependencyIDs);

	/**
	 * Visits the incoming dependency IDs of the given item. This method will
	 * not be called if the item has no incoming dependencies, unless
	 * {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param item
	 *            The item that is being visited. This cannot not be null.
	 * @param incomingIDs
	 *            The IDs of incoming dependent items. This value will not be
	 *            null.
	 * 
	 * @return True to visit the incoming dependencies of the item, false
	 *         otherwise.
	 */
	boolean visitIncoming(JavaItem item, List<Integer> incomingIDs);

	/**
	 * Visits the superclass ID of the given item. This method will not be
	 * called if the item has no superclass, unless {@link #isIncludingEmpty()}
	 * returns true.
	 * 
	 * @param classItem
	 *            The item that is being visited. This cannot not be null.
	 * @param superclassID
	 *            The ID of the superclass item.
	 * 
	 * @return True to visit the superclass of the item, false otherwise.
	 */
	boolean visitSuperclass(JavaItem classItem, Integer superclassID);

	/**
	 * Visits the super interface IDs of the given item. This method will not be
	 * called if the item has no super interfaces, unless
	 * {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param classItem
	 *            The item that is being visited. This cannot not be null.
	 * @param superInterfaceIDs
	 *            The IDs of super interface items.
	 * 
	 * @return True to visit the super interfaces of the item, false otherwise.
	 */
	boolean visitSuperInterfaces(JavaItem classItem, Set<Integer> superInterfaceIDs);

	/**
	 * Visits the subclass and and sub-interface IDs of the given item. This
	 * method will not be called if the item has no subclasses or
	 * sub-interfaces, unless {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param classItem
	 *            The item that is being visited. This cannot not be null.
	 * @param subclassIDs
	 *            The IDs of subclass and sub-interface items.
	 * 
	 * @return True to visit the subclasses of the item, false otherwise.
	 */
	boolean visitSubclasses(JavaItem classItem, Set<Integer> subclassIDs);

	/**
	 * Visits the return type ID of the given item. This method will not be
	 * called if the item has no return type, unless {@link #isIncludingEmpty()}
	 * returns true.
	 * 
	 * @param methodItem
	 *            The item that is being visited. This cannot not be null.
	 * @param returnTypeID
	 *            The ID of the return type item.
	 * 
	 * @return True to visit the return type of the item, false otherwise.
	 */
	boolean visitReturnType(JavaItem methodItem, Integer returnTypeID);

	/**
	 * Visits the method parameter type IDs of the given item. This method will
	 * not be called if the item has no method parameters, unless
	 * {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param methodItem
	 *            The item that is being visited. This cannot not be null.
	 * @param methodParamTypeIDs
	 *            The IDs of method parameter type items.
	 * 
	 * @return True to visit the method parameters of the item, false otherwise.
	 */
	boolean visitMethodParamTypes(JavaItem methodItem, List<Integer> methodParamTypeIDs);

	/**
	 * Visits the visibility attribute of the given item. This method will not
	 * be called if the item does not have the attribute defined, unless
	 * {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param item
	 *            The item that is being visited. This cannot not be null.
	 * @param packagePrivateVisible
	 *            True if package and private scope items in this are visible,
	 *            false otherwise.
	 */
	void visitPackagePrivateVisible(JavaItem item, Boolean packagePrivateVisible);

	/**
	 * Visits the array base class ID of the given item. This method will not be
	 * called if the item has no array base class, unless
	 * {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param arrayClassItem
	 *            The item that is being visited. This cannot not be null.
	 * @param arrayBaseClassID
	 *            The ID of the array base class.
	 * 
	 * @return True to visit the array base class of the item, false otherwise.
	 */
	boolean visitArrayBaseClass(JavaItem arrayClassItem, Integer arrayBaseClassID);

	/**
	 * Visits the ID of the array class that references the given item as an
	 * array base class. This method will not be called if the item is not used
	 * as an array base class, unless {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param classItem
	 *            The item that is being visited. This cannot not be null.
	 * @param usedByArrayClassID
	 *            The IDs of items that use the item as an array base class.
	 * 
	 * @return True to visit the array class that uses this as an array base
	 *         class, false otherwise.
	 */
	boolean visitUsedByArrayClass(JavaItem classItem, Integer usedByArrayClassID);

	/**
	 * Visits the field type of the given item. This method will not be called
	 * if the item has no field type, unless {@link #isIncludingEmpty()} returns
	 * true.
	 * 
	 * @param fieldItem
	 *            The item that is being visited. This cannot not be null.
	 * @param fieldTypeID
	 *            The ID of the field type item.
	 * 
	 * @return True to visit the field type of the item, false otherwise.
	 */
	boolean visitFieldType(JavaItem fieldItem, Integer fieldTypeID);

	/**
	 * Visits the method throws type IDs of the given item. This method will not
	 * be called if the item has no throws types, unless
	 * {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param methodItem
	 *            The item that is being visited. This cannot not be null.
	 * @param methodThrowsTypeIDs
	 *            The IDs of method throws type items.
	 * 
	 * @return True to visit the throws types of the item, false otherwise.
	 */
	boolean visitMethodThrowsTypes(JavaItem methodItem, List<Integer> methodThrowsTypeIDs);

	/**
	 * Visits the binary attribute of the given item. This method will not be
	 * called if the item does not have the attribute defined, unless
	 * {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param item
	 *            The item that is being visited. This cannot not be null.
	 * @param binary
	 *            True if the item comes from a binary JAR, false otherwise.
	 */
	void visitBinary(JavaItem item, Boolean binary);

	/**
	 * Visits the third-party attribute of the given item. This method will not
	 * be called if the item does not have the attribute defined, unless
	 * {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param item
	 *            The item that is being visited. This cannot not be null.
	 * @param thirdParty
	 *            True if the item comes from a third-party JAR, false
	 *            otherwise.
	 */
	void visitThirdParty(JavaItem item, Boolean thirdParty);

	/**
	 * Visits the inner class IDs of the given item. This method will not be
	 * called if the item has no inner classes, unless
	 * {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param classItem
	 *            The item that is being visited. This cannot not be null.
	 * @param innerClassIDs
	 *            The IDs of inner class items.
	 * 
	 * @return True to visit the inner classes of the item, false otherwise.
	 */
	boolean visitInnerClasses(JavaItem classItem, Set<Integer> innerClassIDs);

	/**
	 * Visits the outer class of the given item. This method will not be called
	 * if the item has no outer class, unless {@link #isIncludingEmpty()}
	 * returns true.
	 * 
	 * @param classItem
	 *            The item that is being visited. This cannot not be null.
	 * @param outerClassID
	 *            The ID of the outer class item.
	 * 
	 * @return True to visit the outer class of the item, false otherwise.
	 */
	boolean visitOuterClass(JavaItem classItem, Integer outerClassID);

	/**
	 * Visits the IDs of methods and fields that reference the given item. This
	 * method will not be called if no methods or fields reference the item,
	 * unless {@link #isIncludingEmpty()} returns true.
	 * 
	 * @param classItem
	 *            The item that is being visited. This cannot not be null.
	 * @param usedByMethodsAndFieldIDs
	 *            The IDs of methods and fields that reference the item.
	 * 
	 * @return True to visit the methods and fields that reference the item,
	 *         false otherwise.
	 */
	boolean visitUsedByMethodsAndFields(JavaItem classItem, Set<Integer> usedByMethodsAndFieldIDs);
}
