/*
 * Copyright 2025-current the original author or authors.
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
package com.example.coyote;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.coyote.Adapter;
import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.ApplicationBufferHandler;
import org.apache.tomcat.util.net.SocketEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import com.example.dispatcher.DispatcherHttpServletRequest;
import com.example.dispatcher.DispatcherHttpServletResponse;
import com.example.dispatcher.DispatcherServletContext;

import jakarta.servlet.ServletException;

public class CoyoteWebServer implements WebServer {

	static Log logger = LogFactory.getLog(CoyoteWebServer.class);

	private DispatcherServletContext servletContext = new DispatcherServletContext();

	private Http11NioProtocol server;

	private int port;

	public CoyoteWebServer(int port, ServletContextInitializer[] initializers) {
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
		if (server == null) {
			return;
		}
		try {
			server.stop();
		} catch (Exception e) {
			throw new WebServerException("Cannot stop server", e);
		}
	}

	@Override
	public void start() throws WebServerException {
		try {
			this.server = new Http11NioProtocol();
			this.server.setPort(getPort());
			this.server.setAdapter(new MyServerHandler(this.servletContext));
			this.server.init();
			this.server.start();
			logger.info("Server started on port: " + getPort());
		} catch (Exception e) {
			throw new WebServerException("Cannot start server", e);
		} finally {
		}
	}

	static class MyServerHandler implements Adapter {

		private DispatcherServletContext servletContext;

		public MyServerHandler(DispatcherServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public void service(Request req, Response res) throws Exception {
			// Create new request/response objects for each request
			DispatcherHttpServletRequest servletRequest = new DispatcherHttpServletRequest(servletContext);
			DispatcherHttpServletResponse servletResponse = new DispatcherHttpServletResponse();
			
			// Extract request data from Coyote Request
			servletRequest.setMethod(req.getMethod().toString());
			servletRequest.setRequestURI(req.requestURI().toString());
			
			// Copy request headers
			int headerCount = req.getMimeHeaders().size();
			for (int i = 0; i < headerCount; i++) {
				String name = req.getMimeHeaders().getName(i).toString();
				String value = req.getMimeHeaders().getValue(i).toString();
				servletRequest.addHeader(name, value);
			}
			
			// Copy request body if present
			int contentLength = req.getContentLength();
			if (contentLength > 0) {
				ByteArrayOutputStream bodyStream = new ByteArrayOutputStream(contentLength);
				
				// Create a buffer handler to receive data from the input buffer
				ApplicationBufferHandler bufferHandler = new ApplicationBufferHandler() {
					private ByteBuffer byteBuffer;
					
					@Override
					public void setByteBuffer(ByteBuffer buffer) {
						this.byteBuffer = buffer;
					}
					
					@Override
					public ByteBuffer getByteBuffer() {
						return byteBuffer;
					}
					
					@Override
					public void expand(int size) {
						// Expand the buffer if needed
						if (byteBuffer == null) {
							byteBuffer = ByteBuffer.allocate(size);
						} else if (byteBuffer.capacity() < size) {
							ByteBuffer newBuffer = ByteBuffer.allocate(size);
							byteBuffer.flip();
							newBuffer.put(byteBuffer);
							byteBuffer = newBuffer;
						}
					}
				};
				
				// Read all available data from the input buffer
				int bytesRead;
				while ((bytesRead = req.doRead(bufferHandler)) >= 0) {
					ByteBuffer buffer = bufferHandler.getByteBuffer();
					if (buffer != null && buffer.hasRemaining()) {
						byte[] bytes = new byte[buffer.remaining()];
						buffer.get(bytes);
						bodyStream.write(bytes);
					}
					if (bytesRead == 0) {
						break;
					}
				}
				
				servletRequest.setContent(bodyStream.toByteArray());
			}
			
			// Process through servlet filter chain
			try {
				servletContext.filterChain().doFilter(servletRequest, servletResponse);
			} catch (Exception e) {
				logger.error("Error processing request", e);
				res.setStatus(500);
				res.setMessage("Internal Server Error");
				return;
			}
			
			// Copy response data back to Coyote Response
			res.setStatus(servletResponse.getStatus());
			
			// Copy response headers
			for (String headerName : servletResponse.getHeaderNames()) {
				for (String headerValue : servletResponse.getHeaders(headerName)) {
					res.getMimeHeaders().addValue(headerName).setString(headerValue);
				}
			}
			
			// Write response body
			byte[] responseBody = servletResponse.getContentAsByteArray();
			if (responseBody.length > 0) {
				res.setContentLength(responseBody.length);
				res.doWrite(ByteBuffer.wrap(responseBody));
			}
		}

		@Override
		public boolean prepare(Request req, Response res) throws Exception {
			// Prepare is called before service - just return true to continue
			return true;
		}

		@Override
		public boolean asyncDispatch(Request req, Response res, SocketEvent status) throws Exception {
			// Not supporting async operations for now
			return false;
		}

		@Override
		public void log(Request req, Response res, long time) {
			CoyoteWebServer.logger.info("Request processed in " + time + " ms");
		}

		@Override
		public void checkRecycled(Request req, Response res) {
			// No-op for now
		}

		@Override
		public String getDomain() {
			return "Coyote";
		}
	}

	@Override
	public int getPort() {
		return this.server == null || this.server.getLocalPort() < 0 ? this.port : this.server.getLocalPort();
	}

}
