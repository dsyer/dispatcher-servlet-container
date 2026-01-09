/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class DispatcherFilterChain implements FilterChain {

	@Nullable
	private ServletRequest request;

	@Nullable
	private ServletResponse response;

	private final List<Filter> filters;

	@Nullable
	private Iterator<Filter> iterator;

	public DispatcherFilterChain() {
		this.filters = Collections.emptyList();
	}

	public DispatcherFilterChain(Servlet servlet) {
		this.filters = initFilterList(servlet);
	}

	public DispatcherFilterChain(Servlet servlet, Filter... filters) {
		Assert.notNull(filters, "filters cannot be null");
		Assert.noNullElements(filters, "filters cannot contain null values");
		this.filters = initFilterList(servlet, filters);
	}

	private static List<Filter> initFilterList(Servlet servlet, Filter... filters) {
		Filter[] allFilters = ObjectUtils.addObjectToArray(filters, new ServletFilterProxy(servlet));
		return Arrays.asList(allFilters);
	}

	@Nullable
	public ServletRequest getRequest() {
		return this.request;
	}

	@Nullable
	public ServletResponse getResponse() {
		return this.response;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		Assert.state(this.request == null, "This FilterChain has already been called!");

		if (this.iterator == null) {
			this.iterator = this.filters.iterator();
		}

		if (this.iterator.hasNext()) {
			Filter nextFilter = this.iterator.next();
			nextFilter.doFilter(request, response, this);
		}

		this.request = request;
		this.response = response;
	}

	public void reset() {
		this.request = null;
		this.response = null;
		this.iterator = null;
	}

	private static final class ServletFilterProxy implements Filter {

		private final Servlet delegateServlet;

		private ServletFilterProxy(Servlet servlet) {
			Assert.notNull(servlet, "servlet cannot be null");
			this.delegateServlet = servlet;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {

			this.delegateServlet.service(request, response);
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void destroy() {
		}

		@Override
		public String toString() {
			return this.delegateServlet.toString();
		}

	}

}
