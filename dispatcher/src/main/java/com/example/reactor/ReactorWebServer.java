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
package com.example.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.function.BiFunction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import com.example.dispatcher.DispatcherHttpServletRequest;
import com.example.dispatcher.DispatcherHttpServletResponse;
import com.example.dispatcher.DispatcherServletContext;

import io.netty.handler.codec.http.HttpHeaderNames;
import jakarta.servlet.ServletException;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * @author Dave Syer
 *
 */
class ReactorWebServer implements WebServer {

	static Log logger = LogFactory.getLog(ReactorWebServer.class);

	private DispatcherServletContext servletContext = new DispatcherServletContext();

	private DisposableServer server = null;

	private int port;

	public ReactorWebServer(int port, ServletContextInitializer[] initializers) {
		this.port = port;
		try {
			for (ServletContextInitializer initializer : initializers) {
				initializer.onStartup(servletContext);
			}
		} catch (ServletException e) {
			throw new IllegalStateException("Cannot initialize", e);
		}
	}

	@Override
	public void stop() throws WebServerException {
		server.disposeNow(Duration.ofSeconds(1));
	}

	@Override
	public void start() throws WebServerException {
		InetSocketAddress address = new InetSocketAddress("0.0.0.0", this.port);
		server = HttpServer.create().bindAddress(() -> address).handle(new MyHandler(this.servletContext)).bindNow();
		logger.info("Server started on port: " + server.port());
		startDaemonAwaitThread(server);
	}

	private void startDaemonAwaitThread(DisposableServer disposableServer) {
		Thread awaitThread = new Thread("server") {

			@Override
			public void run() {
				disposableServer.onDispose().block();
			}

		};
		awaitThread.setContextClassLoader(getClass().getClassLoader());
		awaitThread.setDaemon(false);
		awaitThread.start();
	}

	static class MyHandler implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {

		private final DispatcherServletContext servletContext;

		public MyHandler(DispatcherServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public Mono<Void> apply(HttpServerRequest t, HttpServerResponse r) {
			DispatcherHttpServletRequest request = new DispatcherHttpServletRequest(servletContext);
			request.setMethod(t.method().name());
			request.setRequestURI(t.uri());
			return t.receive().aggregate().asByteArray().defaultIfEmpty(new byte[0])
					.flatMap(content -> Mono.from(transfer(request, r, content)));
		}

		private Publisher<Void> transfer(DispatcherHttpServletRequest request, HttpServerResponse r, byte[] body) {
			DispatcherHttpServletResponse response = new DispatcherHttpServletResponse();
			request.setContent(body);
			try {
				servletContext.filterChain().doFilter(request, response);
			} catch (IOException | ServletException e) {
				throw new IllegalStateException("Failed", e);
			}

			r.status(response.getStatus());
			byte[] bytes = response.getContentAsByteArray();
			r.addHeader(HttpHeaderNames.CONTENT_LENGTH, bytes.length + "");
			return r.sendByteArray(Mono.just(bytes));
		}

	}

	@Override
	public int getPort() {
		return server.port();
	}

}