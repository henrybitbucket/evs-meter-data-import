package com.pa.evs.ctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.constant.RestPath;
import com.pa.evs.dto.DMSAccDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.UserDto;
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
	
    @PostMapping(value = {RestPath.CREATE_DMS_MC_USERS})
    @ApiIgnore
    public Object saveDMSMCUser(@RequestBody DMSAccDto user) {
        try {
        	dmsAccService.saveDMSMCUser(user);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
        return ResponseDto.<Object>builder().success(true).build();
    }
}
