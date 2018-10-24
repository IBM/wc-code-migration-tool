package com.ibm.commerce.validation.tools;

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

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * FakeProgressMonitor allows us to pass a non-null monitor to methods for
 * testing purposes.
 * 
 * @author Trent Hoeppner
 */
public class FakeProgressMonitor implements IProgressMonitor {

	/**
	 * Constructor for this.
	 */
	public FakeProgressMonitor() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void beginTask(String arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void done() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void internalWorked(double arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCanceled() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setCanceled(boolean arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTaskName(String arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void subTask(String arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void worked(int arg0) {
		// TODO Auto-generated method stub

	}

}
