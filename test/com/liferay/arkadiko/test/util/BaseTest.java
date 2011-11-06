/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.arkadiko.test.util;


import junit.framework.TestCase;

/**
 * <a href="BaseTest.java.html"><b><i>View Source</i></b></a>
 *
 * @author Raymond Augé
 */
public class BaseTest extends TestCase {

	public String getProjectDir() {
		return _projectDir;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		_projectDir = System.getProperty("project.dir");
	}

	protected String _projectDir;
}