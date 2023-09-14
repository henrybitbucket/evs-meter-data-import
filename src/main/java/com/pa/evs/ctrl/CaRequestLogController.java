package com.pa.evs.ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.constant.RestPath;
import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.ScreenMonitoringDto;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.ScreenMonitoring;
import com.pa.evs.model.Users;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.SimpleMap;

@RestController
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
    
    @PostMapping(RestPath.GET_CA_REQUEST_LOG)
    public ResponseEntity<?> getGantryAccess(HttpServletResponse response, @RequestBody PaginDto<CARequestLog> pagin) throws IOException {
        
        PaginDto<CARequestLog> result = caRequestLogService.search(pagin);
        if (BooleanUtils.isTrue((Boolean) pagin.getOptions().get("downloadCsv"))) {
        	result.getResults().forEach(o -> o.setProfile((String)pagin.getOptions().get("profile")));
            File file = caRequestLogService.downloadCsv(result.getResults(), (Long) pagin.getOptions().get("activateDate"));
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
    
    @PostMapping(RestPath.CA_REQUEST_LOG)
    public ResponseDto<?> save(HttpServletRequest httpServletRequest, @RequestBody CaRequestLogDto dto) {
        try {
            caRequestLogService.save(dto);
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
