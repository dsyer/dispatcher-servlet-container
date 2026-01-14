/*
 * Copyright 2019-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.dispatcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;

/**
 * @author Dave Syer
 *
 */
public class DispatcherServletContext implements ServletContext {

	private static Log logger = LogFactory.getLog(DispatcherServletContext.class);

	private DispatcherServletRegistration servlet;

	private Map<String, DispatcherFilterRegistration> filters = new LinkedHashMap<String, DispatcherFilterRegistration>();

	public DispatcherFilterChain filterChain() {
		return new DispatcherFilterChain(this.servlet.getServlet(), filters.values().stream()
				.map(dynamic -> dynamic.getFilter()).collect(Collectors.toList()).toArray(new Filter[0]));
	}

	@Override
	public String getContextPath() {
		return "/";
	}

	@Override
	public ServletContext getContext(String uripath) {
		return this;
	}

	@Override
	public int getMajorVersion() {
		return 3;
	}

	@Override
	public int getMinorVersion() {
		return 1;
	}

	@Override
	public int getEffectiveMajorVersion() {
		return 3;
	}

	@Override
	public int getEffectiveMinorVersion() {
		return 1;
	}

	@Override
	public String getMimeType(String file) {
		return null;
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		try {
			Set<String> paths = new HashSet<>();
			for (Resource resource : new PathMatchingResourcePatternResolver()
					.getResources("classpath:" + path + "*")) {
				paths.add(resource.getFilename());
			}
			return paths;
		}
		catch (IOException e) {
			return Collections.emptySet();
		}
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		ClassPathResource resource = new ClassPathResource(getResourceLocation(path));
		if (resource.exists()) {
			try {
				return resource.getURL();
			}
			catch (IOException e) {
				return null;
			}
		}
		else {
			return null;
		}
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		ClassPathResource resource = new ClassPathResource(getResourceLocation(path));
		if (resource.exists()) {
			try {
				return resource.getInputStream();
			}
			catch (IOException e) {
				return null;
			}
		}
		else {
			return null;
		}
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return null;
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String name) {
		return null;
	}

	@Override
	public void log(String msg) {
		logger.info(msg);
	}

	@Override
	public void log(String message, Throwable throwable) {
		logger.error(message, throwable);
	}

	@Override
	public String getRealPath(String path) {
		return null;
	}

	@Override
	public String getServerInfo() {
		return "Dispatcher";
	}

	@Override
	public String getInitParameter(String name) {
		return null;
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.emptyEnumeration();
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		return false;
	}

	@Override
	public Object getAttribute(String name) {
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.emptyEnumeration();
	}

	@Override
	public void setAttribute(String name, Object object) {
	}

	@Override
	public void removeAttribute(String name) {
	}

	@Override
	public String getServletContextName() {
		return "dispatcher";
	}

	@Override
	public Dynamic addServlet(String servletName, String className) {
		return addServlet(servletName,
				(Servlet) BeanUtils.instantiateClass(ClassUtils.resolveClassName(className, null)));
	}

	@Override
	public Dynamic addServlet(String servletName, Servlet servlet) {
		if (this.servlet == null) {
			this.servlet = new DispatcherServletRegistration(servletName, servlet, this);
			return this.servlet;
		}
		return null;
	}

	@Override
	public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
		return addServlet(servletName, BeanUtils.instantiateClass(servletClass));
	}

	@Override
	public Dynamic addJspFile(String servletName, String jspFile) {
		return null;
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
		Dynamic added = addServlet(clazz.getName(), clazz);
		if (added != null) {
			@SuppressWarnings("unchecked")
			T result = (T) this.servlet.getServlet();
			return result;
		}
		return null;
	}

	@Override
	public ServletRegistration getServletRegistration(String servletName) {
		return this.servlet;
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return this.servlet != null ? Collections.singletonMap(this.servlet.getName(), this.servlet)
				: Collections.emptyMap();
	}

	@Override
	public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
		return addFilter(filterName, (Filter) BeanUtils.instantiateClass(ClassUtils.resolveClassName(className, null)));
	}

	@Override
	public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
		if (this.filters.containsKey(filterName)) {
			return null;
		}
		this.filters.put(filterName, new DispatcherFilterRegistration(filterName, filter));
		return filters.get(filterName);
	}

	@Override
	public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
		return addFilter(filterName, BeanUtils.instantiateClass(filterClass));
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
		jakarta.servlet.FilterRegistration.Dynamic added = addFilter(clazz.getName(), clazz);
		if (added != null) {
			@SuppressWarnings("unchecked")
			T result = (T) this.filters.get(clazz.getName()).getFilter();
			return result;
		}
		return null;
	}

	@Override
	public FilterRegistration getFilterRegistration(String filterName) {
		return this.filters.get(filterName);
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return this.filters;
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		return null;
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return null;
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return null;
	}

	@Override
	public void addListener(String className) {
	}

	@Override
	public <T extends EventListener> void addListener(T t) {
	}

	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
		return null;
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		return null;
	}

	@Override
	public ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}

	@Override
	public void declareRoles(String... roleNames) {
	}

	@Override
	public String getVirtualServerName() {
		return null;
	}

	@Override
	public int getSessionTimeout() {
		return 0;
	}

	@Override
	public void setSessionTimeout(int sessionTimeout) {
	}

	@Override
	public String getRequestCharacterEncoding() {
		return null;
	}

	@Override
	public void setRequestCharacterEncoding(String encoding) {
	}

	@Override
	public String getResponseCharacterEncoding() {
		return null;
	}

	@Override
	public void setResponseCharacterEncoding(String encoding) {
	}

	protected String getResourceLocation(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return path;
	}

}
