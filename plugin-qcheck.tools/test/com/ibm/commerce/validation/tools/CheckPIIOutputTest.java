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

import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;

import com.ibm.commerce.qcheck.core.FileCreatingTestCase;
import com.ibm.commerce.qcheck.tools.CheckPIIOutput;
import com.ibm.commerce.qcheck.tools.PIIFile;

/**
 * This class tests the {@link CheckPIIOutput} class.
 * 
 * @author Trent Hoeppner
 */
public class CheckPIIOutputTest extends FileCreatingTestCase {

	private static final String CONTENTS_START = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<Report>" + "<Heading>"
			+ "<Version>v15.06</Version><Platform>W</Platform>"
			+ "<Date><Year>2013</Year><Month>7</Month><Day>1</Day></Date>"
			+ "<Time><Hour>5</Hour><Minute>1</Minute></Time>" + "<Dir>E:\\CHKPII\\SCRIPTS\\CHKPII\\</Dir>"
			+ "<Files><FileList>CHKPII\\PIIFILESCHKPII.TXT</FileList></Files>"
			+ "<Language>Chinese - Traditional   (CP 950)  (CPConv: Cp950)</Language>"
			+ "<Parms><Parm>Check variant chars</Parm><Parm>Find code page</Parm></Parms>" + "</Heading>" + "<Files>";

	private static final String CONTENTS_END = "</Files>" + "</Report>";

	private CheckPIIOutput output;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setUp() throws Exception {
		output = new CheckPIIOutput();
	}

