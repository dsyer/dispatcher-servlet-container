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
package com.example.netty;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import com.example.dispatcher.DispatcherHttpServletRequest;
import com.example.dispatcher.DispatcherHttpServletResponse;
import com.example.dispatcher.DispatcherServletContext;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import jakarta.servlet.ServletException;

public class NettyWebServer implements WebServer {

	static Log logger = LogFactory.getLog(NettyWebServer.class);

	private DispatcherServletContext servletContext = new DispatcherServletContext();

	private ChannelFuture server = null;

	private EventLoopGroup group = null;

	private int port;

	public NettyWebServer(int port, ServletContextInitializer[] initializers) {
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
		if (this.server != null) {
			this.server.cancel(true);
			this.server = null;
		}
		if (this.group != null) {
			this.group.shutdownGracefully();
			this.group = null;
		}
	}

	@Override
	public void start() throws WebServerException {
		Transport transport = Transport.detect();
		this.group = new MultiThreadIoEventLoopGroup(transport.ioHandlerFactory());
		try {
			logger.info("Using Netty transport: " + transport.name());
			ServerBootstrap b = new ServerBootstrap();
			b.option(ChannelOption.SO_BACKLOG, 1024);
			b.group(this.group)
					.channel(transport.serverChannelClass())
					.childHandler(new MyServerInitializer(this.servletContext));

			this.server = b.bind(this.port).sync();

			logger.info("Server started on port: " + getPort());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			if (this.group != null) {
				this.group.shutdownGracefully();
				this.group = null;
			}
			throw new WebServerException("Cannot start server", e);
		} catch (RuntimeException e) {
			if (this.group != null) {
				this.group.shutdownGracefully();
				this.group = null;
			}
			throw e;
		} finally {
		}
	}

	static class MyServerInitializer extends ChannelInitializer<SocketChannel> {

		private final DispatcherServletContext servletContext;

		public MyServerInitializer(DispatcherServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public void initChannel(SocketChannel ch) {
			ChannelPipeline p = ch.pipeline();
			p.addLast(new HttpServerCodec());
			p.addLast(new HttpServerExpectContinueHandler());
			p.addLast(new MyServerHandler(this.servletContext));
		}
	}

	static class MyServerHandler extends SimpleChannelInboundHandler<HttpObject> {

		private HttpRequest request;
		private io.netty.handler.codec.http.HttpHeaders responseHeaders = new DefaultHttpHeaders();
		private DispatcherHttpServletRequest servletRequest;
		private DispatcherHttpServletResponse servletResponse;
		private DispatcherServletContext servletContext;

		public MyServerHandler(DispatcherServletContext servletContext) {
			this.servletContext = servletContext;
			this.servletRequest = new DispatcherHttpServletRequest(servletContext);
			this.servletResponse = new DispatcherHttpServletResponse();
			this.servletResponse.setHeaders(new NettyHeadersAdapter(this.responseHeaders));
		}

		@Override
		public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
			if (msg instanceof HttpRequest) {
				HttpRequest request = this.request = (HttpRequest) msg;
				this.servletRequest.setHeaders(new NettyHeadersAdapter(request.headers()));
				this.servletRequest.setMethod(request.method().name());
				this.servletRequest.setRequestURI(request.uri());

				if (HttpUtil.is100ContinueExpected(request)) {
					writeResponse(ctx);
				}
				this.servletRequest.setParameters(RequestUtils.formatParams(request));
			}

			if (msg instanceof HttpContent) {
				HttpContent httpContent = (HttpContent) msg;
				this.servletRequest.setContent(RequestUtils.formatBody(httpContent));

				if (msg instanceof LastHttpContent) {
					LastHttpContent trailer = (LastHttpContent) msg;
					transfer(servletRequest, servletResponse);
					writeResponse(ctx, trailer, this.servletResponse);
					this.responseHeaders = new DefaultHttpHeaders();
					this.servletRequest = new DispatcherHttpServletRequest(servletContext);
					this.servletResponse = new DispatcherHttpServletResponse();
					this.servletResponse.setHeaders(new NettyHeadersAdapter(this.responseHeaders));
				}
			}
		}

