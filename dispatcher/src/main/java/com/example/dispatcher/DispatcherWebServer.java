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
import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.servlet.ServletException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;

/**
 * @author Dave Syer
 *
 */
class DispatcherWebServer implements WebServer {

	static Log logger = LogFactory.getLog(DispatcherWebServer.class);

	private DispatcherServletContext servletContext = new DispatcherServletContext();

	private HttpServer server = null;

	private int port;

	public DispatcherWebServer(int port, ServletContextInitializer[] initializers) {
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
		server.stop(1);
	}

	@Override
	public void start() throws WebServerException {
		Thread thread = new Thread(() -> {
			try {
				server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			server.createContext("/", new MyHandler(servletContext));
			server.setExecutor(null); // creates a default executor
			server.start();
		});
		thread.setName("server");
		thread.setDaemon(false);
		thread.start();
	}

	static class MyHandler implements HttpHandler {

		private final DispatcherServletContext servletContext;

		public MyHandler(DispatcherServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			DispatcherHttpServletRequest request = new DispatcherHttpServletRequest(servletContext);
            request.setMethod(t.getRequestMethod());
            request.setRequestURI(t.getRequestURI().toString());
			DispatcherHttpServletResponse response = new DispatcherHttpServletResponse();
			try {
				servletContext.filterChain().doFilter(request, response);
			}
			catch (IOException | ServletException e) {
				throw new IllegalStateException("Failed", e);
			}

			t.sendResponseHeaders(200, response.getContentLength());
			OutputStream os = t.getResponseBody();
			os.write(response.getContentAsByteArray());
			os.close();
		}
	}

	@Override
	public int getPort() {
		return port;
	}

}