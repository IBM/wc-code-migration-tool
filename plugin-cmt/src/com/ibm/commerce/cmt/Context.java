package com.ibm.commerce.cmt;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.commerce.cmt.plan.IDGenerator;
import com.ibm.commerce.cmt.plan.Issue;

/**
 * This class contains information to identify the source text to replace.
 * 
 * @author Trent Hoeppner
 */
public class Context {

	/**
	 * An enumeration of the property keys that can be included in a context.
	 */
	public static enum Prop implements ContextKey {

		/**
		 * The write buffer for a file in which strings are being replaced as
		 * part of a replace action.
		 */
		FILE_WRITE_BUFFER {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		},

		/**
		 * The Eclipse CompilationUnit class that represents the parsed contents
		 * of a Java file.
		 */
		COMP_UNIT {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		},

		/**
		 * The JavaItemUtil object that is used to perform some functions.
		 */
		JAVA_ITEM_UTIL {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		},

		FILE_CONTENTS(COMP_UNIT, FILE_WRITE_BUFFER) {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) throws Exception {
				Object o = null;
				if (dependency == COMP_UNIT) {
					FileContents contents = context.get(Context.Prop.FILE_CONTENTS);
					CompilationUnit compUnit = loadJavaFile(contents);
					o = compUnit;
				} else if (dependency == FILE_WRITE_BUFFER) {
					FileContents contents = context.get(Context.Prop.FILE_CONTENTS);
					StringBuilder b = new StringBuilder();
					b.append(contents.getContents());
					o = b;
				} else {
					throw new IllegalArgumentException("Cannot generate " + dependency + " from " + this);
				}

				return o;
			}

			private CompilationUnit loadJavaFile(FileContents fileContents) throws Exception {
				ASTParser parser = ASTParser.newParser(AST.JLS8);
				String stringForm = fileContents.getContents();
				parser.setSource(stringForm.toCharArray());
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				CompilationUnit resourceCompUnit = (CompilationUnit) parser.createAST(null);
				return resourceCompUnit;
			}
		},

		FILE(FILE_CONTENTS) {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) throws IOException {
				Object o = null;
				if (dependency == FILE_CONTENTS) {
					File file = context.get(Context.Prop.FILE);
					FileContents contents = new FileContents(file);
					contents.load();
					o = contents;
				} else {
					throw new IllegalArgumentException("Cannot generate " + dependency + " from " + this);
				}

				return o;
			}
		},

		SEARCH_RESULT {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		},

		RANGE {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		},

		ORIGINAL_SOURCE {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		},

		FORMATTED_RANGE {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		},

		ALL_GROUPS {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		},

		ALL_MATCHERS(ALL_GROUPS) {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				Object o = null;
				if (dependency == ALL_GROUPS) {
					List<String> groups = new ArrayList<>();

					List<Matcher> matchers = context.get(ALL_MATCHERS);
					for (Matcher matcher : matchers) {
						for (int i = 0; i < matcher.groupCount() + 1; i++) {
							groups.add(matcher.group(i));
						}
					}

					o = groups;
				} else {
					throw new IllegalArgumentException("Cannot generate " + dependency + " from " + this);
				}

				return o;
			}
		},

		ISSUE(RANGE, ORIGINAL_SOURCE, FORMATTED_RANGE) {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) throws IOException {
				Object o = null;
				if (dependency == RANGE) {
					Issue issue = context.get(Context.Prop.ISSUE);
					o = issue.getLocation().getRange();
				} else if (dependency == ORIGINAL_SOURCE) {
					Issue issue = context.get(Context.Prop.ISSUE);
					o = issue.getSource();
				} else if (dependency == FORMATTED_RANGE) {
					Issue issue = context.get(Context.Prop.ISSUE);
					o = issue.getLocation().getFormattedRange();
					// } else if (dependency == ALL_MATCHERS) {
					// // just create an empty list. making it a dependency on
					// // issue means that when the issue is changed, the list
					// of
					// // matchers will be reset. RegexSearchParam class
					// // automatically adds matchers to the list.
					// return new ArrayList<>();
				} else {
					throw new IllegalArgumentException("Cannot generate " + dependency + " from " + this);
				}

				return o;
			}

		},

		LOG_WRITER {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		},

		JAVA_ITEM_INDEX {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		},

		DEPENDENCY_WORKSPACE {
			@Override
			public Object createValueForDependency(Context context, ContextKey dependency) {
				return null;
			}
		};

		private Set<ContextKey> canGenerate;

		private Prop(Prop... canGenerate) {
			Set<ContextKey> s = new HashSet<>();
			s.addAll(Arrays.asList(canGenerate));
			this.canGenerate = s;
		}

		@Override
		public Set<ContextKey> getCanGenerate() {
			return canGenerate;
		}

	}

	private IDGenerator issueIDGenerator;

	private Map<ContextKey, Object> properties = new HashMap<>();

	/**
	 * Constructor for this.
	 * 
	 * @param issueIDGenerator
	 *            TODO
	 */
	public Context(IDGenerator issueIDGenerator) {
		Check.notNull(issueIDGenerator, "issueIDGenerator");

		this.issueIDGenerator = issueIDGenerator;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(ContextKey key) {
		Check.notNull(key, "key");

		Object o = properties.get(key);
		if (o == null) {
			// find all properties that can generate this key
			List<Prop> derivedFrom = new ArrayList<>();
			for (Prop prop : Prop.values()) {
				if (prop.getCanGenerate().contains(key)) {
					derivedFrom.add(prop);
				}
			}

			if (!derivedFrom.isEmpty()) {
				// try to derive it from an existing value
				for (ContextKey source : derivedFrom) {
					Object sourceValue = get(source);
					if (sourceValue == null) {
						continue;
					}

					try {
						o = source.createValueForDependency(this, key);
						if (o != null) {
							properties.put(key, o);
							break;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		return (T) o;
	}

	public void set(ContextKey key, Object value) {
		if (isDefined(key)) {
			resetDependencies(key);
		}
		properties.put(key, value);
	}

	private void resetDependencies(ContextKey key) {
		for (ContextKey dependency : key.getCanGenerate()) {
			resetDependencies(dependency);
			properties.remove(dependency);
		}
	}

	public boolean isDefined(ContextKey key) {
		return properties.containsKey(key);
	}

	public void reset() {
		properties.clear();
	}

	public IDGenerator getIssueIDGenerator() {
		return issueIDGenerator;
	}

}
