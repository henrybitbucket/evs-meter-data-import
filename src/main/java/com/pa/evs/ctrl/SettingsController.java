package com.pa.evs.ctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.SettingDto;
import com.pa.evs.sv.SettingService;

import io.swagger.v3.oas.annotations.Hidden;

@Controller
@Hidden
public class SettingsController {

	@Autowired
	private SettingService settingService;

	@PostMapping("/api/settings")
	public ResponseEntity<Object> getSettings(@RequestBody PaginDto<SettingDto> pagin) {
		
		try {
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(settingService.search(pagin)).build());
		} catch (Exception ex) {
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(ex.getMessage()).build());
		}
	}
	
	@PostMapping("/api/setting")
	public ResponseEntity<Object> saveSetting(@RequestBody SettingDto dto) {
		
		try {
			settingService.save(dto);
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
		} catch (Exception ex) {
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(ex.getMessage()).build());
		}
	}
	
	@DeleteMapping("/api/setting/{id}")
	public ResponseEntity<Object> deleteSetting(@PathVariable Long id) {
		
		try {
			settingService.delete(id);
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
		} catch (Exception ex) {
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(ex.getMessage()).build());
		}
	}
	
	@GetMapping("/api/setting/{key}")
	public ResponseEntity<Object> getSettingByKey(@PathVariable String key) {
		
		try {
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(settingService.findByKey(key)).build());
		} catch (Exception ex) {
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(ex.getMessage()).build());
		}
	}
}
