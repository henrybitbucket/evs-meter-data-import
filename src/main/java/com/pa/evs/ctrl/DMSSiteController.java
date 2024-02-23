package com.pa.evs.ctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.dto.DMSSiteDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.DMSSiteService;

import springfox.documentation.annotations.ApiIgnore;


@SuppressWarnings("rawtypes")
@RestController
@ApiIgnore
public class DMSSiteController {

	@Autowired
	DMSSiteService dmsSiteService;

	@PostMapping("/api/site")
	public ResponseDto save(@RequestBody DMSSiteDto dto) {
		try {
			dmsSiteService.save(dto);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PostMapping("/api/sites")
	public PaginDto<DMSSiteDto> search(@RequestBody PaginDto<DMSSiteDto> pagin) {
		dmsSiteService.search(pagin);
		return pagin;
	}
	
	
	@SuppressWarnings("unchecked")
	@PostMapping("/api/site/{siteId}/work-orders")
	public PaginDto searchGroup(@RequestBody PaginDto pagin, @PathVariable Long siteId) {
		pagin.getOptions().put("siteId", siteId);
		dmsSiteService.searchWorkOrders(pagin);
		return pagin;
	}
	
	@SuppressWarnings("unchecked")
	@PostMapping("/api/site/{siteId}/locations")
	public PaginDto searchLocations(@RequestBody PaginDto pagin, @PathVariable Long siteId) {
		pagin.getOptions().put("siteId", siteId);
		dmsSiteService.searchLocations(pagin);
		return pagin;
	}
	
	@DeleteMapping("/api/site/{id}")
	public ResponseDto delete(@PathVariable Long id) {
		try {
			dmsSiteService.delete(id);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
}
