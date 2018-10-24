package com.ibm.commerce.cmt.plan;

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Context;

/**
 * This class is an execution step that renames a file.
 * 
 * @author Trent Hoeppner
 */
public class RenameFileStep implements Step {

	private File file;

	private File toFile;

	public RenameFileStep(File file, File toFile) {
		this.file = file;
		this.toFile = toFile;
	}

	@Override
	public void execute(Context context) throws Exception {
		BufferedWriter writer = context.get(Context.Prop.LOG_WRITER);
		try {
			Files.move(file.toPath(), toFile.toPath());
		} catch (FileAlreadyExistsException e) {
			writer.append("Could not move " + file.getAbsolutePath() + " to '" + toFile.getAbsolutePath()
					+ "' because the target already exists.");
		} catch (IOException e) {
			writer.append("Could not move " + file.getAbsolutePath() + " to '" + toFile.getAbsolutePath()
					+ "' because '" + e.getMessage() + "'");
		}
	}

	@Override
	public Node toXML(Document doc) {
		Element renameFile = doc.createElement("renamefile");

		Element fileNode = doc.createElement("file");
		fileNode.setTextContent(file.getAbsolutePath());
		renameFile.appendChild(fileNode);

		Element toFileNode = doc.createElement("tofile");
		toFileNode.setTextContent(toFile.getAbsolutePath());
		renameFile.appendChild(toFileNode);

		return renameFile;
	}

	public File getFile() {
		return file;
	}

	public File getToFile() {
		return toFile;
	}

}
