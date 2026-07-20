package com.mvp.v1.dandionna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DandionnaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DandionnaApplication.class, args);
	}

}
