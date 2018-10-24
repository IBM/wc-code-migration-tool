package com.ibm.commerce.qcheck.core;

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
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * FakeResource allows tests to create resources that do not require an Eclipse
 * environment to run.
 * 
 * @author Trent Hoeppner
 */
public class FakeResource implements ValidatorResource {

	private File file;

	private File baseDir;

	public FakeResource() {
		// TODO Auto-generated constructor stub
	}

	public FakeResource(File file) {
		this.baseDir = file.getAbsoluteFile().getParentFile();
		this.file = file;
	}

	public FakeResource(File baseDir, File file) {
		this.baseDir = baseDir;
		this.file = file;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBaseDir() {
		return baseDir.getAbsolutePath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClassName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public File getFileAsFile() {
		return file;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IResource getFileAsResource() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFilename() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ModelRegistry getModelRegistry() {
		return ModelRegistry.getDefault();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPackageName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPathFilename() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ModelEnum> getSupportedModels() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ASTNode> getTypedNodeList(int type) {
		// TODO Auto-generated method stub
		return null;
	}

}
