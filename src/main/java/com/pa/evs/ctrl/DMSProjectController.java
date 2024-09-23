package com.pa.evs.ctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.dto.ApplicationRequestDto;
import com.pa.evs.dto.DMSApplicationDto;
import com.pa.evs.dto.DMSLocationSiteDto;
import com.pa.evs.dto.DMSProjectDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.DMSProjectService;
import com.pa.evs.sv.DMSSiteService;
import com.pa.evs.sv.WorkOrdersService;

import springfox.documentation.annotations.ApiIgnore;


@SuppressWarnings("rawtypes")
@RestController
@ApiIgnore
public class DMSProjectController {

	@Autowired
	DMSSiteService dmsSiteService;
	
	@Autowired
	DMSProjectService dmsProjectService;
	
	@Autowired
	WorkOrdersService workOrdersService;

	@PostMapping("/api/dms/project")
	public ResponseDto save(@RequestBody DMSProjectDto dto) {
		try {
			dmsProjectService.save(dto);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PostMapping("/api/dms/projects")
	public PaginDto<DMSProjectDto> search(@RequestBody PaginDto<DMSProjectDto> pagin) {
		dmsProjectService.search(pagin);
		return pagin;
	}
	
	@PostMapping("/api/dms/applications")
	public PaginDto<DMSApplicationDto> searchApplications(@RequestBody PaginDto<DMSApplicationDto> pagin) {
		dmsProjectService.searchApplications(pagin);
		return pagin;
	}
	
	// // get public applications as request https://powerautomationsg.atlassian.net/browse/LOCKS-38
	@PostMapping("/api/dms/getAllApplications")
	public Object getAllApplications(@RequestBody ApplicationRequestDto dto) {
		try {
			return ResponseDto.builder().success(true).response(dmsProjectService.getAllApplications(dto)).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PostMapping("/api/dms/applications/{applicationId}/users")
	public PaginDto<Object> searchApplicationUsers(@RequestBody PaginDto<Object> pagin, @PathVariable Long applicationId) {
		pagin.getOptions().put("applicationId", applicationId);
		dmsProjectService.searchApplicationUsers(pagin);
		return pagin;
	}
	
	
	@PostMapping("/api/dms/project/{projectId}/pic-sub-users")
	public PaginDto searchPICUserOfProjects(@RequestBody PaginDto pagin, @PathVariable Long projectId) {
		dmsProjectService.searchSubPicUserInProject(pagin, projectId);
		return pagin;
	}
	
	@SuppressWarnings("unchecked")
	@PostMapping("/api/dms/project/{projectId}/pic-user")
	public Object getPicUser(@RequestBody PaginDto pagin, @PathVariable Long projectId) {
		pagin.getOptions().put("siteId", projectId);
		return pagin;
	}
	
	@PostMapping("/api/dms/project/{projectId}/pic-user/{email}")
	public ResponseDto linkPicUser(@RequestBody DMSLocationSiteDto dto, @PathVariable Long projectId, @PathVariable String email) {
		try {
			dmsProjectService.linkPicUser(email, projectId);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@SuppressWarnings("unchecked")
	@PostMapping("/api/dms/project/{projectId}/link-site")
	public ResponseDto linkSite(@PathVariable Long projectId, @RequestBody Map<String, Object> payload) {
		try {
			List<Map<String, Object>> sites = (List<Map<String, Object>>) payload.computeIfAbsent("sites", k -> new ArrayList<>());
			List<Long> siteIds = sites.stream().map(s -> ((Number) s.get("id")).longValue()).collect(Collectors.toList());
			dmsProjectService.linkSite(projectId, siteIds);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@SuppressWarnings("unchecked")
	@PostMapping("/api/dms/project/{projectId}/link-pic-user")
	public ResponseDto linkPicUsers(@PathVariable Long projectId, @RequestBody Map<String, Object> payload) {
		try {
			List<Map<String, Object>> users = (List<Map<String, Object>>) payload.computeIfAbsent("users", k -> new ArrayList<>());
			List<String> emails = users.stream().map(s -> ((String) s.get("email"))).collect(Collectors.toList());
			dmsProjectService.linkSubPicUsers(emails, projectId);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@DeleteMapping("/api/dms/project/{projectId}/pic-user/{email}")
	public ResponseDto unUinkPicUser(@PathVariable Long siteId, @PathVariable Long projectId, @PathVariable String email) {
		try {
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}

	
	@DeleteMapping("/api/dms/project/{id}")
	public ResponseDto delete(@PathVariable Long id) {
		try {
			dmsProjectService.delete(id);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}	
	
    @PostMapping(value = {"/api/dms/pic-users"})
    @ApiIgnore
    public Object getPicUsers(@RequestBody PaginDto<UserDto> pagin) {
    	dmsProjectService.searchDMSPicUsers(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
}
