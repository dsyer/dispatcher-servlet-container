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
package com.example.helidon;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;

import com.example.dispatcher.DispatcherHttpServletRequest;
import com.example.dispatcher.DispatcherHttpServletResponse;
import com.example.dispatcher.DispatcherServletContext;

import io.helidon.common.uri.UriQuery;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.servlet.ServletException;

class HelidonWebServer implements WebServer {

	static Log logger = LogFactory.getLog(HelidonWebServer.class);

	private DispatcherServletContext servletContext = new DispatcherServletContext();

	private io.helidon.webserver.WebServer server;

	private int port;

	public HelidonWebServer(int port, ServletContextInitializer[] initializers) {
		this.port = port;
		try {
			for (ServletContextInitializer initializer : initializers) {
				initializer.onStartup(servletContext);
			}
		}
		catch (ServletException e) {
			throw new IllegalStateException("Cannot initialize", e);
		}
	}

	@Override
	public void stop() throws WebServerException {
		if (this.server == null) {
			return;
		}
		this.server.stop();
	}

	@Override
	public void start() throws WebServerException {
		this.server = io.helidon.webserver.WebServer.builder()
			.port(this.port)
			.routing(routing -> routing.any(new MyHandler(this.servletContext)))
			.build()
			.start();
		logger.info("Server started on port: " + getPort());
	}

	@Override
	public int getPort() {
		return this.server == null ? this.port : this.server.port();
	}

	static class MyHandler implements Handler {

		private final DispatcherServletContext servletContext;

		MyHandler(DispatcherServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public void handle(ServerRequest request, ServerResponse response) throws Exception {
			DispatcherHttpServletRequest servletRequest = new DispatcherHttpServletRequest(servletContext);
			servletRequest.setHeaders(new HelidonHeadersAdapter(request.headers()));
			DispatcherHttpServletResponse servletResponse = new DispatcherHttpServletResponse();

			servletRequest.setMethod(request.prologue().method().text());
			servletRequest.setRequestURI(request.requestedUri().path().path());
			UriQuery query = request.query();
			if (!query.isEmpty()) {
				servletRequest.setQueryString(query.value());
				servletRequest.setParameters(RequestUtils.formatParams(query));
			}

			servletRequest.setContent(StreamUtils.copyToByteArray(request.content().inputStream()));

			transfer(servletRequest, servletResponse);

			response.status(servletResponse.getStatus());
			for (String headerName : servletResponse.getHeaderNames()) {
				for (String value : servletResponse.getHeaders(headerName)) {
					response.header(headerName, value);
				}
			}

			byte[] body = servletResponse.getContentAsByteArray();
			response.header("Content-Length", Integer.toString(body.length));
			response.send(body);
		}

		private void transfer(DispatcherHttpServletRequest servletRequest,
				DispatcherHttpServletResponse servletResponse) {
			try {
				servletContext.filterChain().doFilter(servletRequest, servletResponse);
			}
			catch (IOException | ServletException e) {
				throw new IllegalStateException("Failed", e);
			}
		}

	}

}

class RequestUtils {
	static Map<String, String[]> formatParams(UriQuery query) {
		Map<String, List<String>> params = query.toMap();
		Map<String, String[]> result = new HashMap<>();
		if (!params.isEmpty()) {
			for (Entry<String, List<String>> p : params.entrySet()) {
				result.put(p.getKey(), p.getValue().toArray(new String[0]));
			}
		}
		return result;
	}
}