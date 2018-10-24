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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.tools.Options;
import com.ibm.commerce.qcheck.ui.internal.UIValidationRunner;

/**
 * The BuilderAction is used to turn validation on and off. It does this by
 * adding and removing the {@link ValidationNature} to the list of natures in
 * eclipse. Simply put, a nature represents some feature which may involve
 * multiple plug-ins.
 * <p>
 * This action is called by clicking a toggle button in the tool bar. If the
 * button is checked, it means the validation is on.
 * 
 * @author Trent Hoeppner
 */
public class BuilderAction implements IWorkbenchWindowActionDelegate {

	/**
	 * Constructor for this.
	 */
	public BuilderAction() {
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
		Options.Attributes.BUILDER_BUTTON_ON.setValue(isChecked);

		IWorkspace workSpace = ResourcesPlugin.getWorkspace();
		IProject[] projects = workSpace.getRoot().getProjects();
		if (projects != null) {
			for (IProject project : projects) {
				if (Debug.COMMENT.isActive()) {
					Debug.COMMENT.log("Project = ", project.getName());
				}
				toggleBuildButton(project, isChecked);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void selectionChanged(IAction action, ISelection newSelection) {
		// do nothing
	}

	/**
	 * Toggles the validation build button on (if the button is not on) or off
	 * (if the button is on). If this button is being toggled off, markers in
	 * all files created by {@link ValidationBuilder} will be deleted.
	 *
	 * @param project
	 *            The project that action was called on. This value cannot be
	 *            null.
	 * @param isChecked
	 *            The current state of the button. True indicates that it is
	 *            currently on, false indicates it is off.
	 */
	private void toggleBuildButton(IProject project, boolean isChecked) {
		try {
			if (!project.isOpen()) {
				if (Debug.FRAMEWORK.isActive()) {
					Debug.FRAMEWORK.log("Project ", project.getName(), " is closed, cannot toggle.");
				}
				return;
			}

			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			boolean validationNatureExists = false;
			for (int i = 0; i < natures.length; ++i) {
				if (ValidationNature.NATURE_ID.equals(natures[i])) {
					validationNatureExists = true;
					break;
				}
			}

			// If the toggle button is checked
			if (isChecked) {
				// Add the nature
				if (Debug.FRAMEWORK.isActive()) {
					Debug.FRAMEWORK.log("Toggling on");
				}

				if (!validationNatureExists) {
					String[] newNatures = new String[natures.length + 1];
					System.arraycopy(natures, 0, newNatures, 0, natures.length);
					newNatures[natures.length] = ValidationNature.NATURE_ID;
					description.setNatureIds(newNatures);
					project.setDescription(description, null);
				}
			} else {
				if (Debug.FRAMEWORK.isActive()) {
					Debug.FRAMEWORK.log("Toggling off");
				}

				UIValidationRunner.INSTANCE.deleteMarkers(project);
			}
		} catch (CoreException e) {
			Debug.FRAMEWORK.log(e);
		}
	}
}
