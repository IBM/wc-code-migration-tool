package com.ibm.commerce.dependency.load;

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.ibm.commerce.cmt.plan.IDGenerator;
import com.ibm.commerce.dependency.model.BaseJavaItem;
import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemIndex;
import com.ibm.commerce.dependency.model.JavaItemType;
import com.ibm.commerce.dependency.model.Relationship;
import com.ibm.commerce.dependency.model.RelationshipType;

/**
 * This class loads/writes an API from a ZIP into a JavaIndex.
 * 
 * @author Trent Hoeppner
 */
public class APIFileManager {

	private static final Pattern API_ZIPNAME_PATTERN = Pattern.compile("api-v([\\d]+)\\.zip");

	private static final Pattern ITEM_PATTERN = Pattern.compile("i n \"([^\"]*)\" id (\\d+) t (\\w+)");

	private static final Pattern RELATIONSHIP_PATTERN = Pattern.compile("r s (\\d+) t (\\d+) r (\\w+)");

	private static byte[] lineSeparatorBytes;

	static {
		try {
			lineSeparatorBytes = ("" + System.getProperty("line.separator")).getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			// of course UTF8 is supported
		}
	}

	/**
	 * Constructor for this class.
	 */
	public void APILoader() {
		// do nothing
	}

	/**
	 * Loads the APIs stored in the given ZIP file into a JavaItemIndex, and
	 * returns it.
	 * <p>
	 * The name of the file contains the version information, and must be of the
	 * form:
	 * <p>
	 * <code>api-vX.zip</code>
	 * <p>
	 * where X is the version number. If it does not match an exception will be
	 * thrown.
	 * 
	 * @param zipFilename
	 *            The ZIP file to load. This value cannot be null or empty.
	 * 
	 * @return The index that contains the JavaItems. This value will not be
	 *         null.
	 * 
	 * @throws IOException
	 *             If an error occurs while reading the ZIP file.
	 */
	public JavaItemIndex loadAPI(File zipFile) throws IOException {
		Matcher matcher = API_ZIPNAME_PATTERN.matcher(zipFile.getName());
		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"zipFile's name must be of the form api-vX.zip, was " + zipFile.getName());
		}

		String version = matcher.group(1);

		JavaItemIndex index = new JavaItemIndex(version);

		readProjects(zipFile, index);

		IDGenerator idGenerator = new IDGenerator(index.getItems().size());
		index.setIDGenerator(idGenerator);

		return index;
	}

	/**
	 * Writes the data in the given index into a ZIP file named below. The
	 * JavaItems in the index are split into groups of 100,000, and each item in
	 * each group is written on a line. The relationships between JavaItems are
	 * similarly split and recorded.
	 * 
	 * @param index
	 *            The index to write. This value cannot be null.
	 * @param serializedFile
	 *            The zIP filename to write to. This value cannot be null.
	 * 
	 * @throws IOException
	 *             If there was an error writing to the file.
	 */
	public void writeAPI(JavaItemIndex index, File zipFile) throws IOException {
		ZipOutputStream zipOut = null;
		try {
			zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
			int itemBatchSize = 100000;
			int lastEndIndex = 0;
			List<JavaItem> allItems = index.getItems();
			while (lastEndIndex < allItems.size()) {
				int currentEndIndex = Math.min(lastEndIndex + itemBatchSize, allItems.size());
				List<JavaItem> batch = allItems.subList(lastEndIndex, currentEndIndex);
				batch = new ArrayList<>(batch);

				// verify item indexes
				int verifyIndex = lastEndIndex;
				for (JavaItem item : batch) {
					if (item.getID() != verifyIndex) {
						throw new IllegalStateException("item " + item + " is out of order. Expected id " + verifyIndex
								+ ", but was " + item.getID());
					}

					verifyIndex++;
				}

				ZipEntry entry = new ZipEntry("items" + lastEndIndex + "-" + currentEndIndex + ".txt");
				zipOut.putNextEntry(entry);
				writePart(batch, zipOut);
				zipOut.closeEntry();

				lastEndIndex = currentEndIndex;
			}

			int relBatchSize = 100000;
			int total = 0;
			boolean lastBatch = false;
			List<Relationship> batch = new ArrayList<>();
			for (int i = 0; i < allItems.size(); i++) {
				JavaItem item = allItems.get(i);
				List<Relationship> relationships = convertToRelationships(item);
				batch.addAll(relationships);

				if (i == allItems.size() - 1) {
					lastBatch = true;
				}

				if (batch.size() > relBatchSize || lastBatch) {
					int nextTotal = total + batch.size();

					ZipEntry entry = new ZipEntry("rels" + total + "-" + nextTotal + ".txt");
					zipOut.putNextEntry(entry);
					writePart(batch, zipOut);
					zipOut.closeEntry();

					total = nextTotal;
					batch.clear();
				}
			}
			zipOut.closeEntry();
		} finally {
			if (zipOut != null) {
				try {
					zipOut.close();
				} catch (IOException e) {
					// swallow to allow main exception to escape
				}
			}
		}
	}

	/**
	 * Writes the given objects to the given output stream, one object per line.
	 * 
	 * @param objects
	 *            The objects to write. Must contain {@link JavaItem JavaItems}
	 *            or {@link Relationship Relationships}. This value cannot be
	 *            null, but may be empty.
	 * @param out
	 *            The output stream to write to. This value cannot be null.
	 * 
	 * @throws IOException
	 *             If there was an error writing to the output stream.
	 */
	private void writePart(List<?> objects, OutputStream out) throws IOException {
		for (Object o : objects) {
			String line = toLine(o);
			out.write(line.getBytes("UTF8"));
			out.write(lineSeparatorBytes);
		}

		out.flush();
	}

	/**
	 * Converts the given object to a line for writing to an output stream.
	 * 
	 * @param object
	 *            The {@link JavaItem} or {@link Relationship} to convert. This
	 *            value cannot be null.
	 * 
	 * @return The string which contains the essential information about the
	 *         object. This value will not be null or empty.
	 */
	private String toLine(Object object) {
		StringBuilder buf = new StringBuilder();
		if (object instanceof JavaItem) {
			JavaItem item = (JavaItem) object;
			buf.append("i n \"");
			buf.append(item.getName());
			buf.append("\" id ");
			buf.append(item.getID());
			buf.append(" t ");
			buf.append(item.getType());
		} else if (object instanceof Relationship) {
			Relationship rel = (Relationship) object;
			buf.append("r s ");
			buf.append(rel.getSourceID());
			buf.append(" t ");
			buf.append(rel.getTargetID());
			buf.append(" r ");
			buf.append(rel.getType().toString());
		}

		return buf.toString();
	}

	/**
	 * Converts the given line to an object.
	 * 
	 * @param index
	 *            The index that will contain all the loaded items. This value
	 *            cannot be null.
	 * @param line
	 *            A line that was previously produced by
	 *            {@link #fromLine(String)}. This value cannot be null or empty.
	 * 
	 * @return The object, either a {@link JavaItem} or a {@link Relationship}.
	 *         This value will not be null.
	 */
	@SuppressWarnings("unchecked")
	private <T> T fromLine(JavaItemIndex index, String line) {

		Object o = null;
		if (line.startsWith("i")) {
			Matcher matcher = ITEM_PATTERN.matcher(line);
			if (!matcher.matches()) {
				throw new IllegalStateException("Could not parse line for JavaItem: " + line);
			}

			String name = matcher.group(1);
			int id = Integer.parseInt(matcher.group(2));
			JavaItemType type = JavaItemType.valueOf(matcher.group(3));

			JavaItem item = new BaseJavaItem(name, index);
			item.setID(id);
			item.setType(type);

			o = item;
		} else if (line.startsWith("r")) {
			Matcher matcher = RELATIONSHIP_PATTERN.matcher(line);
			if (!matcher.matches()) {
				throw new IllegalStateException("Could not parse line for Relationship: " + line);
			}

			int sourceID = Integer.parseInt(matcher.group(1));
			int targetID = Integer.parseInt(matcher.group(2));
			RelationshipType type = RelationshipType.valueOf(matcher.group(3));

			Relationship rel = new Relationship();
			rel.setSourceID(sourceID);
			rel.setTargetID(targetID);
			rel.setType(type);

			o = rel;
		} else {
			throw new IllegalStateException("Found line with no recognized object type: " + line);
		}

		return (T) o;
	}

	/**
	 * Reads the JavaItems and Relationships stored in the given file, links
	 * them together and stores them in a new index.
	 * 
	 * @param serializedFile
	 *            The file to read from, previously written with
	 *            {@link #writeProjects(JavaItemIndex, File)}. This value cannot
	 *            be null.
	 * @param index
	 *            The index that will contain all the loaded items. This value
	 *            cannot be null.
	 * 
	 * @return The index that contains all the loaded items. This value will not
	 *         be null.
	 * 
	 * @throws IOException
	 *             If an error occurs reading from the file.
	 */
	@SuppressWarnings("unchecked")
	private void readProjects(File serializedFile, JavaItemIndex index) throws IOException {
		ZipInputStream zipIn = null;
		try {
			zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(serializedFile)));
			while (true) {
				ZipEntry entry = zipIn.getNextEntry();
				if (entry == null) {
					break;
				}

				List<?> list = readPart(index, zipIn);
				zipIn.closeEntry();
				if (!list.isEmpty()) {
					Object firstElement = list.get(0);
					if (firstElement instanceof JavaItem) {
						// this is for the JavaItemIndex
						long startTime = System.currentTimeMillis();
						List<JavaItem> items = (List<JavaItem>) list;
						for (JavaItem item : items) {
							// if (item.getName().equals("*")) {
							// item = JavaItemUtil.getWildcardType();
							// // don't add it, since it was initialized with
							// // the index already
							// } else {
							// JavaItem primitiveType =
							// JavaItemUtil.getPrimitiveType(item.getName());
							// if (primitiveType != null && item.getID() ==
							// primitiveType.getID()) {
							// item = primitiveType;
							// // don't add it to the index, since it was
							// // initialized with the index already
							// } else {
							item.setAttribute(JavaItem.ATTR_BINARY, true);
							index.addItemPreserveID(item);
							// }
							// }
						}

						System.out.println("  add to index (" + (System.currentTimeMillis() - startTime) + " ms)");
					} else if (firstElement instanceof Relationship) {
						// this is to modify the JavaItems already found
						List<Relationship> relationships = (List<Relationship>) list;
						convertToJavaItemIndex(index, relationships);
					}
				}

			}

		} catch (ArrayIndexOutOfBoundsException e) {
			// done loading
		} finally {
			if (zipIn != null) {
				try {
					zipIn.close();
				} catch (IOException e) {
					// swallow to allow the main exception to escape
				}
			}
		}
	}

	/**
	 * Reads a group of objects from the given input stream. This is called as
	 * part of {@link #readProjects(File)}.
	 * 
	 * @param index
	 *            The index that will contain all the loaded items. This value
	 *            cannot be null.
	 * @param in
	 *            The input stream to read from, which must contain lines
	 *            previously converted from objects using
	 *            {@link #toLine(Object)}.
	 * 
	 * @return The list of objects that were read. This value will not be null,
	 *         but may be empty.
	 * 
	 * @throws IOException
	 *             If an error occurs while reading from the stream.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<?> readPart(JavaItemIndex index, InputStream in) throws IOException {
		long startTime = System.currentTimeMillis();

		ByteArrayOutputStream bufOut = new ByteArrayOutputStream();
		try {
			byte[] byteBuffer = new byte[4096];
			int length = in.read(byteBuffer);
			while (length >= 0) {
				bufOut.write(byteBuffer, 0, length);
				length = in.read(byteBuffer);
			}
		} finally {
			bufOut.close();
		}
		long readingTime = System.currentTimeMillis();

		BufferedReader bufIn = null;
		List list = Collections.emptyList();
		try {
			bufIn = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bufOut.toByteArray()), "UTF8"));
			list = new ArrayList<>();
			String line = bufIn.readLine();
			while (line != null) {
				Object o = fromLine(index, line);
				list.add(o);

				line = bufIn.readLine();
			}
		} finally {
			if (bufIn != null) {
				try {
					bufIn.close();
				} catch (IOException e) {
					// swallow to allow exception to escape
				}
			}
		}

		long parsingTime = System.currentTimeMillis();

		System.out.println("readXMLObject unzip (" + (readingTime - startTime) + " ms), parse ("
				+ (parsingTime - readingTime) + " ms)");

		return list;
	}

	/**
	 * Extracts the relationship objects from the given item. Child and
	 * dependency relationships are recorded for all item types (but not parent
	 * or incoming since those are duplicates). Additionally, classes have the
	 * array base class, superclass and super-interfaces recorded. Methods have
	 * the return type and parameter types recorded. Fields have the field type
	 * recorded.
	 * 
	 * @param item
	 *            The item to get the relationships from. This value cannot be
	 *            null.
	 * 
	 * @return The relationships that the item has with other items. This value
	 *         will not be null but may be empty.
	 */
	private List<Relationship> convertToRelationships(JavaItem item) {
		List<Relationship> relationships = new ArrayList<>();

		Relationship r;
		for (Integer childID : item.getChildrenIDs()) {
			r = RelationshipType.CHILD.create(item, childID);
			relationships.add(r);
		}

		for (Integer dependencyID : item.getDependenciesIDs()) {
			r = RelationshipType.DEPENDENCY.create(item, dependencyID);
			relationships.add(r);
		}

		if (item.getType() == JavaItemType.CLASS) {
			Integer arrayBaseClassID = item.getAttribute(JavaItem.ATTR_ARRAY_BASE_CLASS);
			if (arrayBaseClassID != null) {
				r = RelationshipType.ARRAY_BASE_CLASS.create(item, arrayBaseClassID);
				relationships.add(r);
			}

			Integer superClassID = item.getAttribute(JavaItem.ATTR_SUPERCLASS);
			if (superClassID != null) {
				r = RelationshipType.SUPERCLASS.create(item, superClassID);
				relationships.add(r);
			}

			Set<Integer> superInterfaceIDs = item.getAttribute(JavaItem.ATTR_SUPERINTERFACES);
			if (superInterfaceIDs != null) {
				for (Integer superInterfaceID : superInterfaceIDs) {
					r = RelationshipType.SUPERINTERFACE.create(item, superInterfaceID);
					relationships.add(r);
				}
			}
		}

		if (item.getType() == JavaItemType.METHOD) {
			Integer returnTypeID = item.getAttribute(JavaItem.ATTR_RETURN_TYPE);
			if (returnTypeID != null) {
				r = RelationshipType.METHOD_RETURN_TYPE.create(item, returnTypeID);
				relationships.add(r);
			}

			List<Integer> paramTypeIDs = item.getAttribute(JavaItem.ATTR_METHOD_PARAM_TYPES);
			if (paramTypeIDs != null) {
				for (Integer paramTypeID : paramTypeIDs) {
					r = RelationshipType.METHOD_PARAMETER_TYPE.create(item, paramTypeID);
					relationships.add(r);
				}
			}
		}

		if (item.getType() == JavaItemType.FIELD) {
			Integer typeID = item.getAttribute(JavaItem.ATTR_FIELD_TYPE);
			if (typeID != null) {
				r = RelationshipType.FIELD_TYPE.create(item, typeID);
				relationships.add(r);
			}
		}

		return relationships;
	}

	/**
	 * Finds the items in each given relationship and creates the proper
	 * association between them.
	 * 
	 * @param index
	 *            The index which contains the items that should have
	 *            relationships with each other. This value cannot be null.
	 * @param relationships
	 *            The objects which define the relationships between items. This
	 *            value cannot be null, but may be empty.
	 */
	private void convertToJavaItemIndex(JavaItemIndex index, List<Relationship> relationships) {
		for (Relationship relationship : relationships) {
			if (relationship.getSourceID() == 67630) {
				System.out.println("found AbstractEntityAccessBean");
			}
			relationship.getType().setRelationshipOnObjects(index, relationship);
		}
	}
}
