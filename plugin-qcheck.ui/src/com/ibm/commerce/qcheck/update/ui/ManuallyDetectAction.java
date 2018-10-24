package com.ibm.commerce.qcheck.update.ui;

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

import java.net.URL;
import java.util.Properties;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.tools.Options;
import com.ibm.commerce.qcheck.update.ui.CheckAvailable.Status;

/**
 * The ManuallyDetectAction is used to check for available updates manually and
 * locate to the changed files if there are. Then turn the button to the normal
 * state.
 * <p>
 * This action is called by clicking a button in the tool bar.
 * 
 * @author Trent Hoeppner
 */
public class ManuallyDetectAction implements IWorkbenchWindowActionDelegate {

	/**
	 * The current workbench.
	 */
	private IWorkbenchWindow window;

	/**
	 * The constructor.
	 */
	public ManuallyDetectAction() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dispose() {
		this.window = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run(IAction action) {
		CheckAvailable checkAvailable = CheckAvailable.getInstance();

		/**
		 * 1. get the button color. if red, do check again, get the changed
		 * files, open them in browser and change the color to default color. if
		 * yellow, do check again manually.
		 */
		try {
			CheckAvailable.Status status = checkAvailable.check();
			if (status == Status.OUT_OF_DATE) {
				Properties props = checkAvailable.getServerVersionFile();
				String filename = props.getProperty("filename");

				String serverURL = Options.Attributes.UPDATE_URL.getValue();
				URL url = new URL(serverURL + "/" + filename);
				String command = "cmd /c \"" + "start " + url + "\"";
				Runtime.getRuntime().exec(command);
			}

			if (Debug.FRAMEWORK.isActive()) {
				Debug.FRAMEWORK.log(System.currentTimeMillis() + "\t" + checkAvailable.getImageName());
			}

		} catch (Exception e) {
			Debug.FRAMEWORK.log(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
	}

}
