package com.pa.evs.ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.DMSApplicationGuestSaveReqDto;
import com.pa.evs.dto.DMSApplicationSaveReqDto;
import com.pa.evs.dto.DMSLocationLockDto;
import com.pa.evs.dto.DMSLockDto;
import com.pa.evs.dto.EcodeReq;
import com.pa.evs.dto.LockAddressReq;
import com.pa.evs.dto.LockDto;
import com.pa.evs.dto.LockEventLogSearchReq;
import com.pa.evs.dto.LockRequestDto;
import com.pa.evs.dto.LockWorkOrderReq;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.SaveLogReq;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.DMSLockService;
import com.pa.evs.sv.DMSProjectService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.CsvUtils;
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

	@PostMapping("/api/getLocks")
	public ResponseEntity<Object> getLocks(HttpServletResponse response, @RequestBody LockRequestDto dto) {
		try {
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(dmsLockService.getLocks(dto)).build());
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
	}
	
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
			String phone = SecurityUtils.getPhoneNumber();
			// email = "hr.dms1.2@gmail.com";
			return ResponseDto.<Object>builder().response(dmsLockService.getAssignedLocks(phone, lockOnly, null, null)).success(true).build();
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
			String phone = SecurityUtils.getPhoneNumber();
			return ResponseDto.<Object>builder().response(dmsLockService.getSecretCode(phone, lockId)).success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
	
	@PostMapping("/api/get_ecode")
	public ResponseDto getEcode(HttpServletRequest httpServletRequest,
			@RequestBody EcodeReq req
			) throws Exception {

		try {
			return ResponseDto.<Object>builder().response(dmsLockService.getEcode(req)).success(true).build();
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

	@PostMapping("/api/lock/code")
	public ResponseDto getLockSecretCode(@RequestBody LockDto lockDto) {
		try {
			if (StringUtils.isNotBlank(lockDto.getTimeZone())) {
				TimeZoneHolder.set(lockDto.getTimeZone());
			}
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			String phone = SecurityUtils.getPhoneNumber();
			return ResponseDto.<Object>builder().response(dmsLockService.getSecretCode(phone, lockDto)).success(true).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}

/**	
{
  "timePeriod": {
    "timePeriodDatesIsAlways": true,
    "timePeriodDatesStart": 1711993034779,
    "timePeriodDatesEnd": 1711993034779,
    "timePeriodDayInWeeksIsAlways": false,
    "timePeriodDayInWeeksIsMon": true,
    "timePeriodDayInWeeksIsTue": true,
    "timePeriodDayInWeeksIsWed": true,
    "timePeriodDayInWeeksIsThu": true,
    "timePeriodDayInWeeksIsFri": true,
    "timePeriodDayInWeeksIsSat": true,
    "timePeriodDayInWeeksIsSun": true,
    "timePeriodTimeInDayIsAlways": true,
    "timePeriodTimeInDayHourStart": 0,
    "timePeriodTimeInDayMinuteStart": 0,
    "timePeriodTimeInDayHourEnd": 23,
    "timePeriodTimeInDayMinuteEnd": 59
  },
  "sites": [
    {
      "id": "9",
      "timePeriod": null
    },
    {
      "id": "10",
      "timePeriod": {
        "override": true,
        "timePeriodDatesIsAlways": false,
        "timePeriodDatesStart": 1711993034779,
        "timePeriodDatesEnd": 1711993034779,
        "timePeriodDayInWeeksIsAlways": false,
        "timePeriodDayInWeeksIsMon": false,
        "timePeriodDayInWeeksIsTue": true,
        "timePeriodDayInWeeksIsWed": false,
        "timePeriodDayInWeeksIsThu": true,
        "timePeriodDayInWeeksIsFri": false,
        "timePeriodDayInWeeksIsSat": true,
        "timePeriodDayInWeeksIsSun": true,
        "timePeriodTimeInDayIsAlways": false,
        "timePeriodTimeInDayHourStart": 0,
        "timePeriodTimeInDayMinuteStart": 0,
        "timePeriodTimeInDayHourEnd": 23,
        "timePeriodTimeInDayMinuteEnd": 59
      }
    }
  ],
  "userPhones": [
    "+65012345678",
    "+6597717985",
    "+6500001114"
  ],
  "guests": [
    {
      "name": "BA",
      "phone": "+653323232323",
      "email": null,
      "password": null,
      "createNewUser": false
    },
    {
      "name": "asasas",
      "phone": "+652332323",
      "email": "hr.test.sendaccount12233@gmail.com",
      "password": "12345678@Xx",
      "createNewUser": true
    }
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
	
    @PostMapping(value = {"/api/app_savelog"})
    public ResponseDto saveLog(
    		@RequestBody SaveLogReq dto
    		) throws IOException {
    	
		try {
			AppCodeSelectedHolder.set("DMS");
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				// throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			dmsLockService.saveLog(dto);
			return ResponseDto.<Object>builder().success(true).build();
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
    }
    
    @PostMapping(value = {"/api/app_getlog"})
    public ResponseDto getLog(
    		@RequestBody LockEventLogSearchReq dto
    		) throws IOException {
    	
		try {
			return ResponseDto.<Object>builder().success(true).response(dmsLockService.getLockEventLogs(dto)).build();
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
    }
    
    @PostMapping(value = {"/api/lock/addresses"})
    public ResponseDto getLockAddress(
    		@RequestBody LockAddressReq dto
    		) throws IOException {
    	
		try {
			return ResponseDto.<Object>builder().success(true).response(dmsLockService.getLockAddress(dto)).build();
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
    }
    
    @PostMapping(value = {"/api/lock/work_orders"})
    public ResponseDto getLockWorkOrders(
    		@RequestBody LockWorkOrderReq dto
    		) throws IOException {
    	
		try {
			return ResponseDto.<Object>builder().success(true).response(dmsLockService.getLockWorkOrders(dto)).build();
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
			guestPhone = guestPhone.replace("%2B", "+");
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
	
	@PutMapping("/api/dms/application/{applicationId}/terminate")
	public ResponseDto terminateApplication(HttpServletRequest httpServletRequest, @PathVariable Long applicationId) throws Exception {

		try {
			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
				 throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
			dmsProjectService.terminateApplication(applicationId);
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
	
    @PostMapping("/api/dms-lock/upload")
    public ResponseEntity<Object> uploadMsiSdn(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "file") final MultipartFile file) throws Exception {


        try {
    		List<String> headers = Arrays.asList(
    				"LockName", "LockNumber", "LockBid", "Key", "Message"
    				);
    		List<Map<String, Object>> dtos = dmsLockService.handleUploadLocks(file);
    		File csv = CsvUtils.toCsv(headers, dtos, (idx, it, l) -> {
            	
                List<String> record = new ArrayList<>();

                record.add(StringUtils.isNotBlank((String) it.get("LockName")) ? (String) it.get("LockName") : "");
                record.add(StringUtils.isNotBlank((String) it.get("LockNumber")) ? (String) it.get("LockNumber") : "");
                record.add(StringUtils.isNotBlank((String) it.get("LockBid")) ? (String) it.get("LockBid") : "");
                record.add(StringUtils.isNotBlank((String) it.get("Key")) ? (String) it.get("Key") : "");
                record.add(StringUtils.isNotBlank((String) it.get("Message")) ? (String) it.get("Message") : "Success");
                
                return CsvUtils.postProcessCsv(record);
            }, CsvUtils.buildPathFile("import_dms_lock_result_" + System.currentTimeMillis() + ".csv"), 1l);
        	
        	String fileName = file.getName();
            
        	SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        	
        	try (FileOutputStream logFileFos = new FileOutputStream(CsvUtils.EXPORT_TEMP + "/logs/import_dms_lock_" + sf.format(new Date()) + "_" + System.currentTimeMillis() + ".csv"); 
        			FileInputStream fis = new FileInputStream(csv)) {
        		IOUtils.copy(fis, logFileFos);
        	}
        	
            try (FileInputStream fis = new FileInputStream(csv)) {
                response.setContentLengthLong(csv.length());
                response.setHeader(HttpHeaders.CONTENT_TYPE, "application/csv");
                response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
                response.setHeader("name", fileName);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                IOUtils.copy(fis, response.getOutputStream());
            } finally {
                FileUtils.deleteDirectory(csv.getParentFile());
            }
        } catch (Exception e) {
        	LOGGER.error(e.getMessage(), e);
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
}
