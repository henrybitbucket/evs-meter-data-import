package com.pa.evs.ctrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.sv.DMSLockService;
import com.pa.evs.utils.ApiUtils;

import springfox.documentation.annotations.ApiIgnore;


@DependsOn(value = "settingsController")
@RestController
@ApiIgnore
public class AppPASController {
	static final Logger LOGGER = LoggerFactory.getLogger(AppPASController.class);
	
	static final ObjectMapper MAPPER = new ObjectMapper();
	String token;
	RestTemplate resttemplate = ApiUtils.getRestTemplate();
	
	@Autowired
	DMSLockService dmsLockService;
	
	@GetMapping("/api/pas_locks")
	public Object search() {
		return dmsLockService.search();
	}
	
	@PostMapping("/api/sync_lock/{vendorId}")
	public Object syncLock(@PathVariable Long vendorId) {
		return dmsLockService.syncLock(vendorId);
	}
	
}
