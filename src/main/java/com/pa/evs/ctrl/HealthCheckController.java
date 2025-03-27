package com.pa.evs.ctrl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;

@RestController
@Getter
public class HealthCheckController {
	boolean maintenance = false;;

	@Value("${service.version:1.0}")
	private String version;

	@GetMapping("/hello")
	public ResponseEntity<String> hello() {
		return ResponseEntity.ok().body("COMTRADE:" + version);
	}

	@GetMapping("/health")
	public String checkHealth() {
		return maintenance ? "DOWN" : "UP";
	}
}