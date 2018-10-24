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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.commerce.cmt.plan.IDGenerator;

import junit.framework.TestCase;

/**
 * This class tests the {@link JavaItemFactory} class.
 * 
 * @author Trent Hoeppner
 */
public class JavaItemFactoryTest extends TestCase {

	private JavaItemIndex baseIndex;

	private JavaItemIndex deltaIndex;

	private JavaItemFactory baseFactory;

	private JavaItemFactory deltaFactory;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setUp() {
		baseIndex = new JavaItemIndex("v8");
		baseIndex.setIDGenerator(new IDGenerator(0));
		baseFactory = new JavaItemFactory(baseIndex);
	}

	private void setUpDelta() {
		deltaIndex = new JavaItemIndex("v9", baseIndex);
		deltaFactory = new JavaItemFactory(deltaIndex);
	}

	/**
	 * Tests that if a delta project was just created, all the data will be the
	 * same.
	 */
	public void testCreateProjectDeltaIfJustCreatedWithNameExpectSameAsBase() {
		JavaItem baseProject = baseFactory.createProject("Project");

		setUpDelta();

		JavaItem deltaProject = deltaFactory.createProject("Project");
		assertNamesAndIDsEqual(baseProject, deltaProject);
	}

	/**
	 * Tests that if a delta project was created and a child was added, the name
	 * and ID will be the same but the child will be added.
	 */
	public void testCreateProjectDeltaIfChildAddedExpectInDeltaNotBase() {
		JavaItem baseProject = baseFactory.createProject("Project");

		setUpDelta();

		JavaItem deltaProject = deltaFactory.createProject("Project");
		JavaItem deltaPackage = deltaFactory.createPackage(deltaProject, "package");

		assertNamesAndIDsEqual(baseProject, deltaProject);
		assertEquals("Base children is wrong.", 0, baseProject.getChildrenIDs().size());
		assertEquals("Delta children is wrong.", 1, deltaProject.getChildrenIDs().size());
		assertEquals("Delta child 0 is wrong.", deltaPackage.getID(), (int) deltaProject.getChildrenIDs().get(0));
		assertEquals("Base index size is wrong.", 1, baseIndex.getItems().size());
		assertEquals("Delta index size is wrong.", 2, deltaIndex.getItems().size());
		assertEquals("New delta item ID is wrong.", 1, deltaIndex.getItems().get(1).getID());
	}

	/**
	 * Tests that if a delta project was created and a child was removed from
	 * the index, the name and ID will be the same but the child will be removed
	 * from the parent and the index.
	 */
	public void testCreateProjectDeltaIfChildRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");

		setUpDelta();

		JavaItem deltaProject = deltaFactory.createProject("Project");
		deltaIndex.removeItem(basePackage.getID());

