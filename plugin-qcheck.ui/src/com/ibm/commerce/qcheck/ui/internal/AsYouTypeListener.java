package com.ibm.commerce.qcheck.ui.internal;

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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ModelEnum;
import com.ibm.commerce.qcheck.core.ValidatorResource;
import com.ibm.commerce.qcheck.core.WorkingValidatorResource;
import com.ibm.commerce.qcheck.tools.Options;
import com.ibm.commerce.qcheck.tools.config.TimeEnum;

/**
 * AsYouTypeListener is a listener for changes in Java files, which then trigger
 * validation on the current file. If an event occurs for the same file with no
 * changes, changes after the first will be ignored.
 * 
 * @author Trent Hoeppner
 */
public class AsYouTypeListener implements IElementChangedListener {

	/**
	 * The hash code of the last file that was validated. This file is the
	 * working copy, not the file on disk.
	 */
	private int lastValidationHashCode = 0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void elementChanged(ElementChangedEvent event) {
		if (Debug.FRAMEWORK.isActive()) {
			Debug.FRAMEWORK.log("post reconcile event ");
		}

		Options.ensureLoaded();
		Boolean isOn = Options.Attributes.AS_YOU_TYPE_BUTTON_ON.getValue();
		if (!isOn.booleanValue()) {
			return;
		}
		try {
			IJavaElement element = event.getDelta().getElement();
			if (element == null) {
				Debug.FRAMEWORK.log("Null element, cannot perform as-you-type validation.");
				return;
			}

			IOpenable openable = element.getOpenable();
			if (openable == null) {
				Debug.FRAMEWORK.log("Null openable, cannot perform as-you-type validation.");
				return;
			}

			IBuffer buffer = openable.getBuffer();
			if (buffer == null) {
				Debug.FRAMEWORK.log("Null buffer, cannot perform as-you-type validation.");
				return;
			}

			String bufferContents = buffer.getContents();
			if (bufferContents == null) {
				Debug.FRAMEWORK.log("Null bufferContents, cannot perform as-you-type validation.");
				return;
			}

			CompilationUnit comp = event.getDelta().getCompilationUnitAST();
			if (comp == null) {
				Debug.FRAMEWORK.log("Null compilation unit, cannot perform as-you-type validation.");
				return;
			}

			WorkingValidatorResource validatorResource = new WorkingValidatorResource(element, bufferContents, comp);

			if (lastValidationHashCode == ModelEnum.STRING.getData(validatorResource).hashCode()) {
				return;
			}
			lastValidationHashCode = ModelEnum.STRING.getData(validatorResource).hashCode();

			List<ValidatorResource> resources = new ArrayList<ValidatorResource>(1);
			resources.add(validatorResource);
			UIValidationRunner.INSTANCE.validate(resources, TimeEnum.ASYOUTYPE, new NullProgressMonitor());
		} catch (Exception e) {
			Debug.FRAMEWORK.log(e);
		}

	}
}
