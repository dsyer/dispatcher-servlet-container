/*
 * Copyright 2026-current the original author or authors.
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
package com.example.jetty;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.util.StreamUtils;

import com.example.dispatcher.DispatcherHttpServletRequest;
import com.example.dispatcher.DispatcherHttpServletResponse;
import com.example.dispatcher.DispatcherServletContext;

import jakarta.servlet.ServletException;

class JettyWebServer implements WebServer {

	static Log logger = LogFactory.getLog(JettyWebServer.class);

	private final DispatcherServletContext servletContext = new DispatcherServletContext();

	private org.eclipse.jetty.server.Server server;

	private ServerConnector connector;

	private final int port;

	JettyWebServer(int port, ServletContextInitializer[] initializers) {
		this.port = port;
		try {
			for (ServletContextInitializer initializer : initializers) {
				initializer.onStartup(this.servletContext);
			}
		}
		catch (ServletException e) {
			throw new IllegalStateException("Cannot initialize", e);
		}
	}

	@Override
	public void start() throws WebServerException {
		try {
			this.server = new org.eclipse.jetty.server.Server();
			this.connector = new ServerConnector(this.server);
			this.connector.setPort(this.port);
			this.server.addConnector(this.connector);
			this.server.setHandler(new MyHandler(this.servletContext));
			this.server.start();
			logger.info("Server started on port: " + getPort());
		}
		catch (Exception e) {
			throw new WebServerException("Cannot start server", e);
		}
	}

	@Override
	public void stop() throws WebServerException {
		if (this.server == null) {
			return;
		}
		try {
			this.server.stop();
		}
		catch (Exception e) {
			throw new WebServerException("Cannot stop server", e);
		}
	}

	@Override
	public int getPort() {
		return this.connector == null ? this.port : this.connector.getLocalPort();
	}

	static class MyHandler extends Handler.Abstract {

		private final DispatcherServletContext servletContext;

		MyHandler(DispatcherServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public boolean handle(Request request, Response response, Callback callback) throws Exception {
			DispatcherHttpServletRequest servletRequest = new DispatcherHttpServletRequest(this.servletContext);
			servletRequest.setHeaders(new JettyHeadersAdapter(request.getHeaders()));
			DispatcherHttpServletResponse servletResponse = new DispatcherHttpServletResponse();
			servletResponse.setHeaders(new JettyHeadersAdapter(response.getHeaders()));

			servletRequest.setMethod(request.getMethod());
			servletRequest.setRequestURI(request.getHttpURI().getPath());
			if (request.getHttpURI().getQuery() != null) {
				servletRequest.setQueryString(request.getHttpURI().getQuery());
				servletRequest.setParameters(RequestUtils.formatParams(request));
			}
			servletRequest.setContent(StreamUtils.copyToByteArray(Request.asInputStream(request)));

			transfer(servletRequest, servletResponse);

			response.setStatus(servletResponse.getStatus());
			byte[] body = servletResponse.getContentAsByteArray();
			HttpFields.Mutable headers = response.getHeaders();
			if (!headers.contains(org.springframework.http.HttpHeaders.CONTENT_LENGTH)) {
				headers.put(org.springframework.http.HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));
			}
			response.write(true, ByteBuffer.wrap(body), callback);
			return true;
		}

		private void transfer(DispatcherHttpServletRequest servletRequest,
				DispatcherHttpServletResponse servletResponse) {
			try {
				this.servletContext.filterChain().doFilter(servletRequest, servletResponse);
			}
			catch (IOException | ServletException e) {
				throw new IllegalStateException("Failed", e);
			}
		}

	}

}

class RequestUtils {

	static Map<String, String[]> formatParams(Request request) {
		Map<String, String[]> result = new HashMap<>();
		var fields = Request.extractQueryParameters(request);
		if (!fields.isEmpty()) {
			for (String name : fields.getNames()) {
				result.put(name, fields.getValues(name).toArray(new String[0]));
			}
		}
		return result;
	}

}