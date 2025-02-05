package com.pa.evs.ctrl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.constant.RestPath;
import com.pa.evs.dto.AddressDto;
import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.DeviceSettingDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.RelayStatusLogDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.ScreenMonitoringDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.ScreenMonitoring;
import com.pa.evs.model.Users;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.utils.CsvUtils;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.SimpleMap;

import io.swagger.v3.oas.annotations.Hidden;

@RestController
@Hidden
public class CaRequestLogController {
	
	@Value("${evs.pa.mqtt.address}") private String evsPAMQTTAddress;

	@Value("${evs.pa.mqtt.client.id}") private String mqttClientId;

	private Date lastReboot = new Date();
	
	Number countAlarms = 0;
	
	ScreenMonitoringDto mqttStatus;
	
	Map<String, Integer> countDevices;
	
	List<ScreenMonitoring> systemInformation;

    @Autowired
    CaRequestLogService caRequestLogService;
    
    @SuppressWarnings("unchecked")
	@PostMapping(RestPath.GET_CA_REQUEST_LOG)
    public ResponseEntity<?> getMCUs(HttpServletResponse response, @RequestBody PaginDto<CARequestLog> pagin) throws IOException {
        
    	if (BooleanUtils.isTrue((Boolean) pagin.getOptions().get("downloadCsv")) || "true".equalsIgnoreCase(pagin.getOptions().get("downloadFullMCU") + "")) {
    		pagin.setLimit(Integer.MAX_VALUE);
    	}
        PaginDto<CARequestLog> result = caRequestLogService.search(pagin);
        if (BooleanUtils.isTrue((Boolean) pagin.getOptions().get("downloadCsv"))) {
        	result.getResults().forEach(o -> o.setProfile((String)pagin.getOptions().get("profile")));
            File file = null;
            if ("true".equalsIgnoreCase(pagin.getOptions().get("downloadFullMCU") + "")) {
            	file = caRequestLogService.downloadCsvFullMCUs(result.getResults(), (List<String>) pagin.getOptions().get("sns"));
            } else {
            	file = caRequestLogService.downloadCsv(result.getResults(), (Long) pagin.getOptions().get("activateDate"));
            }
            String fileName = file.getName();
            
            try (FileInputStream fis = new FileInputStream(file)) {
                response.setContentLengthLong(file.length());
                response.setHeader(HttpHeaders.CONTENT_TYPE, "application/csv");
                response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
                response.setHeader("name", fileName);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                IOUtils.copy(fis, response.getOutputStream());
            } finally {
                FileUtils.deleteDirectory(file.getParentFile());
            }
            //set Activation date
            if (pagin.getOptions().get("activateDate") != null) {
                Set<Long> ids = result.getResults().stream().map(CARequestLog::getId).collect(Collectors.toSet());
                caRequestLogService.setActivationDate((Long) pagin.getOptions().get("activateDate"), ids);
            }
        }
        result.getResults().forEach(li -> {
            Users user = li.getInstaller();
            Users installer = new Users();
            if (user != null) {
                installer.setUserId(user.getUserId());
                installer.setUsername(user.getUsername());
                li.setInstaller(installer);
            }
        });
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    @PostMapping("/api/export-by-upload-ca-request-logs")
    public ResponseEntity<?> exportMCUs(
            HttpServletRequest httpServletRequest,
            HttpServletResponse response,
            @RequestParam(value = "file") final MultipartFile file) throws Exception {
    	
    	PaginDto<CARequestLog> pagin = new PaginDto<>();
    	try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			IOUtils.copy(file.getInputStream(), bos);
			List<String> sns = new ArrayList<>();
			for (String l : new String(bos.toByteArray()).split("\r*\n")) {
				if (!l.trim().startsWith("//")) {
					sns.add(l.replaceAll("[^a-zA-Z0-9]", "").trim());
				}
			}
			pagin.getOptions().put("sns", sns);
		} catch (Exception e) {
			//
		}
    	pagin.getOptions().put("downloadFullMCU", true);
    	pagin.getOptions().put("downloadCsv", true);
    	
    	return getMCUs(response, pagin);
    }
    
