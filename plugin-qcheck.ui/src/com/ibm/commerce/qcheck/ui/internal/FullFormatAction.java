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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

import com.ibm.commerce.qcheck.core.Debug;

/**
 * FullFormatAction runs both the Java Format command and the Remove Trailing
 * Whitespace command. This is necessary because the default formatter does not
 * remove trailing whitespace in JavaDoc with an empty line. When Checkstyle
 * checks for line endings, it produces errors.
 * 
 * @author Trent Hoeppner
 */
public class FullFormatAction implements IEditorActionDelegate {

	/**
	 * Constructor for FullFormatAction.
	 */
	public FullFormatAction() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setActiveEditor(IAction action, IEditorPart part) {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run(IAction action) {
		executeCommand("org.eclipse.jdt.ui.edit.text.java.format");
		executeCommand("org.eclipse.ui.edit.text.removeTrailingWhitespace");
	}

	/**
	 * Executes the command with the given ID and null parameters.
	 *
	 * @param commandID
	 *            The ID of the command as declared in the command's
	 *            <code>plugin.xml</code> file. Cannot be null or empty.
	 */
	private void executeCommand(String commandID) {
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage activePage = activeWindow.getActivePage();
		IWorkbenchPartSite site = activePage.getActivePart().getSite();
		IHandlerService handlerService = (IHandlerService) site.getService(IHandlerService.class);
		try {
			handlerService.executeCommand(commandID, null);
		} catch (ExecutionException e) {
			Debug.FRAMEWORK.log(e);
		} catch (NotDefinedException e) {
			Debug.FRAMEWORK.log(e);
		} catch (NotEnabledException e) {
			Debug.FRAMEWORK.log(e);
		} catch (NotHandledException e) {
			Debug.FRAMEWORK.log(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing
	}

}
