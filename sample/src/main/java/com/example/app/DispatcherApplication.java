package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
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

	@GetMapping("/hello")
	public String hello() {
		return "Hello";
	}

}
