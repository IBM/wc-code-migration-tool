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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.osgi.framework.Bundle;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.EclipseUtil;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ModelRegistry;
import com.ibm.commerce.qcheck.core.ProblemActionFactory;
import com.ibm.commerce.qcheck.core.ValidationException;
import com.ibm.commerce.qcheck.core.ValidationResult;
import com.ibm.commerce.qcheck.core.Validator;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.tools.config.TimeEnum;

/**
 * ValidatorRunner is a utility class that allows running validators under a
 * common configuration, but from different sources. The wizard and validation
 * builder both use this class, instead of duplicating the code in each tool.
 * 
 * @author Trent Hoeppner
 */
public class ValidatorRunner {

	/**
	 * The object to get validators for us, depending on the configuration. This
	 * value will be null until the first time the validators are run.
	 */
	private static ConfigurationManager configManager = null;

	/**
	 * A map from monitors (which represent Threads running validators) to
	 * situations in which the monitors are being run. This is used to track
	 * which validation runs should take precedence over others. The ones with
	 * lower precedence will be canceled. This value will never be null.
	 */
	private static Map<IProgressMonitor, TimeEnum> workingMonitors = new HashMap<IProgressMonitor, TimeEnum>();

	/**
	 * Constructor for ValidatorRunner. Private to prevent instantiation.
	 */
	private ValidatorRunner() {
		// do nothing
	}

	/**
	 * Runs configured validators and returns the list of errors produced by
	 * those validators for the given files.
	 *
	 * @param resources
	 *            The resources to validate. Cannot be null.
	 * @param time
	 *            The circumstances under which the validation occurs. Cannot be
	 *            null.
	 * @param actionFactory
	 *            The factory to generate actions to resolve the problems found.
	 *            This value cannot be null.
	 * @param monitor
	 *            The progress monitor that is used to track progress and cancel
	 *            validation. Cannot be null.
	 *
	 * @return The list of errors found by the configured validators for the
	 *         input files. Will not be null.
	 *
	 * @throws ValidationException
	 *             If an error occurs while validating.
	 * @throws IOException
	 *             If an error occurs reading the configuration file or one of
	 *             the files to be validated.
	 * @throws OperationCanceledException
	 *             If the monitor was canceled for some reason.
	 */
	public static List<ValidationResult> runValidators(List<ValidatorResource> resources, TimeEnum time,
			ProblemActionFactory actionFactory, IProgressMonitor monitor)
			throws ValidationException, IOException, OperationCanceledException {
		return runValidators(resources, time, null, actionFactory, monitor);
	}

