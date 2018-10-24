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

import java.util.TimerTask;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.tools.Options;
import com.ibm.commerce.qcheck.tools.Options.Attributes;
import com.ibm.commerce.qcheck.update.ui.utils.UpdateUtil;

/**
 * The class is used to run the method check() of class {@link CheckAvailable}
 * periodically, which extends TimerTask.
 * 
 * @author Trent Hoeppner
 */
public class InitializeButtonsTimerTask extends TimerTask {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		try {
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					loadButtonSetting(Options.Attributes.BUILDER_BUTTON_ON, "com.ibm.commerce.qcheck.ui.Builder");
					loadButtonSetting(Options.Attributes.AS_YOU_TYPE_BUTTON_ON, "com.ibm.commerce.qcheck.ui.asYouType");
				}

				private void loadButtonSetting(Attributes buttonOnOption, String buttonID) {
					Options.ensureLoaded();
					Boolean isOn = buttonOnOption.getValue();
					Debug.FRAMEWORK.log("checking if ", buttonID, " is available now...");
					ToolItem item = UpdateUtil.getItem(buttonID);
					Debug.FRAMEWORK.log("item is null (", item == null, "), setSelection(", isOn, ")");
					item.setSelection(isOn);
				}

			};

			if (Display.getDefault().getThread().equals(Thread.currentThread())) {
				// this thread is the GUI thread
				runnable.run();
			} else {
				// we need to change preferences on the GUI thread
				Display.getDefault().asyncExec(runnable);
			}
		} catch (Exception e) {
			Debug.FRAMEWORK.log(e);
		}
	}
}
