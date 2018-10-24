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
import java.nio.file.Files;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commerce.cmt.Context;

/**
 * This class is an execution step that deletes a file.
 * 
 * @author Trent Hoeppner
 */
public class DeleteFileStep implements Step {

	private File file;

	public DeleteFileStep(File file) {
		this.file = file;
	}

	@Override
	public void execute(Context context) throws Exception {
		try {
			Files.deleteIfExists(file.toPath());
		} catch (IOException e) {
			BufferedWriter writer = context.get(Context.Prop.LOG_WRITER);
			writer.append("Could not delete " + file.getAbsolutePath() + " because '" + e.getMessage()
					+ "', trying again in a second...");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
				// it's ok, just continue
			}

			try {
				Files.deleteIfExists(file.toPath());
			} catch (IOException e2) {
				writer.append("2nd time could not delete " + file.getAbsolutePath() + " because '" + e2.getMessage()
						+ "', giving up");
			}
		}
	}

	@Override
	public Node toXML(Document doc) {
		Element deleteFile = doc.createElement("deletefile");

		Element filenameNode = doc.createElement("file");
		filenameNode.setTextContent(file.getAbsolutePath());
		deleteFile.appendChild(filenameNode);

		return deleteFile;
	}

	public File getFile() {
		return file;
	}

}
