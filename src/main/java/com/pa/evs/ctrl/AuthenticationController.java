package com.pa.evs.ctrl;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.constant.RestPath;
import com.pa.evs.dto.LoginRequestDto;
import com.pa.evs.dto.LoginResponseDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.AuthenticationService;

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
    public ResponseDto<LoginResponseDto> createAuthenticationToken(@RequestBody LoginRequestDto loginRequestDTO) {
        return authenticationService.login(loginRequestDTO);
    }

    @PostMapping(value = {RestPath.USERS})
    public Object getUsers(@RequestBody PaginDto<UserDto> pagin) {
        authenticationService.getUsers(pagin);
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
    
    @PostConstruct
    public void init() {
    	authenticationService.initDataAuths();
    }
}
