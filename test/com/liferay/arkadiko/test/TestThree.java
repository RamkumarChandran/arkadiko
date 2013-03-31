/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
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

package com.liferay.arkadiko.test;

import com.liferay.arkadiko.osgi.OSGiFrameworkFactory;
import com.liferay.arkadiko.osgi.ServiceTrackerInvocationHandler;
import com.liferay.arkadiko.test.beans.HasDependencyOnInterfaceOne;
import com.liferay.arkadiko.test.interfaces.InterfaceOne;
import com.liferay.arkadiko.test.util.BaseTest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.osgi.framework.Bundle;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <a href="TestTest.java.html"><b><i>View Source</i></b></a>
 *
 * @author Raymond Augé
 */
public class TestThree extends BaseTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		_context = new ClassPathXmlApplicationContext("META-INF/test-three.xml");

		_context.registerShutdownHook();
	}

	public void testDeployBundleWithImplementation() throws Exception {
		InterfaceOne interfaceOne = null;

		HasDependencyOnInterfaceOne bean, beanTwo, beanThree = null;

		// Install the bundle with the alternative impl

		Bundle installedBundle = installAndStart(
			_context, "/bundles/bundle-one/bundle-one.jar");

		try {
			// Test that the alternative impl is used by the first bean

			bean = (HasDependencyOnInterfaceOne)_context.getBean(
				HasDependencyOnInterfaceOne.class.getName());

			interfaceOne = bean.getInterfaceOne();

			assertTrue(
				"interfaceOne is not a proxy",
				Proxy.isProxyClass(interfaceOne.getClass()));

			InvocationHandler ih = Proxy.getInvocationHandler(interfaceOne);

			assertTrue(
				"ih not instanceof AKServiceTrackerInvocationHandler",
				ih instanceof ServiceTrackerInvocationHandler);

			ServiceTrackerInvocationHandler akih =
				(ServiceTrackerInvocationHandler)ih;

			assertFalse(
				"currentService is equal to originalService",
				akih.getCurrentService() == akih.getOriginalService());

			beanTwo = (HasDependencyOnInterfaceOne)_context.getBean(
				HasDependencyOnInterfaceOne.class.getName().concat("_TWO"));

			interfaceOne = beanTwo.getInterfaceOne();

			assertTrue(
				"interfaceOne is not a proxy",
				Proxy.isProxyClass(interfaceOne.getClass()));

			ih = Proxy.getInvocationHandler(interfaceOne);

			assertTrue(
				"ih not instanceof AKServiceTrackerInvocationHandler",
				ih instanceof ServiceTrackerInvocationHandler);

			akih = (ServiceTrackerInvocationHandler)ih;

			assertFalse(
				"currentService is equal to originalService",
				akih.getCurrentService() == akih.getOriginalService());

			beanThree = (HasDependencyOnInterfaceOne)_context.getBean(
				HasDependencyOnInterfaceOne.class.getName().concat("_THREE"));

			interfaceOne = beanThree.getInterfaceOne();

			assertFalse(
				"interfaceOne is a proxy",
				Proxy.isProxyClass(interfaceOne.getClass()));
		}
		finally {
			installedBundle.uninstall();
		}

		interfaceOne = bean.getInterfaceOne();

		assertTrue(
			"interfaceOne is not a proxy",
			Proxy.isProxyClass(interfaceOne.getClass()));

		InvocationHandler ih = Proxy.getInvocationHandler(interfaceOne);

		assertTrue(
			"ih not instanceof AKServiceTrackerInvocationHandler",
			ih instanceof ServiceTrackerInvocationHandler);

		ServiceTrackerInvocationHandler akih =
			(ServiceTrackerInvocationHandler)ih;

		assertTrue(
			"currentService not equal to originalService",
			akih.getCurrentService() == akih.getOriginalService());

		interfaceOne = beanTwo.getInterfaceOne();

		assertTrue(
			"interfaceOne is not a proxy",
			Proxy.isProxyClass(interfaceOne.getClass()));

		ih = Proxy.getInvocationHandler(interfaceOne);

		assertTrue(
			"ih not instanceof AKServiceTrackerInvocationHandler",
			ih instanceof ServiceTrackerInvocationHandler);

		akih = (ServiceTrackerInvocationHandler)ih;

		assertTrue(
			"currentService not equal to originalService",
			akih.getCurrentService() == akih.getOriginalService());

		interfaceOne = beanThree.getInterfaceOne();

		assertFalse(
			"interfaceOne is a proxy",
			Proxy.isProxyClass(interfaceOne.getClass()));
	}

	@Override
	protected void tearDown() throws Exception {
		_context.close();

		OSGiFrameworkFactory.stop();

		super.tearDown();
	}

	private AbstractApplicationContext _context;

}