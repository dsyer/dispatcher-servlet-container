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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletSecurityElement;

/**
 * @author Dave Syer
 *
 */
public class DispatcherServletRegistration implements ServletRegistration, Dynamic {

	private String name;

	private Servlet servlet;

	private ServletConfig config;

	public DispatcherServletRegistration(String name, Servlet servlet, ServletContext context) {
		this.name = name;
		this.servlet = servlet;
		this.config = new DispatcherServletConfig(name, context);
		try {
			servlet.init(this.config);
		}
		catch (ServletException e) {
			throw new IllegalStateException(e);
		}
	}

	Servlet getServlet() {
		return servlet;
	}

	@Override
	public Set<String> addMapping(String... urlPatterns) {
		return Collections.singleton("/");
	}

	@Override
	public Collection<String> getMappings() {
		return Collections.singleton("/");
	}

	@Override
	public String getRunAsRole() {
		return null;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getClassName() {
		return servlet.getClass().getName();
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		return false;
	}

	@Override
	public String getInitParameter(String name) {
		return null;
	}

	@Override
	public Set<String> setInitParameters(Map<String, String> initParameters) {
		return null;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return Collections.emptyMap();
	}

	@Override
	public void setAsyncSupported(boolean isAsyncSupported) {
	}

	@Override
	public void setLoadOnStartup(int loadOnStartup) {
	}

	@Override
	public Set<String> setServletSecurity(ServletSecurityElement constraint) {
		return Collections.emptySet();
	}

	@Override
	public void setMultipartConfig(MultipartConfigElement multipartConfig) {
	}

	@Override
	public void setRunAsRole(String roleName) {
	}

}
