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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.compression.CompressionOptions;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentCompressor;
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
		if (server == null) {
			return;
		}
		server.cancel(true);
	}

	@Override
	public void start() throws WebServerException {
		EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.option(ChannelOption.SO_BACKLOG, 1024);
			b.group(group)
					.channel(NioServerSocketChannel.class)
					.childHandler(new MyServerInitializer(this.servletContext));

			this.server = b.bind(this.port).sync();

			logger.info("Server started on port: " + getPort());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			group.shutdownGracefully();
			throw new WebServerException("Cannot start server", e);
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
			p.addLast(new HttpContentCompressor((CompressionOptions[]) null));
			p.addLast(new HttpServerExpectContinueHandler());
			p.addLast(new MyServerHandler(this.servletContext));
		}
	}

	static class MyServerHandler extends SimpleChannelInboundHandler<HttpObject> {

		private HttpRequest request;
		private DispatcherHttpServletRequest servletRequest;
		private DispatcherHttpServletResponse servletResponse;
		private DispatcherServletContext servletContext;

		public MyServerHandler(DispatcherServletContext servletContext) {
			this.servletContext = servletContext;
			this.servletRequest = new DispatcherHttpServletRequest(servletContext);
			this.servletResponse = new DispatcherHttpServletResponse();
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			ctx.flush();
		}

		@Override
		public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
			if (msg instanceof HttpRequest) {
				HttpRequest request = this.request = (HttpRequest) msg;
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
					this.servletRequest = new DispatcherHttpServletRequest(servletContext);
					this.servletResponse = new DispatcherHttpServletResponse();
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
			ctx.write(response);
		}

		private void writeResponse(ChannelHandlerContext ctx, LastHttpContent trailer,
				DispatcherHttpServletResponse servletResponse) {
			boolean keepAlive = HttpUtil.isKeepAlive(request);
			byte[] bytes = servletResponse.getContentAsByteArray();
			HttpResponseStatus status = HttpResponseStatus.valueOf(servletResponse.getStatus());

			FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
					Unpooled.copiedBuffer(bytes));
			httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length + "");

			httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, servletResponse.getContentType());

			if (keepAlive) {
				httpResponse.headers().setInt(HttpHeaderNames.CONTENT_LENGTH,
						httpResponse.content().readableBytes());
				httpResponse.headers().set(HttpHeaderNames.CONNECTION,
						HttpHeaderValues.KEEP_ALIVE);
			}
			ctx.write(httpResponse);

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