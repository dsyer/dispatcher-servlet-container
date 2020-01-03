package com.example.app;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient
class DispatcherApplicationTests {

	private WebClient client;

	@LocalServerPort
	private int port;

	@Autowired
	DispatcherApplicationTests(WebClient.Builder builder) {
		this.client = builder.build();
	}

	@Test
	void home() {
		String value = client.get().uri("http://localhost:" + port + "/").retrieve().bodyToMono(String.class).block();
		assertThat(value).isEqualTo("Home");
	}

	@Test
	void post() {
		String value = client.post().uri("http://localhost:" + port + "/").bodyValue("World").retrieve().bodyToMono(String.class).block();
		assertThat(value).isEqualTo("Hello World");
	}

	@Test
	void hello() {
		String value = client.get().uri("http://localhost:" + port + "/hello").retrieve().bodyToMono(String.class).block();
		assertThat(value).isEqualTo("Hello");
	}

}
