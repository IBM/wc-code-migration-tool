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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.commerce.cmt.Check;

/**
 * This class visits a JavaItem and copies its values to another target
 * JavaItem. The parentID of both items must match.
 * 
 * @author Trent Hoeppner
 */
public class OverwriteJavaItemVisitor implements JavaItemVisitor {

	/**
	 * The item to copy attributes to.
	 */
	private JavaItem targetItem;

	/**
	 * Constructor for this.
	 * 
	 * @param targetItem
	 *            The item to copy attributes to. This value cannot be null.
	 */
	public OverwriteJavaItemVisitor(JavaItem targetItem) {
		Check.notNull(targetItem, "targetItem");
		this.targetItem = targetItem;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This implementation returns true so that null, missing, and empty
	 * attributes can be copied.
	 */
	@Override
	public boolean isIncludingEmpty() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitParent(JavaItem item, Integer parentID) {
		String baseParentIDString = String.valueOf(targetItem.getParentID());
		String deltaParentIDString = String.valueOf(parentID);
		if (!baseParentIDString.equals(deltaParentIDString)) {
			throw new IllegalStateException("The parent should not change in the delta item: expected parentID = "
					+ deltaParentIDString + " but was " + baseParentIDString);
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitChildren(JavaItem item, List<Integer> childrenIDs) {
		targetItem.getChildrenIDs().clear();
		targetItem.getChildrenIDs().addAll(childrenIDs);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitDependencies(JavaItem item, List<Integer> dependencyIDs) {
		targetItem.getDependenciesIDs().clear();
		targetItem.getDependenciesIDs().addAll(dependencyIDs);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitIncoming(JavaItem item, List<Integer> incomingIDs) {
		targetItem.getIncomingIDs().clear();
		targetItem.getIncomingIDs().addAll(incomingIDs);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitSuperclass(JavaItem classItem, Integer superclassID) {
		targetItem.setAttribute(JavaItem.ATTR_SUPERCLASS, superclassID);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitSuperInterfaces(JavaItem classItem, Set<Integer> superInterfaceIDs) {
		Set<Integer> copyForTarget = copySet(superInterfaceIDs);
		targetItem.setAttribute(JavaItem.ATTR_SUPERINTERFACES, copyForTarget);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitSubclasses(JavaItem classItem, Set<Integer> subclassIDs) {
		Set<Integer> copyForTarget = copySet(subclassIDs);
		targetItem.setAttribute(JavaItem.ATTR_SUBCLASSES, copyForTarget);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitReturnType(JavaItem methodItem, Integer returnTypeID) {
		targetItem.setAttribute(JavaItem.ATTR_RETURN_TYPE, returnTypeID);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitMethodParamTypes(JavaItem methodItem, List<Integer> methodParamTypeIDs) {
		List<Integer> copyForTarget = copyList(methodParamTypeIDs);
		targetItem.setAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES, copyForTarget);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visitPackagePrivateVisible(JavaItem item, Boolean packagePrivateVisible) {
		targetItem.setAttribute(JavaItem.ATTR_PROJECT_PRIVATE_VISIBLE, packagePrivateVisible);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitArrayBaseClass(JavaItem arrayClassItem, Integer arrayBaseClassID) {
		targetItem.setAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS, arrayBaseClassID);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitUsedByArrayClass(JavaItem classItem, Integer usedByArrayClassID) {
		targetItem.setAttribute(JavaItem.ATTR_USED_BY_ARRAY_CLASS, usedByArrayClassID);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitFieldType(JavaItem fieldItem, Integer fieldTypeID) {
		targetItem.setAttribute(JavaItem.ATTR_FIELD_TYPE, fieldTypeID);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitMethodThrowsTypes(JavaItem methodItem, List<Integer> methodThrowsTypeIDs) {
		List<Integer> copyForTarget = copyList(methodThrowsTypeIDs);
		targetItem.setAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES, copyForTarget);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visitBinary(JavaItem item, Boolean binary) {
		targetItem.setAttribute(JavaItem.ATTR_BINARY, binary);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visitThirdParty(JavaItem item, Boolean thirdParty) {
		targetItem.setAttribute(JavaItem.ATTR_THIRD_PARTY, thirdParty);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitInnerClasses(JavaItem classItem, Set<Integer> innerClassIDs) {
		Set<Integer> copyForTarget = copySet(innerClassIDs);
		targetItem.setAttribute(JavaItem.ATTR_INNER_CLASSES, copyForTarget);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitOuterClass(JavaItem classItem, Integer outerClassID) {
		targetItem.setAttribute(JavaItem.ATTR_OUTER_CLASS, outerClassID);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visitUsedByMethodsAndFields(JavaItem classItem, Set<Integer> usedByMethodsAndFieldIDs) {
		Set<Integer> copyForTarget = copySet(usedByMethodsAndFieldIDs);
		targetItem.setAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS, copyForTarget);
		return false;
	}

	/**
	 * Creates a copy of the given set.
	 * 
	 * @param set
	 *            The set to copy. If null, null will be returned.
	 * 
	 * @return The copied set. This value will be null if the input set is null.
	 */
	private Set<Integer> copySet(Set<Integer> set) {
		Set<Integer> copyForTarget;
		if (set != null) {
			copyForTarget = new LinkedHashSet<>(set);
		} else {
			copyForTarget = null;
		}

		return copyForTarget;
	}

	/**
	 * Creates a copy of the given list.
	 * 
	 * @param list
	 *            The list to copy. If null, null will be returned.
	 * 
	 * @return The copied list. This value will be null if the input list is
	 *         null.
	 */
	private List<Integer> copyList(List<Integer> list) {
		List<Integer> copyForTarget;
		if (list != null) {
			copyForTarget = new ArrayList<>(list);
		} else {
			copyForTarget = null;
		}

		return copyForTarget;
	}

}
