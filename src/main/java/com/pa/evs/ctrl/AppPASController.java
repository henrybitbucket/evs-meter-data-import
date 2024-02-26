package com.pa.evs.ctrl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.DMSLockDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
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

	@PostMapping("/api/pas_locks")
	public ResponseEntity<Object> search(HttpServletResponse response, @RequestBody PaginDto<DMSLockDto> pagin) {
		try {
			dmsLockService.search(pagin);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
	}

	@PostMapping("/api/sync_lock/{vendorId}")
	public ResponseEntity<Object> syncLock(@PathVariable Long vendorId) {
		try {
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(dmsLockService.syncLock(vendorId)).build());
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
	}

	@GetMapping("/api/dms-lock-vendors")
	public ResponseEntity<Object> getVendors(HttpServletRequest httpServletRequest) throws Exception {
		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().response(dmsLockService.getDMSLockVendors()).success(true).build());
	}

}
