package com.ibm.commerce.dependency.model;

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

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * This class controls the plug-in life cycle.
 * 
 * @author Trent Hoeppner
 */
public class Activator extends Plugin {

	/**
	 * The plug-in ID. This is normally used in the <code>MANIFEST.MF</code>
	 * file and also the <code>plugin.xml</code> file.
	 */
	public static final String PLUGIN_ID = "com.ibm.commerce.dependency.model";

	/**
	 * The singleton instance of this.
	 */
	private static Activator plugin = null;

	/**
	 * Constructor for an <code>Activator</code>.
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
	}

	/**
	 * {@inheritDoc}
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the singleton instance.
	 *
	 * @return The singleton instance or null if this plug-in is not started.
	 */
	public static Activator getDefault() {
		return plugin;
	}
}
