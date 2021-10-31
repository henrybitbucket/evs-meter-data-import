package com.pa.evs.ctrl;

import com.pa.evs.constant.RestPath;
import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.utils.SimpleMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@RestController
public class CaRequestLogController {

	private Date lastReboot = new Date();
	
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
        Number countAlarms = caRequestLogService.countAlarms();
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(
        		SimpleMap.init("countAlarms", countAlarms)
        		.more("critical", 0)
        		.more("lastReboot", lastReboot.getTime())
        		).build());
    }
}
