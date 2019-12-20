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
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;

/**
 * @author Dave Syer
 *
 */
public class DispatcherFilterRegistration implements FilterRegistration, FilterRegistration.Dynamic {

	private String name;

	private Filter filter;

	public DispatcherFilterRegistration(String name, Filter filter) {
		this.name = name;
		this.filter = filter;
	}

	Filter getFilter() {
		return filter;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getClassName() {
		return filter.getClass().getName();
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
		return null;
	}

	@Override
	public void setAsyncSupported(boolean isAsyncSupported) {
	}

	@Override
	public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
			String... servletNames) {
	}

	@Override
	public Collection<String> getServletNameMappings() {
		return null;
	}

	@Override
	public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
			String... urlPatterns) {
	}

	@Override
	public Collection<String> getUrlPatternMappings() {
		return null;
	}

}