	/**
	 * Tests that if the parameter is null, an exception will be thrown.
	 */
	public void testParseIfNullExpectException() {
		try {
			output.parse((String) null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if the parameter is empty, the returned object will not
	 * contain anything.
	 */
	public void testParseIfEmptyExpectEmptyOutput() {
		output.parse("");
		assertNotNull("output is null.", output);
		assertEquals("Number of files is wrong.", 0, output.getFiles().size());
	}

	/**
	 * Tests that if the contents contain 1 file with 1 variant in the file
	 * name, the returned object will contain it.
	 */
	public void testParseIf1File1VariantInFileExpectInOutput() {
		output.parse(CONTENTS_START + "<Dir>COMPONENTS\\FOUNDATION\\WC.EAR\\PROPERTIES\\</Dir>" + "<File>"
				+ "<Name>USERREGISTRATIONEMAILACTIVATENOTIFICATION_DE_DE.PROPERTIES</Name>"
				+ "<Dir>COMPONENTS\\FOUNDATION\\WC.EAR\\PROPERTIES\\</Dir>" + "<FileType>JAVA2-PRB</FileType>"
				+ "<CodePage>ANY</CodePage>" + "<CodePageText>ANY (x'00'-x'7F')</CodePageText>" + "<Errors>0</Errors>"
				+ "<Warnings>0</Warnings>" + "</File>" + CONTENTS_END);
		assertNotNull("output is null.", output);
		assertEquals("Number of files is wrong.", 1, output.getFiles().size());
		PIIFile file = output.getFiles().get(0);
		assertEquals("file dir is wrong.", "COMPONENTS\\FOUNDATION\\WC.EAR\\PROPERTIES\\", file.getDir());
		assertEquals("file baseName is wrong.", "USERREGISTRATIONEMAILACTIVATENOTIFICATION.PROPERTIES",
				file.getBaseName());
		assertEquals("variantInParent is wrong.", false, file.isVariantInParentDir());
		Locale locale = Locale.GERMANY;
		assertEquals("file German FileType is wrong.", "JAVA2-PRB", file.getProperty(locale, "FileType"));
		assertEquals("file German CodePage is wrong.", "ANY", file.getProperty(locale, "CodePage"));
		assertEquals("file German CodePageText is wrong.", "ANY (x'00'-x'7F')",
				file.getProperty(locale, "CodePageText"));
	}

	/**
	 * Tests that if the contents contain 1 file with 1 variant in the
	 * directory, the returned object will contain it.
	 */
	public void testParseIf1File1VariantInDirExpectInOutput() {
		output.parse(CONTENTS_START + "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\FR\\</Dir>"
				+ "<File>" + "<Name>ANONYMIZEUSERNLS.JS</Name>"
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\FR\\</Dir>"
				+ "<FileType>JSON</FileType>" + "<CodePage>UTF-8</CodePage>" + "<CodePageText>UTF-8</CodePageText>"
				+ "<Errors>0</Errors>" + "<Warnings>0</Warnings>" + "</File>" + CONTENTS_END);
		assertNotNull("output is null.", output);
		assertEquals("Number of files is wrong.", 1, output.getFiles().size());
		PIIFile file = output.getFiles().get(0);
		assertEquals("file dir is wrong.", "MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\",
				file.getDir());
		assertEquals("file baseName is wrong.", "ANONYMIZEUSERNLS.JS", file.getBaseName());
		assertEquals("variantInParent is wrong.", true, file.isVariantInParentDir());
		Locale locale = Locale.FRENCH;
		assertEquals("file French FileType is wrong.", "JSON", file.getProperty(locale, "FileType"));
		assertEquals("file French CodePage is wrong.", "UTF-8", file.getProperty(locale, "CodePage"));
		assertEquals("file French CodePageText is wrong.", "UTF-8", file.getProperty(locale, "CodePageText"));
	}

	/**
	 * Tests that if the contents contain 1 file with 1 variant in the
	 * directory, which specifies both the language and country, the returned
	 * object will contain it.
	 */
	public void testParseIf1File1VariantInDirWithCountryExpectInOutput() {
		output.parse(CONTENTS_START
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\FR_FR\\</Dir>" + "<File>"
				+ "<Name>ANONYMIZEUSERNLS.JS</Name>"
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\FR_FR\\</Dir>"
				+ "<FileType>JSON</FileType>" + "<CodePage>UTF-8</CodePage>" + "<CodePageText>UTF-8</CodePageText>"
				+ "<Errors>0</Errors>" + "<Warnings>0</Warnings>" + "</File>" + CONTENTS_END);
		assertNotNull("output is null.", output);
		assertEquals("Number of files is wrong.", 1, output.getFiles().size());
		PIIFile file = output.getFiles().get(0);
		assertEquals("file dir is wrong.", "MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\",
				file.getDir());
		assertEquals("file baseName is wrong.", "ANONYMIZEUSERNLS.JS", file.getBaseName());
		assertEquals("variantInParent is wrong.", true, file.isVariantInParentDir());
		Locale locale = Locale.FRANCE;
		assertEquals("file French FileType is wrong.", "JSON", file.getProperty(locale, "FileType"));
		assertEquals("file French CodePage is wrong.", "UTF-8", file.getProperty(locale, "CodePage"));
		assertEquals("file French CodePageText is wrong.", "UTF-8", file.getProperty(locale, "CodePageText"));
	}

	/**
	 * Tests that if the contents contain 1 file with 2 variant in the
	 * directory, which specifies both the language and country, the returned
	 * object will contain it.
	 */
	public void testParseIf1File2VariantInDirWithCountryExpectInOutput() {
		output.parse(CONTENTS_START
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\FR_FR\\</Dir>" + "<File>"
				+ "<Name>ANONYMIZEUSERNLS.JS</Name>"
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\FR_FR\\</Dir>"
				+ "<FileType>JSON</FileType>" + "<CodePage>UTF-8</CodePage>" + "<CodePageText>UTF-8</CodePageText>"
				+ "<Errors>0</Errors>" + "<Warnings>0</Warnings>" + "</File>"
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\DE_DE\\</Dir>" + "<File>"
				+ "<Name>ANONYMIZEUSERNLS.JS</Name>"
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\DE_DE\\</Dir>"
				+ "<FileType>JSON</FileType>" + "<CodePage>ANY</CodePage>"
				+ "<CodePageText>ANY (x'00'-x'7F')</CodePageText>" + "<Errors>0</Errors>" + "<Warnings>0</Warnings>"
				+ "</File>" + CONTENTS_END);
		assertNotNull("output is null.", output);
		assertEquals("Number of files is wrong.", 1, output.getFiles().size());
		PIIFile file = output.getFiles().get(0);
		checkPIIFile(file, "MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\", "ANONYMIZEUSERNLS.JS",
				true);
		checkVariant(file, Locale.FRANCE, "JSON", "UTF-8", "UTF-8");
		checkVariant(file, Locale.GERMANY, "JSON", "ANY", "ANY (x'00'-x'7F')");
	}

	/**
	 * Tests that if the contents come from a reader and contain 1 file with 2
	 * variant in the directory, which specifies both the language and country,
	 * the returned object will contain it.
	 */
	public void testParseWithReaderIf1File2VariantInDirWithCountryExpectInOutput() {
		String contents = CONTENTS_START
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\FR_FR\\</Dir>" + "<File>"
				+ "<Name>ANONYMIZEUSERNLS.JS</Name>"
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\FR_FR\\</Dir>"
				+ "<FileType>JSON</FileType>" + "<CodePage>UTF-8</CodePage>" + "<CodePageText>UTF-8</CodePageText>"
				+ "<Errors>0</Errors>" + "<Warnings>0</Warnings>" + "</File>"
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\DE_DE\\</Dir>" + "<File>"
				+ "<Name>ANONYMIZEUSERNLS.JS</Name>"
				+ "<Dir>MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\DE_DE\\</Dir>"
				+ "<FileType>JSON</FileType>" + "<CodePage>ANY</CodePage>"
				+ "<CodePageText>ANY (x'00'-x'7F')</CodePageText>" + "<Errors>0</Errors>" + "<Warnings>0</Warnings>"
				+ "</File>" + CONTENTS_END;
		StringBufferInputStream in = new StringBufferInputStream(contents);
		output.parse(in);
		assertNotNull("output is null.", output);
		assertEquals("Number of files is wrong.", 1, output.getFiles().size());
		PIIFile file = output.getFiles().get(0);
		checkPIIFile(file, "MESSAGES\\SOCCOM.BRIDGING.ANONYMIZE\\PUBLIC\\IBM\\SOCIAL\\NLS\\", "ANONYMIZEUSERNLS.JS",
				true);
		checkVariant(file, Locale.FRANCE, "JSON", "UTF-8", "UTF-8");
		checkVariant(file, Locale.GERMANY, "JSON", "ANY", "ANY (x'00'-x'7F')");
	}

	/**
	 * Tests that if null is given, an exception will be thrown.
	 */
	public void testFindSimilarIfFileNullExpectException() {
		PIIFile file = new PIIFile("dir1\\dir2\\dir3", "name1.properties", false);
		output.addFile(file);

		try {
			output.findSimilar(null);
			fail("NullPointerException was not thrown.");
		} catch (NullPointerException e) {
			// success
		}
	}

	/**
	 * Tests that if there is 1 file and the given file is an exact match for
	 * the output file, that file will be in the list.
	 */
	public void testFindSimilarIfHasExactMatchExpect1FileInList() {
		PIIFile file = new PIIFile("dir1\\dir2\\dir3", "name1.properties", false);
		output.addFile(file);

		List<PIIFile> similarFiles = output.findSimilar(new PIIFile("dir1\\dir2\\dir3", "name1.properties", false))
				.getResults();
		assertEquals("Number of similar files is wrong.", 1, similarFiles.size());
		assertEquals("File 0 is wrong.", file, similarFiles.get(0));
	}

	/**
	 * Tests that if there is 1 file and that file is all upper case and the
	 * given file is an exact match for the output file, that file will be in
	 * the list.
	 */
	public void testFindSimilarIfHasExactMatchButUpperCaseExpect1FileInList() {
		PIIFile file = new PIIFile("DIR1\\DIR2\\DIR3", "NAME1.PROPERTIES", false);
		output.addFile(file);

		List<PIIFile> similarFiles = output.findSimilar(new PIIFile("dir1\\dir2\\dir3", "name1.properties", false))
				.getResults();
		assertEquals("Number of similar files is wrong.", 1, similarFiles.size());
		assertEquals("File 0 is wrong.", file, similarFiles.get(0));
	}

	/**
	 * Tests that if there is 1 file and the given file has a longer path than
	 * the output file, that file will be in the list.
	 */
	public void testFindSimilarIfGivenFileLongerExpect1FileInList() {
		PIIFile file = new PIIFile("dir1\\dir2\\dir3", "name1.properties", false);
		output.addFile(file);

		List<PIIFile> similarFiles = output
				.findSimilar(new PIIFile("dir0\\dir1\\dir2\\dir3", "name1.properties", false)).getResults();
		assertEquals("Number of similar files is wrong.", 1, similarFiles.size());
		assertEquals("File 0 is wrong.", file, similarFiles.get(0));
	}

	/**
	 * Tests that if there is 1 file and the given file has a shorter path than
	 * the output file, the list will be empty.
	 */
	public void testFindSimilarIfGivenFileShorterExpect0FilesInList() {
		PIIFile file = new PIIFile("dir1\\dir2\\dir3", "name1.properties", false);
		output.addFile(file);

		List<PIIFile> similarFiles = output.findSimilar(new PIIFile("dir2\\dir3", "name1.properties", false))
				.getResults();
		assertEquals("Number of similar files is wrong.", 0, similarFiles.size());
	}

	/**
	 * Tests that if there is 1 file and the given file is a match but has the
	 * variant in the directory, the list will be empty.
	 */
	public void testFindSimilarIfGivenFileVariantInDirExpect0FilesInList() {
		PIIFile file = new PIIFile("dir1\\dir2\\dir3", "name1.properties", false);
		output.addFile(file);

		List<PIIFile> similarFiles = output.findSimilar(new PIIFile("dir2\\dir3", "name1.properties", true))
				.getResults();
		assertEquals("Number of similar files is wrong.", 0, similarFiles.size());
	}

	/**
	 * Tests that if there are 2 output files and they do not have a common
	 * parent and the given file is an exact match for 1 output file, that file
	 * will be in the list.
	 */
	public void testFindSimilarIf2OutputFiles0Common1HasExactMatchExpect1FileInList() {
		PIIFile file1 = new PIIFile("dir1\\dir2\\dir3", "name2.properties", false);
		output.addFile(file1);
		PIIFile file2 = new PIIFile("dir4\\dir5\\dir6", "name1.properties", false);
		output.addFile(file2);

		List<PIIFile> similarFiles = output.findSimilar(new PIIFile("dir4\\dir5\\dir6", "name1.properties", false))
				.getResults();
		assertEquals("Number of similar files is wrong.", 1, similarFiles.size());
		assertEquals("File 0 is wrong.", file2, similarFiles.get(0));
	}

	/**
	 * Tests that if there are 2 output files and they have a common parent and
	 * the given file is an exact match for 1 output file, that file will be in
	 * the list.
	 */
	public void testFindSimilarIf2OutputFiles1Common1HasExactMatchExpect1FileInList() {
		PIIFile file1 = new PIIFile("dir1\\dir2\\dir3", "name2.properties", false);
		output.addFile(file1);
		PIIFile file2 = new PIIFile("dir1\\dir5\\dir6", "name1.properties", false);
		output.addFile(file2);

		List<PIIFile> similarFiles = output.findSimilar(new PIIFile("dir1\\dir5\\dir6", "name1.properties", false))
				.getResults();
		assertEquals("Number of similar files is wrong.", 1, similarFiles.size());
		assertEquals("File 0 is wrong.", file2, similarFiles.get(0));
	}

	/**
	 * Tests that if there are 2 output files and they have all common parents
	 * and the given file is an exact match for 1 output file, that file will be
	 * in the list.
	 */
	public void testFindSimilarIf2OutputFilesAllCommon1HasExactMatchExpect1FileInList() {
		PIIFile file1 = new PIIFile("dir1\\dir2\\dir3", "name2.properties", false);
		output.addFile(file1);
		PIIFile file2 = new PIIFile("dir1\\dir2\\dir3", "name1.properties", false);
		output.addFile(file2);

		List<PIIFile> similarFiles = output.findSimilar(new PIIFile("dir1\\dir2\\dir3", "name1.properties", false))
				.getResults();
		assertEquals("Number of similar files is wrong.", 1, similarFiles.size());
		assertEquals("File 0 is wrong.", file2, similarFiles.get(0));
	}

	/**
	 * Tests that if the object is empty, some basic tags will be written.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testSaveIfEmptyExpectHeaders() throws Exception {
		output.parse("");
		StringWriter writer = new StringWriter();
		output.save(writer);

		StringBufferInputStream in = new StringBufferInputStream(writer.toString());

		assertEqualsContent("Content is not the same.", sep("<?xml version=\"1.0\" encoding=\"UTF-8\"?>#n",
				"<Report>#n", "<Files>#n", "</Files>#n", "</Report>#n"), in);
	}

	/**
	 * Tests that if the object has 1 file with 1 variant, the file will be
	 * written.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testSaveIf1File1VariantExpect1File() throws Exception {
		output.parse("");
		PIIFile file = new PIIFile("dir", "file.properties", false);
		file.addVariant(Locale.US);
		output.addFile(file);

		StringWriter writer = new StringWriter();
		output.save(writer);

		StringBufferInputStream in = new StringBufferInputStream(writer.toString());

		assertEqualsContent("Content is not the same.",
				sep("<?xml version=\"1.0\" encoding=\"UTF-8\"?>#n", "<Report>#n", "<Files>#n", "<File>#n", "<Name>",
						"file_EN_US.properties", "</Name>#n", "<Dir>", "dir", "</Dir>#n", "</File>#n", "</Files>#n",
						"</Report>#n"),
				in);
	}

	/**
	 * Tests that if the object has 1 file with 1 variant with an additional
	 * property, the file will be written.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testSaveIf1File1VariantWithPropertyExpect1File() throws Exception {
		output.parse("");
		PIIFile file = new PIIFile("dir", "file.properties", false);
		file.addVariant(Locale.US);
		file.setProperty(Locale.US, "CodePage", "UTF-8");
		output.addFile(file);

		StringWriter writer = new StringWriter();
		output.save(writer);

		StringBufferInputStream in = new StringBufferInputStream(writer.toString());

		assertEqualsContent("Content is not the same.",
				sep("<?xml version=\"1.0\" encoding=\"UTF-8\"?>#n", "<Report>#n", "<Files>#n", "<File>#n", "<Name>",
						"file_EN_US.properties", "</Name>#n", "<Dir>", "dir", "</Dir>#n", "<CodePage>", "UTF-8",
						"</CodePage>#n", "</File>#n", "</Files>#n", "</Report>#n"),
				in);
	}

	/**
	 * Tests that if the object has 1 file with 2 variants with an additional
	 * property, the file will be written.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testSaveIf1File2VariantWithPropertyExpect1File() throws Exception {
		output.parse("");
		PIIFile file1 = new PIIFile("dir", "file.properties", false);
		file1.addVariant(Locale.US);
		file1.setProperty(Locale.US, "CodePage", "UTF-8");
		file1.addVariant(Locale.GERMANY);
		file1.setProperty(Locale.GERMANY, "CodePageText", "UTF-8");
		output.addFile(file1);

		StringWriter writer = new StringWriter();
		output.save(writer);

		StringBufferInputStream in = new StringBufferInputStream(writer.toString());

		assertEqualsContent("Content is not the same.",
				sep("<?xml version=\"1.0\" encoding=\"UTF-8\"?>#n", "<Report>#n", "<Files>#n", "<File>#n", "<Name>",
						"file_EN_US.properties", "</Name>#n", "<Dir>", "dir", "</Dir>#n", "<CodePage>", "UTF-8",
						"</CodePage>#n", "</File>#n", "<File>#n", "<Name>", "file_DE_DE.properties", "</Name>#n",
						"<Dir>", "dir", "</Dir>#n", "<CodePageText>", "UTF-8", "</CodePageText>#n", "</File>#n",
						"</Files>#n", "</Report>#n"),
				in);
	}

	/**
	 * Tests that if the object has 2 files with 1 variant with an additional
	 * property, the file will be written.
	 *
	 * @throws Exception
	 *             If an unexpected error occurs.
	 */
	public void testSaveIf2File1VariantWithPropertyExpect1File() throws Exception {
		output.parse("");
		PIIFile file1 = new PIIFile("dir", "file.properties", false);
		file1.addVariant(Locale.US);
		file1.setProperty(Locale.US, "CodePage", "UTF-8");
		PIIFile file2 = new PIIFile("dir", "anotherfile.properties", false);
		file2.addVariant(Locale.GERMANY);
		file2.setProperty(Locale.GERMANY, "CodePageText", "UTF-8");
		output.addFile(file1);
		output.addFile(file2);

		StringWriter writer = new StringWriter();
		output.save(writer);

		StringBufferInputStream in = new StringBufferInputStream(writer.toString());

		assertEqualsContent("Content is not the same.",
				sep("<?xml version=\"1.0\" encoding=\"UTF-8\"?>#n", "<Report>#n", "<Files>#n", "<File>#n", "<Name>",
						"anotherfile_DE_DE.properties", "</Name>#n", "<Dir>", "dir", "</Dir>#n", "<CodePageText>",
						"UTF-8", "</CodePageText>#n", "</File>#n", "<File>#n", "<Name>", "file_EN_US.properties",
						"</Name>#n", "<Dir>", "dir", "</Dir>#n", "<CodePage>", "UTF-8", "</CodePage>#n", "</File>#n",
						"</Files>#n", "</Report>#n"),
				in);
	}

	private void checkVariant(PIIFile file, Locale locale, String expectedFileType, String expectedCodePage,
			String expectedCodeText) {
		assertEquals("file French FileType is wrong.", expectedFileType, file.getProperty(locale, "FileType"));
		assertEquals("file French CodePage is wrong.", expectedCodePage, file.getProperty(locale, "CodePage"));
		assertEquals("file French CodePageText is wrong.", expectedCodeText, file.getProperty(locale, "CodePageText"));
	}

	private void checkPIIFile(PIIFile file, String expectedDir, String expectedBaseName, boolean expectedVariantInDir) {
		assertEquals("file dir is wrong.", expectedDir, file.getDir());
		assertEquals("file baseName is wrong.", expectedBaseName, file.getBaseName());
		assertEquals("variantInParent is wrong.", expectedVariantInDir, file.isVariantInParentDir());
	}
}
