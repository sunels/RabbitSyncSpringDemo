package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class UserApplication {

	public static final String INSTANCE_IDENTITY = UUID.randomUUID().toString().replaceAll("-", "");

	public static void main(String[] args) {
		SpringApplication.run(UserApplication.class, args);
	}

}
