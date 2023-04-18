package com.pa.evs.ctrl;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
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
import com.pa.evs.utils.AppProps;

@Controller
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
	
	@PostConstruct
	public void init() {
		
		try {
			SettingDto st = settingService.findByKey(SettingService.TIME_CHECK_PI_ONLINE);
			if (st == null || st.getValue() == null || !st.getValue().matches("^[0-9]+$")) {
				settingService.save(SettingDto.builder().key(SettingService.TIME_CHECK_PI_ONLINE).value("30").build());
			}
			
			SettingDto st1 = settingService.findByKey(SettingService.TIME_LOGIN_EXPIRED);
			if (st1 == null || st1.getValue() == null || !st1.getValue().matches("^[0-9]+$")) {
				settingService.save(SettingDto.builder().key(SettingService.TIME_LOGIN_EXPIRED).value("3600").build());
			}
			
			SettingDto st2 = settingService.findByKey(SettingService.EXPORT_ADDRESS_HEADER);
			if (st2 == null) {
				settingService.save(SettingDto.builder().key(SettingService.EXPORT_ADDRESS_HEADER).value("Building,Block,Level,Unit,Postcode,Street Address,State.City,Coupled Meter No.,Coupled MCU SN,UpdatedTime,Remark").build());
			} else {
				AppProps.set(SettingService.EXPORT_ADDRESS_HEADER, st2.getValue());
			}
		} catch (Exception e) {
			//
		}
	}
}
