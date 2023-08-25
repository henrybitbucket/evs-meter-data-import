package com.pa.evs;

import java.util.TimeZone;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pa.evs.utils.AppProps;

@SpringBootApplication
@EnableScheduling
public class PAEVSApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		new SpringApplicationBuilder().sources(PAEVSApplication.class).initializers(AppProps::load).run(args);
	}

}
