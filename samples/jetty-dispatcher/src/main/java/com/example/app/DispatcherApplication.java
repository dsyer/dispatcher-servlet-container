package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(proxyBeanMethods = false)
@RestController
public class DispatcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(DispatcherApplication.class, args);
	}

	@GetMapping("/")
	public String home() {
		return "Home";
	}

	@PostMapping("/")
	public String post(@RequestBody String body) {
		return "Hello " + body;
	}

	@GetMapping("/hello")
	public String hello() {
		return "Hello";
	}

}
