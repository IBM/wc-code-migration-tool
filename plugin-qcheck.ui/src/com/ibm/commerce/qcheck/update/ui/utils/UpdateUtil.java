package com.ibm.commerce.qcheck.update.ui.utils;

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
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Properties;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.WorkbenchWindow;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.ui.Activator;

/**
 * UpdateUtil is a utility class to the update version plug in, which provide
 * some methods that are generally useful.
 * 
 * @author Trent Hoeppner
 */
@SuppressWarnings("restriction")
public class UpdateUtil {

	/**
	 * The clock when the updating runs.
	 */
	private static final int EXECUTE_TIME = 8;

	/**
	 * Get the updating button.
	 *
	 * @param buttonID
	 *            The ID of the button. Cannot be null or empty.
	 * @return The handler of the updating button, return null if there is not
	 *         the button.
	 */
	public static ToolItem getItem(String buttonID) {
		Activator activator = Activator.getDefault();
		IWorkbench workbench = activator.getWorkbench();
		IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();

		WorkbenchWindow window = (WorkbenchWindow) workbenchWindow;
		ICoolBarManager coolBarManager = window.getCoolBarManager2();
		CoolBar coolBar = ((CoolBarManager) coolBarManager).getControl();
		Control[] controls = coolBar.getChildren();

		for (Control control : controls) {
			ToolBar toolBar = (ToolBar) control;
			ToolItem[] items = toolBar.getItems();

			for (ToolItem item : items) {
				ContributionItem contribution = (ContributionItem) item.getData();
				String id = contribution.getId();
				if (buttonID.equals(id)) {
					return item;
				}
			}
		}

		Debug.FRAMEWORK.log("Could not find toolbar item for ", buttonID);
		return null;
	}

	/**
	 * Calculate the GMT calendar based on the calendar.
	 *
	 * @param calendar
	 *            The calendar to be converted. Will not be null.
	 * @return The GMT calendar of the calendar. Will not be null.
	 */
	public static Calendar convertToGMT(Calendar calendar) {
		if (calendar != null) {
			int offset = calendar.getTimeZone().getRawOffset();
			calendar.add(Calendar.MILLISECOND, offset);
		}
		return calendar;
	}

	/**
	 * Calculate the time margin between this time and the updating time.
	 *
	 * @param calendar
	 *            The time to be calculated. Will not be null.
	 * @return The milliseconds between this time and the updating time.
	 */
	public static long getMargin(Calendar calendar) {
		Calendar newCalendar = Calendar.getInstance();
		if (calendar.get(Calendar.HOUR_OF_DAY) < EXECUTE_TIME) {
			newCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DAY_OF_MONTH), EXECUTE_TIME, 0, 0);
		} else {
			newCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DAY_OF_MONTH) + 1, EXECUTE_TIME, 0, 0);
		}

		return newCalendar.getTimeInMillis() - calendar.getTimeInMillis();
	}

	/**
	 * Get the version file of the client.
	 *
	 * @param file
	 *            The URL of the version file. Cannot be null.
	 * @return The file handler of the version file. If there doesn't exist the
	 *         file, return null.
	 */
	public static synchronized Properties getProperties(URL file) {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = file.openStream();
			prop.load(input);
		} catch (IOException e) {
			Debug.FRAMEWORK.log(e);
			prop = null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					Debug.FRAMEWORK.log(e);
					prop = null;
				}
			}
		}
		return prop;
	}
}
