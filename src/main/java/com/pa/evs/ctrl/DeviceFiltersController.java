package com.pa.evs.ctrl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.dto.DeviceFiltersDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.DeviceFiltersService;

import io.swagger.v3.oas.annotations.Hidden;

@RestController
@SuppressWarnings("rawtypes")
@Hidden
public class DeviceFiltersController {
	
	@Autowired
	private DeviceFiltersService deviceFiltersService;
	
	@GetMapping("/api/device-filters")
	public ResponseDto getDeviceFilters() {
		try {
			List<DeviceFiltersDto> results = deviceFiltersService.getDeviceFilters();
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).response(results).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PostMapping("/api/device-filters")
	public ResponseDto saveDeviceFilters(@RequestBody DeviceFiltersDto filters) {
		try {
			deviceFiltersService.saveDeviceFilters(filters);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}

}
