package com.pa.evs.ctrl;

import javax.servlet.http.HttpServletRequest;
import com.pa.evs.constant.RestPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.sv.CaRequestLogService;

@RestController
public class CaRequestLogController {

    @Autowired
    CaRequestLogService caRequestLogService;
    
    @PostMapping(RestPath.GET_CA_REQUEST_LOG)
    public ResponseEntity<?> getGantryAccess(HttpServletRequest httpServletRequest, @RequestBody PaginDto<CaRequestLogDto> pagin) {
        
        caRequestLogService.search(pagin);
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    @PostMapping(RestPath.CA_REQUEST_LOG)
    public ResponseEntity<?> save(HttpServletRequest httpServletRequest, @RequestBody CaRequestLogDto dto) {
        
        caRequestLogService.save(dto);
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
}
