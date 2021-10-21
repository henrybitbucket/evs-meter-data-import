package com.pa.evs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PAEVSApplication {

	public static void main(String[] args) {
		SpringApplication.run(PAEVSApplication.class, args);
	}

}
