package com.pa.evs.ctrl;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.DMSApplicationGuestSaveReqDto;
import com.pa.evs.dto.DMSApplicationSaveReqDto;
import com.pa.evs.dto.DMSLocationLockDto;
import com.pa.evs.dto.DMSLockDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.DMSLockService;
import com.pa.evs.sv.DMSProjectService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.TimeZoneHolder;

@DependsOn(value = "settingsController")
@RestController
//@ApiIgnore
@SuppressWarnings("rawtypes")
public class AppPASController {
	static final Logger LOGGER = LoggerFactory.getLogger(AppPASController.class);

	static final ObjectMapper MAPPER = new ObjectMapper();
	String token;
	RestTemplate resttemplate = ApiUtils.getRestTemplate();

	@Autowired
	DMSLockService dmsLockService;
	
	@Autowired
	DMSProjectService dmsProjectService;

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
	
	@PostMapping("/api/lock/{lockId}/location")
	public ResponseDto linkLocation(@RequestBody DMSLocationLockDto dto, @PathVariable Long lockId) {
		try {
			dto.setLockId(lockId);
			dmsLockService.linkLocation(dto);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@DeleteMapping("/api/lock/{lockId}/location/{linkLockLocationId}")
	public ResponseDto unLinkLocation(@PathVariable Long lockId, @PathVariable Long linkLockLocationId) {
		try {
			dmsLockService.unLinkLocation(linkLockLocationId);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}

	@GetMapping("/api/dms-assigned-locks")
	public ResponseDto getAssignedLocks(HttpServletRequest httpServletRequest, @RequestParam(required = false) Boolean lockOnly) throws Exception {

		try {
			String email = SecurityUtils.getEmail();
			// email = "hr.dms1.2@gmail.com";
			return ResponseDto.<Object>builder().response(dmsLockService.getAssignedLocks(email, lockOnly)).success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PostMapping("/api/dms-assigned-locks2")
	public ResponseDto getAssignedLocks2(HttpServletRequest httpServletRequest, @RequestParam(required = false) Boolean lockOnly, @RequestBody Map<String, Object> payload) throws Exception {

		try {
			String userMobile = (String) payload.get("user_mobile");
			return ResponseDto.<Object>builder().response(dmsLockService.getAssignedLocks2(userMobile, lockOnly)).success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@GetMapping("/api/lock/{lockId}/code")
	public ResponseDto getLockSecretCode(HttpServletRequest httpServletRequest, @PathVariable Long lockId, @RequestParam(required = false) String timeZone) throws Exception {

		try {
			if (StringUtils.isNotBlank(timeZone)) {
				TimeZoneHolder.set(timeZone);
			}
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			String email = SecurityUtils.getEmail();
			return ResponseDto.<Object>builder().response(dmsLockService.getSecretCode(email, lockId)).success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PostMapping("/api/lock/{lockId}/code2")
	public ResponseDto getLockSecretCode2(HttpServletRequest httpServletRequest, 
			@PathVariable Long lockId, 
			@RequestParam(required = false) String timeZone, 
			@RequestBody Map<String, Object> payload) throws Exception {

		try {
			if (StringUtils.isNotBlank(timeZone)) {
				TimeZoneHolder.set(timeZone);
			}

			String userMobile = (String) payload.get("user_mobile");
			return ResponseDto.<Object>builder().response(dmsLockService.getSecretCode2(userMobile, lockId)).success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
/**	
	{
	  "guests": [
	    {
	      "name": "HR",
	      "phone": "+84909123456"
	    }
	  ],
	  "projectId": 4,
	  "sites": [
	    {
	      "siteId": 13,
	      "timePeriodDatesEnd": 0,
	      "timePeriodDatesIsAlways": true,
	      "timePeriodDatesStart": 0,
	      "timePeriodDayInWeeksIsAlways": true,
	      "timePeriodDayInWeeksIsFri": true,
	      "timePeriodDayInWeeksIsMon": true,
	      "timePeriodDayInWeeksIsSat": true,
	      "timePeriodDayInWeeksIsSun": true,
	      "timePeriodDayInWeeksIsThu": true,
	      "timePeriodDayInWeeksIsTue": true,
	      "timePeriodDayInWeeksIsWed": true,
	      "timePeriodTimeInDayHourEnd": 0,
	      "timePeriodTimeInDayHourStart": 0,
	      "timePeriodTimeInDayIsAlways": true,
	      "timePeriodTimeInDayMinuteEnd": 0,
	      "timePeriodTimeInDayMinuteStart": 0
	    }
	  ],
	  "userPhones": [
	    "+84909123456"
	  ]
	}	
*/
	@PostMapping("/api/dms/project/{projectId}/application")
	public ResponseDto submitApplication(HttpServletRequest httpServletRequest, @PathVariable Long projectId, @RequestBody DMSApplicationSaveReqDto dto) throws Exception {

		try {
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				// throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			dto.setSubmittedBy(SecurityUtils.getPhoneNumber());
			return ResponseDto.<Object>builder().response(dmsProjectService.submitApplication(projectId, dto)).success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PutMapping("/api/dms/project/{projectId}/application/{applicationId}")
	public ResponseDto updateApplication(HttpServletRequest httpServletRequest, @PathVariable Long projectId, @PathVariable Long applicationId, @RequestBody DMSApplicationSaveReqDto dto) throws Exception {

		try {
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				// throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			dto.setSubmittedBy(SecurityUtils.getPhoneNumber());
			return ResponseDto.<Object>builder().response(dmsProjectService.updateApplication(applicationId, dto)).success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PostMapping("/api/dms/project/{projectId}/application-guest")
	public ResponseDto submitApplicationGuest(HttpServletRequest httpServletRequest, 
			@PathVariable Long projectId,
			@RequestBody DMSApplicationGuestSaveReqDto dto,
			@RequestParam String guestPhone
			) throws Exception {

		try {
			AppCodeSelectedHolder.set("DMS");
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				// throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			if (StringUtils.isBlank(guestPhone) || !guestPhone.trim().matches("^\\+[1-9][0-9]{7,}$")) {
				throw new RuntimeException("Phone invalid(" + guestPhone + ")! (ex: +65909123456)");
			}
			dto.setSubmittedBy(guestPhone);
			return ResponseDto.<Object>builder().response(dmsProjectService.submitApplication(projectId, dto)).success(true).build();
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PutMapping("/api/dms/application/{applicationId}/approve")
	public ResponseDto approveApplication(HttpServletRequest httpServletRequest, @PathVariable Long applicationId) throws Exception {

		try {
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				 throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			dmsProjectService.approveApplication(applicationId);
			return ResponseDto.<Object>builder().success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PutMapping("/api/dms/application/{applicationId}/reject")
	public ResponseDto rejectApplication(HttpServletRequest httpServletRequest, @PathVariable Long applicationId) throws Exception {

		try {
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				 throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			dmsProjectService.rejectApplication(applicationId);
			return ResponseDto.<Object>builder().success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@DeleteMapping("/api/dms/application/{applicationId}/delete")
	public ResponseDto deleteApplication(HttpServletRequest httpServletRequest, @PathVariable Long applicationId) throws Exception {

		try {
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				 throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			dmsProjectService.deleteApplication(applicationId);
			return ResponseDto.<Object>builder().success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@DeleteMapping("/api/dms/application/{applicationId}/site/{siteId}")
	public ResponseDto deleteSiteOfApplication(HttpServletRequest httpServletRequest, @PathVariable Long applicationId, @PathVariable Long siteId) throws Exception {

		try {
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				 throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			dmsProjectService.deleteSiteOfApplication(applicationId, siteId);
			return ResponseDto.<Object>builder().success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
}
