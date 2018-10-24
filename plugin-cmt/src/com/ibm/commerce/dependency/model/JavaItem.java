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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents some part of a Java program. It can be a project, class
 * or others, as listed in {@link JavaItemType}. Each JavaItem has children,
 * except leaf nodes. It may also have {@link #getDependencies() dependencies}
 * on other JavaItems of the same type, or {@link #getIncoming() incoming
 * dependencies} from other items of the same type. This class supports
 * attributes for additional information or relationships beyond dependencies
 * and children.
 * 
 * @author Trent Hoeppner
 */
public interface JavaItem {

	/**
	 * An attribute for classes that defines the superclass, whose value is a
	 * class JavaItem ID.
	 */
	String ATTR_SUPERCLASS = "superclass";

	/**
	 * An attribute for classes that defines the super interfaces, whose value
	 * is a Set of class JavaItem IDs.
	 */
	String ATTR_SUPERINTERFACES = "superinterfaces";

	/**
	 * An attribute for classes that defines the subclasses and subinterfaces,
	 * whose value is a set of class JavaItem IDs.
	 */
	String ATTR_SUBCLASSES = "subclasses";

	/**
	 * An attribute that defines a method's return type, whose value is a class
	 * JavaItem ID.
	 */
	String ATTR_RETURN_TYPE = "returntype";

	/**
	 * An attribute that defines a method's parameter types, whose value is a
	 * list of class JavaItem IDs.
	 */
	String ATTR_METHOD_PARAM_TYPES = "methodparamtypes";

	/**
	 * An attribute for a project that defines whether the private and
	 * package-private classes and methods are visible, whose value is a
	 * boolean.
	 */
	String ATTR_PROJECT_PRIVATE_VISIBLE = "privateVisible";

	/**
	 * An attribute that defines the base class of an array class JavaItem. The
	 * base class may also be an array base class ID.
	 */
	String ATTR_ARRAY_BASE_CLASS = "arraybaseclass";

	/**
	 * An attribute that defines the array class ID that refers to this as a
	 * base class.
	 */
	String ATTR_USED_BY_ARRAY_CLASS = "usedbyarrayclass";

	/**
	 * An attribute that defines the type of a field JavaItem, whose value is a
	 * class JavaItem ID.
	 */
	String ATTR_FIELD_TYPE = "type";

	/**
	 * An attribute that defines the throws types of a method, whose value is a
	 * list of class JavaItem IDs.
	 */
	String ATTR_METHOD_THROWS_TYPES = "methodthrowstypes";

	/**
	 * An attribute that defines whether a project, package, class or method was
	 * discovered from a binary-only JAR, whose value is a boolean.
	 */
	String ATTR_BINARY = "binary";

	/**
	 * An attribute that defines whether a project is from a third party, whose
	 * value is a boolean. If no product projects depend on a particular
	 * third-party project, that third-party project may be removed.
	 */
	String ATTR_THIRD_PARTY = "thirdparty";

	/**
	 * An attribute that defines the IDs of the inner classes of a class, whose
	 * value is a Set&lt;Integer&gt;.
	 */
	String ATTR_INNER_CLASSES = "innerclasses";

	/**
	 * An attribute that defines the ID of the class that encloses a class,
	 * whose value is an Integer.
	 */
	String ATTR_OUTER_CLASS = "outerclass";

	/**
	 * An attribute that defines the IDs of methods that use a class as a return
	 * type, method parameter, or throws exception, and fields with the type
	 */
	String ATTR_USED_BY_METHODS_AND_FIELDS = "usedbymethodsandfields";

	/**
	 * Returns the children of this.
	 *
	 * @return The children of this. Will not be null, but may be empty.
	 */
	public List<JavaItem> getChildren();

	/**
	 * Returns the IDs of the children of this. This is faster than
	 * {@link #getChildren()}.
	 *
	 * @return The IDs of the children of this. Will not be null, but may be
	 *         empty.
	 */
	public List<Integer> getChildrenIDs();

	/**
	 * Returns the children of this that are of the given type. Most item types
	 * have only one type of child, but the {@link JavaItemType#CLASS} type can
	 * have method and field children.
	 * 
	 * @param type
	 *            The type used to filter the children. This value cannot be
	 *            null.
	 * 
	 * @return The children of the specified type. This value will not be null,
	 *         but may be empty.
	 */
	public List<JavaItem> getChildren(JavaItemType type);

	/**
	 * Returns the peers that this depends on. The meaning of a dependency
	 * depends on each Java item type.
	 * <ul>
	 * <li>PROJECT - dependencies are other projects that this project depends
	 * on, based on the MANIFEST.MF file.
	 * <li>PACKAGE - dependencies are packages that this package depends on, in
	 * the sense that at least one class in this package depends on at least one
	 * package in the other package. Normally these dependencies are not set
	 * except in cyclic dependency analysis.
	 * <li>CLASS - dependencies are the classes that are imported or used by
	 * this class.
	 * <li>METHOD - dependencies are the other methods that are called by this
	 * method.
	 * <li>FIELD - no dependencies.
	 * </ul>
	 *
	 * @return The dependencies of this. Will not be null, but may be empty.
	 */
	public List<JavaItem> getDependencies();

	/**
	 * Returns the IDs of the peers that this depends on. This is faster than
	 * {@link #getDependencies()}
	 *
	 * @return The IDs of the dependencies of this. Will not be null, but may be
	 *         empty.
	 */
	public List<Integer> getDependenciesIDs();

	/**
	 * Returns the name of this.
	 *
	 * @return The name of this. Will not be null or empty.
	 */
	public String getName();

	/**
	 * Returns the version of this.
	 * 
	 * @return The version of this. Will not be null or empty.
	 */
	public String getVersion();

	/**
	 * Returns the parent of this.
	 *
	 * @return The parent of this. Check the class description if this value can
	 *         be null.
	 */
	public JavaItem getParent();

	/**
	 * Returns the ID of the parent of this. This is faster than
	 * {@link #getParent()}.
	 *
	 * @return The ID of the parent of this. Check the class description if this
	 *         value can be null.
	 */
	public Integer getParentID();

	/**
	 * Sets the parent ID of this.
	 * 
	 * @param parent
	 *            The parent ID to set. Check the class description if this
	 *            value can be null.
	 */
	public void setParentID(Integer parent);

	/**
	 * Returns the type of this.
	 * 
	 * @return The type of this. This value will not be null.
	 */
	public JavaItemType getType();

	/**
	 * Sets the type of this.
	 * 
	 * @param type
	 *            The type of this. This value cannot be null.
	 */
	public void setType(JavaItemType type);

	/**
	 * Returns the items that depend on this.
	 * 
	 * @return The items that depend on this. This value will not be null, but
	 *         may be empty.
	 */
	public List<JavaItem> getIncoming();

	/**
	 * Returns the IDs of items that depend on this. This should be kept in sync
	 * with the dependencies. For example, if there are two items, A and B, and
	 * A depends on B, then B should be added to A's dependencies list, and A
	 * should be added to B's incoming list. This is faster than
	 * {@link #getIncoming()}.
	 * 
	 * @return The IDs of items that depend on this. This value will not be
	 *         null, but may be empty.
	 */
	public List<Integer> getIncomingIDs();

	/**
	 * Returns the attributes of this. See the CONSTANTS of this class that
	 * start with "ATTR_" to see the attributes available for each type of item.
	 * 
	 * @return The attributes of this. This value may be null or empty if there
	 *         are no attributes.
	 */
	public Map<String, Object> getAttributes();

	/**
	 * Returns the value for an attribute of this. See the CONSTANTS of this
	 * class that start with "ATTR_" to see the attributes available for each
	 * type of item.
	 * 
	 * @param name
	 *            The name of the attribute. This value cannot be null, but may
	 *            be empty.
	 * 
	 * @return The value of the attribute. This value may be null if the
	 *         attribute's value is null, or if the attribute has not been set.
	 */
	public <T> T getAttribute(String name);

	/**
	 * Sets the value for an attribute of this. See the CONSTANTS of this class
	 * that start with "ATTR_" to see the attributes available for each type of
	 * item.
	 * 
	 * @param name
	 *            The name of the attribute. This value cannot be null, but may
	 *            be empty.
	 * @param value
	 *            The value to set. This value may be null.
	 */
	public void setAttribute(String name, Object value);

	/**
	 * Returns the identifier of this. Every item in the system must have a
	 * unique identifier in order to work properly with {@link JavaItemIndex}.
	 * 
	 * @return The identifier of this.
	 */
	public int getID();

	/**
	 * Sets the identifier of this. Every item in the system must have a unique
	 * identifier in order to work properly with {@link JavaItemIndex}.
	 * 
	 * @param id
	 *            The identifier of this.
	 */
	public void setID(int id);

	/**
	 * Returns the index that this belongs to.
	 * 
	 * @return The index that this belongs to. This will be null if this does
	 *         not belong to an index (if this is only for temporary purposes).
	 */
	public JavaItemIndex getIndex();

	/**
	 * Calls the methods of the given visitor for the values and attributes of
	 * this.
	 * 
	 * @param visitor
	 *            The visitor to execute on this. This value cannot be null.
	 */
	public default void accept(JavaItemVisitor visitor) {
		Set<Integer> toVisit = new LinkedHashSet<>();
		boolean visitEmpty = visitor.isIncludingEmpty();

		if (getParentID() != null || visitEmpty) {
			boolean visit = visitor.visitParent(this, getParentID());
			if (visit) {
				toVisit.add(getParentID());
			}
		}

		if (getChildrenIDs().size() > 0 || visitEmpty) {
			boolean visit = visitor.visitChildren(this, getChildrenIDs());
			if (visit) {
				toVisit.addAll(getChildrenIDs());
			}
		}

		if (getDependenciesIDs().size() > 0 || visitEmpty) {
			boolean visit = visitor.visitDependencies(this, getDependenciesIDs());
			if (visit) {
				toVisit.addAll(getDependenciesIDs());
			}
		}

		if (getIncomingIDs().size() > 0 || visitEmpty) {
			boolean visit = visitor.visitIncoming(this, getIncomingIDs());
			if (visit) {
				toVisit.addAll(getIncomingIDs());
			}
		}

		Map<String, Object> attributes = getAttributes();
		if (attributes != null || visitEmpty) {
			if (attributes == null) {
				// we create this empty map so that the logic can handle all
				// attributes with the same logic
				attributes = new HashMap<>();
			}

			Integer superClassID = getAttribute(JavaItem.ATTR_SUPERCLASS);
			if (superClassID != null || visitEmpty) {
				boolean visit = visitor.visitSuperclass(this, superClassID);
				if (visit) {
					toVisit.add(superClassID);
				}
			}

			Set<Integer> superInterfaceIDs = getAttribute(JavaItem.ATTR_SUPERINTERFACES);
			if (superInterfaceIDs != null && superInterfaceIDs.size() > 0 || visitEmpty) {
				boolean visit = visitor.visitSuperInterfaces(this, superInterfaceIDs);
				if (visit) {
					toVisit.addAll(superInterfaceIDs);
				}
			}

			Set<Integer> subClassIDs = getAttribute(JavaItem.ATTR_SUBCLASSES);
			if (subClassIDs != null && subClassIDs.size() > 0 || visitEmpty) {
				boolean visit = visitor.visitSubclasses(this, subClassIDs);
				if (visit) {
					toVisit.addAll(subClassIDs);
				}
			}

			Set<Integer> methodOrFieldIDs = getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS);
			if (methodOrFieldIDs != null && methodOrFieldIDs.size() > 0 || visitEmpty) {
				boolean visit = visitor.visitUsedByMethodsAndFields(this, methodOrFieldIDs);
				if (visit) {
					toVisit.addAll(methodOrFieldIDs);
				}
			}

			Integer classID = getAttribute(JavaItem.ATTR_RETURN_TYPE);
			if (classID != null || visitEmpty) {
				boolean visit = visitor.visitReturnType(this, classID);
				if (visit) {
					toVisit.add(classID);
				}
			}

			List<Integer> paramTypeIDs = getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
			if (paramTypeIDs != null && paramTypeIDs.size() > 0 || visitEmpty) {
				boolean visit = visitor.visitMethodParamTypes(this, paramTypeIDs);
				if (visit) {
					toVisit.addAll(paramTypeIDs);
				}
			}

			List<Integer> throwsTypeIDs = getAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES);
			if (throwsTypeIDs != null && throwsTypeIDs.size() > 0 || visitEmpty) {
				boolean visit = visitor.visitMethodThrowsTypes(this, throwsTypeIDs);
				if (visit) {
					toVisit.addAll(throwsTypeIDs);
				}
			}

			Integer arrayClassID = getAttribute(JavaItem.ATTR_USED_BY_ARRAY_CLASS);
			if (arrayClassID != null || visitEmpty) {
				boolean visit = visitor.visitUsedByArrayClass(this, arrayClassID);
				if (visit) {
					toVisit.add(arrayClassID);
				}
			}

			Integer arrayBaseClassID = getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS);
			if (arrayBaseClassID != null || visitEmpty) {
				boolean visit = visitor.visitArrayBaseClass(this, arrayBaseClassID);
				if (visit) {
					toVisit.add(arrayBaseClassID);
				}
			}

			Integer fieldTypeID = getAttribute(JavaItem.ATTR_FIELD_TYPE);
			if (fieldTypeID != null || visitEmpty) {
				boolean visit = visitor.visitFieldType(this, fieldTypeID);
				if (visit) {
					toVisit.add(fieldTypeID);
				}
			}

			Integer outerClassID = getAttribute(JavaItem.ATTR_OUTER_CLASS);
			if (outerClassID != null || visitEmpty) {
				boolean visit = visitor.visitOuterClass(this, outerClassID);
				if (visit) {
					toVisit.add(outerClassID);
				}
			}

			Set<Integer> innerClassIDs = getAttribute(JavaItem.ATTR_INNER_CLASSES);
			if (innerClassIDs != null && innerClassIDs.size() > 0 || visitEmpty) {
				boolean visit = visitor.visitInnerClasses(this, innerClassIDs);
				if (visit) {
					toVisit.addAll(innerClassIDs);
				}
			}

			Boolean visibility = getAttribute(JavaItem.ATTR_PROJECT_PRIVATE_VISIBLE);
			if (visibility != null || visitEmpty) {
				visitor.visitPackagePrivateVisible(this, visibility);
			}

			Boolean binary = getAttribute(JavaItem.ATTR_BINARY);
			if (binary != null || visitEmpty) {
				visitor.visitBinary(this, binary);
			}

			Boolean thirdParty = getAttribute(JavaItem.ATTR_THIRD_PARTY);
			if (thirdParty != null || visitEmpty) {
				visitor.visitThirdParty(this, thirdParty);
			}
		}

		for (Integer itemID : toVisit) {
			JavaItem item = getIndex().getItem(itemID);
			item.accept(visitor);
		}
	}

	/**
	 * Creates a new copy of this in a new index, including copies of all
	 * dependent IDs and attributes. A new ID will be assigned to the created
	 * item from the given index. Note that the dependent IDs will likely not
	 * match up with what is in the new index and so must be remapped later.
	 * 
	 * @param newFactory
	 *            The factory used to create the item in the new index. This
	 *            value cannot be null.
	 * 
	 * @return The new item, with copies of the dependent IDs in this. This
	 *         value will not be null.
	 */
	default public JavaItem copyTo(JavaItemFactory newFactory) {
		// we use the parent of this object even though it doesn't have the new
		// ID in the new index, this must be resolved later
		JavaItem newItem = newFactory.createItem(getParent(), getName(), getType(), false);

		CopyVisitor visitor = new CopyVisitor(newItem);
		accept(visitor);

		return newItem;
	}

	/**
	 * This class visits an item and copies all its data to a new item.
	 */
	public static class CopyVisitor implements JavaItemVisitor {

		/**
		 * The item to copy data to.
		 */
		private JavaItem newItem;

		/**
		 * Constructor for this.
		 * 
		 * @param newItem
		 *            The item to copy data to. This value cannot be null.
		 */
		public CopyVisitor(JavaItem newItem) {
			this.newItem = newItem;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitParent(JavaItem item, Integer parent) {
			// this is already in the new item, do nothing
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitChildren(JavaItem item, List<Integer> childrenIDs) {
			for (Integer childID : childrenIDs) {
				newItem.getChildrenIDs().add(childID);
			}
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitDependencies(JavaItem item, List<Integer> dependencyIDs) {
			for (Integer dependencyID : dependencyIDs) {
				newItem.getDependenciesIDs().add(dependencyID);
			}
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitIncoming(JavaItem item, List<Integer> incomingIDs) {
			for (Integer incomingID : incomingIDs) {
				newItem.getIncomingIDs().add(incomingID);
			}
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitSuperclass(JavaItem classItem, Integer superclassID) {
			newItem.setAttribute(ATTR_SUPERCLASS, superclassID);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitSuperInterfaces(JavaItem classItem, Set<Integer> superInterfaceIDs) {
			Set<Integer> newSuperInterfaceIDs = new LinkedHashSet<>();
			newSuperInterfaceIDs.addAll(superInterfaceIDs);
			newItem.setAttribute(ATTR_SUPERINTERFACES, newSuperInterfaceIDs);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitSubclasses(JavaItem classItem, Set<Integer> subclassIDs) {
			Set<Integer> newSubclassIDs = new LinkedHashSet<>();
			newSubclassIDs.addAll(subclassIDs);
			newItem.setAttribute(ATTR_SUBCLASSES, newSubclassIDs);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitReturnType(JavaItem methodItem, Integer returnTypeID) {
			newItem.setAttribute(ATTR_RETURN_TYPE, returnTypeID);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitMethodParamTypes(JavaItem methodItem, List<Integer> methodParamTypeIDs) {
			List<Integer> newMethodParamTypes = new ArrayList<>();
			newMethodParamTypes.addAll(methodParamTypeIDs);
			newItem.setAttribute(ATTR_METHOD_PARAM_TYPES, newMethodParamTypes);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visitPackagePrivateVisible(JavaItem item, Boolean packagePrivateVisible) {
			newItem.setAttribute(ATTR_PROJECT_PRIVATE_VISIBLE, packagePrivateVisible);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitArrayBaseClass(JavaItem arrayClassItem, Integer arrayBaseClassID) {
			newItem.setAttribute(ATTR_ARRAY_BASE_CLASS, arrayBaseClassID);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitUsedByArrayClass(JavaItem classItem, Integer usedByArrayClassID) {
			newItem.setAttribute(ATTR_USED_BY_ARRAY_CLASS, usedByArrayClassID);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitFieldType(JavaItem fieldItem, Integer fieldTypeID) {
			newItem.setAttribute(ATTR_FIELD_TYPE, fieldTypeID);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitMethodThrowsTypes(JavaItem methodItem, List<Integer> methodThrowsTypeIDs) {
			List<Integer> newMethodThrowsTypes = new ArrayList<>();
			newMethodThrowsTypes.addAll(methodThrowsTypeIDs);
			newItem.setAttribute(ATTR_METHOD_THROWS_TYPES, newMethodThrowsTypes);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visitBinary(JavaItem item, Boolean binary) {
			newItem.setAttribute(ATTR_BINARY, binary);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visitThirdParty(JavaItem item, Boolean thirdParty) {
			newItem.setAttribute(ATTR_THIRD_PARTY, thirdParty);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitInnerClasses(JavaItem classItem, Set<Integer> innerClassIDs) {
			Set<Integer> newInnerClassIDs = new LinkedHashSet<>();
			newInnerClassIDs.addAll(innerClassIDs);
			newItem.setAttribute(ATTR_INNER_CLASSES, newInnerClassIDs);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitOuterClass(JavaItem classItem, Integer outerClassID) {
			newItem.setAttribute(ATTR_OUTER_CLASS, outerClassID);
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visitUsedByMethodsAndFields(JavaItem classItem, Set<Integer> usedByMethodsAndFieldIDs) {
			Set<Integer> newUsedByMethodsAndFieldIDs = new LinkedHashSet<>();
			newUsedByMethodsAndFieldIDs.addAll(usedByMethodsAndFieldIDs);
			newItem.setAttribute(ATTR_USED_BY_METHODS_AND_FIELDS, newUsedByMethodsAndFieldIDs);
			return false;
		}

	}
}
