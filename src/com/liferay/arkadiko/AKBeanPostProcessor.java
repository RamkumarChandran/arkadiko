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

package com.liferay.arkadiko;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.launch.Framework;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * @author Raymond Augé
 */
public class AKBeanPostProcessor implements BeanPostProcessor, Ordered {

	/**
	 * After properties set.
	 */
	public void afterPropertiesSet() {
		if (_classLoader == null) {
			_classLoader = Thread.currentThread().getContextClassLoader();
		}

		if (_framework == null) {
			throw new IllegalArgumentException("framework is required");
		}

		if (_proxyFactory == null) {
			_proxyFactory = Proxy.class;
		}

		try {
			_proxyFactoryMethod = _proxyFactory.getMethod(
				"newProxyInstance", ClassLoader.class, Class[].class,
				InvocationHandler.class);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(
				"proxy factory must implement newProxyInstance(...); " +
					"see java.lang.reflect.Proxy", e);
		}
	}

	/**
	 * Gets the class loader.
	 *
	 * @return the class loader
	 */
	public ClassLoader getClassLoader() {
		return _classLoader;
	}

	/**
	 * Gets the extra bean properties.
	 *
	 * @return the extra bean properties
	 */
	public Map<String, Object> getExtraBeanProperties() {
		return _extraBeanProperties;
	}

	/**
	 * Gets the framework.
	 *
	 * @return the framework
	 */
	public Framework getFramework() {
		return _framework;
	}

	/**
	 * Gets the order.
	 *
	 * @return the order
	 */
	public int getOrder() {
		return _order;
	}

	/**
	 * Checks if is strict matching.
	 *
	 * @return true, if is strict matching
	 */
	public boolean isStrictMatching() {
		return _strictMatching;
	}

	/**
	 * Post process after initialisation.
	 *
	 * @param bean the bean
	 * @param beanName the bean name
	 * @return the object
	 * @throws BeansException the beans exception
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName)
		throws BeansException {

		List<Class<?>> interfaces = AKIntrospector.getInterfacesAsList(
			bean);

		if (ignoreBean(bean, beanName, interfaces)) {
			return bean;
		}

		Framework framework = getFramework();

		BundleContext bundleContext = framework.getBundleContext();

		registerService(bundleContext, bean, beanName, interfaces);

		return createProxy(bundleContext, bean, beanName, interfaces);
	}

	/**
	 * Post process before initialisation.
	 *
	 * @param bean the bean
	 * @param beanName the bean name
	 * @return the object
	 * @throws BeansException the beans exception
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName)
		throws BeansException {

		return bean;
	}

	/**
	 * Sets the class loader.
	 *
	 * @param classLoader the new class loader
	 */
	public void setClassLoader(ClassLoader classLoader) {
		_classLoader = classLoader;
	}

	/**
	 * Sets the extra bean properties.
	 *
	 * @param extraBeanProperties the extra bean properties
	 */
	public void setExtraBeanProperties(
		Map<String, Object> extraBeanProperties) {

		_extraBeanProperties = extraBeanProperties;
	}

	/**
	 * Sets the framework.
	 *
	 * @param framework the new framework
	 */
	public void setFramework(Framework framework) {
		_framework = framework;
	}

	/**
	 * Sets the ignored bean names.
	 *
	 * @param ignoredBeanNames the new ignored bean names
	 */
	public void setIgnoredBeanNames(List<String> ignoredBeanNames) {
		_ignoredBeanNames = ignoredBeanNames;
	}

	/**
	 * Sets the ignored class names.
	 *
	 * @param ignoredClassNames the new ignored class names
	 */
	public void setIgnoredClassNames(List<String> ignoredClassNames) {
		_ignoredClassNames = ignoredClassNames;
	}

	/**
	 * Sets the order.
	 *
	 * @param order the new order
	 */
	public void setOrder(int order) {
		_order = order;
	}

	/**
	 * A class for generating proxies that must implement the newProxyInstance
	 * as found in {@link java.lang.reflect.Proxy}.
	 *
	 * @param proxyFactory the proxy factory class
	 */
	public void setProxyFactory(Class<?> proxyFactory) {
		_proxyFactory = proxyFactory;
	}

	/**
	 * Whether services must matched on the full list of bean interfaces.
	 *
	 * @param strictMatching
	 */
	public void setStrictMatching(boolean strictMatching) {
		_strictMatching = strictMatching;
	}

