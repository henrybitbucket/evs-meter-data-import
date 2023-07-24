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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

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

import com.pa.evs.constant.Message;
import com.pa.evs.constant.ValueConstant;
import com.pa.evs.dto.ChangePasswordDto;
import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.LoginRequestDto;
import com.pa.evs.dto.LoginResponseDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.PlatformUserLoginDto;
import com.pa.evs.dto.ResetPasswordDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.exception.customException.AuthenticationException;
import com.pa.evs.exception.customException.DuplicateUserException;
import com.pa.evs.model.GroupUser;
import com.pa.evs.model.OTP;
import com.pa.evs.model.Permission;
import com.pa.evs.model.PlatformUserLogin;
import com.pa.evs.model.Role;
import com.pa.evs.model.RolePermission;
import com.pa.evs.model.Token;
import com.pa.evs.model.UserGroup;
import com.pa.evs.model.UserPermission;
import com.pa.evs.model.UserRole;
import com.pa.evs.model.Users;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.repository.PermissionRepository;
import com.pa.evs.repository.PlatformUserLoginRepository;
import com.pa.evs.repository.RolePermissionRepository;
import com.pa.evs.repository.RoleRepository;
import com.pa.evs.repository.UserGroupRepository;
import com.pa.evs.repository.UserPermissionRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.repository.UserRoleRepository;
import com.pa.evs.security.jwt.JwtTokenUtil;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.AuthenticationService;
import com.pa.evs.sv.AuthorityService;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.SettingService;
import com.pa.evs.utils.ApiResponse;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.SimpleMap;
import com.pa.evs.utils.Utils;

@Service
@SuppressWarnings("unchecked")
public class AuthenticationServiceImpl implements AuthenticationService {

	static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private RolePermissionRepository rolePermissionRepository;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Autowired
	private GroupUserRepository groupUserRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private UserPermissionRepository userPermissionRepository;
	
	@Autowired
	private PlatformUserLoginRepository platformUserLoginRepository;

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
	EntityManager em;
	
	@Value("${jwt.header}")
	private String tokenHeader;

	static final String MSG_USER_NOT_FOUND = "User not found!";

	static final String EMAIL = "email";

	static final String USER_PRINCIPAL_NAME = "userPrincipalName";

