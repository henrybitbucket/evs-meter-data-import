package com.pa.evs.ctrl;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
import com.pa.evs.dto.CompanyDto;
import com.pa.evs.dto.CreateDMSAppUserDto;
import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.LoginRequestDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.PlatformUserLoginDto;
import com.pa.evs.dto.ProjectTagDto;
import com.pa.evs.dto.ResetPasswordDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.repository.PlatformUserLoginRepository;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.AuthenticationService;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.SecurityUtils;

import springfox.documentation.annotations.ApiIgnore;

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
    
	@Autowired
	PlatformUserLoginRepository platformUserLoginRepository;

    @PostMapping(value = {RestPath.LOGIN1, RestPath.LOGIN})
    public ResponseDto<? extends Object> createAuthenticationToken(@RequestBody LoginRequestDto loginRequestDTO) {
        try {
        	return authenticationService.login(loginRequestDTO);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    }
    
    @PostMapping(value = {"/api/logout"})
    public ResponseDto<?> logout(HttpServletRequest httpServletRequest) {
        try {
        	String accessToken = httpServletRequest.getHeader("Authorization");
        	authenticationService.logout(accessToken);
        	return ResponseDto.builder().success(true).message("Logout successfully!").build();
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    }
    
    @PostMapping(value = {RestPath.USERS})
    @ApiIgnore
    public Object getUsers(@RequestBody PaginDto<UserDto> pagin) {
        authenticationService.getUsers(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USERPERMISSION})
    @ApiIgnore
    public Object getPermissionsOfUser(@RequestBody PaginDto<UserDto> pagin) {
        authenticationService.getPermissionsOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USERROLELOGGING})
    @ApiIgnore
    public Object getRoleOfUserLogin(@RequestBody PaginDto<RoleDto> pagin) {
        authenticationService.getRoleOfUserLogin(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USERROLE})
    @ApiIgnore
    public Object getRoleOfUser(@RequestBody PaginDto<RoleDto> pagin) {
        authenticationService.getRoleOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USER_PROJECT_LOGGING})
    @ApiIgnore
    public Object getProjectOfUserLogin(@RequestBody PaginDto<ProjectTagDto> pagin) {
        authenticationService.getProjectTagOfUserLogin(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USER_PROJECT})
    @ApiIgnore
    public Object getProjectOfUser(@RequestBody PaginDto<ProjectTagDto> pagin) {
        authenticationService.getProjectTagOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USER_COMPANY})
    @ApiIgnore
    public Object getCompanyOfUser(@RequestBody PaginDto<CompanyDto> pagin) {
        authenticationService.getCompanyOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    
    @GetMapping(value = {RestPath.USERPLATFORM})
    @ApiIgnore
    public Object getPfOfUser(@RequestParam(required = true) String email) {
    	try {
	        Object pfs = authenticationService.getPfOfUser(email);
	        return ResponseDto.<Object>builder().success(true).response(pfs).build();
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    }
    
    @PostMapping(value = {RestPath.USERPLATFORM})
    @ApiIgnore
    public Object savePfOfUser(@RequestBody PlatformUserLoginDto dto) {
        try {
			if (!SecurityUtils.hasAnyRole("SUPER_ADMIN", AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
				throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
        	authenticationService.savePfOfUser(dto);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {RestPath.USERGROUP})
    @ApiIgnore
    public Object getGroupOfUser(@RequestBody PaginDto<GroupUserDto> pagin) {
        authenticationService.getGroupOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    @PostMapping(value = {RestPath.EACHUSERPERMISSION})
    @ApiIgnore
    public Object getPermissionsEachUser(@RequestBody PaginDto<PermissionDto> pagin) {
        authenticationService.getPermissionsEachUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USER})
    @ApiIgnore
    public Object saveUser(@RequestBody UserDto user) {
        try {
        	authenticationService.save(user);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    	
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {RestPath.UPDATEROLE})
    @ApiIgnore
    public ResponseEntity<Object> saveRole(@RequestBody UserDto user) {
        try {
        	authenticationService.saveRole(user);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.UPDATEGROUP})
    @ApiIgnore
    public ResponseEntity<Object> linkGroupUser(@RequestBody UserDto dto) throws IOException {
        try {
        	authenticationService.saveGroup(dto);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.UPDATEPERMISSON})
    @ApiIgnore
    public ResponseEntity<Object> linkPermission(@RequestBody UserDto dto) throws IOException {
        try {
        	authenticationService.savePermission(dto);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.UPDATEPROJECT})
    @ApiIgnore
    public ResponseEntity<Object> saveProject(@RequestBody UserDto user) {
        try {
        	authenticationService.saveProject(user);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.UPDATECOMPANY})
    @ApiIgnore
    public ResponseEntity<Object> saveCompany(@RequestBody UserDto user) {
        try {
        	authenticationService.saveCompany(user);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @GetMapping(value = {RestPath.WHOAMI, RestPath.WHOAMI1})
    public ResponseDto<JwtUser> whoami(HttpServletRequest httpServletRequest){
        return authenticationService.getUser(httpServletRequest);
    }
    
    @DeleteMapping(value = {RestPath.USER + "/{id}"})
    @ApiIgnore
    public Object removeUser(@PathVariable Long id){
        
        try {
        	authenticationService.removeUserById(id);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    	
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @GetMapping(RestPath.USER_USERNAME)
    @ApiIgnore
    public ResponseEntity<?> getUsernameById(HttpServletRequest httpServletRequest, @RequestParam(name = "user_id") Long userId) {
    	return ResponseEntity.<Object>ok(
        		ResponseDto.<Object>builder()
        		.success(true)
        		.response(authenticationService.getUsernameById(userId))
        		.build());
    }
    
    @GetMapping(RestPath.USER_USER_DETAILS)
    @ApiIgnore
    public ResponseEntity<?> getUserById(HttpServletRequest httpServletRequest, @RequestParam(name = "user_id") Long userId) {
    	return ResponseEntity.<Object>ok(
        		ResponseDto.<Object>builder()
        		.success(true)
        		.response(authenticationService.getUserById(userId))
        		.build());
    }
    
    @PostMapping(value = {"/api/user/changePassword"})
    @ApiIgnore
    public ResponseDto<? extends Object> changePassword(@RequestBody ChangePasswordDto changePasswordDto) {
        try {
        	authenticationService.changePwd(changePasswordDto);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {"/api/user/resetPassword"})
    @ApiIgnore
    public ResponseDto<? extends Object> resetPassword(@RequestBody ResetPasswordDto resetPasswordDto) {
        try {
        	authenticationService.resetPwd(resetPasswordDto);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {"/api/user/updatePhoneNumber"})
    @ApiIgnore
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
    @ApiIgnore
    public ResponseEntity<Object> sendOtp(@RequestBody Map<String, Object> dto) throws IOException {
        try {
        	return ResponseEntity.ok(authenticationService.sendOtp(dto));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
    }
    
    @PostMapping(value = {"/api/user/credential-type"})
    @ApiIgnore
    public ResponseEntity<?> getCredentialType(HttpServletRequest httpServletRequest, @RequestParam(name = "username") String username, @RequestBody(required = false) Map<String, Object> payload) {
    	try {
    		if (payload != null && payload.get("username") != null) {
    			username = (String) payload.get("username");
    		}
    		Object cre = authenticationService.getCredentialType(username);
    		if (cre == null) {
    			throw new ApiException("User not found!");	
    		}
    		
        	return ResponseEntity.ok(ResponseDto.<Object>builder().success(true).response(cre).build());
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
    }
    
    @PostMapping(value = {"/api/user/validate-password"})
    @ApiIgnore
    public ResponseEntity<?> validatePassword(HttpServletRequest httpServletRequest, @RequestBody LoginRequestDto loginRequestDTO) {
    	try {
        	return ResponseEntity.ok(ResponseDto.<Object>builder().success(true).response(authenticationService.validatePassword(loginRequestDTO)).build());
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
    }
    
    @PostMapping(value = {RestPath.CREATE_NEW_USER})
    public Object createNewUser(@RequestBody CreateDMSAppUserDto dto, HttpServletRequest request) {
        try {
        	if (StringUtils.isBlank(request.getHeader("A_C"))) {
        		AppCodeSelectedHolder.set("DMS");
        	} else {
        		AppCodeSelectedHolder.set(request.getHeader("A_C"));
        	}
        	authenticationService.saveDMSAppUser(dto);
    		
        	return ResponseDto.<Object>builder().success(true).response(dto).build();
		} catch (Exception e) {
			return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
		} finally {
			AppCodeSelectedHolder.remove();
		}
    }
}
