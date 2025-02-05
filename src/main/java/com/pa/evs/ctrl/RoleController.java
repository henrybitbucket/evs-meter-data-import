package com.pa.evs.ctrl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.sv.RoleService;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;


@RestController
@Hidden
public class RoleController {

    static final Logger logger = LogManager.getLogger(RoleController.class);

    @Value("${evs.pa.report.jasperDir}")
    private String jasperDir;

    @Autowired private RoleService roleService;

    @PostMapping("/api/roles")
    public ResponseEntity<Object> getRoles(HttpServletRequest httpServletRequest, @RequestBody PaginDto<RoleDto> pagin) throws Exception {
        try {
        	roleService.getRoles(pagin);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }

    @PostMapping("/api/role/create")
    public ResponseEntity<Object> createRole(@RequestBody RoleDto dto) throws IOException {
        try {
        	roleService.createRole(dto);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }

    @PutMapping("/api/role/update")
    public ResponseEntity<Object> updateRole(@RequestBody RoleDto dto) throws IOException {
        try {
        	roleService.updateRole(dto);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }

    @DeleteMapping("/api/role/delete/{id}")
    public ResponseEntity<Object> deleteRole(HttpServletRequest httpServletRequest, @PathVariable final Long id){
        try {
        	roleService.deleteRole(id);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping("/api/permissions")
    public ResponseEntity<Object> getPermissions(HttpServletRequest httpServletRequest, @RequestBody PaginDto<PermissionDto> pagin) throws Exception {
        try {
        	roleService.getPermissions(pagin);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
    

}
