package com.ibm.commerce.validation.tools;

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

import java.util.Iterator;
import java.util.Locale;

import com.ibm.commerce.qcheck.tools.PIIFile;

import junit.framework.TestCase;

/**
 * This class tests the {@link PIIFile} class.
 * 
 * @author Trent Hoeppner
 */
public class PIIFileTest extends TestCase {

	/**
	 * Tests that if the directory is null, an exception will be thrown.
	 */
	public void testConstructorIfDirNullExpectException() {
		try {
			new PIIFile(null, "file.properties", true);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the base name is null, an exception will be thrown.
	 */
	public void testConstructorIfBaseNameNullExpectException() {
		try {
			new PIIFile("", null, true);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the base name is empty, an exception will be thrown.
	 */
	public void testConstructorIfBaseNameEmptyExpectException() {
		try {
			new PIIFile("", "", true);
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests that if the parameters are ok, they can be retrieved.
	 */
	public void testConstructorIfParamsOKExpectCanBeRetrieved() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		assertEquals("dir is wrong.", "dir", file.getDir());
		assertEquals("baseName is wrong.", "file.properties", file.getBaseName());
		assertEquals("variantInParentDir is wrong.", true, file.isVariantInParentDir());
	}

	/**
	 * Tests that if the directory has the variant, the variantInParentDir will
	 * be true.
	 */
	public void testCreateIfDirHasVariantExpectCanBeRetrieved() {
		PIIFile file = PIIFile.create("dir\\en_US\\", "file.properties");

		assertEquals("dir is wrong.", "dir\\", file.getDir());
		assertEquals("baseName is wrong.", "file.properties", file.getBaseName());
		assertEquals("variantInParentDir is wrong.", true, file.isVariantInParentDir());
		assertEquals("Number of variants is wrong.", 1, file.getVariants().size());
		assertEquals("Variant 0 is wrong.", "en_US", file.getVariants().iterator().next().toString());
	}

	/**
	 * Tests that if the base name has the variant, the variantInParentDir will
	 * be false.
	 */
	public void testCreateIfBaseNameHasVariantExpectCanBeRetrieved() {
		PIIFile file = PIIFile.create("dir\\", "file_en_US.properties");

		assertEquals("dir is wrong.", "dir\\", file.getDir());
		assertEquals("baseName is wrong.", "file.properties", file.getBaseName());
		assertEquals("variantInParentDir is wrong.", false, file.isVariantInParentDir());
		assertEquals("Number of variants is wrong.", 1, file.getVariants().size());
		assertEquals("Variant 0 is wrong.", "en_US", file.getVariants().iterator().next().toString());
	}

	/**
	 * Tests that if the base name has a _ not part of the locale, the values
	 * can be retrieved.
	 */
	public void testConstructorIfUnderscoreInBaseNameExpectCanBeRetrieved() {
		PIIFile file = PIIFile.create("dir", "storetext_v2_en_US.properties");

		assertEquals("dir is wrong.", "dir", file.getDir());
		assertEquals("baseName is wrong.", "storetext_v2.properties", file.getBaseName());
		assertEquals("variantInParentDir is wrong.", false, file.isVariantInParentDir());
	}

	/**
	 * Tests that if the locale is null, an exception will be thrown.
	 */
	public void testAddVariantIfLocaleNullExpectException() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		try {
			file.addVariant(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if locale is ok, it will be added to the list of variants.
	 */
	public void testAddVariantIfLocaleOKExpectIsInListOfVariants() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		file.addVariant(Locale.US);

		assertEquals("Number of variants is wrong.", 1, file.getVariants().size());
		assertEquals("locale 0 is wrong.", Locale.US, file.getVariants().iterator().next());
	}

	/**
	 * Tests that if the locale is null, an exception will be thrown.
	 */
	public void testSetPropertyIfLocaleNullExpectException() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		file.addVariant(Locale.US);

		try {
			file.setProperty(null, "property", "value");
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the property name is null, an exception will be thrown.
	 */
	public void testSetPropertyIfNameNullExpectException() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		file.addVariant(Locale.US);

		try {
			file.setProperty(Locale.US, null, "value");
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the property name is empty, an exception will be thrown.
	 */
	public void testSetPropertyIfNameEmptyExpectException() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		file.addVariant(Locale.US);

		try {
			file.setProperty(Locale.US, "", "value");
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests that if the value is null, an exception will be thrown.
	 */
	public void testSetPropertyIfValueNullExpectException() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		file.addVariant(Locale.US);

		try {
			file.setProperty(Locale.US, "property", null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the locale was not previously added, an exception will be
	 * thrown.
	 */
	public void testSetPropertyIfVariantNotAddedExpectException() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		try {
			file.setProperty(Locale.US, "property", "value");
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests that if the params are ok, the value can be retrieved.
	 */
	public void testSetPropertyIfParamsOKExpectCanBeRetrieved() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		file.addVariant(Locale.US);

		file.setProperty(Locale.US, "property", "value");
		assertEquals("value is wrong.", "value", file.getProperty(Locale.US, "property"));
	}

	/**
	 * Tests that if the locale is null, an exception will be thrown.
	 */
	public void testGetPropertyIfLocaleNullExpectException() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		file.addVariant(Locale.US);

		try {
			file.getProperty(null, "property");
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the property name is null, an exception will be thrown.
	 */
	public void testGetPropertyIfNameNullExpectException() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		file.addVariant(Locale.US);

		try {
			file.getProperty(Locale.US, null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the property name is empty, an exception will be thrown.
	 */
	public void testGetPropertyIfNameEmptyExpectException() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		file.addVariant(Locale.US);

		try {
			file.getProperty(Locale.US, "");
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests that if the locale was not previously added, an exception will be
	 * thrown.
	 */
	public void testGetPropertyIfVariantNotAddedExpectException() {
		PIIFile file = new PIIFile("dir", "file.properties", true);

		try {
			file.getProperty(Locale.US, "property");
			fail("IllegalArgumentException was not thrown.");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	/**
	 * Tests that if the directory, base name and variantIsParent are all the
	 * same, then true will be returned and the hashcode will be the same.
	 */
	public void testEqualsIfDirAndBaseNameAndVariantIsParentAreSameExpectTrueAndSameHashcode() {
		PIIFile file1 = new PIIFile("dir", "file.properties", true);
		PIIFile file2 = new PIIFile("dir", "file.properties", true);

		assertEquals("equals is wrong.", true, file1.equals(file2));
		assertEquals("hashcode is different", file1.hashCode(), file2.hashCode());
	}

	/**
	 * Tests that if the directory is different, then false will be returned.
	 */
	public void testEqualsIfDirDifferentExpectFalse() {
		PIIFile file1 = new PIIFile("dir1", "file.properties", true);
		PIIFile file2 = new PIIFile("dir2", "file.properties", true);

		assertEquals("equals is wrong.", false, file1.equals(file2));
	}

	/**
	 * Tests that if the base name is different, then false will be returned.
	 */
	public void testEqualsIfBaseNameDifferentExpectFalse() {
		PIIFile file1 = new PIIFile("dir", "file1.properties", true);
		PIIFile file2 = new PIIFile("dir", "file2.properties", true);

		assertEquals("equals is wrong.", false, file1.equals(file2));
	}

	/**
	 * Tests that if isVariantInParent is different, then false will be
	 * returned.
	 */
	public void testEqualsIfVariantInParentDifferentExpectFalse() {
		PIIFile file1 = new PIIFile("dir", "file.properties", true);
		PIIFile file2 = new PIIFile("dir", "file.properties", false);

		assertEquals("equals is wrong.", false, file1.equals(file2));
	}

	/**
	 * Tests that if the directory, base name and variantIsParent are all the
	 * same, but one has a variant added and the other does not, then true will
	 * be returned and the hashcode will be the same.
	 */
	public void testEqualsIfDirAndBaseNameAndVariantIsParentAreSameBut1HasVariantExpectTrueAndSameHashcode() {
		PIIFile file1 = new PIIFile("dir", "file.properties", true);
		PIIFile file2 = new PIIFile("dir", "file.properties", true);

		file1.addVariant(Locale.US);

		assertEquals("equals is wrong.", true, file1.equals(file2));
		assertEquals("hashcode is different", file1.hashCode(), file2.hashCode());
	}

	/**
	 * Tests that if the directory, base name and variantIsParent are all the
	 * same, then 0 will be returned.
	 */
	public void testCompareToIfDirAndBaseNameAndVariantIsParentAreSameExpect0() {
		PIIFile file1 = new PIIFile("dir", "file.properties", true);
		PIIFile file2 = new PIIFile("dir", "file.properties", true);

		assertEquals("compareTo is wrong.", 0, file1.compareTo(file2));
	}

	/**
	 * Tests that if the first directory is before, then -1 will be returned.
	 */
	public void testCompareToIfFirstDirBeforeExpectNeg1() {
		PIIFile file1 = new PIIFile("dir1", "file.properties", true);
		PIIFile file2 = new PIIFile("dir2", "file.properties", true);

		assertEquals("compareTo is wrong.", -1, file1.compareTo(file2));
	}

	/**
	 * Tests that if the first directory is after, then 1 will be returned.
	 */
	public void testCompareToIfFirstDirAfterExpect1() {
		PIIFile file1 = new PIIFile("dir2", "file.properties", true);
		PIIFile file2 = new PIIFile("dir1", "file.properties", true);

		assertEquals("compareTo is wrong.", 1, file1.compareTo(file2));
	}

	/**
	 * Tests that if the first base name is before, then false will be returned.
	 */
	public void testCompareToIfFirstBaseNameBeforeExpectNeg1() {
		PIIFile file1 = new PIIFile("dir", "file1.properties", true);
		PIIFile file2 = new PIIFile("dir", "file2.properties", true);

		assertEquals("compareTo is wrong.", -1, file1.compareTo(file2));
	}

	/**
	 * Tests that if the first base name is after, then false will be returned.
	 */
	public void testCompareToIfFirstBaseNameAfterExpect1() {
		PIIFile file1 = new PIIFile("dir", "file2.properties", true);
		PIIFile file2 = new PIIFile("dir", "file1.properties", true);

		assertEquals("compareTo is wrong.", 1, file1.compareTo(file2));
	}

	/**
	 * Tests that if the first isVariantInParent is true, and the second false,
	 * then 1 will be returned.
	 */
	public void testCompareToIfFirstVariantInParentTrueExpect1() {
		PIIFile file1 = new PIIFile("dir", "file.properties", true);
		PIIFile file2 = new PIIFile("dir", "file.properties", false);

		assertEquals("compareTo is wrong.", 1, file1.compareTo(file2));
	}

	/**
	 * Tests that if the first isVariantInParent is false, and the second true,
	 * then -1 will be returned.
	 */
	public void testCompareToIfFirstVariantInParentFalseExpectNeg1() {
		PIIFile file1 = new PIIFile("dir", "file.properties", false);
		PIIFile file2 = new PIIFile("dir", "file.properties", true);

		assertEquals("compareTo is wrong.", -1, file1.compareTo(file2));
	}

	/**
	 * Tests that if the first directory is before but the first baseName is
	 * after, then -1 will be returned.
	 */
	public void testCompareToIfFirstDirBeforeBaseNameAfterExpectNeg1() {
		PIIFile file1 = new PIIFile("dir1", "file2.properties", true);
		PIIFile file2 = new PIIFile("dir2", "file1.properties", true);

		assertEquals("compareTo is wrong.", -1, file1.compareTo(file2));
	}

	/**
	 * Tests that if the first baseName is before but the first variantInParent
	 * is after, then -1 will be returned.
	 */
	public void testCompareToIfFirstBaseNameBeforeVariantInParentAfterExpectNeg1() {
		PIIFile file1 = new PIIFile("dir", "file1.properties", true);
		PIIFile file2 = new PIIFile("dir", "file2.properties", false);

		assertEquals("compareTo is wrong.", -1, file1.compareTo(file2));
	}

	/**
	 * Tests that if the directory, base name and variantIsParent are all the
	 * same, but one has a variant added and the other does not, then 0 will be
	 * returned.
	 */
	public void testCompareToIfDirAndBaseNameAndVariantIsParentAreSameBut1HasVariantExpect0() {
		PIIFile file1 = new PIIFile("dir", "file.properties", true);
		PIIFile file2 = new PIIFile("dir", "file.properties", true);

		file1.addVariant(Locale.US);

		assertEquals("compareTo is wrong.", 0, file1.compareTo(file2));
	}

	/**
	 * Tests that if the variant is not in the parent directory, the normal
	 * directory will be returned.
	 */
	public void testGetDirWithVariantIfNotVariantInDirExpectSameAsDir() {
		PIIFile file = new PIIFile("dir\\", "file.properties", false);
		file.addVariant(Locale.US);

		assertEquals("dir is wrong.", "dir\\", file.getDir(Locale.US));
	}

	/**
	 * Tests that if the variant is in the parent directory, the directory plus
	 * an encoding directory will be returned.
	 */
	public void testGetDirWithVariantIfVariantInDirExpectSameAsDir() {
		PIIFile file = new PIIFile("dir\\", "file.properties", true);
		file.addVariant(Locale.US);

		assertEquals("dir is wrong.", "dir\\EN_US\\", file.getDir(Locale.US));
	}

	/**
	 * Tests that if the variant is not in the parent directory, the name plus
	 * an encoding will be returned.
	 */
	public void testGetNameWithVariantIfNotVariantInDirExpectSameAsDir() {
		PIIFile file = new PIIFile("dir\\", "file.properties", false);
		file.addVariant(Locale.US);

		assertEquals("dir is wrong.", "file_EN_US.properties", file.getName(Locale.US));
	}

	/**
	 * Tests that if the variant is in the parent directory, the normal base
	 * name will be returned.
	 */
	public void testGetNameWithVariantIfVariantInDirExpectSameAsDir() {
		PIIFile file = new PIIFile("dir\\", "file.properties", true);
		file.addVariant(Locale.US);

		assertEquals("dir is wrong.", "file.properties", file.getName(Locale.US));
	}

	/**
	 * Tests that if the file has no properties, none will be returned.
	 */
	public void testGetPropertiesIfNoPropertiesExpectEmpty() {
		PIIFile file = new PIIFile("dir\\", "file.properties", true);
		file.addVariant(Locale.US);

		assertEquals("Number of properties is wrong.", 0, file.getProperties(Locale.US).size());
	}

	/**
	 * Tests that if the file has 2 properties, they will be returned.
	 */
	public void testGetPropertiesIf2PropertiesExpect2() {
		PIIFile file = new PIIFile("dir\\", "file.properties", true);
		file.addVariant(Locale.US);
		file.setProperty(Locale.US, "prop1", "val1");
		file.setProperty(Locale.US, "prop2", "val2");

		assertEquals("Number of properties is wrong.", 2, file.getProperties(Locale.US).size());
		Iterator<String> nameIterator = file.getProperties(Locale.US).iterator();
		assertEquals("property 0 is wrong.", "prop1", nameIterator.next());
		assertEquals("property 1 is wrong.", "prop2", nameIterator.next());
	}

}