		private void transfer(DispatcherHttpServletRequest servletRequest,
				DispatcherHttpServletResponse servletResponse) {
			try {
				servletContext.filterChain().doFilter(servletRequest, servletResponse);
			} catch (IOException | ServletException e) {
				throw new IllegalStateException("Failed", e);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			cause.printStackTrace();
			ctx.close();
		}

		private void writeResponse(ChannelHandlerContext ctx) {
			FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE,
					Unpooled.EMPTY_BUFFER);
			ctx.writeAndFlush(response);
		}

		private void writeResponse(ChannelHandlerContext ctx, LastHttpContent trailer,
				DispatcherHttpServletResponse servletResponse) {
			boolean keepAlive = HttpUtil.isKeepAlive(request);
			byte[] bytes = servletResponse.getContentAsByteArray();
			HttpResponseStatus status = HttpResponseStatus.valueOf(servletResponse.getStatus());

			FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
					Unpooled.copiedBuffer(bytes));
			httpResponse.headers().set(this.responseHeaders);
			if (!httpResponse.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
				httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length + "");
			}

			if (keepAlive) {
				if (!httpResponse.headers().contains(HttpHeaderNames.CONNECTION)) {
					httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
				}
			}
			ctx.writeAndFlush(httpResponse);

			if (!keepAlive) {
				ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
			}
		}
	}

	@Override
	public int getPort() {
		return server == null ? this.port : ((InetSocketAddress) server.channel().localAddress()).getPort();
	}

}

record Transport(String name, IoHandlerFactory ioHandlerFactory, Class<? extends ServerChannel> serverChannelClass) {

	static Transport detect() {
		Transport epoll = tryNative("epoll", "io.netty.channel.epoll.Epoll", "io.netty.channel.epoll.EpollIoHandler",
				"io.netty.channel.epoll.EpollServerSocketChannel");
		if (epoll != null) {
			return epoll;
		}

		Transport kqueue = tryNative("kqueue", "io.netty.channel.kqueue.KQueue",
				"io.netty.channel.kqueue.KQueueIoHandler", "io.netty.channel.kqueue.KQueueServerSocketChannel");
		if (kqueue != null) {
			return kqueue;
		}

		return new Transport("nio", NioIoHandler.newFactory(), NioServerSocketChannel.class);
	}

	private static Transport tryNative(String name, String detectorClassName, String ioHandlerClassName,
			String channelClassName) {
		try {
			ClassLoader classLoader = NettyWebServer.class.getClassLoader();
			Class<?> detectorClass = Class.forName(detectorClassName, false, classLoader);
			Method isAvailable = detectorClass.getMethod("isAvailable");
			Object available = isAvailable.invoke(null);
			if (!Boolean.TRUE.equals(available)) {
				return null;
			}

			Class<?> ioHandlerClass = Class.forName(ioHandlerClassName, false, classLoader);
			Method newFactory = ioHandlerClass.getMethod("newFactory");
			IoHandlerFactory ioHandlerFactory = (IoHandlerFactory) newFactory.invoke(null);

			@SuppressWarnings("unchecked")
			Class<? extends ServerChannel> channelClass = (Class<? extends ServerChannel>) Class
					.forName(channelClassName, false, classLoader);

			return new Transport(name, ioHandlerFactory, channelClass);
		} catch (Throwable ex) {
			return null;
		}
	}

}

class RequestUtils {
	static Map<String, String[]> formatParams(HttpRequest request) {
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
		Map<String, List<String>> params = queryStringDecoder.parameters();
		Map<String, String[]> result = new java.util.HashMap<>();
		if (!params.isEmpty()) {
			for (Entry<String, List<String>> p : params.entrySet()) {
				String key = p.getKey();
				List<String> vals = p.getValue();
				result.put(key, vals.toArray(new String[0]));
			}
		}
		return result;
	}

	static byte[] formatBody(HttpContent httpContent) {
		return httpContent.content().toString(CharsetUtil.UTF_8).getBytes();
	}
}