package com.pa.evs.ctrl;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.constant.RestPath;
import com.pa.evs.dto.ChangePasswordDto;
import com.pa.evs.dto.CompanyDto;
import com.pa.evs.dto.CreateDMSAppUserDto;
import com.pa.evs.dto.DMSApplicationUserDto;
import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.LocksAccessPermisisonDto;
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
import com.pa.evs.model.LocksAccessPermisison;
import com.pa.evs.repository.PlatformUserLoginRepository;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.AuthenticationService;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.CsvUtils;
import com.pa.evs.utils.SecurityUtils;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class AuthenticationController {
	
	static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);

	ExecutorService ex = Executors.newFixedThreadPool(5);
	
	static final String ACCESS_TOKEN = "access_token";
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	static final String RGX = "^(.*\\.)([^\\.]+\\.[^\\.]+)$";
	
	@Value("${evs.nus.client.id}")
	private String nusClientId;

	@Value("${evs.nus.client.secret}")
	private String nusClientSecret;

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
    @Hidden
    public Object getUsers(@RequestBody PaginDto<UserDto> pagin) {
    	Map<String, Object> options = pagin.getOptions();
    	Boolean isCreateApplication = options.get("isCreateApplication") != null ?  (Boolean) options.get("isCreateApplication") : null;
    	
        authenticationService.getUsers(pagin);
        if (BooleanUtils.isTrue(isCreateApplication)) {
        	PaginDto<DMSApplicationUserDto> paginApplUser = new PaginDto<>();
        	paginApplUser.setLimit(pagin.getLimit());
        	paginApplUser.setOffset(pagin.getOffset());
        	paginApplUser.setTotalRows(pagin.getTotalRows());
        	
        	pagin.getResults().forEach(user -> {
        		DMSApplicationUserDto dto = DMSApplicationUserDto
        				.builder()
        				.id(user.getId())
        				.username(user.getUsername())
        				.email(user.getEmail())
        				.firstName(user.getFirstName())
        				.lastName(user.getLastName())
    					.phoneNumber(user.getPhoneNumber()).build();
        		paginApplUser.getResults().add(dto);
        	});
        	return paginApplUser;
        }
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USERPERMISSION})
    @Hidden
    public Object getPermissionsOfUser(@RequestBody PaginDto<UserDto> pagin) {
        authenticationService.getPermissionsOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USERROLELOGGING})
    @Hidden
    public Object getRoleOfUserLogin(@RequestBody PaginDto<RoleDto> pagin) {
        authenticationService.getRoleOfUserLogin(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USERROLE})
    @Hidden
    public Object getRoleOfUser(@RequestBody PaginDto<RoleDto> pagin) {
        authenticationService.getRoleOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USER_PROJECT_LOGGING})
    @Hidden
    public Object getProjectOfUserLogin(@RequestBody PaginDto<ProjectTagDto> pagin) {
        authenticationService.getProjectTagOfUserLogin(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USER_PROJECT})
    @Hidden
    public Object getProjectOfUser(@RequestBody PaginDto<ProjectTagDto> pagin) {
        authenticationService.getProjectTagOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USER_COMPANY})
    @Hidden
    public Object getCompanyOfUser(@RequestBody PaginDto<CompanyDto> pagin) {
        authenticationService.getCompanyOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {"/api/user/sync-access"})
    @Hidden
    public Object syncAccess(@RequestParam(required = true) String fromUsername, @RequestParam(required = true) String toUsername) {
        
    	try {
            authenticationService.syncAccess(fromUsername, toUsername);
            return ResponseDto.<Object>builder().success(true).build();
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    }
    
    
    @GetMapping(value = {RestPath.USERPLATFORM})
    @Hidden
    public Object getPfOfUser(@RequestParam(required = true) String email) {
    	try {
	        Object pfs = authenticationService.getPfOfUser(email);
	        return ResponseDto.<Object>builder().success(true).response(pfs).build();
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    }
    
    @PostMapping(value = {RestPath.USERPLATFORM})
    @Hidden
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
    @Hidden
    public Object getGroupOfUser(@RequestBody PaginDto<GroupUserDto> pagin) {
        authenticationService.getGroupOfUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    @PostMapping(value = {RestPath.EACHUSERPERMISSION})
    @Hidden
    public Object getPermissionsEachUser(@RequestBody PaginDto<PermissionDto> pagin) {
        authenticationService.getPermissionsEachUser(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.USER})
    @Hidden
    public Object saveUser(@RequestBody UserDto user) {
        try {
        	authenticationService.save(user);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    	
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {RestPath.UPDATEROLE})
    @Hidden
    public ResponseEntity<Object> saveRole(@RequestBody UserDto user) {
        try {
        	authenticationService.saveRole(user);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.UPDATEGROUP})
    @Hidden
    public ResponseEntity<Object> linkGroupUser(@RequestBody UserDto dto) throws IOException {
        try {
        	authenticationService.saveGroup(dto);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.UPDATEPERMISSON})
    @Hidden
    public ResponseEntity<Object> linkPermission(@RequestBody UserDto dto) throws IOException {
        try {
        	authenticationService.savePermission(dto);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.UPDATEPROJECT})
    @Hidden
    public ResponseEntity<Object> saveProject(@RequestBody UserDto user) {
        try {
        	authenticationService.saveProject(user);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.UPDATECOMPANY})
    @Hidden
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
    @Hidden
    public Object removeUser(@PathVariable Long id){
        
        try {
        	authenticationService.removeUserById(id);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
    	
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @GetMapping(RestPath.USER_USERNAME)
    @Hidden
    public ResponseEntity<?> getUsernameById(HttpServletRequest httpServletRequest, @RequestParam(name = "user_id") Long userId) {
    	return ResponseEntity.<Object>ok(
        		ResponseDto.<Object>builder()
        		.success(true)
        		.response(authenticationService.getUsernameById(userId))
        		.build());
    }
    
    @GetMapping(RestPath.USER_USER_DETAILS)
    @Hidden
    public ResponseEntity<?> getUserById(HttpServletRequest httpServletRequest, @RequestParam(name = "user_id") Long userId) {
    	return ResponseEntity.<Object>ok(
        		ResponseDto.<Object>builder()
        		.success(true)
        		.response(authenticationService.getUserById(userId))
        		.build());
    }
    
    @PostMapping(value = {"/api/user/changePassword"})
    @Hidden
    public ResponseDto<? extends Object> changePassword(@RequestBody ChangePasswordDto changePasswordDto) {
        try {
        	authenticationService.changePwd(changePasswordDto);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {"/api/user/resetPassword"})
    @Hidden
    public ResponseDto<? extends Object> resetPassword(@RequestBody ResetPasswordDto resetPasswordDto) {
        try {
        	authenticationService.resetPwd(resetPasswordDto);
		} catch (Exception e) {
			return ResponseDto.<Object>builder().success(false).message(e.getMessage()).build();
		}
        return ResponseDto.<Object>builder().success(true).build();
    }
    
    @PostMapping(value = {"/api/user/updatePhoneNumber"})
    @Hidden
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
    
    @PostMapping(value = {"/api/user/credential-type"})
    @Hidden
    public ResponseEntity<?> getCredentialType(HttpServletRequest httpServletRequest, @RequestParam(name = "username") String username, @RequestBody(required = false) Map<String, Object> payload) {
    	try {
    		if (payload != null && payload.get("username") != null) {
    			username = ((String) payload.get("username")).toLowerCase();
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
    @Hidden
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
    
    @PostMapping("/api/user/upload")
    public ResponseEntity<Object> uploadMsiSdn(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "file") final MultipartFile file) throws Exception {

        try {
    		List<String> headers = Arrays.asList(
    				"No.","Company","Hours","Days","Contact Name","Email","Contact Mobile","Pwd", "Message"
    				);
    		List<Map<String, Object>> dtos = authenticationService.handleUploadDMSUsers(file);
    		File csv = CsvUtils.toCsv(headers, dtos, (idx, it, l) -> {
            	
                List<String> record = new ArrayList<>();

                record.add(StringUtils.isNotBlank((String) it.get("No.")) ? (String) it.get("No.") : "");
                record.add(StringUtils.isNotBlank((String) it.get("Company")) ? (String) it.get("Company") : "");
                record.add(StringUtils.isNotBlank((String) it.get("Hours")) ? (String) it.get("Hours") : "");
                record.add(StringUtils.isNotBlank((String) it.get("Days")) ? (String) it.get("Days") : "");
                record.add(StringUtils.isNotBlank((String) it.get("Contact Name")) ? (String) it.get("Contact Name") : "");
                record.add(StringUtils.isNotBlank((String) it.get("Email")) ? (String) it.get("Email") : "");
                record.add(StringUtils.isNotBlank((String) it.get("Contact Mobile")) ? (String) it.get("Contact Mobile") : "");
                record.add(StringUtils.isNotBlank((String) it.get("Pwd")) ? (String) it.get("Pwd") : "");
                record.add(StringUtils.isNotBlank((String) it.get("Message")) ? (String) it.get("Message") : "Success");
                
                return CsvUtils.postProcessCsv(record);
            }, CsvUtils.buildPathFile("import_user_result_" + System.currentTimeMillis() + ".csv"), 1l);
        	
        	String fileName = file.getName();
            
        	SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        	
        	try (FileOutputStream logFileFos = new FileOutputStream(CsvUtils.EXPORT_TEMP + "/logs/import_user_result_" + sf.format(new Date()) + "_" + System.currentTimeMillis() + ".csv"); 
        			FileInputStream fis = new FileInputStream(csv)) {
        		IOUtils.copy(fis, logFileFos);
        	}
        	
            try (FileInputStream fis = new FileInputStream(csv)) {
                response.setContentLengthLong(csv.length());
                response.setHeader(HttpHeaders.CONTENT_TYPE, "application/csv");
                response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
                response.setHeader("name", fileName);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                IOUtils.copy(fis, response.getOutputStream());
            } finally {
                FileUtils.deleteDirectory(csv.getParentFile());
            }
        } catch (Exception e) {
        	LOGGER.error(e.getMessage(), e);
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping(value = {RestPath.CREATE_NEW_ACCESS_PERMISSION})
    @Hidden
    public Object createAccessPermission(@RequestBody LocksAccessPermisisonDto dto, HttpServletRequest request) {
        try {
        	String url = authenticationService.createAccessPermission(dto);
        	if (StringUtils.isNotBlank(url)) {
        		return ResponseDto.<Object>builder().success(true).response(url).build();
        	} else {
        		return ResponseEntity.ok(ResponseDto.builder().success(false).build());
        	}
		} catch (Exception e) {
			return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
		}
    }
    
    @PostMapping(value = {RestPath.GET_ACCESS_PERMISSIONS})
    public Object getAccessPermission(@RequestBody PaginDto<LocksAccessPermisisonDto> pagin) {
        authenticationService.getAccessPermissions(pagin);
        return ResponseDto.<Object>builder().success(true).response(pagin).build();
    }
    
    @PostMapping(value = {RestPath.CREATE_NEW_ACCESS_PERMISSION_FROM_EMAIL})
    @Hidden
    public Object createAccessPermissionFromEmail(HttpServletRequest request) {
        try {
        	authenticationService.accessEmailAndProcessEmail();
        	return ResponseDto.<Object>builder().success(true).build();
		} catch (Exception e) {
			return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
		}
    }
    
    @GetMapping("/api/google/oauth/get-code")
    @Hidden
    public void redirectToGoogle(HttpServletResponse response) throws IOException {
    	String redirectUri = "http://localhost:8080/oauth/callback";
        String scope = "https://www.googleapis.com/auth/userinfo.profile";

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + nusClientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)
                + "&access_type=offline";

        response.sendRedirect(authUrl);
    }

}
