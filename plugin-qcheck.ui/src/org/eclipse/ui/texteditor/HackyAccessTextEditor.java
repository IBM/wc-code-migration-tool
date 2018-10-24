/**
 *
 */
package org.eclipse.ui.texteditor;

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

import org.eclipse.jface.text.source.ISourceViewer;

/**
 * This class is used to allow an outside class to get the ISourceViewer for a
 * particular text editor. It is pretty hacky but is critical for implementing
 * quick assist in existing editor implementations.
 * 
 * @author Trent Hoeppner
 */
public class HackyAccessTextEditor extends AbstractTextEditor {

	/**
	 * The editor to get the viewer for.
	 */
	private AbstractTextEditor editor;

	/**
	 * Constructor for this.
	 *
	 * @param editor
	 *            The editor to get the viewer for. This value cannot be null.
	 */
	public HackyAccessTextEditor(AbstractTextEditor editor) {
		this.editor = editor;
	}

	/**
	 * Returns the viewer for the editor supplied in the constructor.
	 *
	 * @return The viewer for the editor in the constructor. This value will be
	 *         null if the editor's viewer is null.
	 */
	public ISourceViewer getEditorSourceViewer() {
		return editor.getSourceViewer();
	}
}