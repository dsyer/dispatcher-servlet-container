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
package com.example.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.netty.NettyWebServerFactory;
import com.example.reactor.ReactorWebServerFactory;
import com.example.standard.DispatcherWebServerFactory;

import reactor.netty.http.server.HttpServer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServerProperties.class)
@ConditionalOnMissingBean(WebServerFactory.class)
public class DispatcherWebServerAutoConfiguration {

	private static Log logger = LogFactory.getLog(DispatcherWebServerAutoConfiguration.class);

	@Bean
	@ConditionalOnClass(HttpServer.class)
	public ConfigurableServletWebServerFactory reactorWebServerFactory(ServerProperties server) {
		logger.info("Creating ReactorWebServerFactory");
		return new ReactorWebServerFactory(server);
	}

	@Bean
	@ConditionalOnMissingClass("reactor.netty.http.server.HttpServer")
	@ConditionalOnClass(name = "io.netty.bootstrap.ServerBootstrap")
	public ConfigurableServletWebServerFactory nettyWebServerFactory(ServerProperties server) {
		logger.info("Creating NettyWebServerFactory");
		return new NettyWebServerFactory(server);
	}

	@Bean
	@ConditionalOnMissingClass("io.netty.bootstrap.ServerBootstrap")
	public ConfigurableServletWebServerFactory defaultWebServerFactory(ServerProperties server) {
		logger.info("Creating DispatcherWebServerFactory");
		return new DispatcherWebServerFactory(server);
	}

}