    @PostMapping("/api/meters")
    public ResponseEntity<?> getMeters(HttpServletResponse response, @RequestBody PaginDto<CARequestLog> pagin) throws IOException {
        
    	if (BooleanUtils.isTrue((Boolean) pagin.getOptions().get("downloadCsv"))) {
    		pagin.setLimit(Integer.MAX_VALUE);
    	}
        PaginDto<CARequestLog> result = caRequestLogService.searchMMSMeter(pagin);
        if (BooleanUtils.isTrue((Boolean) pagin.getOptions().get("downloadCsv"))) {
        	result.getResults().forEach(o -> o.setProfile((String)pagin.getOptions().get("profile")));
            // File file = caRequestLogService.downloadCsvMeter(result.getResults(), (Long) pagin.getOptions().get("activateDate"));
        	
    		List<String> headers = Arrays.asList(
    				"MSN","Remark for meter","City","Postal","Building","Street","Block","Level","Unit"
    				);
    		File file = CsvUtils.toCsv(headers, result.getResults(), (idx, it, l) -> {
            	
                List<String> record = new ArrayList<>();

                record.add(StringUtils.isNotBlank(it.getMsn()) ? it.getMsn() : "");
                record.add(StringUtils.isNotBlank(it.getRemarkMeter()) ? it.getRemarkMeter() : "");
                record.add(it.getBuilding() != null && StringUtils.isNotBlank(it.getBuilding().getAddress().getCity()) ? it.getBuilding().getAddress().getCity() : "");
                record.add(it.getBuilding() != null && StringUtils.isNotBlank(it.getBuilding().getAddress().getPostalCode()) ? it.getBuilding().getAddress().getPostalCode() : "");
                record.add(it.getBuilding() != null && StringUtils.isNotBlank(it.getBuilding().getName()) ? it.getBuilding().getName() : "");
                record.add(it.getBuilding() != null && StringUtils.isNotBlank(it.getBuilding().getAddress().getStreet()) ? it.getBuilding().getAddress().getStreet() : "");
                record.add(it.getBlock() != null && StringUtils.isNotBlank(it.getBlock().getName()) ? it.getBlock().getName() : "");
                record.add(it.getFloorLevel() != null && StringUtils.isNotBlank(it.getFloorLevel().getName()) ? it.getFloorLevel().getName() : "");
                record.add(it.getBuildingUnit() != null && StringUtils.isNotBlank(it.getBuildingUnit().getName()) ? it.getBuildingUnit().getName() : "");
                
                return CsvUtils.postProcessCsv(record);
            }, CsvUtils.buildPathFile("export_meter_result_" + System.currentTimeMillis() + ".csv"), 1l);
        	
            String fileName = file.getName();
            
            try (FileInputStream fis = new FileInputStream(file)) {
                response.setContentLengthLong(file.length());
                response.setHeader(HttpHeaders.CONTENT_TYPE, "application/csv");
                response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
                response.setHeader("name", fileName);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                IOUtils.copy(fis, response.getOutputStream());
            } finally {
                FileUtils.deleteDirectory(file.getParentFile());
            }
            //set Activation date
            if (pagin.getOptions().get("activateDate") != null) {
                Set<Long> ids = result.getResults().stream().map(CARequestLog::getId).collect(Collectors.toSet());
                caRequestLogService.setActivationDate((Long) pagin.getOptions().get("activateDate"), ids);
            }
        }
        result.getResults().forEach(li -> {
            Users user = li.getInstaller();
            Users installer = new Users();
            if (user != null) {
                installer.setUserId(user.getUserId());
                installer.setUsername(user.getUsername());
                li.setInstaller(installer);
            }
        });
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    @DeleteMapping("/api/meter/{msn}")
    public ResponseEntity<?> removeMeter(
    		HttpServletRequest httpServletRequest,
    		@PathVariable final String msn
    		) throws Exception {
    	
		try {
			caRequestLogService.removeMeter(msn);
		} catch (Exception e) {
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).errorDescription(e.getMessage()).build());
		}
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping(RestPath.CA_REQUEST_LOG)
    public ResponseDto<?> save(HttpServletRequest httpServletRequest, @RequestBody CaRequestLogDto dto) {
        try {
            caRequestLogService.save(dto);
            return ResponseDto.<Object>builder().message(dto.getMessage()).success(true).build();
        } catch (Exception e) {
            return ResponseDto.<Object>builder().success(false).errorDescription(e.getMessage()).build();
        }
    }
    
