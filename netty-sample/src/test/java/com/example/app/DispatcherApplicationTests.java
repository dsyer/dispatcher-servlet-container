package com.example.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class DispatcherApplicationTests {
	
	@Autowired
	private RestTestClient client;

	@LocalServerPort
	private int port;

	public static void main(String[] args) {
		DispatcherApplication.main(args);
	}

	@Test
	void home() {
		String value = client.get().uri("http://localhost:" + port + "/").exchange().expectBody(String.class).returnResult().getResponseBody();
		assertThat(value).isEqualTo("Home");
	}

	@Test
	void post() {
		String value = client.post().uri("http://localhost:" + port + "/").body("World").exchange().expectBody(String.class).returnResult().getResponseBody();
		assertThat(value).isEqualTo("Hello World");
	}

	@Test
	void hello() {
		String value = client.get().uri("http://localhost:" + port + "/hello").exchange().expectBody(String.class).returnResult().getResponseBody();
		assertThat(value).isEqualTo("Hello");
	}

}