	/**
	 * Runs configured validators and returns the list of errors produced by
	 * those validators for the given files, filtered by the given validator
	 * names.
	 *
	 * @param resources
	 *            The resources to validate. Cannot be null.
	 * @param time
	 *            The circumstances under which the validation occurs. Cannot be
	 *            null.
	 * @param configValidatorNames
	 *            The list of validators to use. The values must be specified in
	 *            the configuration file as <code>name</code> attributes in
	 *            <code>validator</code> elements. A null value indicates that
	 *            all validators will be used. An empty list indicates that no
	 *            validators will be used.
	 * @param actionFactory
	 *            The factory to generate actions to resolve the problems found.
	 *            This value cannot be null.
	 * @param monitor
	 *            The progress monitor that is used to track progress and cancel
	 *            validation. Cannot be null.
	 *
	 * @return The list of errors found by the configured validators for the
	 *         input files. Will not be null.
	 *
	 * @throws ValidationException
	 *             If an error occurs while validating.
	 * @throws IOException
	 *             If an error occurs reading the configuration file or one of
	 *             the files to be validated.
	 * @throws OperationCanceledException
	 *             If the monitor was canceled for some reason.
	 */
	public static List<ValidationResult> runValidators(List<ValidatorResource> resources, TimeEnum time,
			List<String> configValidatorNames, ProblemActionFactory actionFactory, IProgressMonitor monitor)
			throws ValidationException, IOException, OperationCanceledException {

		EclipseUtil.getDefault().reloadDebugConfig();

		// we let the validators interrupt each other:
		// asyoutype can be interrupted by toolkitbuild and manual
		// toolkit can be interrupted by manual
		// manual cannot be interrupted
		synchronized (workingMonitors) {
			for (IProgressMonitor otherMonitor : workingMonitors.keySet()) {
				TimeEnum otherTime = workingMonitors.get(otherMonitor);

				switch (time) {
				case ASYOUTYPE:
					if (otherTime == TimeEnum.ASYOUTYPE) {
						// this one has more current changes than another
						// one that started running earlier, so cancel the
						// other one
						otherMonitor.setCanceled(true);
					} else if (otherTime == TimeEnum.FULLTOOLKITBUILD || otherTime == TimeEnum.INCREMENTALTOOLKITBUILD
							|| otherTime == TimeEnum.MANUAL) {
						monitor.setCanceled(true);
					}
					break;
				case INCREMENTALTOOLKITBUILD:
					if (otherTime == TimeEnum.ASYOUTYPE || otherTime == TimeEnum.INCREMENTALTOOLKITBUILD) {
						otherMonitor.setCanceled(true);
					} else if (otherTime == TimeEnum.FULLTOOLKITBUILD || otherTime == TimeEnum.MANUAL) {
						monitor.setCanceled(true);
					}
					break;
				case FULLTOOLKITBUILD:
					if (otherTime == TimeEnum.ASYOUTYPE || otherTime == TimeEnum.INCREMENTALTOOLKITBUILD
							|| otherTime == TimeEnum.FULLTOOLKITBUILD) {
						otherMonitor.setCanceled(true);
					} else if (otherTime == TimeEnum.MANUAL) {
						monitor.setCanceled(true);
					}
					break;
				case MANUAL:
					if (otherTime == TimeEnum.ASYOUTYPE || otherTime == TimeEnum.FULLTOOLKITBUILD
							|| otherTime == TimeEnum.INCREMENTALTOOLKITBUILD) {
						otherMonitor.setCanceled(true);
					}
					break;
				default:
					throw new IllegalArgumentException("Invalid time parameter: " + time);
				}
			}

			workingMonitors.put(monitor, time);
		}

		List<ValidationResult> allResults = new ArrayList<ValidationResult>();
		try {
			monitor.beginTask("Running Commerce validators", 1);

			// only allow Java files
			if (Debug.FRAMEWORK.isActive()) {
				Debug.FRAMEWORK.log("filtering out files and filling up resources");
			}

			Iterator<ValidatorResource> resourceIterator = resources.iterator();
			while (resourceIterator.hasNext()) {
				ValidatorResource resource = resourceIterator.next();

				// TODO - hacky - load some common variables then uncache it
				resource.getBaseDir();
				ModelRegistry.getDefault().clearValidator(resource);
			}

			initConfigurationManager();

			EclipseUtil.getDefault().checkCanceled(monitor);

			// analyze the files
			if (Debug.FRAMEWORK.isActive()) {
				Debug.FRAMEWORK.log("getting validators");
			}

			Map<Validator, List<ValidatorResource>> validatorToResourceMap;
			if (configValidatorNames == null) {
				validatorToResourceMap = configManager.getValidators(resources, time);
			} else {
				validatorToResourceMap = configManager.getValidators(resources, time, configValidatorNames);
			}

			// split validators/resources into two groups
			// 1. validators that need EXTERNAL models - run all related
			// resources for one validator at a time
			// 2. validators that do not need EXTERNAL models - run all such
			// validators on each resource, one at a time

			if (Debug.FRAMEWORK.isActive()) {
				Debug.FRAMEWORK.log("splitting resources between external and non-external");
			}

			Map<Validator, List<ValidatorResource>> externalValidatorToResourcesMap = new HashMap<Validator, List<ValidatorResource>>();
			Map<Validator, List<ValidatorResource>> batchValidatorToResourcesMap = new HashMap<Validator, List<ValidatorResource>>();
			Map<ValidatorResource, List<Validator>> resourceToNonExternalValidatorsMap = new HashMap<ValidatorResource, List<Validator>>();

			for (Entry<Validator, List<ValidatorResource>> entry : validatorToResourceMap.entrySet()) {
				Validator validator = entry.getKey();
				List<ValidatorResource> resourcesToValidate = entry.getValue();

				if (validator.getRequiredModels().contains(ModelEnum.EXTERNAL)) {
					externalValidatorToResourcesMap.put(validator, resourcesToValidate);
					if (Debug.FRAMEWORK.isActive()) {
						Debug.FRAMEWORK.log("added ", resourcesToValidate.size(),
								" files to external validator " + validator);
					}
				} else if (validator.getRequiredModels().contains(ModelEnum.BATCH)) {
					batchValidatorToResourcesMap.put(validator, resourcesToValidate);
					if (Debug.FRAMEWORK.isActive()) {
						Debug.FRAMEWORK.log("added ", resourcesToValidate.size(),
								" files to batch validator " + validator);
					}
				} else {
					for (ValidatorResource resource : resourcesToValidate) {
						List<Validator> validatorsToRun = resourceToNonExternalValidatorsMap.get(resource);
						if (validatorsToRun == null) {
							validatorsToRun = new ArrayList<Validator>();
							resourceToNonExternalValidatorsMap.put(resource, validatorsToRun);
						}

						if (!validatorsToRun.contains(validator)) {
							validatorsToRun.add(validator);
						}
						if (Debug.FRAMEWORK.isActive()) {
							Debug.FRAMEWORK.log("added non-external validator ", validator,
									" to file " + resource.getFileAsFile());
						}
					}
				}
			}

			if (Debug.FRAMEWORK.isActive()) {
				Debug.FRAMEWORK.log("running external validators");
			}

			for (Validator validator : externalValidatorToResourcesMap.keySet()) {
				List<ValidatorResource> resourcesToValidate = validatorToResourceMap.get(validator);
				int start = 0;
				int end = Math.min(start + 300, resourcesToValidate.size());
				while (end - start > 0) {
					EclipseUtil.getDefault().checkCanceled(monitor);

					if (Debug.FRAMEWORK.isActive()) {
						Debug.FRAMEWORK.log("Analyzing ", end - start, " files.");
					}

					List<ValidatorResource> resourcesChunk = resourcesToValidate.subList(start, end);

					try {
						List<ValidationResult> results = validator.analyze(resourcesChunk, actionFactory,
								new SubProgressMonitor(monitor, 1));
						allResults.addAll(results);
					} catch (Exception e) {
						Debug.FRAMEWORK.log(e);
					}

					start = end;
					end = Math.min(start + 300, resourcesToValidate.size());
				}

			}

			if (Debug.FRAMEWORK.isActive()) {
				Debug.FRAMEWORK.log("running batch validators");
			}

			for (Validator validator : batchValidatorToResourcesMap.keySet()) {
				List<ValidatorResource> resourcesToValidate = validatorToResourceMap.get(validator);
				EclipseUtil.getDefault().checkCanceled(monitor);

				if (Debug.FRAMEWORK.isActive()) {
					Debug.FRAMEWORK.log("Analyzing ", resourcesToValidate.size(), " files.");
				}

				try {
					List<ValidationResult> results = validator.analyze(resourcesToValidate, actionFactory,
							new SubProgressMonitor(monitor, 1));
					allResults.addAll(results);
				} catch (Exception e) {
					Debug.FRAMEWORK.log(e);
				}
			}

			if (Debug.FRAMEWORK.isActive()) {
				Debug.FRAMEWORK.log("running non-external validators");
			}

			for (ValidatorResource resource : resourceToNonExternalValidatorsMap.keySet()) {
				List<Validator> validatorsToRun = resourceToNonExternalValidatorsMap.get(resource);

				for (Validator validator : validatorsToRun) {
					EclipseUtil.getDefault().checkCanceled(monitor);

					try {
						List<ValidationResult> results = validator.analyze(Arrays.asList(resource), actionFactory,
								new SubProgressMonitor(monitor, 1));
						allResults.addAll(results);
					} catch (Exception e) {
						Debug.FRAMEWORK.log(e);
					}

					// don't keep models in memory
					ModelRegistry.getDefault().clearValidator(resource);
				}
			}

			// sleep for a small time, so that if toolkitbuild happens before
			// asyoutype, the toolkitbuild will still be there when asyoutype
			// starts and the asyoutype won't overwrite the toolkitbuild markers
			try {
				Thread.currentThread().sleep(1200);
			} catch (InterruptedException e) {
				Debug.FRAMEWORK.log(e);
			}
		} finally {
			monitor.done();

			synchronized (workingMonitors) {
				workingMonitors.remove(monitor);
			}
		}

		return allResults;
	}

	/**
	 * Initializes the configuration manager, which serves as long as the
	 * plug-in is running.
	 *
	 * @throws MalformedURLException
	 *             If there was an error forming the URL of the configuration.
	 */
	private synchronized static void initConfigurationManager() {
		if (configManager == null) {
			Bundle bundle = Activator.getDefault().getBundle();
			URL baseConfigURL = FileLocator.find(bundle, new Path("data\\basevalidatorconfig.xml"), null);

			URL globalConfigURL = EclipseUtil.getDefault().getDropinsURL("wizard/userconfig/globalvalidatorconfig.xml");

			URL localConfigURL = EclipseUtil.getDefault().getWorkspaceRootURL("localvalidatorconfig.xml");
			configManager = new ConfigurationManager("junk", baseConfigURL, globalConfigURL, localConfigURL);
		}
	}
}
