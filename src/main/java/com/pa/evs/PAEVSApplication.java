package com.pa.evs;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(
		exclude = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class}
)
public class PAEVSApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder().sources(PAEVSApplication.class).initializers(com.pa.evs.utils.AppProps::load).run(args);
	}

}
