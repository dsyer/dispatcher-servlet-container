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

import java.io.File;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.Jsp;
import org.springframework.boot.web.servlet.server.Session;

/**
 * @author Dave Syer
 *
 */
class DispatcherWebServerFactory implements ConfigurableServletWebServerFactory {

	private ServerProperties server;

	private List<ServletContextInitializer> initializers = new ArrayList<>();

	public DispatcherWebServerFactory(ServerProperties server) {
		this.server = server;
	}

	@Override
	public void setPort(int port) {
	}

	@Override
	public void setAddress(InetAddress address) {
	}

	@Override
	public void setErrorPages(Set<? extends ErrorPage> errorPages) {
	}

	@Override
	public void setSsl(Ssl ssl) {
	}

	@Override
	public void setSslStoreProvider(SslStoreProvider sslStoreProvider) {
	}

	@Override
	public void setHttp2(Http2 http2) {
	}

	@Override
	public void setCompression(Compression compression) {
	}

	@Override
	public void setServerHeader(String serverHeader) {
	}

	@Override
	public void addErrorPages(ErrorPage... errorPages) {
	}

	@Override
	public WebServer getWebServer(ServletContextInitializer... initializers) {
		return new DispatcherWebServer(server.getPort() != null ? server.getPort() : 8080, initializers);
	}

	@Override
	public void setContextPath(String contextPath) {
	}

	@Override
	public void setDisplayName(String displayName) {
	}

	@Override
	public void setSession(Session session) {
	}

	@Override
	public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
	}

	@Override
	public void setMimeMappings(MimeMappings mimeMappings) {
	}

	@Override
	public void setDocumentRoot(File documentRoot) {
	}

	@Override
	public void setInitializers(List<? extends ServletContextInitializer> initializers) {
		this.initializers = new ArrayList<>(initializers);
	}

	@Override
	public void addInitializers(ServletContextInitializer... initializers) {
		for (ServletContextInitializer initializer : initializers) {
			this.initializers.add(initializer);
		}
	}

	@Override
	public void setJsp(Jsp jsp) {
	}

	@Override
	public void setLocaleCharsetMappings(Map<Locale, Charset> localeCharsetMappings) {
	}

	@Override
	public void setInitParameters(Map<String, String> initParameters) {
	}

}
