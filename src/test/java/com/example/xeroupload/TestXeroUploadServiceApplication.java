package com.example.xeroupload;

import org.springframework.boot.SpringApplication;

public class TestXeroUploadServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(XeroUploadServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
