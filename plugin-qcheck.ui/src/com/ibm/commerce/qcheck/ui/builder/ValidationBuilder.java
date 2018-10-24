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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.IResourceValidatorResource;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.TimeEnum;
import com.ibm.commerce.qcheck.ui.internal.UIValidationRunner;

/**
 * ValidationBuilder is used to run the validation tools during a normal build
 * cycle. Builders are run along with compile cycles.
 * 
 * @author Trent Hoeppner
 */
public class ValidationBuilder extends IncrementalProjectBuilder {

	/**
	 * The ID of this builder, used in <code>plugin.xml</code> to associate this
	 * builder with the {@link ValidationNature}.
	 */
	public static final String BUILDER_ID = "com.ibm.commerce.qcheck.ui.builder";

	/**
	 * Constructor for this.
	 */
	public ValidationBuilder() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("Validating as a build", 1);

			Debug.FRAMEWORK.log("Validation started at ", DateFormat.getDateTimeInstance().format(new Date()));
			if (kind == FULL_BUILD) {
				fullBuild(new SubProgressMonitor(monitor, 1));
			} else {
				IResourceDelta delta = getDelta(getProject());
				if (delta == null) {
					fullBuild(new SubProgressMonitor(monitor, 1));
				} else {
					incrementalBuild(delta, new SubProgressMonitor(monitor, 1));
				}
			}
		} finally {
			monitor.done();
		}

		return null;
	}

	/**
	 * Performs a full build, which results in validating all resources in a
	 * project.
	 *
	 * @throws CoreException
	 *             If an error occurs when building.
	 */
	private void fullBuild(IProgressMonitor monitor) throws CoreException {
		ValidationResourceVisitor visitor = new ValidationResourceVisitor();
		getProject().accept(visitor);
		UIValidationRunner.INSTANCE.validate(visitor.getResources(), TimeEnum.FULLTOOLKITBUILD, monitor);
	}

	/**
	 * Performs an incremental build, only validating resources that have
	 * changed since the last build.
	 *
	 * @param delta
	 *            The resources that have changed. Cannot be null.
	 *
	 * @throws CoreException
	 *             If an error occurs when building.
	 */
	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		// the visitor does the work
		ValidationDeltaVisitor visitor = new ValidationDeltaVisitor();
		delta.accept(visitor);
		UIValidationRunner.INSTANCE.validate(visitor.getResources(), TimeEnum.INCREMENTALTOOLKITBUILD, monitor);
	}

	/**
	 * Adds the given resource to the given list as a ValidatorResource, but
	 * only if it is a file resource.
	 *
	 * @param resource
	 *            The resource to add. Cannot be null.
	 * @param resources
	 *            The list of resources so far. Cannot be null, but may be
	 *            empty.
	 */
	private void addIfFileResource(IResource resource, List<ValidatorResource> resources) {
		if (resource.getType() == IResource.FILE) {
			try {
				IResourceValidatorResource validatorResource = new IResourceValidatorResource(resource);
				resources.add(validatorResource);
			} catch (IOException e) {
				Debug.FRAMEWORK.log(e);
			}
		}
	}

	/**
	 * ValidationDeltaVisitor is used to traverse changed files in the resource
	 * tree and add, update, or remove markers as each resource is validated.
	 * Resources that have been removed will have their markers removed as well.
	 */
	private class ValidationDeltaVisitor implements IResourceDeltaVisitor {

		/**
		 * The resources accumulated by this visitor. See {@link #getResources}
		 * for details.
		 */
		private List<ValidatorResource> resources = new ArrayList<ValidatorResource>();

		/**
		 * Returns the resources that have been accumulated by this visitor so
		 * far. This method should only be called after the visitor has
		 * finished.
		 *
		 * @return The resources that have been visited by this. Will not be
		 *         null, but may be empty if no resources were visited.
		 */
		public List<ValidatorResource> getResources() {
			return resources;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				addIfFileResource(resource, resources);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				IFile file = resource.getProject().getFile(resource.getProjectRelativePath());
				UIValidationRunner.INSTANCE.deleteMarkers(file);
				break;
			case IResourceDelta.CHANGED:
				addIfFileResource(resource, resources);
				break;
			default:
				break;
			}

			// return true to continue visiting children.
			return true;
		}
	}

	/**
	 * ValidationResourceVisitor is used to traverse all files for a project
	 * during a full build and add a marker for each error that is found.
	 */
	private class ValidationResourceVisitor implements IResourceVisitor {

		/**
		 * The resources accumulated by this visitor. See {@link #getResources}
		 * for details.
		 */
		private List<ValidatorResource> resources = new ArrayList<ValidatorResource>();

		/**
		 * Returns the resources that have been accumulated by this visitor so
		 * far. This method should only be called after the visitor has
		 * finished.
		 *
		 * @return The resources that have been visited by this. Will not be
		 *         null, but may be empty if no resources were visited.
		 */
		public List<ValidatorResource> getResources() {
			return resources;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean visit(IResource resource) {
			addIfFileResource(resource, resources);

			return true;
		}
	}

}