    @PostMapping("/api/update-device-vendor/{msn}/{vendorId}")
    public ResponseDto<?> updateVendor(
    		HttpServletRequest httpServletRequest,
    		@PathVariable String msn,
    		@PathVariable Long vendorId
    		) {
        try {
            caRequestLogService.updateVendor(msn, vendorId);;
            return ResponseDto.<Object>builder().success(true).build();
        } catch (Exception e) {
            return ResponseDto.<Object>builder().success(false).errorDescription(e.getMessage()).build();
        }
    }
    
    @GetMapping(RestPath.CA_REQUEST_LOG_GET_CIDS)
    public ResponseEntity<?> getCids(HttpServletRequest httpServletRequest) {
        List<String> cids = caRequestLogService.getCids(false);
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(cids).build());
    }
    
    @GetMapping(RestPath.CA_CAL_DASHBOARD)
    public ResponseEntity<?> calDashboard(HttpServletRequest httpServletRequest) {
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(
        		SimpleMap.init("countAlarms", countAlarms)
        		.more("critical", 0)
        		.more("lastReboot", lastReboot.getTime())
        		.more("mqttStatus", mqttStatus)
        		.more("mqttAddress", evsPAMQTTAddress)
        		.more("countDevices", countDevices)
        		.more("systemInformation", systemInformation)
        		).build());
    }
    
    @PostMapping(RestPath.CA_ALARM_MARK_VIEW_ALL)
    public ResponseEntity<?> markViewAll(HttpServletRequest httpServletRequest) {
        caRequestLogService.markViewAll();
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }

    @GetMapping(RestPath.CA_REQUEST_LOG_GET_COUNT_DEVICES)
    public ResponseEntity<?> getCountDevices(HttpServletRequest httpServletRequest) {
        Map<String, Integer> result = caRequestLogService.getCountDevices();
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(result).build());
    }

    @GetMapping(RestPath.GET_DASHBOARD)
    public ResponseEntity<?> getDashboard(HttpServletRequest httpServletRequest) {
        List<ScreenMonitoring> result = caRequestLogService.getDashboard();
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(result).build());
    }
    
    @PostMapping("/api/caRequestLog/search")
    public ResponseEntity<Object> searchLogsByUser(HttpServletRequest httpServletRequest, @RequestBody PaginDto<CaRequestLogDto> pagin) throws Exception {
        try {
        	caRequestLogService.searchCaRequestLog(pagin);
        } catch (Exception e) {
        	e.printStackTrace();
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
    
    @PostMapping(RestPath.CA_REQUEST_LOG_POST_SEND_COMMAND)
    public ResponseEntity<Object> sendRLSCommandForDevices(HttpServletRequest httpServletRequest, @RequestBody PaginDto<CARequestLog> pagin, @RequestParam String command) throws Exception {
        try {
        	
        	if (StringUtils.isBlank(command) || !("PW1".equals(command) || "PW0".equals(command) || "RLS".equals(command))) {
        		throw new ApiException("Command only accept PW0 or PW1 or RLS");
        	}
        	
        	pagin.setOffset(0);
        	pagin.setLimit(Integer.MAX_VALUE);
        	PaginDto<CARequestLog> result = caRequestLogService.search(pagin);
        	List<CARequestLog> listDevice = result.getResults();
        	String commandSendBy = SecurityUtils.getEmail();
        	
        	if (listDevice.isEmpty()) {
        		return ResponseEntity.ok(ResponseDto.builder().success(false).message("No device found!").build());
        	}
        	
        	String uuid = UUID.randomUUID().toString();
        	new Thread(() -> caRequestLogService.sendRLSCommandForDevices(listDevice, command, pagin.getOptions(), commandSendBy, uuid)).start();
        	return ResponseEntity.ok(ResponseDto.builder().success(true).response(uuid).build());
        } catch (Exception e) {
        	e.printStackTrace();
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
    }
    
    @PostMapping(RestPath.RLS_STATUS_GET_LOGS)
    public ResponseEntity<Object> getRelayStatusLogs(HttpServletRequest httpServletRequest, @RequestBody PaginDto<RelayStatusLogDto> pagin) throws Exception {
    	try {
        	caRequestLogService.getRelayStatusLogs(pagin);
        } catch (Exception e) {
        	e.printStackTrace();
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
    
    @PostMapping(RestPath.CA_REQUEST_LOG_BATCH_COUPLE_DEVICES)
    public ResponseEntity<Object> batchCoupleDevices(HttpServletRequest httpServletRequest, @RequestBody List<Map<String, String>> listInput) throws Exception {
    	try {
    		List<Map<String, String>> output = caRequestLogService.batchCoupleDevices(listInput);
    		if (output.isEmpty()) {
    			return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    		}
    		return ResponseEntity.ok(ResponseDto.builder().success(false).response(output).build());
        } catch (Exception e) {
        	e.printStackTrace();
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
    }
    
    @PostMapping("/api/device-settings/upload")
    public ResponseEntity<Object> uploadDeviceSettings(
            HttpServletRequest httpServletRequest,
            HttpServletResponse response,
            @RequestParam(value = "file") final MultipartFile file,
            @RequestParam(required = false) final Boolean isProcess) throws Exception {

        try {
        	List<DeviceSettingDto> result = caRequestLogService.uploadDeviceSettings(file,isProcess);
        	
        	if (BooleanUtils.isTrue(isProcess)) {
        		String fileName = file.getName() + "_result.csv";
            	File resultFile = CsvUtils.writeDeviceSettingsCsv(result, fileName, null);

                try (FileInputStream fis = new FileInputStream(resultFile)) {
                    response.setContentLengthLong(resultFile.length());
                    response.setHeader(HttpHeaders.CONTENT_TYPE, "application/csv");
                    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
                    response.setHeader("name", fileName);
                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                    IOUtils.copy(fis, response.getOutputStream());
                } finally {
                    FileUtils.deleteDirectory(resultFile.getParentFile());
                }
                return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
        	} else {
        		return ResponseEntity.ok(ResponseDto.builder().success(true).response(result).build());
        	}
        } catch (Exception e) {
        	return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
    }
    
	@PostMapping("/api/devices-node")
	public ResponseEntity<Object> updateDevicesNode(HttpServletRequest httpServletRequest, HttpServletResponse response,
			@RequestParam List<Long> deviceIds, 
			@RequestParam String ieiNode, 
			@RequestParam Boolean isDistributed, 
			@RequestParam(required = false) Integer sendMDTToPi,
			@SuppressWarnings("rawtypes") @RequestBody PaginDto filter) throws Exception {

		try {
			caRequestLogService.updateDevicesNode(deviceIds, ieiNode, isDistributed, sendMDTToPi, filter);
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
		} catch (Exception e) {
			return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
		}
	}
    
    @PostConstruct
    public void init() {
    	SchedulerHelper.scheduleJob("0/10 * * * * ? *", () -> {
    		
    		countAlarms = caRequestLogService.countAlarms();
    		mqttStatus = caRequestLogService.mqttStatusCheck();
    		countDevices = caRequestLogService.getCountDevices();
    		systemInformation = caRequestLogService.getDashboard();
    		
    	}, "GET_SYSTEM_PROPERTIES");
    }
}
