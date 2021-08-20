package com.pa.evs.ctrl;

import com.pa.evs.constant.RestPath;
import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.sv.CaRequestLogService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class CaRequestLogController {

    @Autowired
    CaRequestLogService caRequestLogService;
    
    @PostMapping(RestPath.GET_CA_REQUEST_LOG)
    public ResponseEntity<?> getGantryAccess(HttpServletResponse response, @RequestBody PaginDto<CARequestLog> pagin) throws IOException {
        
        PaginDto<CARequestLog> result = caRequestLogService.search(pagin);
        
        if (BooleanUtils.isTrue((Boolean) pagin.getOptions().get("downloadCsv"))) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String tag = sdf.format(new Date());
            String fileName = "ca_request_log-" + tag + ".csv";
            response.setContentType("application/csv");
            response.setHeader("name", fileName);
            File f = caRequestLogService.downloadCsv(result.getResults());
            InputStream in = new FileInputStream(f);
            IOUtils.copy(in, response.getOutputStream());
            FileUtils.deleteDirectory(f.getParentFile());
            return ResponseEntity.ok().build();
//            return ResponseEntity.ok(caRequestLogService.downloadCsv(result.getResults()));
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    @PostMapping(RestPath.CA_REQUEST_LOG)
    public ResponseEntity<?> save(HttpServletRequest httpServletRequest, @RequestBody CaRequestLogDto dto) {
        
        caRequestLogService.save(dto);
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
}
