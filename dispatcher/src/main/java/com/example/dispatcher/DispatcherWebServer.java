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
import java.lang.management.ManagementFactory;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;

/**
 * @author Dave Syer
 *
 */
class DispatcherWebServer implements WebServer {

	static Log logger = LogFactory.getLog(DispatcherWebServer.class);

	private DisposableServer disposable;

	private DispatcherServletContext servletContext = new DispatcherServletContext();

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
		if (disposable != null) {
			disposable.dispose();
		}
	}

	@Override
	public void start() throws WebServerException {
		Thread thread = new Thread(() -> {
			HttpServer created = HttpServer.create();
			if (port > 0) {
				created = created.port(port);
			}
			disposable = created.route(routes -> routes.route(request -> true,
					(request, response) -> response.sendByteArray(publish(request)))).bindNow();
			logger.info("Running on port: " + disposable.port());
			logger.info("Class count: " + ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
			disposable.onDispose().block();
			disposable = null;
		});
		thread.setName("server");
		thread.setDaemon(false);
		thread.start();
	}

	private Publisher<byte[]> publish(HttpServerRequest nettyRequest) {
		DispatcherHttpServletRequest request = new DispatcherHttpServletRequest(servletContext);
		request.setMethod(nettyRequest.method().name());
		DispatcherHttpServletResponse response = new DispatcherHttpServletResponse();
		try {
			servletContext.filterChain().doFilter(request, response);
		}
		catch (IOException | ServletException e) {
			throw new IllegalStateException("Failed", e);
		}
		return Mono.just(response.getContentAsByteArray());
	}

	@Override
	public int getPort() {
		if (port > 0) {
			return port;
		}
		int count = 0;
		while (disposable == null && count++ < 10) {
			try {
				Thread.sleep(20L);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (disposable != null) {
			return disposable.port();
		}
		return port;
	}

}