package com.ibm.commerce.qcheck.ui;

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

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.ui.internal.AsYouTypeListener;
import com.ibm.commerce.qcheck.update.ui.CheckAvailableStartup;

/**
 * Activator controlimport
 * com.ibm.commerce.qcheck.update.ui.CheckAvailableStartup; import
 * com.ibm.commerce.qcheck.update.ui.CheckAvailableStartup; s the plug-in life
 * cycle.
 * 
 * @author Trent Hoeppner
 */
public class Activator extends AbstractUIPlugin {

	/**
	 * The plug-in ID. This is normally used in the <code>MANIFEST.MF</code>
	 * file and also the <code>plugin.xml</code> file.
	 */
	public static final String PLUGIN_ID = "com.ibm.commerce.qcheck.ui";

	/**
	 * The singleton instance of this.
	 */
	private static Activator plugin = null;

	private AsYouTypeListener asYouTypeListener;

	/**
	 * The getter method of the asYouTyprListener.
	 * 
	 * @return The single asYouTypeListener.
	 */
	public AsYouTypeListener getAsYouTypeListener() {
		return asYouTypeListener;
	}

	/**
	 * Constructor for this.
	 */
	public Activator() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		asYouTypeListener = new AsYouTypeListener();
		JavaCore.addElementChangedListener(asYouTypeListener, ElementChangedEvent.POST_RECONCILE);
	}

	/**
	 * {@inheritDoc}
	 */
	public void stop(BundleContext context) throws Exception {
		CheckAvailableStartup.getTimer().cancel();
		Debug.COMMENT.log("Timer canceled...");
		plugin = null;
		super.stop(context);
		JavaCore.removeElementChangedListener(asYouTypeListener);
	}

	/**
	 * Returns the singleton instance.
	 * 
	 * @return The singleton instance or null if this plug-in is not started.
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            The relative path to the image, from the root of the plug-in.
	 *            Cannot be null or empty.
	 * 
	 * @return The image descriptor, or null if no image could be found.
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