	protected void addExtraBeanProperties(Hashtable<String,Object> properties) {
		if ((_extraBeanProperties == null) || _extraBeanProperties.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Object> entry :
				_extraBeanProperties.entrySet()) {

			properties.put(entry.getKey(), entry.getValue());
		}
	}

	protected Filter createFilter(
			BundleContext bundleContext, String beanName,
			List<Class<?>> interfaces)
		throws AKBeansException {

		StringBuffer sb = new StringBuffer(interfaces.size() * 5 + 10);

		sb.append("(&");

		if (!isStrictMatching()) {
			sb.append("(|");
		}

		for (Class<?> clazz : interfaces) {
			sb.append('(');
			sb.append(Constants.OBJECTCLASS);
			sb.append('=');
			sb.append(clazz.getName());
			sb.append(')');
		}

		if (!isStrictMatching()) {
			sb.append(')');
		}

		sb.append('(');
		sb.append(AKConstants.BEAN_NAME);
		sb.append('=');
		sb.append(beanName);
		sb.append(")(!(");
		sb.append(AKConstants.ORIGINAL_BEAN);
		sb.append("=*)))");

		try {
			return bundleContext.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException e) {
			throw new AKBeansException(e.getMessage(), e);
		}
	}

	protected Object createProxy(
		BundleContext bundleContext, Object bean, String beanName,
		List<Class<?>> interfaces) throws BeansException {

		Filter filter = createFilter(bundleContext, beanName, interfaces);

		AKServiceTrackerInvocationHandler serviceTrackerInvocationHandler =
			new AKServiceTrackerInvocationHandler(
				bundleContext, filter, bean);

		serviceTrackerInvocationHandler.open();

		try {
			return _proxyFactoryMethod.invoke(
				null, getClassLoader(),
				interfaces.toArray(new Class<?>[interfaces.size()]),
				serviceTrackerInvocationHandler);
		}
		catch (Exception e) {
			throw new AKBeansException(e.getMessage(), e);
		}
	}

	protected boolean ignoreBean(
		Object bean, String beanName, List<Class<?>> interfaces) {

		if (interfaces.isEmpty() ||
			ignoreBeanByBeanName(beanName) ||
			ignoreBeanByClassName(bean.getClass().getName())) {

			return true;
		}

		return false;
	}

	protected boolean ignoreBeanByBeanName(String beanName) {
		if (_ignoredBeanNames == null) {
			return false;
		}

		for (String ignoredBeanName : _ignoredBeanNames) {
			if (beanName.equals(ignoredBeanName)) {
				return true;
			}
			else if (ignoredBeanName.startsWith(AKConstants.STAR) &&
					 beanName.endsWith(ignoredBeanName.substring(1))) {

				return true;
			}
			else if (ignoredBeanName.endsWith(AKConstants.STAR) &&
					 beanName.startsWith(
						 ignoredBeanName.substring(
							 0, ignoredBeanName.length() - 1))) {

				return true;
			}
		}

		return false;
	}

	protected boolean ignoreBeanByClassName(String className) {
		if (_ignoredClassNames == null) {
			return false;
		}

		for (String ignoredClassName : _ignoredClassNames) {
			if (className.equals(ignoredClassName)) {
				return true;
			}
			else if (ignoredClassName.startsWith(AKConstants.STAR) &&
					 className.endsWith(ignoredClassName.substring(1))) {

				return true;
			}
			else if (ignoredClassName.endsWith(AKConstants.STAR) &&
					 className.startsWith(
						 ignoredClassName.substring(
							 0, ignoredClassName.length() - 1))) {

				return true;
			}
		}

		return false;
	}

	protected void registerService(
		BundleContext bundleContext, Object bean, String beanName,
		List<Class<?>> interfaces) {

		List<String> names = new ArrayList<String>();

		for (Class<?> interfaceClass : interfaces) {
			names.add(interfaceClass.getName());
		}

		Hashtable<String,Object> properties = new Hashtable<String, Object>();

		properties.put(AKConstants.BEAN_NAME, beanName);
		properties.put(AKConstants.ORIGINAL_BEAN, Boolean.TRUE);

		addExtraBeanProperties(properties);

		bundleContext.registerService(
			names.toArray(new String[names.size()]), bean, properties);
	}

	private Class<?> _proxyFactory;
	private ClassLoader _classLoader;
	private Map<String, Object> _extraBeanProperties;
	private Method _proxyFactoryMethod;
	private Framework _framework;
	private List<String> _ignoredBeanNames;
	private List<String> _ignoredClassNames;
	private int _order = 20;
	private boolean _strictMatching = false;

}
