package com.pa.evs.ctrl;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.constant.RestPath;
import com.pa.evs.dto.ChangePasswordDto;
import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.LoginRequestDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.PlatformUserLoginDto;
import com.pa.evs.dto.ResetPasswordDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.AuthenticationService;
import com.pa.evs.sv.EVSPAService;

@RestController
public class AuthenticationController {
	
	static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);

	ExecutorService ex = Executors.newFixedThreadPool(5);
	
	static final String ACCESS_TOKEN = "access_token";
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	static final String RGX = "^(.*\\.)([^\\.]+\\.[^\\.]+)$";

    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    ApplicationContext applicationContext;

    @PostMapping(value = {RestPath.LOGIN1, RestPath.LOGIN})
    public ResponseDto<? extends Object> createAuthenticationToken(@RequestBody LoginRequestDto loginRequestDTO) {
        try {
        	return authenticationService.login(loginRequestDTO);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    }
    
    @PostMapping(value = {RestPath.USERS})
    public Object getUsers(@RequestBody PaginDto<UserDto> pagin) {
        authenticationService.getUsers(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USERPERMISSION})
    public Object getPermissionsOfUser(@RequestBody PaginDto<UserDto> pagin) {
        authenticationService.getPermissionsOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USERROLELOGGING})
    public Object getRoleOfUserLogin(@RequestBody PaginDto<RoleDto> pagin) {
        authenticationService.getRoleOfUserLogin(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USERROLE})
    public Object getRoleOfUser(@RequestBody PaginDto<RoleDto> pagin) {
        authenticationService.getRoleOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @Secured(value = "SUPER_ADMIN")
    @GetMapping(value = {RestPath.USERPLATFORM})
    public Object getPfOfUser(@RequestParam(required = true) String email) {
        Object pfs = authenticationService.getPfOfUser(email);
        return ResponseDto.<Object>builder().success(true).response(pfs).build();
    }
    
    @Secured(value = "SUPER_ADMIN")
    @PostMapping(value = {RestPath.USERPLATFORM})
    public Object savePfOfUser(@RequestBody PlatformUserLoginDto dto) {
        try {
        	authenticationService.savePfOfUser(dto);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {RestPath.USERGROUP})
    public Object getGroupOfUser(@RequestBody PaginDto<GroupUserDto> pagin) {
        authenticationService.getGroupOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    @PostMapping(value = {RestPath.EACHUSERPERMISSION})
    public Object getPermissionsEachUser(@RequestBody PaginDto<PermissionDto> pagin) {
        authenticationService.getPermissionsEachUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USER})
    public Object saveUser(@RequestBody UserDto user) {
        try {
        	authenticationService.save(user);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    	
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {RestPath.UPDATEROLE})
    public ResponseEntity<Object> saveRole(@RequestBody UserDto user) {
        try {
        	authenticationService.saveRole(user);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.UPDATEGROUP})
    public ResponseEntity<Object> linkGroupUser(@RequestBody UserDto dto) throws IOException {
        try {
        	authenticationService.saveGroup(dto);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.UPDATEPERMISSON})
    public ResponseEntity<Object> linkPermission(@RequestBody UserDto dto) throws IOException {
        try {
        	authenticationService.savePermission(dto);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }

    @GetMapping(value = {RestPath.WHOAMI, RestPath.WHOAMI1})
    public ResponseDto<JwtUser> getUser(HttpServletRequest httpServletRequest){
        return authenticationService.getUser(httpServletRequest);
    }
    
    @DeleteMapping(value = {RestPath.USER + "/{id}"})
    public Object removeUser(@PathVariable Long id){
        
        try {
        	authenticationService.removeUserById(id);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    	
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @GetMapping(RestPath.USER_USERNAME)
    public ResponseEntity<?> getUsernameById(HttpServletRequest httpServletRequest, @RequestParam(name = "user_id") Long userId) {
    	return ResponseEntity.<Object>ok(
        		ResponseDto.<Object>builder()
        		.success(true)
        		.response(authenticationService.getUsernameById(userId))
        		.build());
    }
    
    @GetMapping(RestPath.USER_USER_DETAILS)
    public ResponseEntity<?> getUserById(HttpServletRequest httpServletRequest, @RequestParam(name = "user_id") Long userId) {
    	return ResponseEntity.<Object>ok(
        		ResponseDto.<Object>builder()
        		.success(true)
        		.response(authenticationService.getUserById(userId))
        		.build());
    }
    
    @PostMapping(value = {"/api/user/changePassword"})
    public ResponseDto<? extends Object> changePassword(@RequestBody ChangePasswordDto changePasswordDto) {
        try {
        	authenticationService.changePwd(changePasswordDto);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {"/api/user/resetPassword"})
    public ResponseDto<? extends Object> resetPassword(@RequestBody ResetPasswordDto resetPasswordDto) {
        try {
        	authenticationService.resetPwd(resetPasswordDto);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {"/api/user/updatePhoneNumber"})
    public ResponseDto<? extends Object> updatePhoneNumber(@RequestBody Map<String, Object> dto) {
        try {
        	String phoneNumber = (String) dto.get("phoneNumber");
        	authenticationService.updatePhoneNumber(phoneNumber);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    // {"email": "henry@gmail.com", "otpType": "sms", "actionType": "reset_pwd"}
    @PostMapping(value = {"/api/otp"})
    public ResponseEntity<Object> sendOtp(@RequestBody Map<String, Object> dto) throws IOException {
        try {
        	return ResponseEntity.ok(authenticationService.sendOtp(dto));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
    }
    
    @PostMapping(value = {"/api/user/preLogin"})
    public ResponseEntity<?> preLogin(HttpServletRequest httpServletRequest, @RequestParam(name = "username") String username) {
    	try {
        	return ResponseEntity.ok(ResponseDto.<Object>builder().success(true).response(authenticationService.preLogin(username)).build());
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
    }
    
    @PostConstruct
    public void init() {
    	authenticationService.initDataAuths();
    }
}
