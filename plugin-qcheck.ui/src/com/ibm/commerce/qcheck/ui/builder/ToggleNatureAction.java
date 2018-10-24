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

import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.ui.internal.UIValidationRunner;

/**
 * ToggleNatureAction is used to turn validation on and off. It does this by
 * adding and removing the {@link ValidationNature} to the list of natures in
 * eclipse. Simply put, a nature represents some feature which may involve
 * multiple plug-ins.
 * <p>
 * This action is called by pressing a button in the pop-up menu for a project.
 * 
 * @author Trent Hoeppner
 */
public class ToggleNatureAction implements IObjectActionDelegate {

	/**
	 * The objects that were selected at the time that this action was called.
	 */
	private ISelection selection;

	/**
	 * {@inheritDoc}
	 */
	public void run(IAction action) {
		if (Debug.COMMENT.isActive()) {
			Debug.COMMENT.log("ToggleNatureAction.run()");
		}
		if (selection instanceof IStructuredSelection) {
			for (Iterator it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject) {
					project = (IProject) element;
					if (Debug.COMMENT.isActive()) {
						Debug.COMMENT.log("Project = ", project.getName());
					}
				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
					if (Debug.COMMENT.isActive()) {
						Debug.COMMENT.log("Project = ", project.getName());
					}
				}

				if (Debug.COMMENT.isActive()) {
					Debug.COMMENT.log("Project null? ", project == null);
				}
				if (project != null) {
					toggleNature(project);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IAction action, ISelection newSelection) {
		this.selection = newSelection;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// do nothing
	}

	/**
	 * Toggles the {@link ValidationNature} on (if currently off) or off (if
	 * currently on). If this nature is being toggled off, markers in all files
	 * created by {@link ValidationBuilder} will be deleted.
	 * 
	 * @param project
	 *            The pop-up menu of the project that action was called on.
	 *            Cannot be null.
	 */
	private void toggleNature(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			for (int i = 0; i < natures.length; ++i) {
				if (ValidationNature.NATURE_ID.equals(natures[i])) {
					// Remove the nature
					if (Debug.COMMENT.isActive()) {
						Debug.COMMENT.log("Toggling off");
					}
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);

					deleteMarkers(project);
					return;
				}
			}

			// Add the nature
			if (Debug.COMMENT.isActive()) {
				Debug.COMMENT.log("Toggling on");
			}
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = ValidationNature.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Removes markers related to the validation plug-in for the given resource.
	 * 
	 * @param resource
	 *            The resource to remove markers from. Cannot be null.
	 * 
	 * @exception CoreException
	 *                If an error occurs while removing the markers.
	 */
	private void deleteMarkers(IResource resource) throws CoreException {
		resource.deleteMarkers(UIValidationRunner.MARKER_TYPE, false, IResource.DEPTH_ZERO);

		if (resource instanceof IContainer) {
			IContainer container = (IContainer) resource;
			IResource[] members = container.members();
			for (IResource member : members) {
				deleteMarkers(member);
			}
		}
	}

}
