package com.pa.evs.ctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.dto.DMSLocationSiteDto;
import com.pa.evs.dto.DMSSiteDto;
import com.pa.evs.dto.DMSWorkOrdersDto;
import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.DMSSiteService;
import com.pa.evs.sv.WorkOrdersService;

import springfox.documentation.annotations.ApiIgnore;


@SuppressWarnings("rawtypes")
@RestController
@ApiIgnore
public class DMSSiteController {

	@Autowired
	DMSSiteService dmsSiteService;
	
	@Autowired
	WorkOrdersService workOrdersService;

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
	@PostMapping("/api/work-orders")
	public PaginDto searchGroup(@RequestBody PaginDto pagin) {
		workOrdersService.search(pagin);
		return pagin;
	}
	
	@PostMapping("/api/work-order/{siteId}")
	public ResponseDto createWorkOthers(@RequestBody DMSWorkOrdersDto dto, @PathVariable Long siteId) {
		try {
			//dto.setGroup(GroupUserDto.builder().id(groupId).build());
			dto.setSite(DMSSiteDto.builder().id(siteId).build());
			workOrdersService.save(dto);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@SuppressWarnings("unchecked")
	@PostMapping("/api/site/{siteId}/locations")
	public PaginDto searchLocations(@RequestBody PaginDto pagin, @PathVariable Long siteId) {
		pagin.getOptions().put("siteId", siteId);
		dmsSiteService.searchLocations(pagin);
		return pagin;
	}
	
	@PostMapping("/api/site/{siteId}/location")
	public ResponseDto linkLocation(@RequestBody DMSLocationSiteDto dto, @PathVariable Long siteId) {
		try {
			dto.setSiteId(siteId);
			dmsSiteService.linkLocation(dto);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@DeleteMapping("/api/site/{siteId}/location/{linkSiteLocationId}")
	public ResponseDto unLinkLocation(@PathVariable Long siteId, @PathVariable Long linkSiteLocationId) {
		try {
			dmsSiteService.unLinkLocation(linkSiteLocationId);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
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
	
	@DeleteMapping("/api/work-order/{id}")
	public ResponseDto deleteWorkOrder(@PathVariable Long id) {
		try {
			workOrdersService.delete(id);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}	
}
