package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.TopFilter.HttpServletRequestHolder;
import com.pa.evs.constant.Message;
import com.pa.evs.constant.ValueConstant;
import com.pa.evs.dto.ChangePasswordDto;
import com.pa.evs.dto.CompanyDto;
import com.pa.evs.dto.CreateDMSAppUserDto;
import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.LoginRequestDto;
import com.pa.evs.dto.LoginResponseDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.PlatformUserLoginDto;
import com.pa.evs.dto.ProjectTagDto;
import com.pa.evs.dto.ResetPasswordDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.exception.customException.AuthenticationException;
import com.pa.evs.exception.customException.DuplicateUserException;
import com.pa.evs.model.AppCode;
import com.pa.evs.model.Company;
import com.pa.evs.model.GroupUser;
import com.pa.evs.model.Login;
import com.pa.evs.model.OTP;
import com.pa.evs.model.Permission;
import com.pa.evs.model.PlatformUserLogin;
import com.pa.evs.model.ProjectTag;
import com.pa.evs.model.Role;
import com.pa.evs.model.RolePermission;
import com.pa.evs.model.SubGroup;
import com.pa.evs.model.SubGroupMember;
import com.pa.evs.model.SubGroupMemberRole;
import com.pa.evs.model.Token;
import com.pa.evs.model.UserAppCode;
import com.pa.evs.model.UserCompany;
import com.pa.evs.model.UserGroup;
import com.pa.evs.model.UserPermission;
import com.pa.evs.model.UserProject;
import com.pa.evs.model.UserRole;
import com.pa.evs.model.Users;
import com.pa.evs.repository.AppCodeRepository;
import com.pa.evs.repository.CompanyRepository;
import com.pa.evs.repository.CountryCodeRepository;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.repository.LoginRepository;
import com.pa.evs.repository.PermissionRepository;
import com.pa.evs.repository.PlatformUserLoginRepository;
import com.pa.evs.repository.ProjectTagRepository;
import com.pa.evs.repository.RolePermissionRepository;
import com.pa.evs.repository.RoleRepository;
import com.pa.evs.repository.SubGroupMemberRepository;
import com.pa.evs.repository.SubGroupMemberRoleRepository;
import com.pa.evs.repository.SubGroupRepository;
import com.pa.evs.repository.UserAppCodeRepository;
import com.pa.evs.repository.UserCompanyRepository;
import com.pa.evs.repository.UserGroupRepository;
import com.pa.evs.repository.UserPermissionRepository;
import com.pa.evs.repository.UserProjectRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.repository.UserRoleRepository;
import com.pa.evs.security.jwt.JwtTokenUtil;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.AuthenticationService;
import com.pa.evs.sv.AuthorityService;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.NotificationService;
import com.pa.evs.sv.SettingService;
import com.pa.evs.utils.ApiResponse;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.SimpleMap;
import com.pa.evs.utils.Utils;

import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;

@Service
@SuppressWarnings("unchecked")
public class AuthenticationServiceImpl implements AuthenticationService {

	static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

	@Autowired
	private UserAppCodeRepository userAppCodeRepository;
	
	@Autowired
	private AppCodeRepository appCodeRepository;

	@Autowired
	private LoginRepository loginRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private UserProjectRepository userProjectRepository;
	
	@Autowired
	private UserCompanyRepository userCompanyRepository;
	
	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private RolePermissionRepository rolePermissionRepository;

	@Autowired
	private UserGroupRepository userGroupRepository;
	
	@Autowired
	private SubGroupRepository subGroupRepository;
	
	@Autowired
	private SubGroupMemberRepository subGroupMemberRepository;
	
	@Autowired
	private SubGroupMemberRoleRepository subGroupMemberRoleRepository;

	@Autowired
	private GroupUserRepository groupUserRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private UserPermissionRepository userPermissionRepository;
	
	@Autowired
	private PlatformUserLoginRepository platformUserLoginRepository;
	
	@Autowired
	private ProjectTagRepository projectTagRepository;

	@Autowired
	AuthorityService authorityService;

	@Autowired
	private ApiResponse apiResponse;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	PasswordEncoder passwordEncoder;
	
	@Autowired
	EVSPAService evsPAService;
	
	@Autowired
	SettingService settingService;
	
	@Autowired
	NotificationService notificationService;

	@Autowired
	EntityManager em;
	
	@Autowired
	CountryCodeRepository countryCodeRepository;
	
	@Value("${jwt.header}")
	private String tokenHeader;

	static final String MSG_USER_NOT_FOUND = "User not found!";

	static final String EMAIL = "email";

	static final String USER_PRINCIPAL_NAME = "userPrincipalName";

	static Map<String, List<Permission>> map = new HashMap<String, List<Permission>>();
	
	List<String> cCodes = new ArrayList<>();

	@Transactional
	@Override
	public ResponseDto<LoginResponseDto> login(LoginRequestDto loginRequestDTO) {
		String email = loginRequestDTO.getEmail();
		String password = loginRequestDTO.getPassword();
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
		} catch (DisabledException e) {
			throw new AuthenticationException(Message.USER_IS_DISABLE, e);
		} catch (BadCredentialsException e) {
			throw new AuthenticationException(Message.INVALID_USERNAME_PASSWORD, e);
		} catch (Exception e) {
			throw new AuthenticationException(Message.INVALID_USERNAME_PASSWORD, e);
		}

		// Reload password post-security so we can generate the token
		final JwtUser userDetails = (JwtUser) userDetailsService.loadUserByUsername(email);
		if (SecurityUtils.getByPassUser() != null) {
			Map<String, Object> claims = new LinkedHashMap<>();
			claims.put("tokenExpireDate", userDetails.getTokenExpireDate());
			final String token = jwtTokenUtil.doGenerateToken(claims, userDetails.getUsername());
			return apiResponse.response(ValueConstant.SUCCESS, ValueConstant.TRUE,
					LoginResponseDto.builder().token(token).authorities(
							userDetails.getAuthorities().stream().map(au -> au.getAuthority()).collect(Collectors.toList()))
							.changePwdRequire(userDetails.getChangePwdRequire())
							.appCodes(userDetails.getAppCodes())
							.phoneNumber(userDetails.getPhoneNumber())
							.email(userDetails.getEmail())
							.firstName(userDetails.getFirstName())
							.lastName(userDetails.getLastName())
							.id(userDetails.getId())
							.build());
		}
		
		PlatformUserLoginDto pf = this.getPfOfUser(userDetails.getEmail())
				.stream()
				.filter(it -> it.getName().equals(StringUtils.isBlank(loginRequestDTO.getPf()) ? "OTHER" : loginRequestDTO.getPf()))
				.findFirst()
				.orElse(null);
		
		if (pf == null || pf.getActive() != Boolean.TRUE 
				|| pf.getStartTime() == null 
				|| pf.getEndTime() == null 
				|| pf.getStartTime() > System.currentTimeMillis() 
				|| pf.getEndTime() < System.currentTimeMillis()) {
			throw new AuthenticationException(Message.USER_IS_DISABLE, new RuntimeException(Message.USER_IS_DISABLE));
		}
		
		Users user = userRepository.findByEmail(userDetails.getEmail());
		if (user.getLastChangePwd() == null || user.getLastChangePwd() <= 0l) {
			user.setLastChangePwd(System.currentTimeMillis());
		}
		
		if (BooleanUtils.isTrue(user.getLoginOtpRequire()) || BooleanUtils.isTrue(user.getFirstLoginOtpRequire())) {
			List<OTP> otps = em.createQuery("FROM OTP where email = '" + email + "' AND otp = '" + loginRequestDTO.getOtp() + "' AND endTime > " + System.currentTimeMillis() + "l  ORDER BY id DESC ").getResultList();
			if (otps.isEmpty() || otps.get(0).getStartTime() > System.currentTimeMillis() || otps.get(0).getEndTime() < System.currentTimeMillis()) {
				throw new RuntimeException("otp invalid!");
			}
			invalidOtp(email, loginRequestDTO.getOtp());
		}
		
		Long pwdValidTimeRange = 90 * 24 * 60 * 60 * 1000l;
		try {
			pwdValidTimeRange = Long.parseLong(AppProps.get("PWD_VALID_IN_MLS", pwdValidTimeRange + ""));
		} catch (Exception e) {
			pwdValidTimeRange = 90 * 24 * 60 * 60 * 1000l;
			LOGGER.error(e.getMessage(), e);
		}

		if ((user.getLastChangePwd() + pwdValidTimeRange) < System.currentTimeMillis()) {
			user.setChangePwdRequire(true);
			userDetails.setChangePwdRequire(true);
			userRepository.save(user);
		}