	static Map<String, List<Permission>> map = new HashMap<String, List<Permission>>();

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
		}

		// Reload password post-security so we can generate the token
		final JwtUser userDetails = (JwtUser) userDetailsService.loadUserByUsername(email);
		
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
		
		if (BooleanUtils.isTrue(user.getLoginOtpRequire())) {
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
		return apiResponse.response(ValueConstant.SUCCESS, ValueConstant.TRUE,
				LoginResponseDto.builder().token(token).authorities(
						userDetails.getAuthorities().stream().map(au -> au.getAuthority()).collect(Collectors.toList()))
						.changePwdRequire(userDetails.getChangePwdRequire())
						.phoneNumber(userDetails.getPhoneNumber())
						.email(userDetails.getEmail())
						.firstName(user.getFirstName())
						.lastName(user.getLastName())
						.build());
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
		user.setLastLogin(new Date());
		userRepository.save(user);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void loadRoleAndPermission() {
		List<Role> roles = roleRepository.findAll();
		for (Role role : roles) {
			List<Permission> permissions = new ArrayList();
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

		if (StringUtils.isBlank(dto.getUsername())) {
			dto.setUsername(dto.getEmail());
		}
		if (dto.getId() != null) {
			Optional<Users> opt = userRepository.findById(dto.getId());
			if (opt.isPresent()) {
				en = opt.get();
			} else {
				en = new Users();
				en.setUsername(dto.getUsername());
				en.setEmail(dto.getEmail());
				en.setChangePwdRequire(true);
			}
		} else {
			en = new Users();
			en.setUsername(dto.getUsername());
			en.setEmail(dto.getEmail());
			en.setChangePwdRequire(true);
		}

		if (en.getUserId() == null && userRepository.findByEmail(dto.getEmail()) != null) {
			throw new RuntimeException("Email already exists!");
		}
		
		if (dto.getPhoneNumber() != null) {
			Users existsUser = userRepository.findByPhoneNumber(dto.getPhoneNumber());
			if (existsUser != null && en.getUserId() != null && existsUser.getUserId().longValue() != en.getUserId().longValue()) {
				throw new RuntimeException("Phone already exists!");
			}
		}
		
		if (en.getUserId() != null && dto.getChangePwdRequire() != null) {
			en.setChangePwdRequire(dto.getChangePwdRequire());
		}

		en.setFirstName(dto.getFirstName());
		en.setLastName(dto.getLastName());
		en.setPhoneNumber(dto.getPhoneNumber());
		en.setStatus(dto.getStatus());
		en.setLoginOtpRequire(dto.getLoginOtpRequire());

		if (StringUtils.isNotBlank(dto.getPassword())) {

			Utils.validatePwd(dto.getPassword(), en.getLastPwd());
			en.setPassword(passwordEncoder.encode(dto.getPassword()));
			en.setLastChangePwd(System.currentTimeMillis());

		} else if (dto.getId() == null && StringUtils.isBlank(dto.getPassword())) {
			en.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
			en.setLastChangePwd(System.currentTimeMillis());
		}

		userRepository.save(en);
		Long userId = en.getUserId();

		// Role

		Set<String> roleNames = new HashSet<>();
		dto.getRoles().forEach(roleNames::add);
		userRoleRepository.deleteNotInRoles(userId,
				roleNames.isEmpty() ? new HashSet<>(Arrays.asList("-1")) : roleNames);
		List<String> existsRoleNames = userRoleRepository.findRoleNameByUserId(userId);
		List<Role> roles = userRoleRepository.findRoleByRoleNameIn(roleNames);
		for (Role role : roles) {
			if (!existsRoleNames.contains(role.getName())) {
				UserRole userRole = new UserRole();
				userRole.setUser(en);
				userRole.setRole(role);
				userRoleRepository.save(userRole);
			}
		}
		;
		// End role
	}

	@Transactional
	@Override
	public void saveRole(UserDto dto) {
		Optional<Users> user = userRepository.findById(dto.getId());
		if (user.isPresent()) {
			List<Role> rus = roleRepository.findRoleByUserUserId(dto.getId());
			Map<Long, Role> mRs = new LinkedHashMap<>();
			rus.forEach(ru -> mRs.put(ru.getId(), ru));
			Set<String> userRoles = new HashSet<>();
			for (RoleDto roleDto : dto.getRole()) {
				userRoles.add(roleDto.getName());
				if (!mRs.containsKey(roleDto.getId())) {
					Optional<Role> role = roleRepository.findById(roleDto.getId());
					if (role.isPresent()) {
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
			}
			
			try {
				userRoleRepository.deleteNotInRoles(user.get().getUserId(),
						userRoles.isEmpty() ? new HashSet<>(Arrays.asList("-1")) : userRoles);
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	@Transactional
	@Override
	public void saveGroup(UserDto dto) {
		Optional<Users> user = userRepository.findById(dto.getId());
		if (user.isPresent()) {
			Set<Long> userGroups = new HashSet<>();
			List<GroupUser> gus = groupUserRepository.findGroupByUserUserId(dto.getId());
			Map<Long, GroupUser> mGs = new LinkedHashMap<>();
			gus.forEach(gu -> mGs.put(gu.getId(), gu));

			for (GroupUserDto groupUserDto : dto.getGroupUsers()) {
				userGroups.add(groupUserDto.getId());
				if (!mGs.containsKey(groupUserDto.getId())) {
					Optional<GroupUser> groupUser = groupUserRepository.findById(groupUserDto.getId());
					UserGroup userGroup = new UserGroup();
					if (groupUser.isPresent()) {
						userGroup.setGroupUser(groupUser.get());
						userGroup.setUser(user.get());
						try {
							userGroupRepository.save(userGroup);
						} catch (Exception e) {
							LOGGER.error(e.getMessage(), e);
						}
					}
				}
			}

			try {
				groupUserRepository.deleteNotInGroups(user.get().getUserId(),
						userGroups.isEmpty() ? new HashSet<>(Arrays.asList(-1l)) : userGroups);
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	@Transactional
	@Override
	public void savePermission(UserDto dto) {
		Optional<Users> user = userRepository.findById(dto.getId());
		if (user.isPresent()) {

			List<Permission> permissions = userPermissionRepository.findPermissionByUserUserId(dto.getId());
			Map<Long, Permission> mPs = new LinkedHashMap<>();
			permissions.forEach(p -> mPs.put(p.getId(), p));
			Set<Long> newPs = new LinkedHashSet<>();
			for (PermissionDto permissionDto : dto.getPermissions()) {

				newPs.add(permissionDto.getId());
				if (!mPs.containsKey(permissionDto.getId())) {
					Optional<Permission> permission = permissionRepository.findById(permissionDto.getId());
					UserPermission userPermission = new UserPermission();
					if (permission.isPresent()) {
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
			}
			userPermissionRepository.flush();
			for (Map.Entry<Long, Permission> en : mPs.entrySet()) {
				if (!newPs.contains(en.getKey())) {
					userPermissionRepository.deleteByUserIdAndPermissionId(dto.getId(), en.getKey());
				}
			}
		}
	}

	public ResponseDto<LoginResponseDto> passLogin(LoginRequestDto loginRequestDTO) {
		String email = loginRequestDTO.getEmail();
		// Reload password post-security so we can generate the token
		final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
		final String token = jwtTokenUtil.generateToken(userDetails);

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
		String token = request.getHeader(tokenHeader);
		String email = jwtTokenUtil.getUsernameFromToken(token);
		JwtUser jwtUser = (JwtUser) userDetailsService.loadUserByUsername(email);
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
		em.createQuery("UPDATE CARequestLog c set installer = null where c.installer.userId = " + userId)
				.executeUpdate();
		em.createQuery("UPDATE Log c set user = null where c.user.userId = " + userId).executeUpdate();
		em.createQuery("UPDATE LogBatch c set user = null where c.user.userId = " + userId).executeUpdate();
		em.createQuery("UPDATE GroupTask c set user = null where c.user.userId = " + userId).executeUpdate();
		em.createQuery("DELETE FROM UserGroup c where c.user.userId = " + userId).executeUpdate();
		em.createQuery("DELETE FROM UserPermission c where c.user.userId = " + userId).executeUpdate();
		em.createQuery("DELETE FROM UserRole c where c.user.userId = " + userId).executeUpdate();
		em.flush();
		userRepository.deleteById(userId);
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
	}

	@Override
	public void getUsers(PaginDto<UserDto> pagin) {
		StringBuilder sqlBuilder = new StringBuilder("FROM Users");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Users");

		StringBuilder sqlCommonBuilder = new StringBuilder();
		sqlCommonBuilder.append(" WHERE 1=1 ");
		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY userId asc");
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
					.roleDescs(user.getRoles().stream().map(authority -> {
						roles.add(authority.getRole().getName());
						return SimpleMap.init("name", authority.getRole().getName()).more("desc",
								authority.getRole().getDesc());
					}).collect(Collectors.toList())).build();
			dto.setRoles(roles);
			pagin.getResults().add(dto);
		});
	}

	@Override
	public void getPermissionsOfUser(PaginDto<UserDto> pagin) {
		Users user = userRepository.findByEmail(SecurityUtils.getEmail());

		if (user == null) {
			user = userRepository.findByUsername(SecurityUtils.getEmail());
		}
		permissionRepository.findAll();
		List<PermissionDto> permissionsDto = new ArrayList();
		for (UserRole userRole : user.getRoles()) {
			if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN")) {
				for (Permission per : permissionRepository.findAll()) {
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
			List<UserPermission> userPermissions = userPermissionRepository.findAll();
			for (UserPermission userPer : userPermissions) {
				if (userPer.getUser().getUserId() == user.getUserId()) {
					List<PermissionDto> permissionss = new ArrayList();
					boolean check = false;
					for (PermissionDto per : permissionsDto) {
						if (per.getId() == userPer.getPermission().getId()) {
							check = true;
						}
					}
					if (check != true) {
						PermissionDto PerDto = new PermissionDto();
						PerDto.setId(userPer.getPermission().getId());
						PerDto.setName(userPer.getPermission().getName());
						PerDto.setDescription(userPer.getPermission().getDescription());
						permissionsDto.add(PerDto);
					}
				}
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
				if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN")) {
					check = true;
				}
			}
			if (check == true) {
				StringBuilder sqlBuilder = new StringBuilder("FROM Role");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Role");

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
				if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN")) {
					check = true;
				}
			}
			if (check == true) {
				StringBuilder sqlBuilder = new StringBuilder("FROM Role");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Role");

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
	public void getGroupOfUser(PaginDto<GroupUserDto> pagin) {
		Map<String, Object> map = pagin.getOptions();

		Object userId = (Object) map.get("userId");

		StringBuilder sqlBuilder = new StringBuilder("FROM UserGroup ur");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM UserGroup ur");

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
				if (StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN")) {
					check = true;
				}
			}
			if (check == true) {
				StringBuilder sqlBuilder = new StringBuilder("FROM Permission");
				StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Permission");

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
				sqlBuilder.append(" WHERE up.user.userId = " + userId);
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
		if (StringUtils.isBlank(email)) {
			throw new ApiException("email is required");
		}
		String otpType = (String) dto.get("otpType");
		String actionType = (String) dto.get("actionType");
		Users user = userRepository.findByEmail(email);
		if (("reset_pwd".equalsIgnoreCase(actionType) || "login".equalsIgnoreCase(actionType)) && user == null) {
			throw new ApiException("email doesn't exists!");
		}
		String phone = user == null ? null : user.getPhoneNumber();

		if ("sms".equalsIgnoreCase(otpType) && StringUtils.isBlank(phone)) {
			otpType = "email";
			// throw new ApiException("phone is required");
		}
		OTP otp = OTP.builder().actionType(actionType).phone(phone).email(email).build();
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
			msgId = evsPAService.sendSMS("MMS-" + otp.getOtp(), phone.trim());
			otp.setTrack("AWS SNS: " + msgId + " SMS: " + "MMS-" + otp.getOtp());	
			reMsg = "OTP has been sent to " + phone;
		}
		if ("email".equalsIgnoreCase(otpType)) {
			msgId = evsPAService.sendEmail("<html><body>" + "MMS-" + otp.getOtp() + "</body></html>", email.trim(), AppProps.get("EMAIL_OTP_SUBJECT", "MMS-OTP"));
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
		em.createQuery("UPDATE OTP set endTime = startTime where email = '" + email + "' AND actionType = '" + actionType + "'").executeUpdate();
		em.flush();
		em.persist(otp);
		return ResponseDto.builder().success(true).message(reMsg).build();
	}
	
	void invalidOtp(String email, String otp) {
		em.createQuery("UPDATE OTP set endTime = startTime where email = '" + email + "' AND otp = '" + otp + "'").executeUpdate();
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
	
	@Override
	public Object preLogin(String username) {
		Users user = userRepository.findByEmail(username);

        if (user == null) {
        	user = userRepository.findByUsername(username);
        }
        
        if (user == null) {
        	throw new ApiException(MSG_USER_NOT_FOUND);
        }
        
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put(username, username);
        userDetails.put("loginOtpRequire", user.getLoginOtpRequire());
        
		return userDetails;
	}
}