		assertNamesAndIDsEqual(baseProject, deltaProject);
		assertEquals("Base children is wrong.", 1, baseProject.getChildrenIDs().size());
		assertEquals("Delta children is wrong.", 0, deltaProject.getChildrenIDs().size());
		checkIndexSizes(2, 1);
	}

	/**
	 * Tests that if a delta project was created and a parent was removed from
	 * the index, the name and ID of the child will be the same but the parent
	 * will be removed from the child and the index.
	 */
	public void testCreatePackageDeltaIfParentRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");

		setUpDelta();

		JavaItem deltaProject = deltaFactory.createProject("Project");
		JavaItem deltaPackage = deltaFactory.createPackage(deltaProject, "package");
		deltaIndex.removeItem(deltaProject.getID());

		assertNamesAndIDsEqual(basePackage, deltaPackage);
		assertNotNull("Base parent is null.", basePackage.getParentID());
		assertEquals("Delta parent is not null.", null, deltaPackage.getParentID());
		checkIndexSizes(2, 1);
	}

	/**
	 * Tests that if a delta project was created and a dependency was removed
	 * from the index, the name and ID will be the same but the dependency will
	 * be removed from the parent and the index.
	 */
	public void testCreateProjectDeltaIfDependencyRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem baseDependent = baseFactory.createProject("Dependent");
		baseProject.getDependenciesIDs().add(baseDependent.getID());
		baseDependent.getIncomingIDs().add(baseProject.getID());

		setUpDelta();

		JavaItem deltaProject = deltaFactory.createProject("Project");
		JavaItem deltaDependent = deltaProject.getDependencies().get(0);
		deltaIndex.removeItem(deltaDependent.getID());

		assertNamesAndIDsEqual(baseProject, deltaProject);
		assertEquals("Base dependencies is wrong.", 1, baseProject.getDependenciesIDs().size());
		assertEquals("Delta dependencies is wrong.", 0, deltaProject.getDependenciesIDs().size());
		checkIndexSizes(2, 1);
	}

	/**
	 * Tests that if a delta project was created with a dependency and the first
	 * project was removed from the index, the name and ID will be the same but
	 * the first project will be removed from the dependency and the index.
	 */
	public void testCreateProjectDeltaIfIncomingRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem baseDependent = baseFactory.createProject("Dependent");
		baseProject.getDependenciesIDs().add(baseDependent.getID());
		baseDependent.getIncomingIDs().add(baseProject.getID());

		setUpDelta();

		JavaItem deltaProject = deltaFactory.createProject("Project");
		JavaItem deltaDependent = deltaProject.getDependencies().get(0);
		deltaIndex.removeItem(deltaProject.getID());

		assertNamesAndIDsEqual(baseProject, deltaProject);
		assertEquals("Base incoming is wrong.", 1, baseDependent.getIncomingIDs().size());
		assertEquals("Delta incoming is wrong.", 0, deltaDependent.getIncomingIDs().size());
		checkIndexSizes(2, 1);
	}

	/**
	 * Tests that if a delta class was created and a superclass was removed from
	 * the index, the name and ID of the class will be the same but the
	 * superclass will be removed from the class and the index.
	 */
	@SuppressWarnings("cast")
	public void testCreateClassDeltaIfSuperclassRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseSubclass = baseFactory.createClass(basePackage, "subclass");
		JavaItem baseSuperclass = baseFactory.createClass(basePackage, "superclass");
		baseSubclass.setAttribute(JavaItem.ATTR_SUPERCLASS, baseSuperclass.getID());
		baseSuperclass.setAttribute(JavaItem.ATTR_SUBCLASSES,
				new LinkedHashSet<Integer>(Arrays.asList(baseSubclass.getID())));

		setUpDelta();

		JavaItem deltaPackage = deltaIndex.getItem(basePackage.getID());
		JavaItem deltaSubclass = deltaFactory.createClass(deltaPackage, "subclass");
		JavaItem deltaSuperclass = deltaIndex.getItem(baseSuperclass.getID());
		deltaIndex.removeItem(deltaSuperclass.getID());

		assertNamesAndIDsEqual(baseSubclass, deltaSubclass);
		assertEquals("Base superclass is wrong.", (Object) baseSuperclass.getID(),
				baseSubclass.getAttribute(JavaItem.ATTR_SUPERCLASS));
		assertEquals("Delta superclass is not null.", (Object) null,
				deltaSubclass.getAttribute(JavaItem.ATTR_SUPERCLASS));
		checkIndexSizes(4, 3);
	}

	/**
	 * Tests that if a delta class was created and a superinterface was removed
	 * from the index, the name and ID of the class will be the same but the
	 * superinterface will be removed from the class and the index.
	 */
	@SuppressWarnings({ "unchecked" })
	public void testCreateClassDeltaIfSuperinterfaceRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseSubclass = baseFactory.createClass(basePackage, "subclass");
		JavaItem baseSuperInterface = baseFactory.createClass(basePackage, "superinterface");
		baseSubclass.setAttribute(JavaItem.ATTR_SUPERINTERFACES,
				new LinkedHashSet<Integer>(Arrays.asList(baseSuperInterface.getID())));
		baseSuperInterface.setAttribute(JavaItem.ATTR_SUBCLASSES,
				new LinkedHashSet<Integer>(Arrays.asList(baseSubclass.getID())));

		assertEquals("Base superinterfaces size is wrong.", 1,
				((Set<Integer>) baseSubclass.getAttribute(JavaItem.ATTR_SUPERINTERFACES)).size());

		setUpDelta();

		JavaItem deltaPackage = deltaIndex.getItem(basePackage.getID());
		JavaItem deltaSubclass = deltaFactory.createClass(deltaPackage, "subclass");
		JavaItem deltaSuperInterface = deltaIndex.getItem(baseSuperInterface.getID());
		deltaIndex.removeItem(deltaSuperInterface.getID());

		assertNamesAndIDsEqual(baseSubclass, deltaSubclass);
		assertEquals("Base superinterfaces size is wrong.", 1,
				((Set<Integer>) baseSubclass.getAttribute(JavaItem.ATTR_SUPERINTERFACES)).size());
		assertEquals("Delta superinterfaces size is wrong.", 0,
				((Set<Integer>) deltaSubclass.getAttribute(JavaItem.ATTR_SUPERINTERFACES)).size());
		checkIndexSizes(4, 3);
	}

	/**
	 * Tests that if a delta class was created and a subclass was removed from
	 * the index, the name and ID of the class will be the same but the subclass
	 * will be removed from the class and the index.
	 */
	@SuppressWarnings({ "unchecked" })
	public void testCreateClassDeltaIfSubclassRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseSubclass = baseFactory.createClass(basePackage, "subclass");
		JavaItem baseSuperclass = baseFactory.createClass(basePackage, "superclass");
		baseSubclass.setAttribute(JavaItem.ATTR_SUPERCLASS, baseSuperclass.getID());
		baseSuperclass.setAttribute(JavaItem.ATTR_SUBCLASSES,
				new LinkedHashSet<Integer>(Arrays.asList(baseSubclass.getID())));

		setUpDelta();

		JavaItem deltaPackage = deltaIndex.getItem(basePackage.getID());
		JavaItem deltaSubclass = deltaFactory.createClass(deltaPackage, "subclass");
		JavaItem deltaSuperclass = deltaIndex.getItem(baseSuperclass.getID());
		deltaIndex.removeItem(deltaSubclass.getID());

		assertNamesAndIDsEqual(baseSuperclass, deltaSuperclass);
		assertEquals("Base subclasses size is wrong.", 1,
				((Set<Integer>) baseSuperclass.getAttribute(JavaItem.ATTR_SUBCLASSES)).size());
		assertEquals("Delta subclasses size is wrong.", 0,
				((Set<Integer>) deltaSuperclass.getAttribute(JavaItem.ATTR_SUBCLASSES)).size());
		checkIndexSizes(4, 3);
	}

	/**
	 * Tests that if a delta interface was created and a subclass was removed
	 * from the index, the name and ID of the interface will be the same but the
	 * subclass will be removed from the class and the index.
	 */
	@SuppressWarnings({ "unchecked" })
	public void testCreateClassDeltaIfSuperinterfaceAndSubclassRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseSubclass = baseFactory.createClass(basePackage, "subclass");
		JavaItem baseSuperinterface = baseFactory.createClass(basePackage, "superinterface");
		baseSubclass.setAttribute(JavaItem.ATTR_SUPERINTERFACES,
				new LinkedHashSet<Integer>(Arrays.asList(baseSuperinterface.getID())));
		baseSuperinterface.setAttribute(JavaItem.ATTR_SUBCLASSES,
				new LinkedHashSet<Integer>(Arrays.asList(baseSubclass.getID())));

		setUpDelta();

		JavaItem deltaPackage = deltaIndex.getItem(basePackage.getID());
		JavaItem deltaSubclass = deltaFactory.createClass(deltaPackage, "subclass");
		JavaItem deltaSuperinterface = deltaIndex.getItem(baseSuperinterface.getID());
		deltaIndex.removeItem(deltaSubclass.getID());

		assertNamesAndIDsEqual(baseSuperinterface, deltaSuperinterface);
		assertEquals("Base subclasses size is wrong.", 1,
				((Set<Integer>) baseSuperinterface.getAttribute(JavaItem.ATTR_SUBCLASSES)).size());
		assertEquals("Delta subclasses size is wrong.", 0,
				((Set<Integer>) deltaSuperinterface.getAttribute(JavaItem.ATTR_SUBCLASSES)).size());
		checkIndexSizes(4, 3);
	}

	/**
	 * Tests that if a delta method was created and a return type was removed
	 * from the index, the name and ID of the method will be the same but the
	 * return type will be removed from the method and the index.
	 */
	@SuppressWarnings("cast")
	public void testCreateMethodDeltaIfReturnTypeRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseClass = baseFactory.createClass(basePackage, "class");
		JavaItem baseMethod = baseFactory.createMethod(baseClass, "method", Collections.emptyList());
		JavaItem baseReturnType = baseFactory.createClass(basePackage, "returntype");
		baseMethod.setAttribute(JavaItem.ATTR_RETURN_TYPE, baseReturnType.getID());
		baseReturnType.setAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS,
				new LinkedHashSet<Integer>(Arrays.asList(baseMethod.getID())));

		setUpDelta();

		JavaItem deltaClass = deltaIndex.getItem(baseClass.getID());
		JavaItem deltaMethod = deltaFactory.createMethod(deltaClass, "method", Collections.emptyList());
		JavaItem deltaReturnType = deltaIndex.getItem(baseReturnType.getID());
		deltaIndex.removeItem(deltaReturnType.getID());

		assertNamesAndIDsEqual(baseMethod, deltaMethod);
		assertEquals("Base return type is wrong.", (Object) baseReturnType.getID(),
				baseMethod.getAttribute(JavaItem.ATTR_RETURN_TYPE));
		assertEquals("Delta return type is not null.", (Object) null,
				deltaMethod.getAttribute(JavaItem.ATTR_RETURN_TYPE));
		checkIndexSizes(5, 4);
	}

	/**
	 * Tests that if a delta method was created with a return type, and the
	 * method was removed from the index, the name and ID of the return type
	 * will be the same but the method will be removed from the return type's
	 * usedbymethods set and the index.
	 */
	@SuppressWarnings({ "unchecked" })
	public void testCreateClassDeltaIfMethodThatReturnsClassIsRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseClass = baseFactory.createClass(basePackage, "class");
		JavaItem baseMethod = baseFactory.createMethod(baseClass, "method", Collections.emptyList());
		JavaItem baseReturnType = baseFactory.createClass(basePackage, "returntype");
		baseMethod.setAttribute(JavaItem.ATTR_RETURN_TYPE, baseReturnType.getID());
		baseReturnType.setAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS,
				new LinkedHashSet<Integer>(Arrays.asList(baseMethod.getID())));

		setUpDelta();

		JavaItem deltaClass = deltaIndex.getItem(baseClass.getID());
		JavaItem deltaMethod = deltaFactory.createMethod(deltaClass, "method", Collections.emptyList());
		JavaItem deltaReturnType = deltaIndex.getItem(baseReturnType.getID());
		deltaIndex.removeItem(deltaMethod.getID());

		assertNamesAndIDsEqual(baseReturnType, deltaReturnType);
		assertEquals("Base usedbymethods is wrong.", 1,
				((Set<Integer>) baseReturnType.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS)).size());
		assertEquals("Delta usedbymethods is wrong.", 0,
				((Set<Integer>) deltaReturnType.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS)).size());
		checkIndexSizes(5, 4);
	}

	/**
	 * Tests that if a delta method was created and a parameter type was removed
	 * from the index, the name and ID of the method will be the same but the
	 * parameter type will be removed from the method and the index.
	 */
	@SuppressWarnings({ "unchecked" })
	public void testCreateMethodDeltaIfParamTypeRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseClass = baseFactory.createClass(basePackage, "class");
		JavaItem baseParamType = baseFactory.createClass(basePackage, "paramtype");
		JavaItem baseMethod = baseFactory.createMethod(baseClass, "method", Arrays.asList(baseParamType.getID()));
		baseParamType.setAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS,
				new LinkedHashSet<Integer>(Arrays.asList(baseMethod.getID())));

		setUpDelta();

		JavaItem deltaClass = deltaIndex.getItem(baseClass.getID());
		JavaItem deltaMethod = deltaFactory.createMethod(deltaClass, "method", Collections.emptyList());
		JavaItem deltaParamType = deltaIndex.getItem(baseParamType.getID());
		deltaIndex.removeItem(deltaParamType.getID());

		assertNamesAndIDsEqual(baseMethod, deltaMethod);
		assertEquals("Base param types size is wrong.", 1,
				((List<Integer>) baseMethod.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES)).size());
		assertEquals("Delta param types size is wrong.", 0,
				((List<Integer>) deltaMethod.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES)).size());
		checkIndexSizes(5, 4);
	}

	/**
	 * Tests that if a delta method was created with a parameter type, and the
	 * method was removed from the index, the name and ID of the parameter type
	 * will be the same but the method will be removed from the parameter type's
	 * usedbymethods set and the index.
	 */
	@SuppressWarnings({ "unchecked" })
	public void testCreateClassDeltaIfMethodWithParamTypeRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseClass = baseFactory.createClass(basePackage, "class");
		JavaItem baseParamType = baseFactory.createClass(basePackage, "paramtype");
		JavaItem baseMethod = baseFactory.createMethod(baseClass, "method",Arrays.asList(baseParamType.getID()));
		baseParamType.setAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS,
				new LinkedHashSet<Integer>(Arrays.asList(baseMethod.getID())));

		setUpDelta();

		JavaItem deltaClass = deltaIndex.getItem(baseClass.getID());
		JavaItem deltaMethod = deltaFactory.createMethod(deltaClass, "method", Collections.emptyList());
		JavaItem deltaParamType = deltaIndex.getItem(baseParamType.getID());
		deltaIndex.removeItem(deltaMethod.getID());

		assertNamesAndIDsEqual(baseParamType, deltaParamType);
		assertEquals("Base usedbymethods size is wrong.", 1,
				((Set<Integer>) baseParamType.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS)).size());
		assertEquals("Delta usedbymethods size is wrong.", 0,
				((Set<Integer>) deltaParamType.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS)).size());
		checkIndexSizes(5, 4);
	}

	/**
	 * Tests that if a delta method was created and a throws type was removed
	 * from the index, the name and ID of the method will be the same but the
	 * throws type will be removed from the method and the index.
	 */
	@SuppressWarnings({ "unchecked" })
	public void testCreateMethodDeltaIfThrowsTypeRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseClass = baseFactory.createClass(basePackage, "class");
		JavaItem baseMethod = baseFactory.createMethod(baseClass, "method", Collections.emptyList());
		JavaItem baseThrowsType = baseFactory.createClass(basePackage, "throwstype");
		baseMethod.setAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES, Arrays.asList(baseThrowsType.getID()));
		baseThrowsType.setAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS,
				new LinkedHashSet<Integer>(Arrays.asList(baseMethod.getID())));

		setUpDelta();

		JavaItem deltaClass = deltaIndex.getItem(baseClass.getID());
		JavaItem deltaMethod = deltaFactory.createMethod(deltaClass, "method", Collections.emptyList());
		JavaItem deltaThrowsType = deltaIndex.getItem(baseThrowsType.getID());
		deltaIndex.removeItem(deltaThrowsType.getID());

		assertNamesAndIDsEqual(baseMethod, deltaMethod);
		assertEquals("Base throws types size is wrong.", 1,
				((List<Integer>) baseMethod.getAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES)).size());
		assertEquals("Delta throws types size is wrong.", 0,
				((List<Integer>) deltaMethod.getAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES)).size());
		checkIndexSizes(5, 4);
	}

	/**
	 * Tests that if a delta method was created with a throws type, and the
	 * method was removed from the index, the name and ID of the throws type
	 * will be the same but the method will be removed from the throws type's
	 * usedbymethods set and the index.
	 */
	@SuppressWarnings({ "unchecked" })
	public void testCreateClassDeltaIfMethodWithThrowsTypeRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseClass = baseFactory.createClass(basePackage, "class");
		JavaItem baseMethod = baseFactory.createMethod(baseClass, "method", Collections.emptyList());
		JavaItem baseThrowsType = baseFactory.createClass(basePackage, "paramtype");
		baseMethod.setAttribute(JavaItem.ATTR_METHOD_THROWS_TYPES, Arrays.asList(baseThrowsType.getID()));
		baseThrowsType.setAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS,
				new LinkedHashSet<Integer>(Arrays.asList(baseMethod.getID())));

		setUpDelta();

		JavaItem deltaClass = deltaIndex.getItem(baseClass.getID());
		JavaItem deltaMethod = deltaFactory.createMethod(deltaClass, "method", Collections.emptyList());
		JavaItem deltaThrowsType = deltaIndex.getItem(baseThrowsType.getID());
		deltaIndex.removeItem(deltaMethod.getID());

		assertNamesAndIDsEqual(baseThrowsType, deltaThrowsType);
		assertEquals("Base usedbymethods size is wrong.", 1,
				((Set<Integer>) baseThrowsType.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS)).size());
		assertEquals("Delta usedbymethods size is wrong.", 0,
				((Set<Integer>) deltaThrowsType.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS)).size());
		checkIndexSizes(5, 4);
	}

	/**
	 * Tests that if a delta array class was created and an array base class was
	 * removed from the index, the name and ID of the array class will be the
	 * same but the array base class will be removed from the class and the
	 * index.
	 */
	@SuppressWarnings("cast")
	public void testCreateClassDeltaIfArrayBaseClassRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseArrayClass = baseFactory.createClass(basePackage, "class[]");
		JavaItem baseArrayBaseClass = baseFactory.createClass(basePackage, "class");
		baseArrayClass.setAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS, baseArrayBaseClass.getID());
		baseArrayBaseClass.setAttribute(JavaItem.ATTR_USED_BY_ARRAY_CLASS, baseArrayClass.getID());

		setUpDelta();

		JavaItem deltaPackage = deltaIndex.getItem(basePackage.getID());
		JavaItem deltaArrayClass = deltaFactory.createClass(deltaPackage, "class[]");
		JavaItem deltaArrayBaseClass = deltaIndex.getItem(baseArrayBaseClass.getID());
		deltaIndex.removeItem(deltaArrayBaseClass.getID());

		assertNamesAndIDsEqual(baseArrayClass, deltaArrayClass);
		assertEquals("Base arrayBaseClass is wrong.", (Object) baseArrayBaseClass.getID(),
				baseArrayClass.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS));
		assertEquals("Delta arrayBaseClass is not null.", (Object) null,
				deltaArrayClass.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS));
		checkIndexSizes(4, 3);
	}

	/**
	 * Tests that if a delta array base class was created and an array class was
	 * removed from the index, the name and ID of the array base class will be
	 * the same but the array class will be removed from the class and the
	 * index.
	 */
	@SuppressWarnings("cast")
	public void testCreateClassDeltaIfArrayClassRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseArrayClass = baseFactory.createClass(basePackage, "class[]");
		JavaItem baseArrayBaseClass = baseFactory.createClass(basePackage, "class");
		baseArrayClass.setAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS, baseArrayBaseClass.getID());
		baseArrayBaseClass.setAttribute(JavaItem.ATTR_USED_BY_ARRAY_CLASS, baseArrayClass.getID());

		setUpDelta();

		JavaItem deltaPackage = deltaIndex.getItem(basePackage.getID());
		JavaItem deltaArrayBaseClass = deltaFactory.createClass(deltaPackage, "class");
		JavaItem deltaArrayClass = deltaIndex.getItem(baseArrayClass.getID());
		deltaIndex.removeItem(deltaArrayClass.getID());

		assertNamesAndIDsEqual(baseArrayBaseClass, deltaArrayBaseClass);
		assertEquals("Base arrayClass is wrong.", (Object) baseArrayClass.getID(),
				baseArrayBaseClass.getAttribute(JavaItem.ATTR_USED_BY_ARRAY_CLASS));
		assertEquals("Delta arrayClass is not null.", (Object) null,
				deltaArrayBaseClass.getAttribute(JavaItem.ATTR_USED_BY_ARRAY_CLASS));
		checkIndexSizes(4, 3);
	}

	/**
	 * Tests that if a delta field was created and a field type was removed from
	 * the index, the name and ID of the field will be the same but the field
	 * type will be removed from the field and the index.
	 */
	@SuppressWarnings("cast")
	public void testCreateFieldDeltaIfFieldTypeRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseClass = baseFactory.createClass(basePackage, "class");
		JavaItem baseField = baseFactory.createField(baseClass, "field");
		JavaItem baseFieldType = baseFactory.createClass(basePackage, "fieldtype");
		baseField.setAttribute(JavaItem.ATTR_FIELD_TYPE, baseFieldType.getID());
		baseFieldType.setAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS,
				new LinkedHashSet<Integer>(Arrays.asList(baseField.getID())));

		setUpDelta();

		JavaItem deltaClass = deltaIndex.getItem(baseClass.getID());
		JavaItem deltaField = deltaFactory.createField(deltaClass, "field");
		JavaItem deltaFieldType = deltaIndex.getItem(baseFieldType.getID());
		deltaIndex.removeItem(deltaFieldType.getID());

		assertNamesAndIDsEqual(baseField, deltaField);
		assertEquals("Base field type is wrong.", (Object) baseFieldType.getID(),
				baseField.getAttribute(JavaItem.ATTR_FIELD_TYPE));
		assertEquals("Delta field type is not null.", (Object) null, deltaField.getAttribute(JavaItem.ATTR_FIELD_TYPE));
		checkIndexSizes(5, 4);
	}

	/**
	 * Tests that if a delta field was created with a field type, and the field
	 * was removed from the index, the name and ID of the field type will be the
	 * same but the field will be removed from the field type's usedbymethods
	 * set and the index.
	 */
	@SuppressWarnings({ "unchecked" })
	public void testCreateClassDeltaIfFieldOfTypeClassIsRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseClass = baseFactory.createClass(basePackage, "class");
		JavaItem baseField = baseFactory.createField(baseClass, "field");
		JavaItem baseFieldType = baseFactory.createClass(basePackage, "fieldtype");
		baseField.setAttribute(JavaItem.ATTR_FIELD_TYPE, baseFieldType.getID());
		baseFieldType.setAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS,
				new LinkedHashSet<Integer>(Arrays.asList(baseField.getID())));

		setUpDelta();

		JavaItem deltaPackage = deltaIndex.getItem(basePackage.getID());
		JavaItem deltaField = deltaIndex.getItem(baseField.getID());
		JavaItem deltaFieldType = deltaFactory.createClass(deltaPackage, "fieldtype");
		deltaIndex.removeItem(deltaField.getID());

		assertNamesAndIDsEqual(baseFieldType, deltaFieldType);
		assertEquals("Base usedbymethods is wrong.", 1,
				((Set<Integer>) baseFieldType.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS)).size());
		assertEquals("Delta usedbymethods is wrong.", 0,
				((Set<Integer>) deltaFieldType.getAttribute(JavaItem.ATTR_USED_BY_METHODS_AND_FIELDS)).size());
		checkIndexSizes(5, 4);
	}

	/**
	 * Tests that if a delta inner class was created and an outer class was
	 * removed from the index, the name and ID of the inner class will be the
	 * same but the outer class will be removed from the inner class and the
	 * index.
	 */
	@SuppressWarnings("cast")
	public void testCreateClassDeltaIfOuterClassRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseInnerClass = baseFactory.createClass(basePackage, "outer$inner");
		JavaItem baseOuterClass = baseFactory.createClass(basePackage, "outer");
		baseInnerClass.setAttribute(JavaItem.ATTR_OUTER_CLASS, baseOuterClass.getID());
		baseOuterClass.setAttribute(JavaItem.ATTR_INNER_CLASSES,
				new LinkedHashSet<Integer>(Arrays.asList(baseInnerClass.getID())));

		setUpDelta();

		JavaItem deltaPackage = deltaIndex.getItem(basePackage.getID());
		JavaItem deltaInnerClass = deltaFactory.createClass(deltaPackage, "outer$inner");
		JavaItem deltaOuterClass = deltaIndex.getItem(baseOuterClass.getID());
		deltaIndex.removeItem(deltaOuterClass.getID());

		assertNamesAndIDsEqual(baseInnerClass, deltaInnerClass);
		assertEquals("Base outer class is wrong.", (Object) baseOuterClass.getID(),
				baseInnerClass.getAttribute(JavaItem.ATTR_OUTER_CLASS));
		assertEquals("Delta outer class is not null.", (Object) null,
				deltaInnerClass.getAttribute(JavaItem.ATTR_OUTER_CLASS));
		checkIndexSizes(4, 3);
	}

	/**
	 * Tests that if a delta outer class was created and an inner class was
	 * removed from the index, the name and ID of the outer class will be the
	 * same but the inner class will be removed from the outer class and the
	 * index.
	 */
	@SuppressWarnings({ "unchecked" })
	public void testCreateClassDeltaIfInnerClassRemovedExpectInBaseNotDelta() {
		JavaItem baseProject = baseFactory.createProject("Project");
		JavaItem basePackage = baseFactory.createPackage(baseProject, "package");
		JavaItem baseInnerClass = baseFactory.createClass(basePackage, "outer$inner");
		JavaItem baseOuterClass = baseFactory.createClass(basePackage, "outer");
		baseInnerClass.setAttribute(JavaItem.ATTR_OUTER_CLASS, baseOuterClass.getID());
		baseOuterClass.setAttribute(JavaItem.ATTR_INNER_CLASSES,
				new LinkedHashSet<Integer>(Arrays.asList(baseInnerClass.getID())));

		setUpDelta();

		JavaItem deltaPackage = deltaIndex.getItem(basePackage.getID());
		JavaItem deltaInnerClass = deltaIndex.getItem(baseInnerClass.getID());
		JavaItem deltaOuterClass = deltaFactory.createClass(deltaPackage, "outer");
		deltaIndex.removeItem(deltaInnerClass.getID());

		assertNamesAndIDsEqual(baseOuterClass, deltaOuterClass);
		assertEquals("Base inner classes size is wrong.", 1,
				((Set<Integer>) baseOuterClass.getAttribute(JavaItem.ATTR_INNER_CLASSES)).size());
		assertEquals("Delta inner classes size is wrong.", 0,
				((Set<Integer>) deltaOuterClass.getAttribute(JavaItem.ATTR_INNER_CLASSES)).size());
		checkIndexSizes(4, 3);
	}

	private void checkIndexSizes(int baseIndexSize, int deltaIndexSize) {
		assertEquals("Base index size is wrong.", baseIndexSize, baseIndex.size());
		assertEquals("Delta index size is wrong.", deltaIndexSize, deltaIndex.size());
		assertEquals("Base index items size is wrong.", baseIndexSize, baseIndex.getItems().size());
		assertEquals("Delta index items size is wrong.", baseIndexSize, deltaIndex.getItems().size());
	}

	private void assertNamesAndIDsEqual(JavaItem baseProject, JavaItem deltaProject) {
		assertEquals("IDs are different.", baseProject.getID(), deltaProject.getID());
		assertEquals("Names are different.", baseProject.getName(), deltaProject.getName());
	}
}
