package com.ibm.commerce.qcheck.ui.builder;

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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.IResourceValidatorResource;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.Options;
import com.ibm.commerce.qcheck.tools.config.TimeEnum;
import com.ibm.commerce.qcheck.ui.internal.UIValidationRunner;

/**
 * The AsYouTypeAction is used to turn AsYouTypeListener on or off. It does this
 * by changing the flag in the AsYouTypeListener.
 * <p>
 * This action is called by clicking a toggle button in the tool bar. If the
 * button is checked, it means the listener is on.
 * 
 * @author Trent Hoeppner
 */
public class AsYouTypeAction implements IWorkbenchWindowActionDelegate {

	/**
	 * Constructor for this.
	 */
	public AsYouTypeAction() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dispose() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run(IAction action) {
		boolean isChecked = action.isChecked();

		Options.ensureLoaded();
		Options.Attributes.AS_YOU_TYPE_BUTTON_ON.setValue(isChecked);

		IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (isChecked) {
			List<ValidatorResource> currentFile = getCurrentResource(workbenchPage);
			if (currentFile.size() != 0) {
				UIValidationRunner.INSTANCE.validate(currentFile, TimeEnum.ASYOUTYPE, new NullProgressMonitor());
			}
		} else {
			List<ValidatorResource> openedFiles = getOpenedResources(workbenchPage);
			if (openedFiles.size() != 0) {
				UIValidationRunner.INSTANCE.deleteMarkers(openedFiles);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing
	}

	/**
	 * This method is used to get all the files that are opened.
	 *
	 * @param workbenchPage
	 *            The current active workbench page. This value cannot be null.
	 *
	 * @return A list of the opened files. This value will not be null.
	 */
	private List<ValidatorResource> getOpenedResources(IWorkbenchPage workbenchPage) {
		IEditorReference[] editorReferences = workbenchPage.getEditorReferences();
		List<ValidatorResource> resources = new ArrayList<ValidatorResource>();
		for (int i = 0; i < editorReferences.length; i++) {
			try {
				IEditorInput editorInput = editorReferences[i].getEditorInput();
				buildResources(editorInput, resources);
			} catch (PartInitException e) {
				Debug.COMMENT.log(e.getMessage());
			}
		}
		return resources;
	}

	/**
	 * This method is used to get the currently edited file.
	 *
	 * @param workbenchPage
	 *            The current active workbench page. This value cannot be null.
	 *
	 * @return The current editing file resource. This value will not be null.
	 */
	private List<ValidatorResource> getCurrentResource(IWorkbenchPage workbenchPage) {
		List<ValidatorResource> resources = new ArrayList<ValidatorResource>(1);
		IEditorPart editorPart = workbenchPage.getActiveEditor();
		IEditorInput editorInput = editorPart.getEditorInput();
		buildResources(editorInput, resources);
		return resources;
	}

	/**
	 * This method adds the file to the list of resources that to be validated.
	 *
	 * @param editorInput
	 *            The editor of that file. This value cannot be null.
	 * @param resources
	 *            The list of files to be validated. This value cannot be null.
	 */
	private void buildResources(IEditorInput editorInput, List<ValidatorResource> resources) {
		if (editorInput instanceof IFileEditorInput) {
			IFile currentFile = ((IFileEditorInput) editorInput).getFile();
			try {
				ValidatorResource resource = new IResourceValidatorResource(currentFile);
				resources.add(resource);
			} catch (IOException e) {
				Debug.COMMENT.log(e.getMessage());
			}
		}
	}
}
