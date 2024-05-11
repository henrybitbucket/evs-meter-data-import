package com.pa.evs.ctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.constant.RestPath;
import com.pa.evs.dto.DMSAccDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.VendorDMSAccDto;
import com.pa.evs.dto.VendorDto;
import com.pa.evs.sv.DMSAccService;

import springfox.documentation.annotations.ApiIgnore;

@RestController
public class DMSAccController {

	@Autowired
	private DMSAccService dmsAccService;

	@PostMapping(value = { RestPath.GET_DMS_MC_USERS })
	@ApiIgnore
	public Object getDMSMCUsers(@RequestBody PaginDto<DMSAccDto> pagin) {
		dmsAccService.getDMSMCUsers(pagin);
		return ResponseDto.<Object>builder().success(true).response(pagin).build();
	}

	@PostMapping(value = { RestPath.SAVE_DMS_MC_USERS })
	@ApiIgnore
	public Object saveDMSMCUser(@RequestBody DMSAccDto user) {
		try {
			dmsAccService.saveDMSMCUser(user);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
		return ResponseDto.<Object>builder().success(true).build();
	}

	@GetMapping(value = { RestPath.GET_VENDOR_AND_MC_USERS })
	@ApiIgnore
	public Object getVendorAndMcAccs(@PathVariable Long vendorId) {
		try {
			return ResponseDto.<Object>builder().response(dmsAccService.getVendorAndMcAccs(vendorId)).success(true)
					.build();
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
	}

	@PostMapping(value = { RestPath.SAVE_OR_UPDATE_VENDOR_AND_MC_USERS })
	@ApiIgnore
	public Object saveOrUpdateVendorAndUser(@RequestBody VendorDMSAccDto dto) {
		try {
			dmsAccService.saveOrUpdateVendorAndUser(dto);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
		return ResponseDto.<Object>builder().success(true).build();
	}

	@DeleteMapping(value = { RestPath.DELETE_VENDOR_AND_MC_USERS })
	@ApiIgnore
	public Object deleteVendor(@PathVariable Long vendorId) {
		try {
			dmsAccService.deleteVendor(vendorId);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
		return ResponseDto.<Object>builder().success(true).build();
	}
	
	@PostMapping(value = { RestPath.GET_PAGIN_VENDORS })
	@ApiIgnore
	public Object getVendorsUsers(@RequestBody PaginDto<VendorDto> pagin) {
		dmsAccService.getVendorsUsers(pagin);
		return ResponseDto.<Object>builder().success(true).response(pagin).build();
	}
}
