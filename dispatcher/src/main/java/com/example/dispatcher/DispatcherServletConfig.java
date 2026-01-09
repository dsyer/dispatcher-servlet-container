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

import java.util.Collections;
import java.util.Enumeration;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * @author Dave Syer
 *
 */
public class DispatcherServletConfig implements ServletConfig {

	private ServletContext context;

	private String name;

	public DispatcherServletConfig(String name, ServletContext context) {
		this.name = name;
		this.context = context;
	}

	@Override
	public String getServletName() {
		return this.name;
	}

	@Override
	public ServletContext getServletContext() {
		return this.context;
	}

	@Override
	public String getInitParameter(String name) {
		return null;
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.emptyEnumeration();
	}

}
