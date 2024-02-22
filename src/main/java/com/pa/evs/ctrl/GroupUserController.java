package com.pa.evs.ctrl;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.sv.AuthenticationService;
import com.pa.evs.sv.GroupUserService;

import springfox.documentation.annotations.ApiIgnore;


@RestController
@ApiIgnore
public class GroupUserController {

    static final Logger logger = LogManager.getLogger(GroupUserController.class);

    @Value("${evs.pa.report.jasperDir}")
    private String jasperDir;

    @Autowired private GroupUserService groupUserService;
    
    @Autowired private AuthenticationService authenticationService;

    @PostMapping("/api/groupUsers")
    public ResponseEntity<Object> groupUsers(HttpServletRequest httpServletRequest, @RequestBody PaginDto<GroupUserDto> pagin) throws Exception {
        try {
        	groupUserService.getGroupUser(pagin);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@PostMapping("/api/myGroupUsers")
    public ResponseEntity<Object> myGroupUsers(
    		HttpServletRequest httpServletRequest, 
    		@RequestParam(required = false) String isOwner,
    		@RequestBody PaginDto pagin
    		) throws Exception {
        try {
        	if ("true".equalsIgnoreCase(pagin.getOptions().get("isOwner") + "")) {
        		pagin.setResults(authenticationService.getSubGroupOwner());
        		pagin.setTotalPages(pagin.getResults().size());
        	} else {
        		pagin.setResults(authenticationService.getSubGroupOfUser((String) pagin.getOptions().get("email") ));
        		pagin.setTotalPages(pagin.getResults().size());
        	}
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }  

    @SuppressWarnings({ "rawtypes", "unchecked" })
	@PostMapping("/api/getRoleOfMemberSubGroup")
    public ResponseEntity<Object> getSubGroupMemberRoles(
    		HttpServletRequest httpServletRequest, 
    		@RequestParam(required = false) String isOwner,
    		@RequestBody PaginDto pagin
    		) throws Exception {
        try {
        	authenticationService.getRoleOfMemberSubGroup(pagin);
        } catch (Exception e) {e.printStackTrace();
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    } 
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@PostMapping("/api/getUserOfSubGroup")
    public ResponseEntity<Object> getUserOfSubGroup(
    		HttpServletRequest httpServletRequest, 
    		@RequestParam(required = false) String isOwner,
    		@RequestBody PaginDto pagin
    		) throws Exception {
        try {
    		pagin.setResults(authenticationService.getUserOfSubGroup(pagin.getOptions()));
    		pagin.setTotalPages(pagin.getResults().size());
        } catch (Exception e) {e.printStackTrace();
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
    
    @PostMapping("/api/subGroupUser/create")
    public ResponseEntity<Object> createSubGroupUser(@RequestBody Map<String, Object> payload) throws IOException {
        try {
        	authenticationService.saveSubGroup(payload);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping("/api/subGroupUser/addUserToSubGroup")
    public ResponseEntity<Object> addUserToSubGroup(@RequestBody Map<String, Object> payload) throws IOException {
        try {
        	authenticationService.addUserToSubGroup(payload);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping("/api/subGroupUser/addRoleToSubGroupMember")
    public ResponseEntity<Object> addRoleToSubGroupMember(@RequestBody Map<String, Object> payload) throws IOException {
        try {
        	authenticationService.addRoleToSubGroupMember(payload);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping("/api/subGroupUser/removeUserFromSubGroup")
    public ResponseEntity<Object> removeUserFromSubGroup(@RequestBody Map<String, Object> payload) throws IOException {
        try {
        	authenticationService.removeUserFromSubGroup(payload);
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
    
    @DeleteMapping("/api/subGroupUser/delete/{id}")
    public ResponseEntity<Object> deleteSubGroupUser(HttpServletRequest httpServletRequest, @PathVariable final Long id){
        try {
        	authenticationService.deleteSubGroup(id);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
}
