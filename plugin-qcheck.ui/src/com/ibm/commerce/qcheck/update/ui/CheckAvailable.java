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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;
import org.osgi.framework.Bundle;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.EclipseUtil;
import com.ibm.commerce.qcheck.tools.Options;
import com.ibm.commerce.qcheck.ui.Activator;
import com.ibm.commerce.qcheck.update.ui.utils.UpdateUtil;

/**
 * This class is used to check if there are available updates. There is a button
 * to indicate if there are updates(The default color of the button is green).
 * If there are, change the color of that button to be red. If some exceptions
 * happen, change the color to be yellow.
 * 
 * @author Trent Hoeppner
 */
public class CheckAvailable {

	/**
	 * The format of time.
	 */
	private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy.MM.dd-HH.mm");

	/**
	 * The singleton object.
	 */
	private static CheckAvailable checkStatus = null;

	/**
	 * The path where images are put.
	 */
	private final static String IMG_PATH = "icons" + File.separator;

	/**
	 * This is used to get the icons.
	 */
	private Bundle bundle;

	public static enum Status {
		UP_TO_DATE, ERROR, OUT_OF_DATE
	}

	/**
	 * The icon's name of the button.
	 */
	private String imageName = "qcheckready.png";

	/**
	 * The getter method of imageName.
	 *
	 * @return The variable imageName.
	 */
	public String getImageName() {
		return imageName;
	}

	/**
	 * The setter method of the imageName.
	 *
	 * @param imageName
	 *            The imageName.
	 */
	private void setImageName(String imageName) {
		this.imageName = imageName;
	}

	/**
	 * Use Singleton Pattern to get one the singleton instance.
	 *
	 * @return The singleton instance. Will not be null.
	 */
	public static synchronized CheckAvailable getInstance() {
		if (checkStatus == null) {
			checkStatus = new CheckAvailable();
		}
		return checkStatus;
	}

	/**
	 * The constructor, just do some initial work.
	 */
	private CheckAvailable() {
		this.bundle = Activator.getDefault().getBundle();
	}

	/**
	 * This method is the action of checking new available updates.
	 *
	 * @return The compare result. Will not be null.
	 */
	public Status check() {
		final Status status = compareVersions();

		// final Runnable checkRunnable = new Runnable() {
		//
		// public void run() {
		// ToolItem item =
		// UpdateUtil.getItem("com.ibm.commerce.qcheck.update.ui.CheckForAvaliables");
		// modifyButton(status, item);
		// if (Debug.FRAMEWORK.isActive()) {
		// Debug.FRAMEWORK.log(System.currentTimeMillis() + "\t" +
		// getImageName());
		// }
		// }
		// };
		//
		// if (Display.getDefault().getThread().equals(Thread.currentThread()))
		// {
		// // this thread is the GUI thread
		// checkRunnable.run();
		// } else {
		// // we need to change preferences on the GUI thread
		// Display.getDefault().asyncExec(checkRunnable);
		// }

		return status;
	}

	/**
	 * This method compares the current version file to the server version file.
	 *
	 * @return The status which represent if there are available updates or not.
	 *         Will not be null.
	 */
	private Status compareVersions() {
		Properties serverProp = getServerVersionFile();

		URL clientVersionURL = EclipseUtil.getDefault().getPluginsURL("version.txt");
		Properties clientProp = UpdateUtil.getProperties(clientVersionURL);
		if (clientProp == null || serverProp == null) {
			return Status.ERROR;
		}

		try {
			long serverTime = TIMESTAMP_FORMAT.parse(serverProp.getProperty("timestamp")).getTime();
			String tmp = clientProp.getProperty("timestamp");
			long clientTime = 0;
			if (tmp != null) {
				clientTime = TIMESTAMP_FORMAT.parse(clientProp.getProperty("timestamp")).getTime();
			}

			if (serverTime > clientTime) {
				// There are new available updates
				return Status.OUT_OF_DATE;
			}
		} catch (ParseException e) {
			Debug.FRAMEWORK.log(e);
			return Status.ERROR;
		}

		return Status.UP_TO_DATE;
	}

	/**
	 * Get the version file of the server.
	 *
	 * @return The file of the server.
	 */
	public Properties getServerVersionFile() {
		Options.ensureLoaded();
		String serverVersionString = Options.Attributes.UPDATE_URL.getValue() + "/version.txt";
		URL serverVersionURL;
		try {
			serverVersionURL = new URL(serverVersionString);
		} catch (MalformedURLException e1) {
			Debug.FRAMEWORK.log(e1);
			return null;
		}

		Properties serverProp = UpdateUtil.getProperties(serverVersionURL);
		if (serverProp != null) {
			return serverProp;
		} else {
			Debug.FRAMEWORK.log("There is no version file on the server.");
			return null;
		}
	}

	/**
	 * Modify the icons of the button.
	 *
	 * @param status
	 *            The status of the check. Cannot be null.
	 * @param item
	 *            The handle of the button. Cannot be null.
	 */
	public void modifyButton(Status status, ToolItem item) {
		if (item == null) {
			if (Debug.FRAMEWORK.isActive()) {
				Debug.FRAMEWORK.log("Could not get a reference to the update status button.");
			}
			return;
		}

		switch (status) {
		case OUT_OF_DATE:
			setImageName("qcheckerror.png");
			item.setToolTipText("New validator updates are available, click here");
			break;
		case ERROR:
			setImageName("qcheckwarning.png");
			item.setToolTipText("Error: Could not determine if a validator update is available");
			break;
		case UP_TO_DATE:
			setImageName("qcheckready.png");
			item.setToolTipText("Validators are up-to-date");
			break;
		default:
			throw new IllegalArgumentException("Illegal status received: " + status);
		}

		InputStream input = null;
		try {
			input = FileLocator.openStream(bundle, new Path(IMG_PATH + getImageName()), false);
			Image image = new Image(Display.getCurrent(), input);
			item.setImage(image);
		} catch (IOException e) {
			Debug.FRAMEWORK.log(e);
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException e) {
				Debug.FRAMEWORK.log(e);
			}
		}
	}

}
