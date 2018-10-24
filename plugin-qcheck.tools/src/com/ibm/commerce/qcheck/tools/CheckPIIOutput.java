package com.ibm.commerce.qcheck.tools;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.commerce.qcheck.core.Param;

/**
 * This class represents the output of the CHKPII program. It is designed to
 * parse the XML format.
 * 
 * @author Trent Hoeppner
 */
public class CheckPIIOutput {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private List<PIIFile> files;

	private Map<String, Set<Path>> childToPossibleParentsMap = new HashMap<String, Set<Path>>();

	private Map<Path, Set<Path>> parentToChildrenMap = new HashMap<Path, Set<Path>>();

	/**
	 * Constructor for this.
	 */
	public CheckPIIOutput() {
		files = new ArrayList<PIIFile>();
	}

	/**
	 * Parses the given contents to generate an output class that represents the
	 * important parts of the CHKPII output.
	 *
	 * @param contents
	 *            The contents to parse. This value cannot be null, but may be
	 *            empty.
	 *
	 * @return The output represented as an object. This value will not be null.
	 */
	public void parse(String contents) {
		Param.notNull(contents, "contents");

		parse(new StringBufferInputStream(contents));
	}

	/**
	 * Parses the contents of the given stream to generate an output class that
	 * represents the important parts of the CHKPII output.
	 * <p>
	 * This method does not close the given stream when finished.
	 *
	 * @param in
	 *            The stream with the contents to parse. This value cannot be
	 *            null.
	 *
	 * @return The output represented as an object. This value will not be null.
	 */
	public void parse(InputStream in) {
		Handler handler = new Handler();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			saxParser.parse(in, handler);
			Collections.sort(files);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the contents of this to the given writer. The original input(s)
	 * will not be preserved, some of the data is removed, such as header
	 * information and summary information.
	 * <p>
	 * This method does not close the given writer when finished.
	 *
	 * @param writer
	 *            The object to write to. This value cannot be null.
	 *
	 * @throws IOException
	 *             If an error occurs while writing.
	 */
	public void save(Writer writer) throws IOException {
		StringBuffer buf = new StringBuffer();
		write(buf, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		write(buf, "<Report>");
		write(buf, "<Files>");

		for (PIIFile file : files) {
			for (Locale locale : file.getVariants()) {
				write(buf, "<File>");
				write(buf, "<Name>", file.getName(locale), "</Name>");
				write(buf, "<Dir>", file.getDir(locale), "</Dir>");
				for (String propertyName : file.getProperties(locale)) {
					write(buf, "<", propertyName, ">", file.getProperty(locale, propertyName), "</", propertyName, ">");
				}
				write(buf, "</File>");
			}
		}

		write(buf, "</Files>");
		write(buf, "</Report>");
		writer.write(buf.toString());
	}

	public void write(StringBuffer buf, String... message) {
		for (String m : message) {
			buf.append(m);
		}
		buf.append(LINE_SEPARATOR);
	}

	/**
	 * Returns the files that have been {@link #parse(String) parsed}.
	 *
	 * @return The files that were found in the output from CHKPII. This value
	 *         will not be null, but may be empty.
	 */
	public List<PIIFile> getFiles() {
		return Collections.unmodifiableList(files);
	}

	public PIIFile addFile(PIIFile file) {
		int existingIndex = Collections.binarySearch(files, file);
		if (existingIndex >= 0) {
			file = files.get(existingIndex);
		} else {
			int insertIndex = -(existingIndex + 1);
			files.add(insertIndex, file);

			Path path = createPath(file);
			while (path != null) {
				Set<Path> possibleParents = childToPossibleParentsMap.get(path.getName());
				if (possibleParents == null) {
					possibleParents = new HashSet<Path>();
					childToPossibleParentsMap.put(path.getName(), possibleParents);
				}

				possibleParents.add(path.getParent());

				Set<Path> children = parentToChildrenMap.get(path.getParent());
				if (children == null) {
					children = new HashSet<Path>();
					parentToChildrenMap.put(path.getParent(), children);
				}

				children.add(path);

				path = path.getParent();
			}
		}

		return file;
	}

	private class Handler extends DefaultHandler {

		private Pattern namePattern = Pattern.compile("([^_]+)_([A-Z]{2})(_([A-Z]{2}))?\\.(.+)");

		private Pattern dirPattern = Pattern.compile("(.+)\\\\([A-Z]{2})(_([A-Z]{2}))?\\\\");

		private boolean inFile = false;

		/**
		 * The buffer for the current element's contents. This is necessary
		 * because the {@link #characters(char[], int, int)} method can be
		 * called multiple times for a single element's contents.
		 */
		private StringBuffer currentContents = new StringBuffer();

		private Map<String, String> currentProperties = new LinkedHashMap<String, String>();

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			if (qName.equals("File")) {
				inFile = true;
				currentProperties.clear();
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equals("File")) {
				inFile = false;
				String dir = currentProperties.get("Dir");
				String baseName = currentProperties.get("Name");

				if (baseName == null) {
					System.out.println("Name is null for Dir " + dir + ", skipping");
					return;
				}

				if (dir == null) {
					System.out.println("Dir is null for baseName " + baseName + ", skipping");
					return;
				}

				boolean variantInParent;
				Matcher baseNameMatcher = namePattern.matcher(baseName);
				String language = null;
				String country = null;
				if (baseNameMatcher.matches()) {
					language = baseNameMatcher.group(2).toLowerCase();
					country = baseNameMatcher.group(4);
					baseName = baseNameMatcher.group(1) + "." + baseNameMatcher.group(5);
					variantInParent = false;
				} else {
					Matcher dirMatcher = dirPattern.matcher(dir);
					if (dirMatcher.matches()) {
						language = dirMatcher.group(2).toLowerCase();
						country = dirMatcher.group(4);
						dir = dirMatcher.group(1) + "\\";
					}
					variantInParent = true;
				}

				if (language != null) {
					PIIFile file = new PIIFile(dir, baseName, variantInParent);
					file = addFile(file);

					Locale locale;
					if (country == null) {
						locale = new Locale(language);
					} else {
						locale = new Locale(language, country);
					}
					file.addVariant(locale);

					for (String key : currentProperties.keySet()) {
						if (!key.equals("Dir") && !key.equals("Name")) {
							String value = currentProperties.get(key);
							file.setProperty(locale, key, value);
						}
					}
				}
			} else if (currentContents != null) {
				currentProperties.put(qName, currentContents.toString().trim());
				currentContents.setLength(0);
			}
		}

		@Override
		public void characters(char[] charData, int startIndex, int length) throws SAXException {
			if (inFile) {
				String subString = String.valueOf(charData, startIndex, length);
				currentContents.append(subString);
			} else {
				currentContents.setLength(0);
			}
		}

	}

	/**
	 * Searches this for files that are similar to the given file. First, an
	 * exact match is sought, so that the target file should have the same path
	 * as the given file. If no file is found (perhaps because the given file is
	 * new and was not previously translated), an exact match with the parent
	 * directory is sought. If a match is found at this level, all children are
	 * considered similar. If no match is found, a match with the next parent
	 * directory is sought, and so on until the top directory is reached.
	 * <p>
	 * The resulting list can be used to make suggestions for the encoding of
	 * the given file.
	 * <p>
	 * The search is done by using the US English locale to substitute as
	 * appropriate to find a match.
	 *
	 * @param file
	 *            The file to find a match for. This value cannot be null.
	 *
	 * @return The result object with the files most similar to the given file.
	 *         This value will not be null.
	 */
	public PIIFileResults findSimilar(PIIFile file) {
		Param.notNull(file, "file");

		Path inputPath = createPath(file);
		Set<Path> possibleParents = childToPossibleParentsMap.get(inputPath.getName());
		if (possibleParents == null) {
			possibleParents = new HashSet<Path>();
		}
		Object[] returnValues = removeInvalidParents(inputPath, possibleParents);
		Set<Path> remainingPaths = (Set<Path>) returnValues[0];
		boolean exactMatch = (Boolean) returnValues[1];

		List<PIIFile> foundFiles = new ArrayList<PIIFile>();
		for (Path path : remainingPaths) {
			if (path.isLeaf()) {
				foundFiles.add(path.getFile());
			} else {
				Set<PIIFile> childFiles = path.getChildFiles();
				foundFiles.addAll(childFiles);
			}
		}

		PIIFileResults results = new PIIFileResults(foundFiles, exactMatch);

		return results;
	}

	private Object[] removeInvalidParents(Path inputPath, Set<Path> possibleParents) {
		Set<Path> remainingPaths = new HashSet<Path>();
		for (Path possibleParent : possibleParents) {
			Path candidateParent = possibleParent;
			Path requiredParent = inputPath.getParent();
			boolean found = false;
			while (requiredParent != null && candidateParent != null) {
				if (!candidateParent.getName().equals(requiredParent.getName())) {
					// not a candidate anymore
					break;
				} else if (candidateParent.getParent() == null) {
					// the parents match, and this is the last part of the
					// candidate path
					found = true;
					break;
				}
				requiredParent = requiredParent.getParent();
				candidateParent = candidateParent.getParent();
			}

			if (found) {
				remainingPaths.add(possibleParent);
			}
		}

		// if we only have one parent path, check if any child of that path is
		// an exact match
		boolean exact = false;
		if (remainingPaths.size() == 1) {
			Path remaining = remainingPaths.iterator().next();
			Set<Path> children = parentToChildrenMap.get(remaining);
			for (Path child : children) {
				if (child.getName().equals(inputPath.getName())) {
					// exact match, we can forget about the others in the parent
					remainingPaths.clear();
					remainingPaths.add(child);
					exact = true;
					break;
				}
			}
		}

		Object[] returnValues = new Object[] { remainingPaths, exact };

		return returnValues;
	}

	private Path createPath(PIIFile file) {
		String textPath = file.getDir(Locale.US).toUpperCase() + "\\" + file.getName(Locale.US).toUpperCase();
		StringTokenizer tokenizer = new StringTokenizer(textPath, "\\/");
		Path current = null;
		while (tokenizer.hasMoreTokens()) {
			String pathToken = tokenizer.nextToken();
			current = new Path(pathToken, current);
		}

		current.file = file;

		return current;
	}

	private class Path {

		private String name;

		private Path parent;

		// if null, this is not a leaf
		private PIIFile file;

		public Path(String name, Path parent) {
			this.name = name;
			this.parent = parent;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Path other = (Path) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (parent == null) {
				if (other.parent != null)
					return false;
			} else if (!parent.equals(other.parent))
				return false;
			return true;
		}

		public boolean isLeaf() {
			return file != null;
		}

		public Set<PIIFile> getChildFiles() {
			Set<PIIFile> children = new HashSet<PIIFile>();
			if (!isLeaf()) {
				Set<Path> directChildren = parentToChildrenMap.get(this);
				for (Path directChild : directChildren) {
					if (directChild.isLeaf()) {
						children.add(directChild.file);
					} else {
						children.addAll(directChild.getChildFiles());
					}
				}
			}

			return children;
		}

		public String getName() {
			return name;
		}

		public Path getParent() {
			return parent;
		}

		public PIIFile getFile() {
			return file;
		}

		@Override
		public String toString() {
			String string = "";
			if (parent != null) {
				string = parent.toString() + "\\";
			}

			string += name;

			return string;
		}
	}
}
