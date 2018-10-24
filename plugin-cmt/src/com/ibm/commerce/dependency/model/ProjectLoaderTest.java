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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * ProjectLoaderTest tests the {@link ProjectLoader} class.
 * 
 * @author Trent Hoeppner
 */
public class ProjectLoaderTest extends TestCase {

	/**
	 * The instance that is being tested. This value will be null before the
	 * first test case is run.
	 */
	private ProjectLoader loader;

	/**
	 * Constructor for this.
	 *
	 * @param testName
	 *            The name of the test used by JUnit. Cannot be null or empty.
	 */
	public ProjectLoaderTest(String testName) {
		super(testName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		JavaItemIndex index = new JavaItemIndex("v8");
		JavaItemFactory factory = new JavaItemFactory(index);
		loader = new ProjectLoader(factory);
	}

	/**
	 * Tests that a project can be loaded with an empty Class-Path.
	 */
	public void testLoadIfValidNameEmptyClassPathExpectNameAndNoDependencies() {
		JavaItem project = loader.load("Hello", "Class-Path: ");
		assertEquals("Project name is wrong.", "Hello", project.getName());
		assertEquals("Project has wrong number of dependencies.", 0, project.getDependencies().size());
		assertEquals("Project has wrong number of incoming.", 0, project.getIncoming().size());
	}

	/**
	 * Tests that if the project name is null, an exception will be thrown.
	 */
	public void testLoadIfNullProjectNameExpectException() {
		try {
			loader.load(null, "Class-Path: ");
			fail("NullPointerException not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the project name is empty, an exception will be thrown.
	 */
	public void testLoadIfEmptyProjectNameExpectException() {
		try {
			loader.load("", "Class-Path: ");
			fail("IllegalArgumentException not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests that if the manifest is null, an exception will be thrown.
	 */
	public void testLoadIfNullManifestExpectException() {
		try {
			loader.load("Hello", (String) null);
			fail("NullPointerException not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that a project can be loaded with an empty manifest.
	 */
	public void testLoadIfValidNameEmptyManifestExpectNameAndNoDependencies() {
		JavaItem project = loader.load("Hello", "");
		assertEquals("Project name is wrong.", "Hello", project.getName());
		assertEquals("Project has wrong number of dependencies.", 0, project.getDependencies().size());
		assertEquals("Project has wrong number of incoming.", 0, project.getIncoming().size());
	}

	/**
	 * Tests that a project can be loaded with a manifest that specifies one
	 * dependency in the Class-Path.
	 */
	public void testLoadIfOneDependencyExpectOneDependency() {
		JavaItem project = loader.load("Hello", "Class-Path: Hello2");
		assertEquals("Project has wrong number of dependencies.", 1, project.getDependencies().size());
		assertEquals("First dependency is wrong.", "Hello2", project.getDependencies().get(0).getName());
		assertEquals("First dependency's incoming is wrong.", project,
				project.getDependencies().get(0).getIncoming().get(0));
	}

	/**
	 * Tests that a project can be loaded with a manifest that specifies one
	 * dependency in the Class-Path, which has a hyphen and an underscore, and a
	 * dot.
	 */
	public void testLoadIfOneDependencyWithHyphenUnderscoreDotExpectOneDependency() {
		JavaItem project = loader.load("Hello", "Class-Path: Hello1.2.jar");
		assertEquals("Project has wrong number of dependencies.", 1, project.getDependencies().size());
		assertEquals("Fifth dependency is wrong.", "Hello1.2", project.getDependencies().get(0).getName());
		assertEquals("First dependency's incoming is wrong.", project,
				project.getDependencies().get(0).getIncoming().get(0));
	}

	/**
	 * Tests that a project can be loaded with a manifest that specifies one
	 * dependency (that has the ".jar" suffix) in the Class-Path.
	 */
	public void testLoadIfOneDependencyWithJARExtensionExpectOneDependency() {
		JavaItem project = loader.load("Hello", "Class-Path: Hello2.jar");
		assertEquals("Project has wrong number of dependencies.", 1, project.getDependencies().size());
		assertEquals("First dependency is wrong.", "Hello2", project.getDependencies().get(0).getName());
		assertEquals("First dependency's incoming is wrong.", project,
				project.getDependencies().get(0).getIncoming().get(0));
	}

	/**
	 * Tests that a project can be loaded with a manifest that specifies two
	 * dependencies in the Class-Path.
	 */
	public void testLoadIfTwoDependenciesExpectTwoDependencies() {
		JavaItem project = loader.load("Hello", "Class-Path: Hello2 Hello3");
		assertEquals("Project has wrong number of dependencies.", 2, project.getDependencies().size());
		assertEquals("First dependency is wrong.", "Hello2", project.getDependencies().get(0).getName());
		assertEquals("Second dependency is wrong.", "Hello3", project.getDependencies().get(1).getName());
		assertEquals("First dependency's incoming is wrong.", project,
				project.getDependencies().get(0).getIncoming().get(0));
		assertEquals("Second dependency's incoming is wrong.", project,
				project.getDependencies().get(1).getIncoming().get(0));
	}

	/**
	 * Tests that a project can be loaded with a manifest that specifies two
	 * dependencies in the Class-Path, and one dependency wraps a line with DOS
	 * return values.
	 */
	public void testLoadIfTwoDependenciesOneHasDOSLineWrappingExpectTwoDependencies() {
		JavaItem project = loader.load("Hello", "Class-Path: Hello2 Hell\r\n o3");
		assertEquals("Project has wrong number of dependencies.", 2, project.getDependencies().size());
		assertEquals("First dependency is wrong.", "Hello2", project.getDependencies().get(0).getName());
		assertEquals("Second dependency is wrong.", "Hello3", project.getDependencies().get(1).getName());
		assertEquals("First dependency's incoming is wrong.", project,
				project.getDependencies().get(0).getIncoming().get(0));
		assertEquals("Second dependency's incoming is wrong.", project,
				project.getDependencies().get(1).getIncoming().get(0));
	}

	/**
	 * Tests that a project can be loaded with a manifest that specifies two
	 * dependencies in the Class-Path, and one dependency wraps a line with Unix
	 * return values.
	 */
	public void testLoadIfTwoDependenciesOneHasUnixLineWrappingExpectTwoDependencies() {
		JavaItem project = loader.load("Hello", "Class-Path: Hello2 Hell\n o3");
		assertEquals("Project has wrong number of dependencies.", 2, project.getDependencies().size());
		assertEquals("First dependency is wrong.", "Hello2", project.getDependencies().get(0).getName());
		assertEquals("Second dependency is wrong.", "Hello3", project.getDependencies().get(1).getName());
		assertEquals("First dependency's incoming is wrong.", project,
				project.getDependencies().get(0).getIncoming().get(0));
		assertEquals("Second dependency's incoming is wrong.", project,
				project.getDependencies().get(1).getIncoming().get(0));
	}

	/**
	 * Tests that a project can be loaded with a manifest that specifies two
	 * dependencies in the Class-Path, and one dependency wraps a line with Mac
	 * return values.
	 */
	public void testLoadIfTwoDependenciesOneHasMacLineWrappingExpectTwoDependencies() {
		JavaItem project = loader.load("Hello", "Class-Path: Hello2 Hell\r o3");
		assertEquals("Project has wrong number of dependencies.", 2, project.getDependencies().size());
		assertEquals("First dependency is wrong.", "Hello2", project.getDependencies().get(0).getName());
		assertEquals("Second dependency is wrong.", "Hello3", project.getDependencies().get(1).getName());
		assertEquals("First dependency's incoming is wrong.", project,
				project.getDependencies().get(0).getIncoming().get(0));
		assertEquals("Second dependency's incoming is wrong.", project,
				project.getDependencies().get(1).getIncoming().get(0));
	}

	/**
	 * Tests that a project can be loaded with a manifest that specifies two
	 * dependencies in the Class-Path, and one dependency wraps a line with DOS
	 * return values, and after the Class-Path there is another attribute.
	 */
	public void testLoadIfTwoDependenciesOneHasDOSLineWrappingAndAttributeAfterClassPathExpectTwoDependencies() {
		JavaItem project = loader.load("Hello", "Class-Path: Hello2 Hell\r\n o3\r\nJunk-Attribute");
		assertEquals("Project has wrong number of dependencies.", 2, project.getDependencies().size());
		assertEquals("First dependency is wrong.", "Hello2", project.getDependencies().get(0).getName());
		assertEquals("Second dependency is wrong.", "Hello3", project.getDependencies().get(1).getName());
		assertEquals("First dependency's incoming is wrong.", project,
				project.getDependencies().get(0).getIncoming().get(0));
		assertEquals("Second dependency's incoming is wrong.", project,
				project.getDependencies().get(1).getIncoming().get(0));
	}

	/**
	 * Tests that a project can be loaded with a manifest that specifies two
	 * dependencies in the Class-Path, and one dependency wraps a line with DOS
	 * return values, and after the Class-Path there is an empty line followed
	 * by another attribute.
	 */
	public void testLoadIfTwoDependenciesOneHasDOSLineWrappingAndEmptyLinePlusAttributeAfterClassPathExpectTwoDependencies() {
		JavaItem project = loader.load("Hello", "Class-Path: Hello2 Hell\r\n o3\r\n\r\nJunk-Attribute");
		assertEquals("Project has wrong number of dependencies.", 2, project.getDependencies().size());
		assertEquals("First dependency is wrong.", "Hello2", project.getDependencies().get(0).getName());
		assertEquals("Second dependency is wrong.", "Hello3", project.getDependencies().get(1).getName());
		assertEquals("First dependency's incoming is wrong.", project,
				project.getDependencies().get(0).getIncoming().get(0));
		assertEquals("Second dependency's incoming is wrong.", project,
				project.getDependencies().get(1).getIncoming().get(0));
	}

	/**
	 * Tests that loading one project, then loading a project that depends on it
	 * will result in the second project having the first one as a dependency.
	 */
	public void testLoadIfLoadingTwoProjectsAndSecondDependsOnFirstExpectSecondContainsTheFirst() {
		JavaItem project1 = loader.load("Hello1", "Class-Path: ");
		JavaItem project2 = loader.load("Hello2", "Class-Path: Hello1");

		assertTrue("Reference to the first project is not the same.", project2.getDependencies().get(0) == project1);
		assertEquals("First project's incoming is wrong.", project2, project1.getIncoming().get(0));
	}

	/**
	 * Tests that loading one project, then loading a second and third project
	 * that both depend on it will result in the third project having the first
	 * one as a dependency.
	 */
	public void testLoadIfLoadingThreeProjectsAndSecondAndThirdDependsOnFirstExpectThirdContainsTheFirst() {
		JavaItem project1 = loader.load("Hello1", "Class-Path: ");
		JavaItem project2 = loader.load("Hello2", "Class-Path: Hello1");
		JavaItem project3 = loader.load("Hello3", "Class-Path: Hello1");

		assertTrue("Reference to the first project is not the same.", project3.getDependencies().get(0) == project1);
		assertEquals("Project's first incoming is wrong.", project2, project1.getIncoming().get(0));
		assertEquals("Project's second incoming is wrong.", project3, project1.getIncoming().get(1));
	}

	/**
	 * Tests that loading one project that depends on a project that has not
	 * been loaded, then loading the dependent project, will result in the
	 * first's reference to the second being the same.
	 */
	public void testLoadIfTwoProjectsAndFirstDependsOnSecondExpectFirstRefersToSecond() {
		JavaItem project1 = loader.load("Hello1", "Class-Path: Hello2");
		JavaItem project2 = loader.load("Hello2", "Class-Path: ");

		assertTrue("Reference to the second project is not the same.", project1.getDependencies().get(0) == project2);
		assertEquals("Second project's incoming is wrong.", project1, project2.getIncoming().get(0));
	}

	/**
	 * Tests that loading one project that depends on a project that has not
	 * been loaded, then loading the dependent project which also depends on the
	 * first, will result in the first referring to the second and the second
	 * referring to the first.
	 */
	public void testLoadIfTwoProjectsAndTheyDependOnEachOtherExpectMutualCycle() {
		JavaItem project1 = loader.load("Hello1", "Class-Path: Hello2");
		JavaItem project2 = loader.load("Hello2", "Class-Path: Hello1");

		assertTrue("Reference from the first project to the second is not the same.",
				project1.getDependencies().get(0) == project2);
		assertTrue("Reference from the second project to the first is not the same.",
				project2.getDependencies().get(0) == project1);
		assertEquals("First project's incoming is wrong.", project2, project1.getIncoming().get(0));
		assertEquals("Second project's incoming is wrong.", project1, project2.getIncoming().get(0));
	}

	/**
	 * Tests that loading three projects that all depend on each other will
	 * result in all projects referring to each other.
	 */
	public void testLoadIfThreeProjectsAndTheyDependOnEachOtherExpectMutualCycle() {
		JavaItem project1 = loader.load("Hello1", "Class-Path: Hello2 Hello3");
		JavaItem project2 = loader.load("Hello2", "Class-Path: Hello1 Hello3");
		JavaItem project3 = loader.load("Hello3", "Class-Path: Hello1 Hello2");

		assertTrue("Reference from the first project to the second is not the same.",
				project1.getDependencies().get(0) == project2);
		assertTrue("Reference from the first project to the third is not the same.",
				project1.getDependencies().get(1) == project3);
		assertTrue("Reference from the second project to the first is not the same.",
				project2.getDependencies().get(0) == project1);
		assertTrue("Reference from the second project to the third is not the same.",
				project2.getDependencies().get(1) == project3);
		assertTrue("Reference from the third project to the first is not the same.",
				project3.getDependencies().get(0) == project1);
		assertTrue("Reference from the third project to the second is not the same.",
				project3.getDependencies().get(1) == project2);

		assertEquals("First project's first incoming is wrong.", project2, project1.getIncoming().get(0));
		assertEquals("First project's second incoming is wrong.", project3, project1.getIncoming().get(1));
		assertEquals("Second project's first incoming is wrong.", project1, project2.getIncoming().get(0));
		assertEquals("Second project's second incoming is wrong.", project3, project2.getIncoming().get(1));
		assertEquals("Third project's first incoming is wrong.", project1, project3.getIncoming().get(0));
		assertEquals("Third project's second incoming is wrong.", project2, project3.getIncoming().get(1));
	}

	/**
	 * Tests that a project that refers to itself can be loaded.
	 */
	public void testLoadIfProjectDependsOnItselfExpectReferenceIsSameAsOriginal() {
		JavaItem project1 = loader.load("Hello1", "Class-Path: Hello1");

		assertTrue("Reference from project to itself is not the same.", project1.getDependencies().get(0) == project1);
		assertEquals("Project's incoming is wrong.", project1, project1.getIncoming().get(0));
	}

	/**
	 * Tests that a manifest file can be loaded.
	 *
	 * @throws Exception
	 *             If an unexpected error occurred.
	 */
	public void testLoadManifestIfFileExistsAndValidExpectFullString() throws Exception {
		String manifest = loader.loadManifest(new File("testData\\validManifest.mf"));

		assertEquals("Manifest was not loaded correctly.",
				"Manifest-Version: 1.0\r\n"
						+ "Class-Path: Enablement-BaseComponentsLogic.jar Enablement-Relationship\r\n"
						+ " ManagementLogic.jar Price-Server-FEP.jar Price-DataObjects.jar Enable\r\n"
						+ " ment-RelationshipManagementData.jar\r\n" + "\r\n" + "",
				manifest);
	}

	/**
	 * Tests that if the file does not exist then an exception will be thrown.
	 */
	public void testLoadManifestIfFileDoesNotExistExpectException() {
		try {
			loader.loadManifest(new File("testData\\notExistingManifest.mf"));
			fail("IOException was not thrown.");
		} catch (IOException e) {
			// success
		}
	}

	/**
	 * Tests that if the file is null then an exception will be thrown.
	 *
	 * @throws Exception
	 *             If an unexpected error occurred.
	 */
	public void testLoadManifestIfFileIsNullExpectException() throws Exception {
		try {
			loader.loadManifest(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that projects from a manifest file can be loaded.
	 *
	 * @throws Exception
	 *             If an unexpected error occurred.
	 */
	public void testLoadFromManifestIfFileExistsAndValidExpectProjectsOK() throws Exception {
		JavaItem project = loader.load("Hello", new File("testData\\validManifest.mf"));

		assertEquals("Project has wrong number of dependencies.", 5, project.getDependencies().size());
		assertEquals("First dependency is wrong.", "Enablement-BaseComponentsLogic",
				project.getDependencies().get(0).getName());
		assertEquals("Second dependency is wrong.", "Enablement-RelationshipManagementLogic",
				project.getDependencies().get(1).getName());
		assertEquals("Third dependency is wrong.", "Price-Server-FEP", project.getDependencies().get(2).getName());
		assertEquals("Fourth dependency is wrong.", "Price-DataObjects", project.getDependencies().get(3).getName());
		assertEquals("Fifth dependency is wrong.", "Enablement-RelationshipManagementData",
				project.getDependencies().get(4).getName());
	}
}
