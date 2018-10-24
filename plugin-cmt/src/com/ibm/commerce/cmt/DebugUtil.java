package com.ibm.commerce.cmt;

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

import com.ibm.commerce.dependency.model.JavaItem;
import com.ibm.commerce.dependency.model.JavaItemIndex;

/**
 * This class is used for debugging purposes.
 * 
 * @author Trent Hoeppner
 */
public class DebugUtil {

	public static void verifyExists(JavaItemIndex index, String timeMessage) {
		// verifyClass(index, timeMessage, "com.ibm.commerce.user.objects",
		// "UserRegistryAccessBean");
	}

	public static void verifyClass(JavaItemIndex index, String timeMessage, String packageName, String className) {
		JavaItem javaClass = index.findClass(packageName, className);
		if (javaClass == null) {
			System.out.println(timeMessage + ": " + packageName + "." + className + " is NOT loaded");
		} else {
			System.out.println(timeMessage + ": " + javaClass + " is loaded");
			System.out.println("    attributes: " + javaClass.getAttributes());
			System.out.println("    children:");
			for (JavaItem child : javaClass.getChildren()) {
				System.out.println("        " + child);
			}
			System.out.println("    dependencies:");
			for (JavaItem dependency : javaClass.getDependencies()) {
				System.out.println("        " + dependency);
			}
			System.out.println("    incoming:");
			for (JavaItem incoming : javaClass.getIncoming()) {
				System.out.println("        " + incoming);
			}
		}
	}

	public static void stopAtPackage(JavaItem javaPackage, String timeMessage) {
		stopAtPackage(javaPackage.getName(), timeMessage);
	}

	public static void stopAtClassName(String className, String timeMessage) {
		// if (className.equals("UserRegistryAccessBean")) {
		// System.out.println(timeMessage + ": stopping at " + className);
		// }
	}

	public static void stopAtPackage(String packageName, String timeMessage) {
		// if (packageName.equals("com.ibm.commerce.user.objects")) {
		// System.out.println(timeMessage + ": stopping at " + packageName);
		// }
	}

	public static void stopAtPackageAndClassAndProject(String packageName, String className, String projectName,
			String timeMessage) {
		// if (packageName.equals("com.ibm.commerce.user.objects") &&
		// className.equals("UserRegistryAccessBean")
		// && projectName.equals("Content-Server-FEP")) {
		// System.out.println(timeMessage + ": stopping at " + projectName + ":"
		// + packageName + "." + className);
		// }
	}

}
