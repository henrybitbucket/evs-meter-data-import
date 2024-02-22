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
import com.pa.evs.dto.CreateUserDto;
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
import com.pa.evs.model.PlatformUserLogin;
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
	private PlatformUserLoginRepository platformUserLoginRepository;

    @PostMapping(value = {RestPath.LOGIN1, RestPath.LOGIN})
    public ResponseDto<? extends Object> createAuthenticationToken(@RequestBody LoginRequestDto loginRequestDTO) {
        try {
        	return authenticationService.login(loginRequestDTO);
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
    
    @GetMapping(value = {RestPath.USERPLATFORM})
    @ApiIgnore
    public Object getPfOfUser(@RequestParam(required = true) String email) {
    	try {
			if (!SecurityUtils.hasAnyRole(AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
				throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
			}
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
			if (!SecurityUtils.hasAnyRole(AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
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

    @GetMapping(value = {RestPath.WHOAMI, RestPath.WHOAMI1})
    public ResponseDto<JwtUser> getUser(HttpServletRequest httpServletRequest){
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
    
    @PostMapping(value = {"/api/user/preLogin"})
    @ApiIgnore
    public ResponseEntity<?> preLogin(HttpServletRequest httpServletRequest, @RequestParam(name = "username") String username) {
    	try {
        	return ResponseEntity.ok(ResponseDto.<Object>builder().success(true).response(authenticationService.preLogin(username)).build());
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
    public Object createNewUser(@RequestBody CreateUserDto dto) {
        try {
        	AppCodeSelectedHolder.set("DMS");
        	
        	UserDto userDto = new UserDto();
        	userDto.setEmail(dto.getEmail());
        	userDto.setFullName(dto.getFullName());
        	userDto.setFirstName(dto.getFirstName());
        	userDto.setLastName(dto.getLastName());
        	userDto.setAvatar(dto.getAvatar());
        	userDto.setIdentification(dto.getIdentification());
        	userDto.setPhoneNumber(dto.getPhoneNumber());
        	userDto.setStatus(dto.getStatus());
        	userDto.setPassword(dto.getPassword());
        	userDto.setLoginOtpRequire(dto.getLoginOtpRequire());
        	authenticationService.save(userDto);
        	
        	PlatformUserLogin pf = platformUserLoginRepository.findByEmailAndName(dto.getEmail(), "OTHER");
    		if (pf == null) {
    			PlatformUserLogin newPf = new PlatformUserLogin();
    			newPf.setActive(false);
    			newPf.setEmail(dto.getEmail());
    			newPf.setName("OTHER");
    			newPf.setStartTime(0l);
    			newPf.setEndTime(4102444800000l);
    			platformUserLoginRepository.save(newPf);
    		} else {
    			pf.setActive(false);
    			pf.setStartTime(0l);
    			pf.setEndTime(4102444800000l);
        		platformUserLoginRepository.save(pf);	
    		}
    		
    		pf = platformUserLoginRepository.findByEmailAndName(dto.getEmail(), "MOBILE");
    		if (pf == null) {
    			PlatformUserLogin newPf = new PlatformUserLogin();
    			newPf.setActive(true);
    			newPf.setEmail(dto.getEmail());
    			newPf.setName("MOBILE");
    			newPf.setStartTime(0l);
    			newPf.setEndTime(4102444800000l);
    			platformUserLoginRepository.save(newPf);
    		} else {
    			pf.setActive(true);
    			pf.setStartTime(0l);
    			pf.setEndTime(4102444800000l);
        		platformUserLoginRepository.save(pf);	
    		}
    		
        	return ResponseDto.<Object>builder().success(true).build();
		} catch (Exception e) {
			return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
		} finally {
			AppCodeSelectedHolder.remove();
		}
    }
    
    @PostConstruct
    public void init() {
    	authenticationService.initDataAuths();
    }
}
