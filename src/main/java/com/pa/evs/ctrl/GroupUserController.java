package com.pa.evs.ctrl;

import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.sv.GroupUserService;
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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


@RestController
public class GroupUserController {

    static final Logger logger = LogManager.getLogger(GroupUserController.class);

    @Value("${evs.pa.report.jasperDir}")
    private String jasperDir;

    @Autowired private GroupUserService groupUserService;

    @PostMapping("/api/groupUsers")
    public ResponseEntity<Object> getRoles(HttpServletRequest httpServletRequest, @RequestBody PaginDto<GroupUserDto> pagin) throws Exception {
        try {
        	groupUserService.getGroupUser(pagin);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }

    @PostMapping("/api/groupUser/create")
    public ResponseEntity<Object> createGroupUser(@RequestBody GroupUserDto dto) throws IOException {
        try {
        	groupUserService.createGroupUser(dto);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }

    @PutMapping("/api/groupUser/update")
    public ResponseEntity<Object> updateGroupUser(@RequestBody GroupUserDto dto) throws IOException {
        try {
        	groupUserService.updateGroupUser(dto);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }

    @DeleteMapping("/api/groupUser/delete/{id}")
    public ResponseEntity<Object> deleteGroupUser(HttpServletRequest httpServletRequest, @PathVariable final Long id){
        try {
        	groupUserService.deleteGroupUser(id);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
}