		final String token = jwtTokenUtil.generateToken(userDetails);
		this.updateLastLogin(email);
		this.loadRoleAndPermission();
		String dmsLockToken = null;
//		if (userDetails.getAppCodes().contains("DMS") && StringUtils.isNotBlank(user.getPhoneNumber())) {
//			if (pwd.length() < 8) {
//				pwd += "0000";
//			}
//			dmsLockToken = ChinaPadLockUtils.getTokenAppChinaLockServer(user.getPhoneNumber().length() <= 12 ? user.getPhoneNumber().substring(1) : user.getLcPhoneNumber(), pwd);
//			if (StringUtils.isBlank(dmsLockToken)) {
//				ChinaPadLockUtils.createUserChinaLockServer(user.getPhoneNumber().length() <= 12 ? user.getPhoneNumber().substring(1) : user.getLcPhoneNumber(), userDetails.getPhone(), pwd);
//			}
//			dmsLockToken = ChinaPadLockUtils.getTokenAppChinaLockServer(user.getPhoneNumber().length() <= 12 ? user.getPhoneNumber().substring(1) : user.getLcPhoneNumber(), pwd);
//		}
		return apiResponse.response(ValueConstant.SUCCESS, ValueConstant.TRUE,
				LoginResponseDto.builder().token(token).authorities(
						userDetails.getAuthorities().stream().map(au -> au.getAuthority()).collect(Collectors.toList()))
						.changePwdRequire(userDetails.getChangePwdRequire())
						.appCodes(userDetails.getAppCodes())
						.phoneNumber(userDetails.getPhoneNumber())
						.email(userDetails.getEmail())
						.firstName(user.getFirstName())
						.lastName(user.getLastName())
						.id(userDetails.getId())
						.lockToken(userDetails.getAppCodes().contains("DMS") ? dmsLockToken : null)
						.build());
	}
	


	@Transactional
	@Override
	public void logout(String token) {
		Claims claims = jwtTokenUtil.getAllClaimsFromToken(token);
        final String username = claims.getSubject();
        final String tokenId = new ArrayList<>(claims.getAudience()).get(0);
        
        Optional<Login> loginOpt = loginRepository.findByTokenIdAndUserName(tokenId, username);
        
        if (!loginOpt.isPresent()) {
        	throw new RuntimeException("Token invalid!");
        }
        loginRepository.deleteByTokenIdAndUserName(tokenId, username);
	}
	
	@Override
	@Transactional
	public void changePwd(ChangePasswordDto changePasswordDto) {
		Users user = userRepository.findByEmail(SecurityUtils.getEmail());
		if (user.getChangePwdRequire() == Boolean.TRUE && "ON".equalsIgnoreCase(AppProps.get("OTP_MODE_CHANGE_PWD"))) {
			List<OTP> otps = em.createQuery("FROM OTP where email = '" + SecurityUtils.getEmail() + "' AND otp = '" + changePasswordDto.getOtp() + "' AND endTime > " + System.currentTimeMillis() + "l  ORDER BY id DESC ").getResultList();
			if (otps.isEmpty() || otps.get(0).getStartTime() > System.currentTimeMillis() || otps.get(0).getEndTime() < System.currentTimeMillis()) {
				throw new RuntimeException("otp invalid!");
			}
			invalidOtp(SecurityUtils.getEmail(), changePasswordDto.getOtp());
		}


		Utils.validatePwd(changePasswordDto.getPassword(), user.getLastPwd());
		user.setPassword(passwordEncoder.encode(changePasswordDto.getPassword()));
		user.setChangePwdRequire(false);
		user.setLastChangePwd(System.currentTimeMillis());
		userRepository.save(user);
	}
	
	@Override
	@Transactional
	public void resetPwd(ResetPasswordDto changePasswordDto) {
		
		if (StringUtils.isNotBlank(changePasswordDto.getToken())) {
			List<Token> tokens = em.createQuery("FROM Token where token = '" + changePasswordDto.getToken() + "'").getResultList();
			if (tokens.isEmpty() || tokens.get(0).getStartTime() > System.currentTimeMillis() || tokens.get(0).getEndTime() < System.currentTimeMillis()) {
				throw new RuntimeException("token invalid!");
			}
			invalidToken(tokens.get(0).getToken());
			changePasswordDto.setEmail(tokens.get(0).getEmail());
		}
		
		List<OTP> otps = em.createQuery("FROM OTP where email = '" + changePasswordDto.getEmail() + "' AND otp = '" + changePasswordDto.getOtp() + "' AND endTime > " + System.currentTimeMillis() + "l ORDER BY id DESC ").getResultList();
		if (otps.isEmpty() || otps.get(0).getStartTime() > System.currentTimeMillis() || otps.get(0).getEndTime() < System.currentTimeMillis()) {
			throw new RuntimeException("otp invalid!");
		}
		invalidOtp(changePasswordDto.getEmail(), changePasswordDto.getOtp());

		Users user = userRepository.findByEmail(changePasswordDto.getEmail());

		Utils.validatePwd(changePasswordDto.getPassword(), user.getLastPwd());

		user.setPassword(passwordEncoder.encode(changePasswordDto.getPassword()));
		user.setLastChangePwd(System.currentTimeMillis());
		user.setChangePwdRequire(false);
		userRepository.save(user);
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void updateLastLogin(String email) {
		Users user = userRepository.findByEmail(email);

		if (user == null) {
			user = userRepository.findByUsername(email);
		}
		if (user == null) {
			user = userRepository.findByPhoneNumber(email);
		}
		if (user == null) {
			return;
		}
		user.setLastLogin(new Date());
		user.setFirstLoginOtpRequire(false);
		userRepository.save(user);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void loadRoleAndPermission() {
		List<Role> roles = roleRepository.findAll();
		for (Role role : roles) {
			List<Permission> permissions = new ArrayList<>();
			List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(role.getId());
			for (RolePermission rolePer : rolePermissions) {
				Optional<Permission> permisstion = permissionRepository.findById(rolePer.getPermission().getId());
				if (permisstion.isPresent()) {
					permissions.add(permisstion.get());
				}
			}
			map.put(role.getName(), permissions);
		}
	}

	@Transactional
	@Override
	public void save(UserDto dto) {

		Users en = null;
		if (dto.getEmail() != null) {
			dto.setEmail(dto.getEmail().toLowerCase());
		}

		if (StringUtils.isBlank(dto.getUsername())) {
			dto.setUsername(dto.getEmail());
		}
		
		if (dto.getUsername() != null) {
			dto.setUsername(dto.getUsername().toLowerCase());
		}
		
		boolean isNewUser = true;
		if (dto.getId() != null && dto.getId().longValue() > 0) {
			
			Optional<Users> opt = userRepository.findById(dto.getId());
			if (opt.isPresent()) {
				if (!SecurityUtils.hasAnyRole("SUPER_ADMIN", AppCodeSelectedHolder.get() + "_SUPER_ADMIN") && !opt.get().getEmail().equals(SecurityUtils.getEmail())) {
					throw new RuntimeException("Access denied!");
				}
				en = opt.get();
				isNewUser = false;
			} else {
				throw new RuntimeException("User not found!");
			}
		} else {
			en = new Users();
			en.setUsername(dto.getUsername());
			en.setEmail(dto.getEmail());
			en.setChangePwdRequire(true);
			en.setFirstLoginOtpRequire(BooleanUtils.isTrue(dto.getFirstLoginOtpRequire()));
			isNewUser = true;
		}

		if (en.getUserId() == null && userRepository.findByEmail(dto.getEmail()) != null) {
			throw new RuntimeException("Email already exists!");
		}
		
		if (dto.getPhoneNumber() != null && dto.getPhoneNumber().trim().startsWith("+")) {
			
			String phone = "+" + dto.getPhoneNumber().trim().replaceAll("[^0-9]", "");
			String callingCode = null;
			for (String c : cCodes) {
				if (phone.trim().startsWith("+" + c)) {
					callingCode = c;
					break;
				}
			}
			
			if (StringUtils.isBlank(callingCode)) {
				throw new RuntimeException("Unknown phone code!");
			}
			String lcPhone = phone.substring(callingCode.length() + 1);
			if (!"86".equals(callingCode) && !"91".equals(callingCode) && !lcPhone.matches("^(0[1-9][0-9]{1,8})|([1-9][0-9]{1,9})$")) {
				throw new RuntimeException("Phone invalid (Maximum 10 numeric characters)!");
			}
			if ("86".equals(callingCode) && !lcPhone.matches("^(0[1-9][0-9]{1,9})|([1-9][0-9]{1,10})$")) {
				throw new RuntimeException("Phone invalid (Maximum 11 numeric characters)!");
			}
			if ("91".equals(callingCode) && !lcPhone.matches("^(0[1-9][0-9]{1,10})|([1-9][0-9]{1,11})$")) {
				throw new RuntimeException("Phone invalid (Maximum 12 numeric characters)!");
			}
			
			phone = "+" + callingCode + (lcPhone.startsWith("0") ? lcPhone.substring(1) : lcPhone);
			
			Users existsUser = userRepository.findByPhoneNumber(phone);
			if ((existsUser != null && en.getUserId() != null && existsUser.getUserId().longValue() != en.getUserId().longValue()) || (en.getUserId() == null && existsUser != null)) {
				throw new RuntimeException("Phone already exists!");
			}
			
			en.setPhoneNumber(phone);
			en.setLcPhoneNumber(lcPhone);
			en.setCallingCode(callingCode);
		} else if (StringUtils.isBlank(dto.getPhoneNumber())) {
			en.setPhoneNumber(null);
			en.setCallingCode(null);
			en.setLcPhoneNumber(null);
		}
		
		if (en.getUserId() != null && dto.getChangePwdRequire() != null) {
			en.setChangePwdRequire(dto.getChangePwdRequire());
		}

		en.setIdentification(dto.getIdentification());
		en.setFirstName(dto.getFirstName());
		en.setLastName(dto.getLastName());
		
		if (SecurityUtils.hasAnyRole("SUPER_ADMIN", AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
			en.setStatus(dto.getStatus());
			if (dto.getLoginOtpRequire() != null) {
				en.setLoginOtpRequire(dto.getLoginOtpRequire());
			}
		}

		if (dto.getId() != null && dto.getUpdatePwd() == Boolean.TRUE && StringUtils.isBlank(dto.getPassword())) {
			throw new RuntimeException("Password is required!");
		}
		
		if (dto.getId() != null && dto.getUpdatePwd() != Boolean.TRUE) {
			dto.setPassword(null);
		}
		
		if (StringUtils.isNotBlank(dto.getPassword())) {

			Utils.validatePwd(dto.getPassword(), en.getLastPwd());
			en.setPassword(passwordEncoder.encode(dto.getPassword()));
			en.setLastChangePwd(System.currentTimeMillis());

		} else if (dto.getId() == null && StringUtils.isBlank(dto.getPassword())) {
			en.setPassword(StringUtils.isNotBlank(dto.getHPwd()) ? dto.getHPwd() : passwordEncoder.encode(UUID.randomUUID().toString()));
			en.setLastChangePwd(System.currentTimeMillis());
		}

		en.setAutoDeleteDate(dto.getAutoDeleteDate());
		userRepository.save(en);
		
		Long userId = en.getUserId();

		// Role
//		Set<String> roleNames = new HashSet<>();
//		dto.getRoles().forEach(roleNames::add);
//		userRoleRepository.deleteNotInRoles(userId, AppCodeSelectedHolder.get(),
//				roleNames.isEmpty() ? new HashSet<>(Arrays.asList("-1")) : roleNames);
//		List<String> existsRoleNames = userRoleRepository.findRoleNameByUserId(userId);
//		List<Role> roles = userRoleRepository.findRoleByRoleNameIn(roleNames);
//		for (Role role : roles) {
//			if (!existsRoleNames.contains(role.getName())) {
//				UserRole userRole = new UserRole();
//				userRole.setUser(en);
//				userRole.setRole(role);
//				userRoleRepository.save(userRole);
//			}
//		}
		// End role
		
		// Project

		Set<String> projectNames = new HashSet<>();
		dto.getProjects().forEach(projectNames::add);
		userProjectRepository.deleteNotInProjects(userId,
				projectNames.isEmpty() ? new HashSet<>(Arrays.asList("-1")) : projectNames);
		List<String> existsProjectNames = userProjectRepository.findProjectNameByUserId(userId);
		List<ProjectTag> projects = userProjectRepository.findProjectByProjectTagNameIn(projectNames);
		for (ProjectTag project : projects) {
			if (!existsProjectNames.contains(project.getName())) {
				UserProject userProject = new UserProject();
				userProject.setUser(en);
				userProject.setProject(project);
				userProjectRepository.save(userProject);
			}
		}
		// End Project 
		
		// App code
		
		if (en.getAppCodes() == null) {
			en.setAppCodes(new ArrayList<>());
		}
		if (en.getAppCodes().isEmpty()) {
			if (dto.getAppCodes().isEmpty()) {
				dto.getAppCodes().add(AppCodeSelectedHolder.get());
			}
			
			userAppCodeRepository.deleteNotInAppCodes(en.getUserId(), dto.getAppCodes());
			userAppCodeRepository.flush();
			List<String> existAppCodes = userAppCodeRepository.findAppCodeNameByUserUserId(en.getUserId());
			
			for (AppCode ac : appCodeRepository.findByNameIn(dto.getAppCodes())) {
				if (!existAppCodes.contains(ac.getName())) {
					UserAppCode uac = new UserAppCode();
					uac.setUser(en);
					uac.setAppCode(ac);
					userAppCodeRepository.save(uac);
					en.getAppCodes().add(uac);
				}
			}
		}
		userRepository.save(en);
		dto.setId(en.getUserId());
		
		if (isNewUser) {
			String phoneNumber = en.getPhoneNumber();
			String email = en.getEmail();
			if (dto.getSendLoginToPhone() == Boolean.TRUE && StringUtils.isNotBlank(phoneNumber)) {
				notificationService.sendSMS(AppCodeSelectedHolder.get() + "-Account credentials: " + phoneNumber + " / " + dto.getPassword(), phoneNumber.trim());
			}
			if (dto.getSendLoginToEmail() == Boolean.TRUE) {
				notificationService.sendEmail("<html><body>" + AppCodeSelectedHolder.get() + "-Account credentials " + email + " / " + dto.getPassword() + "</body></html>", email, AppProps.get("EMAIL_NEW_USER_SUBJECT", AppCodeSelectedHolder.get() + " - Account Information"));
			}
		}
		//
		
		if ("DMS".equals(AppCodeSelectedHolder.get())) {
			if (StringUtils.isBlank(en.getPhoneNumber())) {
				throw new RuntimeException("Phone number is required!");
			}
//			if (StringUtils.isNotBlank(dto.getPassword()) && (isNewUser || dto.getUpdatePwd() == Boolean.TRUE)) {
//				String res = ChinaPadLockUtils.createUserChinaLockServer(en.getPhoneNumber().length() <= 12 ? en.getPhoneNumber().substring(1) : en.getLcPhoneNumber(), en.getPhoneNumber(), dto.getPassword());
//				if ("false".equalsIgnoreCase(AppProps.get("DMS_IGNORE_CREATE_LOCK_SERVER_USER_ERROR", "false")) && !("1".equals(res) || "2".equals(res))) {
//					throw new RuntimeException("Cannot create user (China padlock.)");
//				}
//			}
		}
	}

	@Transactional
	@Override
	public void saveRole(UserDto dto) {
		
		if (!SecurityUtils.hasAnyRole("SUPER_ADMIN", AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
			 throw new RuntimeException("Access denied!");
		}
		
		Optional<Users> user = userRepository.findById(dto.getId());
		if (user.isPresent()) {
			List<Role> rus = roleRepository.findRoleByUserUserId(dto.getId());
			Map<Long, Role> mRs = new LinkedHashMap<>();
			rus.forEach(ru -> mRs.put(ru.getId(), ru));
			Set<Long> userRoles = new HashSet<>();
			for (RoleDto roleDto : dto.getRole()) {
				
				if (!mRs.containsKey(roleDto.getId()) && !userRoles.contains(roleDto.getId())) {
					Optional<Role> role = roleRepository.findById(roleDto.getId());
					if (role.isPresent() && AppCodeSelectedHolder.get().equals(role.get().getAppCode().getName())) {
						UserRole userRole1 = new UserRole();
						userRole1.setRole(role.get());
						userRole1.setUser(user.get());
						try {
							userRoleRepository.save(userRole1);
						} catch (Exception e) {
							LOGGER.error(e.getMessage(), e);
						}
					}
				}
				userRoles.add(roleDto.getId());
			}
			
			try {
//				if ("henry@gmail.com".equalsIgnoreCase(user.get().getEmail())) {
//					userRoles.add("SUPER_ADMIN");
//					userRoles.add(AppCodeSelectedHolder.get() + "_SUPER_ADMIN");
//				}
				userRoleRepository.deleteNotInRoleIds(user.get().getUserId(), AppCodeSelectedHolder.get(),
						userRoles.isEmpty() ? new HashSet<>(Arrays.asList(-1l)) : userRoles);
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(e.getMessage(), e);
			}
		} else {
			throw new RuntimeException("User not found!");
		}
		loadRoleAndPermission();
	}

	@Transactional
	@Override
	public void saveGroup(UserDto dto) {
		
		if (!SecurityUtils.hasAnyRole("SUPER_ADMIN", AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
			 throw new RuntimeException("Access denied!");
		}
		
		Optional<Users> user = userRepository.findById(dto.getId());
		if (user.isPresent()) {
			Set<Long> userGroups = new HashSet<>();
			List<GroupUser> gus = groupUserRepository.findGroupByUserUserId(dto.getId());
			Map<Long, GroupUser> mGs = new LinkedHashMap<>();
			gus.forEach(gu -> mGs.put(gu.getId(), gu));

			for (GroupUserDto groupUserDto : dto.getGroupUsers()) {
				
				if (!mGs.containsKey(groupUserDto.getId()) && !userGroups.contains(groupUserDto.getId())) {
					Optional<GroupUser> groupUser = groupUserRepository.findById(groupUserDto.getId());
					UserGroup userGroup = new UserGroup();
					if (groupUser.isPresent() && AppCodeSelectedHolder.get().equals(groupUser.get().getAppCode().getName()) ) {
						userGroup.setGroupUser(groupUser.get());
						userGroup.setUser(user.get());
						try {
							userGroupRepository.save(userGroup);
						} catch (Exception e) {
							LOGGER.error(e.getMessage(), e);
						}
					}
				}
				
				userGroups.add(groupUserDto.getId());
			}

			try {
				groupUserRepository.deleteNotInGroups(user.get().getUserId(), AppCodeSelectedHolder.get(),
						userGroups.isEmpty() ? new HashSet<>(Arrays.asList(-1l)) : userGroups);
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(e.getMessage(), e);
			}
		} else {
			throw new RuntimeException("User not found!");
		}
		
		loadRoleAndPermission();
	}

	@Transactional
	@Override
	public void savePermission(UserDto dto) {
		
		if (!SecurityUtils.hasAnyRole("SUPER_ADMIN", AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
			 throw new RuntimeException("Access denied!");
		}
		
		Optional<Users> user = userRepository.findById(dto.getId());
		if (user.isPresent()) {

			List<Permission> permissions = userPermissionRepository.findPermissionByAppCodeNameAndUserUserId(AppCodeSelectedHolder.get(), dto.getId());
			Map<Long, Permission> mPs = new LinkedHashMap<>();
			permissions.forEach(p -> mPs.put(p.getId(), p));
			Set<Long> newPs = new LinkedHashSet<>();
			for (PermissionDto permissionDto : dto.getPermissions()) {

				if (!mPs.containsKey(permissionDto.getId()) && !newPs.contains(permissionDto.getId())) {
					Optional<Permission> permission = permissionRepository.findById(permissionDto.getId());
					UserPermission userPermission = new UserPermission();
					if (permission.isPresent() && AppCodeSelectedHolder.get().equals(permission.get().getAppCode().getName())) {
						userPermission.setPermission(permission.get());
						userPermission.setUser(user.get());
						try {
							userPermissionRepository.save(userPermission);
							mPs.put(permission.get().getId(), permission.get());
						} catch (Exception e) {
							LOGGER.error(e.getMessage(), e);
						}
					}
				}
				
				newPs.add(permissionDto.getId());
			}
			userPermissionRepository.flush();
			for (Map.Entry<Long, Permission> en : mPs.entrySet()) {
				if (!newPs.contains(en.getKey()) && en.getValue().getAppCode().getName().equals(AppCodeSelectedHolder.get())) {
					userPermissionRepository.deleteByUserIdAndPermissionId(dto.getId(), en.getKey());
				}
			}
		} else {
			throw new RuntimeException("User not found!");
		}
		
		loadRoleAndPermission();
	}
	
	@Transactional
	@Override
	public void saveProject(UserDto dto) {
		Optional<Users> user = userRepository.findById(dto.getId());
		if (user.isPresent()) {
			List<String> projectNames = userProjectRepository.findProjectNameByUserId(dto.getId());
			Set<String> projectNamesInput = new HashSet<>();
			for (String project : dto.getProjects()) {
				projectNamesInput.add(project);
				if (!projectNames.contains(project)) {
					Optional<ProjectTag> tagOpt = projectTagRepository.findByName(project);
					if (tagOpt.isPresent()) {
						UserProject userProject = new UserProject();
						userProject.setUser(user.get());
						userProject.setProject(tagOpt.get());
						try {
							userProjectRepository.save(userProject);
						} catch (Exception e) {
							LOGGER.error(e.getMessage(), e);
						}
					}
				}
			}
			
			try {
				userProjectRepository.deleteNotInProjects(user.get().getUserId(),
						projectNamesInput.isEmpty() ? new HashSet<>(Arrays.asList("-1")) : projectNamesInput);
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(e.getMessage(), e);
			}
		}
	}
	
	@Override
	@Transactional
	public void saveCompany(UserDto dto) {
		Optional<Users> user = userRepository.findById(dto.getId());
		if (user.isPresent()) {
			
			List<String> companyNames = userCompanyRepository.findCompanyNameByUserId(dto.getId());
			Set<String> companyNamesInput = new HashSet<>();
			for (String company : dto.getCompanies()) {
				companyNamesInput.add(company);
				if (!companyNames.contains(company)) {
					Optional<Company> cpnOpt = companyRepository.findByNameAndAppCodeName(company, AppCodeSelectedHolder.get());
					if (cpnOpt.isPresent()) {
						UserCompany userCompany = new UserCompany();
						userCompany.setUser(user.get());
						userCompany.setCompany(cpnOpt.get());
						try {
							userCompanyRepository.save(userCompany);
						} catch (Exception e) {
							LOGGER.error(e.getMessage(), e);
						}
					}
				}
			}
			
			try {
				userCompanyRepository.deleteNotInCompanies(user.get().getUserId(),
						AppCodeSelectedHolder.get(),
						companyNamesInput.isEmpty() ? new HashSet<>(Arrays.asList("-1")) : companyNamesInput);
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	public ResponseDto<LoginResponseDto> passLogin(LoginRequestDto loginRequestDTO) {
		String email = loginRequestDTO.getEmail();
		// Reload password post-security so we can generate the token
		final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
		Map<String, Object> claims = new LinkedHashMap<>();
		final String token = jwtTokenUtil.doGenerateToken(claims, userDetails.getUsername());

		return apiResponse.response(ValueConstant.SUCCESS, ValueConstant.TRUE,
				LoginResponseDto.builder().token(token).build());
	}

	void validateUsernamePassword(String email, String password) {
		if (email == null || password == null) {
			throw new NullPointerException(Message.USERNAME_OR_PASSWORD_NOT_NULL);
		}

		Users existUser = userRepository.findByEmail(email);
		if (existUser != null) {
			throw new DuplicateUserException(Message.EXIST_EMAIL);
		}

	}

	@Override
	public ResponseDto<JwtUser> getUser(HttpServletRequest request) {
		JwtUser jwtUser = (JwtUser) userDetailsService.loadUserByUsername(SecurityUtils.getEmail());
		jwtUser.setGroups(groupUserRepository.findGroupByUserUserId(jwtUser.getId()).stream().map(ug -> ug.getName()).collect(Collectors.toList()));
		return apiResponse.response(ValueConstant.SUCCESS, ValueConstant.TRUE, jwtUser);
	}

	@Override
	public Object getUsernameById(Long userId) {
		return new HashMap<>();
	}

	@Override
	public Object getUserById(Long userId) {
		return new HashMap<>();
	}

	@Override
	@Transactional
	public void removeUserById(Long userId) {
		
		boolean isCurrent = userId.longValue() == SecurityUtils.getUser().getId().longValue();
		if (!isCurrent && !SecurityUtils.hasAnyRole("SUPER_ADMIN", AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
			 throw new RuntimeException("Access denied!");
		}
		
		Optional<Users> user = userRepository.findById(userId);
		if (!user.isPresent()) {
			throw new RuntimeException("User not found!");
		}
		
		
		em.createQuery("UPDATE CARequestLog c set installer = null where c.installer.userId = " + userId)
				.executeUpdate();
		em.createQuery("UPDATE MeterCommissioningReport c set installer = null where c.installer.userId = " + userId)
		.executeUpdate();
		em.createQuery("UPDATE P2ReportAck c set installer = null where c.installer.userId = " + userId)
		.executeUpdate();
		em.createQuery("UPDATE Log c set user = null where c.user.userId = " + userId).executeUpdate();
		em.createQuery("UPDATE LogBatch c set user = null where c.user.userId = " + userId).executeUpdate();
		em.createQuery("UPDATE GroupTask c set user = null where c.user.userId = " + userId).executeUpdate();
		em.createQuery("DELETE FROM UserGroup c where c.user.userId = " + userId).executeUpdate();
		em.createQuery("DELETE FROM UserPermission c where c.user.userId = " + userId).executeUpdate();
		em.createQuery("DELETE FROM UserRole c where c.user.userId = " + userId).executeUpdate();
		em.createQuery("DELETE FROM UserAppCode c where c.user.userId = " + userId).executeUpdate();
		em.createQuery("DELETE FROM UserProject c where c.user.userId = " + userId).executeUpdate();
		em.createQuery("DELETE FROM UserCompany c where c.user.userId = " + userId).executeUpdate();
		em.createQuery("DELETE FROM DMSProjectPicUser c where c.picUser.userId = " + userId).executeUpdate();
		
		em.createQuery("UPDATE DeviceFilters c set user = null where c.user.userId = " + userId).executeUpdate();
		
		em.flush();
		
		userRepository.deleteById(userId);
		
		if (isCurrent) {
			logout(HttpServletRequestHolder.get().getHeader("Authorization"));
		}
		
	}

	@Transactional
	@Override
	public void initDataAuths() {
		Map<String, String> roleMap = new LinkedHashMap<>();
		roleMap.put("SUB_ADMIN", "SubAdmin");
		roleMap.put("STAFF", "Staff");
		roleMap.put("SUPER_ADMIN", "Super Admin");
		roleMap.put("INSTALLER", "Installer");
		roleMap.put("FACTORY_STAFF", "Factory Staff");

		List<String> existsRoles = roleRepository.findByNameIn(roleMap.keySet()).stream().map(r -> r.getName())
				.collect(Collectors.toList());
		roleMap.forEach((k, v) -> {
			if (!existsRoles.contains(k)) {
				Role role = new Role();
				role.setName(k);
				role.setDesc(v);
				roleRepository.save(role);
			}
		});

		Users user = userRepository.findByUsername("henry");
		if (user == null) {
			UserDto user1 = new UserDto();
			user1.setUsername("henry");
			user1.setEmail("henry@gmail.com");
			user1.setPassword("P0wer!2");
			user1.setFirstName("admin");
			user1.setLastName("admin");
			user1.setApproved(System.currentTimeMillis());
			user1.setRoles(Arrays.asList("SUPER_ADMIN"));
			save(user1);
		}
		
		
		///update user appcode
		Map<String, String> appCodeMap = new LinkedHashMap<>();
		appCodeMap.put("MMS", "MMS");
		appCodeMap.put("DMS", "DMS");
		
		List<String> existsAppCodes = appCodeRepository.findByNameIn(appCodeMap.keySet()).stream().map(ac -> ac.getName())
				.collect(Collectors.toList());
		appCodeMap.forEach((k, v) -> {
			if (!existsAppCodes.contains(k)) {
				AppCode ac = new AppCode();
				ac.setName(k);
				ac.setDesc(v);
				appCodeRepository.save(ac);
			}
		});
		appCodeRepository.flush();
		
		userRepository.findAll()
		.forEach(us -> {
			List<UserAppCode> uacs = us.getAppCodes();
			if (uacs == null || uacs.isEmpty()) {
				us.setAppCodes(new ArrayList<>());
				UserAppCode userAppCodeMMS = new UserAppCode();
				userAppCodeMMS.setUser(us);
				userAppCodeMMS.setAppCode(appCodeRepository.findByName("MMS"));
				
				userAppCodeRepository.save(userAppCodeMMS);
				us.getAppCodes().add(userAppCodeMMS);
				
				if ("henry@gmail.com".equalsIgnoreCase(us.getEmail())) {
					UserAppCode userAppCodeDMS = new UserAppCode();
					userAppCodeDMS.setUser(us);
					userAppCodeDMS.setAppCode(appCodeRepository.findByName("DMS"));
					
					userAppCodeRepository.save(userAppCodeDMS);
					us.getAppCodes().add(userAppCodeDMS);
				}
				userRepository.save(us);
			}
		});
		
		loginRepository.deleteExpiredLogin(System.currentTimeMillis());
		SchedulerHelper.scheduleJob("0 0/5 * * * ? *", () -> {
			loginRepository.deleteExpiredLogin(System.currentTimeMillis());
		}, "REMOVE_LOGIN");
	}

	@Override
	public void getUsers(PaginDto<UserDto> pagin) {
		
		StringBuilder sqlBuilder = new StringBuilder("FROM Users us");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Users us");
		
		Map<String, Object> options = pagin.getOptions();
        String queryUserName = options.get("queryUserName") != null ?  (String) options.get("queryUserName") : null;
        String queryFirstName = options.get("queryFirstName") != null ?  (String) options.get("queryFirstName") : null;
        String queryLastName = options.get("queryLastName") != null ?  (String) options.get("queryLastName") : null;
        String queryPhoneNumber = options.get("queryPhoneNumber") != null ?  (String) options.get("queryPhoneNumber") : null;

		StringBuilder sqlCommonBuilder = new StringBuilder();
		sqlCommonBuilder.append(" WHERE 1=1 ");
		
		if (StringUtils.isNotBlank(queryUserName)) {
			sqlCommonBuilder.append(" AND lower(username) like '%" + queryUserName.toLowerCase() + "%' ");
		}
		if (StringUtils.isNotBlank(queryFirstName)) {
			sqlCommonBuilder.append(" AND lower(firstName) like '%" + queryFirstName.toLowerCase() + "%' ");
		}
		if (StringUtils.isNotBlank(queryLastName)) {
			sqlCommonBuilder.append(" AND lower(lastName) like '%" + queryLastName.toLowerCase() + "%' ");
		}
		if (StringUtils.isNotBlank(queryPhoneNumber)) {
			sqlCommonBuilder.append(" AND (phoneNumber like '%" + queryPhoneNumber + "%' OR lcPhoneNumber like '%" + queryPhoneNumber + "%') ");
		}
		
		if ("true".equalsIgnoreCase(options.get("hasPhone") + "")) {
			sqlCommonBuilder.append(" AND us.phoneNumber is not null ");
		}
		
		sqlCommonBuilder.append(" AND (exists (select 1 from UserAppCode uac where uac.appCode.name = '" + AppCodeSelectedHolder.get() + "' and uac.user.userId = us.userId)) ");
		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY us.userId asc");
		sqlCountBuilder.append(sqlCommonBuilder);

		if (pagin.getOffset() == null || pagin.getOffset() < 0) {
			pagin.setOffset(0);
		}

		if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
			pagin.setLimit(100);
		}

		Query queryCount = em.createQuery(sqlCountBuilder.toString());

		Long count = ((Number) queryCount.getSingleResult()).longValue();
		pagin.setTotalRows(count);
		pagin.setResults(new ArrayList<>());
		if (count == 0l) {
			return;
		}

		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(pagin.getOffset());
		query.setMaxResults(pagin.getLimit());

		List<Users> users = query.getResultList();
		users.forEach(user -> {

			List<String> roles = new ArrayList<>();

			UserDto dto = UserDto.builder().id(user.getUserId()).username(user.getUsername()).email(user.getEmail())
					.fullName(user.getFullName()).firstName(user.getFirstName()).lastName(user.getLastName())
					.phoneNumber(user.getPhoneNumber()).avatar(user.getAvatar())
					.fullName(user.getFirstName() + " " + user.getLastName()).status(user.getStatus())
					.changePwdRequire(user.getChangePwdRequire())
					.loginOtpRequire(user.getLoginOtpRequire())
					.identification(user.getIdentification())
					.lcPhoneNumber(user.getLcPhoneNumber())
					.callingCode(user.getCallingCode())
					.roleDescs(user.getRoles().stream().filter(r -> r.getRole().getAppCode().getName().equals(AppCodeSelectedHolder.get())).map(authority -> {
						roles.add(authority.getRole().getName());
						return SimpleMap.init("name", authority.getRole().getName()).more("desc",
								authority.getRole().getDesc());
					}).collect(Collectors.toList())).build();
			dto.setRoles(roles);
			pagin.getResults().add(dto);
		});
	}

	@Transactional
	@Override
	public void getPermissionsOfUser(PaginDto<UserDto> pagin) {
		
		if (map.isEmpty()) {
			loadRoleAndPermission();
		}
		
		Users user = userRepository.findByEmail(SecurityUtils.getEmail());

		if (user == null) {
			user = userRepository.findByUsername(SecurityUtils.getEmail());
		}
		List<PermissionDto> permissionsDto = new ArrayList<>();
		for (UserRole userRole : user.getRoles()) {
			if (!userRole.getRole().getAppCode().getName().equals(AppCodeSelectedHolder.get())) {
				continue;
			}
			if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN") || StringUtils.equals(userRole.getRole().getName(), AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
				for (Permission per : permissionRepository.findAll()) {
					if (!per.getAppCode().getName().equals(AppCodeSelectedHolder.get())) {
						continue;
					}
					if (StringUtils.equals(per.getName(), "PAGE_ROLES_EDIT_ROLE_BUTTON_PERM")
							|| StringUtils.equals(per.getName(), "PAGE_ROLES_REMOVE_ROLE_BUTTON_PERM")) {

					} else {
						PermissionDto permiss = new PermissionDto();
						permiss.setId(per.getId());
						permiss.setName(per.getName());
						permiss.setDescription(per.getDescription());
						permissionsDto.add(permiss);
					}
				}
			} else {
				Iterator<Map.Entry<String, List<Permission>>> itr = map.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry<String, List<Permission>> it = itr.next();
					List<PermissionDto> permissionss = new ArrayList();
					for (Permission permission : it.getValue()) {
						if (!permission.getAppCode().getName().equals(AppCodeSelectedHolder.get())) {
							continue;
						}
						PermissionDto perDto = new PermissionDto();
						perDto.setId(permission.getId());
						perDto.setName(permission.getName());
						perDto.setDescription(permission.getDescription());
						if (permissionsDto.isEmpty()) {
							permissionss.add(perDto);
						} else {
							boolean check = false;
							for (PermissionDto per : permissionsDto) {
								if (per.getId() == perDto.getId()) {
									check = true;
								}
							}
							if (check != true) {
								permissionss.add(perDto);
							}
						}
					}
					if (StringUtils.equals(userRole.getRole().getName(), it.getKey())) {
						permissionsDto.addAll(permissionss);
					}
				}
			}
		}
		List<Permission> allPermissions = userPermissionRepository.findByUserUserId(user.getUserId()).stream().map(up -> up.getPermission()).collect(Collectors.toList());
		boolean isSuperAdmin = SecurityUtils.hasAnyRole("SUPER_ADMIN") || SecurityUtils.hasAnyRole(AppCodeSelectedHolder.get() + "_SUPER_ADMIN");
		if (isSuperAdmin) {
			allPermissions = permissionRepository.findAll();
		}
		for (Permission permission : allPermissions) {
			// UserPermission userPer : userPermissions
			if (!permission.getAppCode().getName().equals(AppCodeSelectedHolder.get())) {
				continue;
			}
			List<PermissionDto> permissionss = new ArrayList();
			boolean check = false;
			for (PermissionDto per : permissionsDto) {
				if (per.getId() == permission.getId()) {
					check = true;
				}
			}
			if (check != true) {
				PermissionDto PerDto = new PermissionDto();
				PerDto.setId(permission.getId());
				PerDto.setName(permission.getName());
				PerDto.setDescription(permission.getDescription());
				permissionsDto.add(PerDto);
			}
		}
		UserDto dto = UserDto.builder().permissions(permissionsDto).build();
		pagin.getResults().add(dto);
	}

	@Override
	public void getRoleOfUserLogin(PaginDto<RoleDto> pagin) {

		Users user = userRepository.findByEmail(SecurityUtils.getEmail());

		if (user == null) {
			user = userRepository.findByUsername(SecurityUtils.getEmail());
		}

		if (user != null) {
			boolean check = false;
			for (UserRole userRole : user.getRoles()) {
				if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN") || StringUtils.equals(userRole.getRole().getName(), AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
					check = true;
				}
			}
			if (check == true) {
				StringBuilder sqlBuilder = new StringBuilder("FROM Role");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Role");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE 1 = 1");
				// sqlCommonBuilder.append(" AND appCode.name = '" + AppCodeSelectedHolder.get() + "' ");
				
				sqlBuilder.append(sqlCommonBuilder);
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(10000);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<Role> roles = query.getResultList();
				roles.forEach(role -> {

					RoleDto dto = RoleDto.builder().id(role.getId()).name(role.getName()).desc(role.getDesc()).build();
					pagin.getResults().add(dto);
				});
			} else {
				StringBuilder sqlBuilder = new StringBuilder("FROM UserRole ur");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM UserRole ur");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE ur.user.userId = " + user.getUserId());
				sqlCommonBuilder.append(" AND ur.role.appCode.name = '" + AppCodeSelectedHolder.get() + "' ");
				sqlCommonBuilder.append(" AND (exists (select 1 from UserAppCode uac where uac.appCode.name = '" + AppCodeSelectedHolder.get() + "' and uac.user.userId = ur.user.userId)) ");
				
				sqlBuilder.append(sqlCommonBuilder);
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(10000);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<UserRole> userRoles = query.getResultList();
				userRoles.forEach(userRole -> {

					RoleDto dto = RoleDto.builder().id(userRole.getRole().getId()).name(userRole.getRole().getName())
							.desc(userRole.getRole().getDesc()).build();
					pagin.getResults().add(dto);
				});
			}
		}
	}

	@Override
	public void getRoleOfUser(PaginDto<RoleDto> pagin) {

		Map<String, Object> map = pagin.getOptions();

		Object userId = (Object) map.get("userId");

		Optional<Users> user = userRepository.findById(Long.parseLong(userId.toString()));

		if (user.isPresent()) {
			boolean check = false;
			for (UserRole userRole : user.get().getRoles()) {
				if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN") || StringUtils.equals(userRole.getRole().getName(), AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
					check = true;
				}
			}
			
			if (check == true) {
				StringBuilder sqlBuilder = new StringBuilder("FROM Role r");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Role r");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE 1 = 1");
				sqlCommonBuilder.append(" AND r.appCode.name = '" + AppCodeSelectedHolder.get() + "' ");
				
				sqlBuilder.append(sqlCommonBuilder);
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(20);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<Role> roles = query.getResultList();
				roles.forEach(role -> {

					RoleDto dto = RoleDto.builder().id(role.getId()).name(role.getName()).desc(role.getDesc()).build();
					pagin.getResults().add(dto);
				});
			} else {
				StringBuilder sqlBuilder = new StringBuilder("FROM UserRole ur");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM UserRole ur");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE ur.user.userId = " + userId);
				
				sqlCommonBuilder.append(" AND ur.role.appCode.name = '" + AppCodeSelectedHolder.get() + "' ");
				sqlCommonBuilder.append(" AND (exists (select 1 from UserAppCode uac where uac.appCode.name = '" + AppCodeSelectedHolder.get() + "' and uac.user.userId = ur.user.userId)) ");
				
				sqlBuilder.append(sqlCommonBuilder);
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(20);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<UserRole> userRoles = query.getResultList();
				userRoles.forEach(userRole -> {

					RoleDto dto = RoleDto.builder().id(userRole.getRole().getId()).name(userRole.getRole().getName())
							.desc(userRole.getRole().getDesc()).build();
					pagin.getResults().add(dto);
				});
			}
		}
	}

	@Override
	public void getProjectTagOfUserLogin(PaginDto<ProjectTagDto> pagin) {

		Users user = userRepository.findByEmail(SecurityUtils.getEmail());

		if (user == null) {
			user = userRepository.findByUsername(SecurityUtils.getEmail());
		}

		if (user != null) {
			boolean check = false;
			for (UserRole userRole : user.getRoles()) {
				if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN") || StringUtils.equals(userRole.getRole().getName(), AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
					check = true;
				}
			}
			if (check == true) {
				StringBuilder sqlBuilder = new StringBuilder("FROM ProjectTag");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM ProjectTag");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE 1 = 1");
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(10000);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<ProjectTag> projects = query.getResultList();
				projects.forEach(project -> {
					ProjectTagDto dto = ProjectTagDto.builder().id(project.getId()).name(project.getName()).description(project.getDescription()).build();
					pagin.getResults().add(dto);
				});
			} else {
				StringBuilder sqlBuilder = new StringBuilder("FROM UserProject ur");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM UserProject ur");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE ur.user.userId = " + user.getUserId());
				sqlBuilder.append(" WHERE ur.user.userId = " + user.getUserId());
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(10000);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<UserProject> userProjects = query.getResultList();
				userProjects.forEach(userProject -> {
					ProjectTagDto dto = ProjectTagDto.builder().id(userProject.getProject().getId()).name(userProject.getProject().getName())
							.description(userProject.getProject().getDescription()).build();
					pagin.getResults().add(dto);
				});
			}
		}
	}	
	
	@Override
	public void getProjectTagOfUser(PaginDto<ProjectTagDto> pagin) {

		Map<String, Object> map = pagin.getOptions();

		Object userId = (Object) map.get("userId");

		Optional<Users> user = userRepository.findById(Long.parseLong(userId.toString()));

		if (user.isPresent()) {
			boolean check = false;
			for (UserRole userRole : user.get().getRoles()) {
				if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN") || StringUtils.equals(userRole.getRole().getName(), AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
					check = true;
				}
			}
			if (check == true) {
				StringBuilder sqlBuilder = new StringBuilder("FROM ProjectTag");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM ProjectTag");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE 1 = 1");
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(20);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<ProjectTag> projects = query.getResultList();
				projects.forEach(project -> {
					ProjectTagDto dto = ProjectTagDto.builder().id(project.getId()).name(project.getName()).description(project.getDescription()).build();
					pagin.getResults().add(dto);
				});
			} else {
				StringBuilder sqlBuilder = new StringBuilder("FROM UserProject ur");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM UserProject ur");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE ur.user.userId = " + userId);
				sqlBuilder.append(" WHERE ur.user.userId = " + userId);
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(20);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<UserProject> userProjects = query.getResultList();
				userProjects.forEach(userProject -> {
					ProjectTagDto dto = ProjectTagDto.builder().id(userProject.getProject().getId()).name(userProject.getProject().getName())
							.description(userProject.getProject().getDescription()).build();
					pagin.getResults().add(dto);
				});
			}
		}
	}
	
	@Override
	public void getCompanyOfUser(PaginDto<CompanyDto> pagin) {

		Map<String, Object> map = pagin.getOptions();

		Object userId = (Object) map.get("userId");

		Optional<Users> user = userRepository.findById(Long.parseLong(userId.toString()));

		if (user.isPresent()) {
			boolean check = false;
			for (UserRole userRole : user.get().getRoles()) {
				if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN") || StringUtils.equals(userRole.getRole().getName(), AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
					check = true;
				}
			}
			if (check == true) {
				StringBuilder sqlBuilder = new StringBuilder("FROM Company");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Company");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE 1 = 1");
				
				sqlCommonBuilder.append(" and appCode.name = '" + AppCodeSelectedHolder.get() + "'");
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(20);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<Company> companies = query.getResultList();
				companies.forEach(company -> {
					CompanyDto dto = CompanyDto.builder().id(company.getId()).name(company.getName()).description(company.getDescription()).build();
					pagin.getResults().add(dto);
				});
			} else {
				StringBuilder sqlBuilder = new StringBuilder("FROM UserCompany ur");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM UserCompany ur");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE ur.user.userId = " + userId);
				sqlCommonBuilder.append(" AND ur.company.appCode.name = '" + AppCodeSelectedHolder.get() + "' ");
				
				sqlBuilder.append(sqlCommonBuilder);
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(20);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<UserCompany> userCompanies = query.getResultList();
				userCompanies.forEach(userCpn -> {
					CompanyDto dto = CompanyDto.builder().id(userCpn.getCompany().getId()).name(userCpn.getCompany().getName())
							.description(userCpn.getCompany().getDescription()).build();
					pagin.getResults().add(dto);
				});
			}
		}
	}	
	
	@Override
	public void getGroupOfUser(PaginDto<GroupUserDto> pagin) {
		Map<String, Object> map = pagin.getOptions();

		Object userId = (Object) map.get("userId");

		StringBuilder sqlBuilder = new StringBuilder("FROM UserGroup ur");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM UserGroup ur");

		StringBuilder sqlCommonBuilder = new StringBuilder();
		sqlCommonBuilder.append(" WHERE ur.user.userId = " + userId);
		
		sqlCommonBuilder.append(" AND ur.groupUser.appCode.name = '" + AppCodeSelectedHolder.get() + "' ");
		sqlCommonBuilder.append(" AND (exists (select 1 from UserAppCode uac where uac.appCode.name = '" + AppCodeSelectedHolder.get() + "' and uac.user.userId = ur.user.userId)) ");
		
		
		sqlBuilder.append(sqlCommonBuilder);
		sqlCountBuilder.append(sqlCommonBuilder);

		if (pagin.getOffset() == null || pagin.getOffset() < 0) {
			pagin.setOffset(0);
		}

		if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
			pagin.setLimit(20);
		}

		Query queryCount = em.createQuery(sqlCountBuilder.toString());

		Long count = ((Number) queryCount.getSingleResult()).longValue();
		pagin.setTotalRows(count);
		pagin.setResults(new ArrayList<>());
		if (count == 0l) {
			return;
		}

		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(pagin.getOffset());
		query.setMaxResults(pagin.getLimit());

		List<UserGroup> userGroups = query.getResultList();
		userGroups.forEach(userGroup -> {
			GroupUserDto dto = GroupUserDto.builder().id(userGroup.getGroupUser().getId())
					.name(userGroup.getGroupUser().getName()).description(userGroup.getGroupUser().getDescription())
					.build();
			pagin.getResults().add(dto);
		});

	}

	@Override
	public void getPermissionsEachUser(PaginDto<PermissionDto> pagin) {
		Map<String, Object> map = pagin.getOptions();

		Object userId = (Object) map.get("userId");

		Optional<Users> user = userRepository.findById(Long.parseLong(userId.toString()));

		if (user.isPresent()) {
			boolean check = false;
			for (UserRole userRole : user.get().getRoles()) {
				if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN") || StringUtils.equals(userRole.getRole().getName(), AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
					check = true;
				}
			}
			if (check == true) {
				StringBuilder sqlBuilder = new StringBuilder("FROM Permission ");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Permission ");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE 1 = 1");
				
				sqlCommonBuilder.append(" AND appCode.name = '" + AppCodeSelectedHolder.get() + "' ");
				
				sqlBuilder.append(sqlCommonBuilder);
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(20);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<Permission> permissions = query.getResultList();
				permissions.forEach(permission -> {

					PermissionDto dto = PermissionDto.builder().id(permission.getId()).name(permission.getName()).fixed(true)
							.description(permission.getDescription()).build();
					pagin.getResults().add(dto);
				});
				pagin.getOptions().remove("allIn");
				pagin.getOptions().put("fixed", true);
			} else {
				StringBuilder sqlBuilder = new StringBuilder("FROM UserPermission up");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM UserPermission up");

				StringBuilder sqlCommonBuilder = new StringBuilder();
				sqlCommonBuilder.append(" WHERE up.user.userId = " + userId);
				
				sqlCommonBuilder.append(" AND up.permission.appCode.name = '" + AppCodeSelectedHolder.get() + "' ");
				sqlCommonBuilder.append(" AND (exists (select 1 from UserAppCode uac where uac.appCode.name = '" + AppCodeSelectedHolder.get() + "' and uac.user.userId = up.user.userId)) ");
				
				
				sqlBuilder.append(sqlCommonBuilder);
				sqlCountBuilder.append(sqlCommonBuilder);

				if (pagin.getOffset() == null || pagin.getOffset() < 0) {
					pagin.setOffset(0);
				}

				if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
					pagin.setLimit(20);
				}

				Query queryCount = em.createQuery(sqlCountBuilder.toString());

				Long count = ((Number) queryCount.getSingleResult()).longValue();
				pagin.setTotalRows(count);
				pagin.setResults(new ArrayList<>());
				if (count == 0l) {
					return;
				}

				Query query = em.createQuery(sqlBuilder.toString());
				query.setFirstResult(pagin.getOffset());
				query.setMaxResults(pagin.getLimit());

				List<UserPermission> userPermissions = query.getResultList();
				userPermissions.forEach(permission -> {

					PermissionDto dto = PermissionDto.builder().id(permission.getPermission().getId())
							.name(permission.getPermission().getName())
							.description(permission.getPermission().getDescription()).build();
					pagin.getResults().add(dto);
				});
				
				if (pagin.getOffset() == 0 && "true".equalsIgnoreCase(pagin.getOptions().get("allIn") + "")) {
					List<PermissionDto> allIn = new ArrayList<>();
					query = em.createQuery(sqlBuilder.toString());
					query.setFirstResult(0);

					userPermissions = query.getResultList();
					userPermissions.forEach(permission -> {

						PermissionDto dto = PermissionDto.builder().id(permission.getPermission().getId())
								.name(permission.getPermission().getName())
								.description(permission.getPermission().getDescription()).build();
						allIn.add(dto);
					});
					pagin.getOptions().put("allIn", allIn);
				}
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public List<PlatformUserLoginDto> getPfOfUser(String email) {
		
		List<PlatformUserLogin> pfs = platformUserLoginRepository.findByEmail(email);
		if (pfs.isEmpty()) {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.YEAR, 1);
			pfs.add(platformUserLoginRepository.save(PlatformUserLogin.builder().active(true).email(email).name("MOBILE").startTime(System.currentTimeMillis()).endTime(c.getTimeInMillis()).build()));
			c.add(Calendar.YEAR, 99);
			pfs.add(platformUserLoginRepository.save(PlatformUserLogin.builder().active(true).email(email).name("OTHER").startTime(System.currentTimeMillis()).endTime(c.getTimeInMillis()).build()));
		}
		return pfs
				.stream()
				.map(pf -> PlatformUserLoginDto.builder()
						.id(pf.getId())
						.active(pf.getActive())
						.name(pf.getName())
						.description(pf.getDescription())
						.email(pf.getEmail())
						.startTime(pf.getStartTime())
						.endTime(pf.getEndTime())
						.build())
				.collect(Collectors.toList());
	}

	@Transactional
	@Override
	public void savePfOfUser(PlatformUserLoginDto dto) {
		PlatformUserLogin pf = platformUserLoginRepository.findByEmailAndName(dto.getEmail(), dto.getName());
		if (pf == null) {
			throw new ApiException("user notfound!");
		}
		pf.setStartTime(dto.getStartTime());
		pf.setEndTime(dto.getEndTime());
		pf.setActive(dto.getActive() == null ? true : dto.getActive());
		platformUserLoginRepository.save(pf);
	}

	@Transactional
	@Override
	public ResponseDto<? extends Object> sendOtp(Map<String, Object> dto) {
		String email = (String) dto.get("email");
		String account = (String) dto.get("account");
		if (StringUtils.isBlank(email)) {
			email = account;
		}
		if (StringUtils.isBlank(email)) {
			throw new ApiException("email is required");
		}
		email = email.toLowerCase();
		
		String otpType = (String) dto.get("otpType");
		String actionType = (String) dto.get("actionType");
		Users user = userRepository.findByEmail(email);
		
		if (user == null) {
			user = userRepository.findByPhoneNumber(email);
		}
		if (user == null) {
			user = Users.builder().email(email.matches("^[+][0-9]+$") ? null : email).phoneNumber(email.matches("^[+][0-9]+$") ? email : null).build();
		}
		
		if (("reset_pwd".equalsIgnoreCase(actionType) || "login".equalsIgnoreCase(actionType)) && user == null) {
			if (StringUtils.isNotBlank((String) dto.get("account")) && StringUtils.isBlank((String) dto.get("email"))) {
				throw new ApiException("account doesn't exists!");	
			}
			throw new ApiException("email doesn't exists!");
		}
		String phone = user == null ? null : user.getPhoneNumber();

		if ("sms".equalsIgnoreCase(otpType) && StringUtils.isBlank(phone)) {
			otpType = "email";
			// throw new ApiException("phone is required");
		}
		email = user.getEmail();
		OTP otp = OTP.builder().actionType(actionType).phone(phone).email(user.getEmail()).build();
		int otpLenth = 6;
		try {
			otpLenth = Integer.parseInt(AppProps.get("otp_length", "6"));
			if (otpLenth < 4) {
				otpLenth = 4;
			}
		} catch (Exception e) {
			otpLenth = 6;
		}
		otp.setOtp(Utils.randomOtp(otpLenth));
		String msgId = null;
		String reMsg = null;
		if ("sms".equalsIgnoreCase(otpType)) {
			msgId = notificationService.sendSMS("MMS-" + otp.getOtp(), phone.trim());
			otp.setTrack("AWS SNS: " + msgId + " SMS: " + "MMS-" + otp.getOtp());	
			reMsg = "OTP has been sent to " + phone;
		}
		
		if ("email".equalsIgnoreCase(otpType)) {
			msgId = notificationService.sendEmail("<html><body>" + "MMS-" + otp.getOtp() + "</body></html>", email.trim(), AppProps.get("EMAIL_OTP_SUBJECT", "MMS-OTP"));
			otp.setTrack("AWS SES: " + msgId + " EMAIL: " + "<html><body>" + "MMS-" + otp.getOtp() + "</body></html>");
			reMsg = "OTP has been sent to email " + email;
		}

		otp.setOtpType(otpType);

		otp.setStartTime(System.currentTimeMillis());
		String exp = AppProps.get("otp_expiry_in_mls", (30 * 60 * 1000l) + "");
		if ("reset_pwd".equalsIgnoreCase(actionType)) {
			exp = AppProps.get("otp_reset_pwd_expiry_in_mls", (1 * 60 * 60 * 1000l) + "");
		}
		if ("change_pwd".equalsIgnoreCase(actionType)) {
			exp = AppProps.get("otp_change_pwd_expiry_in_mls", (1 * 60 * 60 * 1000l) + "");
		}
		if ("registry".equalsIgnoreCase(actionType)) {
			exp = AppProps.get("otp_registry_expiry_in_mls", (1 * 60 * 60 * 1000l) + "");
		}
		try {
			otp.setEndTime(otp.getStartTime() + Long.parseLong(exp));
		} catch (Exception e) {
			otp.setEndTime(otp.getStartTime() + (30 * 60 * 1000l));
		}
		em.createQuery("UPDATE OTP set endTime = startTime where (email = '" + email + "' or phone = '" + phone + "') AND actionType = '" + actionType + "'").executeUpdate();
		em.flush();
		em.persist(otp);
		return ResponseDto.builder().success(true).message(reMsg).build();
	}
	
	@Override
	public void invalidOtp(String phoneOrEmail, String otp) {
		em.createQuery("UPDATE OTP set endTime = startTime where (email = '" + phoneOrEmail + "' or phone = '" + phoneOrEmail + "') AND otp = '" + otp + "'").executeUpdate();
	}
	
	void invalidToken(String token) {
		em.createQuery("UPDATE Token set endTime = startTime where token = '" + token + "'").executeUpdate();
	}

	@Override
	@Transactional
	public void updatePhoneNumber(String phoneNumber) {
		Users user = userRepository.findByEmail(SecurityUtils.getEmail());
		user.setPhoneNumber(phoneNumber);
		userRepository.save(user);
	}
	
	@Transactional(readOnly = true)
	@Override
	public Object getCredentialType(String username) {
		Users user = userRepository.findByEmail(username);

        if (user == null) {
        	user = userRepository.findByUsername(username);
        }
        
        if (user == null) {
        	user = userRepository.findByPhoneNumber(username);
        }
        
        if (user == null) {
        	List<Users> us = userRepository.findByLcPhoneNumber(username);
        	if (us.size() == 1) {
        		user = us.get(0);
        	}
        }
        
        if (user == null) {
        	return null;
        }
        
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("username", username);
        userDetails.put("loginOtpRequire", (BooleanUtils.isTrue(user.getLoginOtpRequire()) || BooleanUtils.isTrue(user.getFirstLoginOtpRequire())));
        userDetails.put("app", user == null ? new ArrayList<>() : user.getAppCodes().stream().map(a -> a.getAppCode().getName()).collect(Collectors.toList()));
		return userDetails;
	}
	
	@Override
	@Transactional
	public void assignAppCodeForEmail(String appCode, String email) {
		Users user = userRepository.findByEmail(email);
		if (user == null) {
			throw new RuntimeException(MSG_USER_NOT_FOUND);
		}
		
		AppCode code = appCodeRepository.findByName(appCode);
		if (code == null) {
			throw new RuntimeException("App code not found!");
		}

        if (userAppCodeRepository.findByAppCodeNameAndUserEmail(appCode, user.getEmail()) == null) {
        	UserAppCode userAppCode = new UserAppCode();
        	userAppCode.setAppCode(code);
        	userAppCode.setUser(user);
        	userAppCodeRepository.save(userAppCode);
        }
	}
	
	@Override
	@Transactional
	public void assignAppCodeForPhone(String appCode, String phone) {
		Users user = userRepository.findByPhoneNumber(phone);
		if (user == null) {
			throw new RuntimeException(MSG_USER_NOT_FOUND);
		}
		
		AppCode code = appCodeRepository.findByName(appCode);
		if (code == null) {
			throw new RuntimeException("App code not found!");
		}

        if (userAppCodeRepository.findByAppCodeNameAndUserEmail(appCode, user.getEmail()) == null) {
        	UserAppCode userAppCode = new UserAppCode();
        	userAppCode.setAppCode(code);
        	userAppCode.setUser(user);
        	userAppCodeRepository.save(userAppCode);
        }
	}
	
	@Transactional
	@Override
	public boolean validatePassword(LoginRequestDto loginRequestDTO) {
		String email = SecurityUtils.getEmail();
		String password = loginRequestDTO.getPassword();
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
			return true;
		} catch (DisabledException e) {
			throw new AuthenticationException(Message.USER_IS_DISABLE, e);
		} catch (BadCredentialsException e) {
			throw new AuthenticationException(Message.INVALID_USERNAME_PASSWORD, e);
		}
	}
	
	@Override
	@Transactional
	public void saveSubGroup(Map<String, Object> payload) {
		String name = (String) payload.get("name"); 
		
		if (StringUtils.isBlank(name)) {
			throw new RuntimeException("Group name must not be empty!");
		}
		
		String groupType = (String) payload.get("groupType");
		String desc = (String) payload.get("desc");
		
//		if (StringUtils.isNotBlank(parentGroupName)) {
//			groupUserRepository.findByName(parentGroupName).orElseThrow(() -> new RuntimeException("Parent group doesn't exists"));
//		}
		SubGroup subGroup = subGroupRepository.findByNameAndOwner(name, SecurityUtils.getEmail()).orElse(new SubGroup());
		subGroup.setDesc(desc);
		subGroup.setName(name);
		subGroup.setOwner(SecurityUtils.getEmail());
		// subGroup.setParentGroupName(parentGroupName);
		subGroup.setGroupType(groupType);
		subGroupRepository.save(subGroup);
	}

	@Override
	@Transactional
	public void deleteSubGroup(Long id) {
		SubGroup subGroup;
		if (SecurityUtils.hasAnyRole("STAFF")) {
			subGroup = subGroupRepository.findById(id).orElseThrow(() -> new RuntimeException("Group doesn't exists"));
		} else {
			subGroup = subGroupRepository.findByIdAndOwner(id, SecurityUtils.getEmail()).orElseThrow(() -> new RuntimeException("Group doesn't exists"));
		}
		subGroup.setOwner(subGroup.getOwner() + "_delete" + System.currentTimeMillis());
		subGroup.setName(subGroup.getName() + "_delete" + System.currentTimeMillis());
		subGroupRepository.save(subGroup);
	}
	
	@Override
	@Transactional
	public void addUserToSubGroup(Map<String, Object> payload) {
		
		String name = (String) payload.get("name"); 
		if (StringUtils.isBlank(name)) {
			throw new RuntimeException("Group name must not be empty!");
		}
		
		List<String> members = (List<String>) payload.get("members");
		
		Map<String, Users> existsUsers = new LinkedHashMap<>();
		
		userRepository.findByEmailIn(members)
				.forEach(u -> {
			existsUsers.put(u.getEmail(), u);
		});
		
		SubGroup subGroup = SecurityUtils.hasAnyRole("STAFF") ? subGroupRepository.findByName(name).orElseThrow(() -> new RuntimeException("Group doesn't exists")) 
				: subGroupRepository.findByNameAndOwner(name, SecurityUtils.getEmail()).orElseThrow(() -> new RuntimeException("Group doesn't exists"));
		
		Map<String, SubGroupMember> map = new LinkedHashMap<>();
		subGroupMemberRepository.findByGroupIdAndEmailIn(subGroup.getId(), members)
		.forEach(mb -> map.put(mb.getEmail(), mb));
		
		boolean isPaStaff = SecurityUtils.hasAnyRole("STAFF");
		
		members.forEach(mb -> {
			
			
			if (!isPaStaff && !checkMatchAnyCompany(SecurityUtils.getEmail(), mb)) {
				throw new RuntimeException(mb + " not in your company!");
			}
			
			Users us = existsUsers.get(mb);
			if (us == null) {
				throw new RuntimeException("Email " + mb + " doesn't exist!");
			}
//			if (us.getProjects().stream().anyMatch(up -> "ALL".equalsIgnoreCase(up.getProject().getName()))) {
//				throw new RuntimeException("Email " + mb + " doesn't exist!");
//			}
			if (map.get(mb) != null) {
				throw new RuntimeException("Email " + mb + " already exist in this group!");
			}
			
			SubGroupMember groupMember = new SubGroupMember();
			groupMember.setEmail(mb);
			groupMember.setGroup(subGroup);
			subGroupMemberRepository.save(groupMember);
		});
	}
	
	@Override
	@Transactional
	public void removeUserFromSubGroup(Map<String, Object> payload) {
		
		String name = (String) payload.get("name"); 
		if (StringUtils.isBlank(name)) {
			throw new RuntimeException("Group name must not be empty!");
		}
		
		List<String> members = (List<String>) payload.get("members");
		SubGroup subGroup;
		boolean isPaStaff = SecurityUtils.hasAnyRole("STAFF");
		if (SecurityUtils.hasAnyRole("STAFF")) {
			subGroup = subGroupRepository.findByName(name).orElseThrow(() -> new RuntimeException("Group doesn't exists"));
		} else {
			subGroup = subGroupRepository.findByNameAndOwner(name, SecurityUtils.getEmail()).orElseThrow(() -> new RuntimeException("Group doesn't exists"));
		}
		
		subGroupMemberRepository.findByGroupIdAndEmailIn(subGroup.getId(), members)
		.forEach(mb -> {
			if (!isPaStaff && !checkMatchAnyCompany(SecurityUtils.getEmail(), mb.getEmail())) {
				throw new RuntimeException(mb.getEmail() + " not in your company!");
			}
			subGroupMemberRoleRepository.findByMemberId(mb.getId())
			.forEach(subGroupMemberRoleRepository::delete);
			subGroupMemberRepository.delete(mb);
		});
	}
	
	private boolean checkMatchAnyCompany(String email1, String email2) {
		List<String> currCpns = userCompanyRepository.findCompanyNameByUserEmail(email1);
		Users user = userAppCodeRepository.findByAppCodeNameAndUserEmail(AppCodeSelectedHolder.get(), email2);
		if (user == null) {
			return false;
		}

		if (currCpns.indexOf("ALL") > -1) {
			return true;
		}
		
		if (currCpns.isEmpty() && (user.getCompanies() == null || user.getCompanies().isEmpty())) {
			return true;
		}
		
		return user.getCompanies().stream().anyMatch(cp -> "ALL".equalsIgnoreCase(cp.getCompany().getName()) || currCpns.contains(cp.getCompany().getName()));
	}
	
	@Override
	@Transactional
	public void addRoleToSubGroupMember(Map<String, Object> payload) {
		
		String name = (String) payload.get("name"); 
		if (StringUtils.isBlank(name)) {
			throw new RuntimeException("Group name must not be empty!");
		}
		
		String member = (String) payload.get("member"); 
		List<String> roles = (List<String>) payload.get("roles");
		SubGroup subGroup;
		if (SecurityUtils.hasAnyRole("STAFF")) {
			subGroup = subGroupRepository.findByName(name).orElseThrow(() -> new RuntimeException("Group doesn't exists"));
		} else {
			subGroup = subGroupRepository.findByNameAndOwner(name, SecurityUtils.getEmail()).orElseThrow(() -> new RuntimeException("Group doesn't exists"));
		}
		
		boolean isPaStaff = SecurityUtils.hasAnyRole("STAFF");
		if (!isPaStaff && !checkMatchAnyCompany(SecurityUtils.getEmail(), member)) {
			throw new RuntimeException(member + " not in your company!");
		}
		
		subGroupMemberRepository.findByGroupIdAndEmailIn(subGroup.getId(), Arrays.asList(member))
		.forEach(mb -> {
			Map<String, SubGroupMemberRole> map = new LinkedHashMap<>();
			subGroupMemberRoleRepository.findByMemberId(mb.getId())
			.forEach(mbr -> map.put(mbr.getRole(), mbr));
			
			roles.forEach(r -> {
				SubGroupMemberRole groupMemberRole = map.get(r);
				if (groupMemberRole == null) {
					groupMemberRole = new SubGroupMemberRole();
				}
				groupMemberRole.setRole(r);
				groupMemberRole.setMember(mb);
				groupMemberRole.setEmail(mb.getEmail());
				subGroupMemberRoleRepository.save(groupMemberRole);
			});
			map.forEach((k, v) -> {
				if (!roles.contains(k)) {
					subGroupMemberRoleRepository.delete(v);
				}
			});
		});
	}	
	
	@Override
	@Transactional
	public void removeRoleFromSubGroupMember(Map<String, Object> payload) {
		
		String name = (String) payload.get("name"); 
		if (StringUtils.isBlank(name)) {
			throw new RuntimeException("Group name must not be empty!");
		}
		
		String member = (String) payload.get("member"); 
		
		boolean isPaStaff = SecurityUtils.hasAnyRole("STAFF");
		if (!isPaStaff && !checkMatchAnyCompany(SecurityUtils.getEmail(), member)) {
			throw new RuntimeException(member + " not in your company!");
		}
		
		List<String> roles = (List<String>) payload.get("roles");
		SubGroup subGroup;
		if (SecurityUtils.hasAnyRole("STAFF")) {
			subGroup = subGroupRepository.findByName(name).orElseThrow(() -> new RuntimeException("Group doesn't exists"));
		} else {
			subGroup = subGroupRepository.findByNameAndOwner(name, SecurityUtils.getEmail()).orElseThrow(() -> new RuntimeException("Group doesn't exists"));
		}
		
		subGroupMemberRepository.findByGroupIdAndEmailIn(subGroup.getId(), Arrays.asList(member))
		.forEach(mb -> {
			subGroupMemberRoleRepository.findByMemberIdAndRoleIn(mb.getId(), roles)
			.forEach(subGroupMemberRoleRepository::delete);
		});
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<SubGroup> getSubGroupOwner() {
		return subGroupRepository.findByOwner(SecurityUtils.getEmail());
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<SubGroup> getSubGroupOfUser(String email) {

		List<SubGroup> groups = new ArrayList<>();
		Set<Long> sgIdCheck = new HashSet<>();
		if (SecurityUtils.hasAnyRole("STAFF")) {
			subGroupRepository.findAll()
				.forEach(group -> {
					if (!group.getName().contains("_delete") && !sgIdCheck.contains(group.getId())) {
						groups.add(group);
						sgIdCheck.add(group.getId());
					}
				});
			return groups;
		}
		
		List<SubGroupMember> groupMembers = subGroupMemberRepository.findByEmail(email);
		groupMembers.forEach(g -> {
			SubGroup group = g.getGroup();
			if (!group.getName().contains("_delete") && !sgIdCheck.contains(group.getId())) {
				groups.add(group);
				sgIdCheck.add(group.getId());
				List<String> roles = subGroupMemberRoleRepository.findByMemberId(g.getId()).stream().map(r -> r.getRole()).collect(Collectors.toList());
				List<String> permissions = rolePermissionRepository.findByRoleNameIn(roles).stream().map(rp -> rp.getPermission().getName()).collect(Collectors.toList());
				group.setRoles(roles);
				group.setPermissions(permissions);
			}
		});
		return groups;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	@Transactional(readOnly = true)
	public List getUserOfSubGroup(Map<String, Object> payload) {
		Number subGroupId = (Number) payload.get("subGroupId");
		List<SubGroupMember> subGroupMembers = subGroupMemberRepository.findByGroupId(subGroupId.longValue());
		
		Map<String, Long> emailMemberIds = new LinkedHashMap<>();
		subGroupMembers.forEach(gm -> emailMemberIds.put(gm.getEmail(), gm.getId()));
		
		List<SubGroupMemberRole> memberRoles = subGroupMemberRoleRepository.findByMemberIdIn(emailMemberIds.values());
		
		Map<String, Role> roleMap = new LinkedHashMap<>();
		roleRepository.findByNameIn(memberRoles.stream().map(mr -> mr.getRole()).collect(Collectors.toList()))
		.forEach(r -> roleMap.put(r.getName(), r));
		
		Map<Long, List<Role>> mRoles = new LinkedHashMap<>();// map role memberId
		memberRoles.forEach(mr -> {
			List<Role> rls = mRoles.computeIfAbsent(mr.getMember().getId(), k -> new ArrayList<>());
			if (roleMap.get(mr.getRole()) != null) {
				rls.add(roleMap.get(mr.getRole()));
			}
		});
		
		return userAppCodeRepository.findByAppCodeNameAndUserEmailIn(AppCodeSelectedHolder.get(), emailMemberIds.keySet())
				.stream()
				.map(user -> {
					List<Role> roleInSgs = mRoles.computeIfAbsent(emailMemberIds.get(user.getEmail()), k -> new ArrayList<>());
					List<String> roles = new ArrayList<>();
		
					UserDto dto = UserDto.builder().id(user.getUserId()).username(user.getUsername()).email(user.getEmail())
							.fullName(user.getFullName()).firstName(user.getFirstName()).lastName(user.getLastName())
							.phoneNumber(user.getPhoneNumber()).avatar(user.getAvatar())
							.fullName(user.getFirstName() + " " + user.getLastName()).status(user.getStatus())
							.changePwdRequire(user.getChangePwdRequire())
							.loginOtpRequire(user.getLoginOtpRequire())
							.identification(user.getIdentification())
							.companies(user.getCompanies().stream().map(uc -> uc.getCompany().getName()).collect(Collectors.toList()))
							.roleDescs(roleInSgs.stream().filter(r -> r.getAppCode().getName().equals(AppCodeSelectedHolder.get())).map(authority -> {
								roles.add(authority.getName());
								return SimpleMap.init("name", authority.getName()).more("desc",
										authority.getDesc());
							}).collect(Collectors.toList())).build();
					dto.setRoles(roles);
					return dto;
				})
				.filter(it -> it != null)
				.collect(Collectors.toList());
	}
	
	@Override
	public void getRoleOfMemberSubGroup(PaginDto<RoleDto> pagin) {
		String memberEmail = (String) pagin.getOptions().get("memberEmail");
		Number groupId = (Number) pagin.getOptions().get("groupId");
		SubGroupMember subGroupMember  = subGroupMemberRepository.findByGroupIdAndEmail(groupId.longValue(), memberEmail).orElseThrow(() -> new RuntimeException("Member doesn't exists"));
		List<SubGroupMemberRole> memberRoles = subGroupMemberRoleRepository.findByMemberId(subGroupMember.getId());
		
		roleRepository.findByNameIn(memberRoles.stream().map(mr -> mr.getRole()).collect(Collectors.toList()))
		.forEach(r -> {
			RoleDto dto = RoleDto.builder().id(r.getId()).name(r.getName())
					.desc(r.getDesc()).build();
			pagin.getResults().add(dto);
		});
		pagin.setTotalRows(Long.valueOf(pagin.getResults().size()));
	}

	@Transactional
	@Override
	public void saveDMSAppUser(CreateDMSAppUserDto dto) {
		if (dto.getEmail() == null || !dto.getEmail().trim().matches("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")) {
			throw new RuntimeException("Invalid email!");
		}
		
		if (dto.getPhoneNumber() == null || !dto.getPhoneNumber().trim().matches("^\\+\\d{1,3}\\d{8,}$")) {
			throw new RuntimeException("Invalid phone! (ex: +6500001111)");
		}
		
		dto.setEmail(dto.getEmail().trim());
		
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
    	userDto.setFirstLoginOtpRequire(dto.getFirstLoginOtpRequire());
    	userDto.setHPwd(dto.getHPwd());
    	userDto.setAutoDeleteDate(dto.getAutoDeleteDate());
    	save(userDto);
    	
    	PlatformUserLogin pf = platformUserLoginRepository.findByEmailAndName(dto.getEmail(), "OTHER");
		if (pf == null) {
			PlatformUserLogin newPf = new PlatformUserLogin();
			newPf.setActive(false);
			newPf.setEmail(dto.getEmail());
			newPf.setName("OTHER");
			newPf.setStartTime(System.currentTimeMillis());
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
			newPf.setStartTime(System.currentTimeMillis());
			newPf.setEndTime(4102444800000l);
			platformUserLoginRepository.save(newPf);
		} else {
			pf.setActive(true);
			pf.setStartTime(0l);
			pf.setEndTime(4102444800000l);
    		platformUserLoginRepository.save(pf);	
		}
		if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
			Users user = userRepository.findByEmail(dto.getEmail());
			permissionRepository.findByAppCodeNameAndNameIn(AppCodeSelectedHolder.get(), dto.getPermissions())
			.forEach(p -> {
				UserPermission up = new UserPermission();
				up.setUser(user);
				up.setPermission(p);
				userPermissionRepository.save(up);
			});
			
		}
		
		
		dto.setId(userDto.getId());
	}

	@Transactional
	@Override
	public void syncAccess(String fromUsername, String toUsername) {

		if (!SecurityUtils.hasAnyRole("SUPER_ADMIN", AppCodeSelectedHolder.get() + "_SUPER_ADMIN")) {
			throw new RuntimeException("Access denied!");
		}
		
		if ("henry@gmail.com".equalsIgnoreCase(toUsername)) {
			throw new RuntimeException("Access denied!");
		}
		
		Users from = userRepository.findByEmail(fromUsername);
		Users to = userRepository.findByEmail(toUsername);
		
		if (from == null) {
			throw new RuntimeException("User " + fromUsername + " notfound!");
		}
		
		if (to == null) {
			throw new RuntimeException("User " + toUsername + " notfound!");
		}

		String appCode = AppCodeSelectedHolder.get();
		List<GroupUser> groups = groupUserRepository.findGroupByUserUserId(from.getUserId(), appCode);
		List<Role> roles = roleRepository.findRoleByUserUserId(from.getUserId(), appCode);
		List<Permission> permissions = permissionRepository.findPermissionByUserUserId(from.getUserId(), appCode);
		
		userGroupRepository.findByUserIdAndAppCode(to.getUserId(), appCode)
		.forEach(userGroupRepository::delete);
		userGroupRepository.flush();
		
		userRoleRepository.findByUserIdAndAppCode(to.getUserId(), appCode)
		.forEach(userRoleRepository::delete);
		userRoleRepository.flush();
		
		userPermissionRepository.findByUserIdAndAppCode(to.getUserId(), appCode)
		.forEach(userPermissionRepository::delete);
		userPermissionRepository.flush();
		
		for (GroupUser g : groups) {
			userGroupRepository.save(UserGroup.builder().groupUser(g).user(to).build());
		}
		for (Role r : roles) {
			userRoleRepository.save(UserRole.builder().role(r).user(to).build());
		}
		for (Permission p : permissions) {
			userPermissionRepository.save(UserPermission.builder().permission(p).user(to).build());
		}
	}
	
	@PostConstruct
	public void init() {
		new Thread(() -> {
			countryCodeRepository.findAll().forEach(c -> {
				cCodes.add(c.getCallingCode().replaceAll("[^0-9]", ""));
			});
			cCodes.sort((o1, o2) -> {
				return Integer.parseInt(o1) < Integer.parseInt(o2) ? 1 : -1;
			});
		}).start();
	}

}
 