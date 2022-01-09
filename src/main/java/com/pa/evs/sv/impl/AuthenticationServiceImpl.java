package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

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
import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.LoginRequestDto;
import com.pa.evs.dto.LoginResponseDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.exception.customException.AuthenticationException;
import com.pa.evs.exception.customException.DuplicateUserException;
import com.pa.evs.model.GroupUser;
import com.pa.evs.model.Permission;
import com.pa.evs.model.Role;
import com.pa.evs.model.RolePermission;
import com.pa.evs.model.UserGroup;
import com.pa.evs.model.UserPermission;
import com.pa.evs.model.UserRole;
import com.pa.evs.model.Users;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.repository.PermissionRepository;
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
import com.pa.evs.utils.ApiResponse;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.SimpleMap;


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
    EntityManager em;

    @Value("${jwt.header}")
    private String tokenHeader;
    
    static final String MSG_USER_NOT_FOUND = "User not found!";
    
    static final String EMAIL = "email";
    
    static final String USER_PRINCIPAL_NAME = "userPrincipalName";
    
    static Map<String, List<Permission>> map = new HashMap<String, List<Permission>>();

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
        final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        final String token = jwtTokenUtil.generateToken(userDetails);
        this.updateLastLogin(email);
        this.loadRoleAndPermission();
		return apiResponse.response(ValueConstant.SUCCESS, ValueConstant.TRUE,
				LoginResponseDto.builder().token(token).authorities(
						userDetails.getAuthorities().stream().map(au -> au.getAuthority()).collect(Collectors.toList()))
						.build());
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
		for(Role role : roles) {
			List<Permission> permissions = new ArrayList();
			List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(role.getId());
			for(RolePermission rolePer : rolePermissions) {
				Optional<Permission> permisstion = permissionRepository.findById(rolePer.getPermission().getId());
				if(permisstion.isPresent()) {
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
			}
		} else {
			en = new Users();
			en.setUsername(dto.getUsername());
			en.setEmail(dto.getEmail());
		}
		
		en.setFirstName(dto.getFirstName());
		en.setLastName(dto.getLastName());
		en.setPhoneNumber(dto.getPhoneNumber());
		en.setStatus(dto.getStatus());

		if (StringUtils.isNotBlank(dto.getPassword())) {
			en.setPassword(passwordEncoder.encode(dto.getPassword()));
		} else if (dto.getId() == null && StringUtils.isBlank(dto.getPassword())) {
			en.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
		}
		
		userRepository.save(en);
		Long userId = en.getUserId();
		
		// Role
		
		Set<String> roleNames = new HashSet<>();
		dto.getRoles().forEach(roleNames::add);
		userRoleRepository.deleteNotInRoles(userId, roleNames.isEmpty() ? new HashSet<>(Arrays.asList("-1")) : roleNames);
		List<String> existsRoleNames = userRoleRepository.findRoleNameByUserId(userId);
		List<Role> roles = userRoleRepository.findRoleByRoleNameIn(roleNames);
		for (Role role : roles) {
			if (!existsRoleNames.contains(role.getName())) {
				UserRole userRole = new UserRole();
				userRole.setUser(en);
				userRole.setRole(role);
				userRoleRepository.save(userRole);
			}
		};
		// End role
	}
	
	@Override
	public void saveRole (UserDto dto) {
		Optional<Users> user = userRepository.findById(dto.getId());
		if(user.isPresent()) {
			for(RoleDto roleDto : dto.getRole()) {
				Optional<Role> role = roleRepository.findById(roleDto.getId());
				if(role.isPresent()) {
					UserRole userRole = new UserRole();
					userRole.setRole(role.get());
					userRole.setUser(user.get());
					try {
						userRoleRepository.save(userRole);
					} catch (Exception e) {
						LOGGER.error(e.getMessage(), e);
		      		}
				}
			}
		}
	}
	
	@Override
	public void saveGroup (UserDto dto) {
		Optional<Users> user = userRepository.findById(dto.getId());
		if(user.isPresent()) {
			for(GroupUserDto groupUserDto : dto.getGroupUsers()) {
				Optional<GroupUser> groupUser = groupUserRepository.findById(groupUserDto.getId());
				UserGroup userGroup = new UserGroup();
				if(groupUser.isPresent()) {
					userGroup.setGroupUser(groupUser.get());;
					userGroup.setUser(user.get());
					try {
						userGroupRepository.save(userGroup);
					} catch (Exception e) {
						LOGGER.error(e.getMessage(), e);
		      		}
				}
			}
		}
	}
	
	@Override
	public void savePermission (UserDto dto) {
		Optional<Users> user = userRepository.findById(dto.getId());
		if(user.isPresent()) {
			for(PermissionDto permissionDto : dto.getPermissions()) {
				Optional<Permission> permission = permissionRepository.findById(permissionDto.getId());
				UserPermission userPermission = new UserPermission();
				if(permission.isPresent()) {
					userPermission.setPermission(permission.get());;
					userPermission.setUser(user.get());
					try {
						userPermissionRepository.save(userPermission);
					} catch (Exception e) {
						LOGGER.error(e.getMessage(), e);
		      		}
				}
			}
		}
	}
	
    public ResponseDto<LoginResponseDto> passLogin(LoginRequestDto loginRequestDTO) {
        String email = loginRequestDTO.getEmail();
        // Reload password post-security so we can generate the token
        final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        final String token = jwtTokenUtil.generateToken(userDetails);

        return apiResponse.response(ValueConstant.SUCCESS,ValueConstant.TRUE,LoginResponseDto.builder().token(token).build());
    }
    
    void validateUsernamePassword(String email, String password){
        if (email == null || password == null){
            throw new NullPointerException(Message.USERNAME_OR_PASSWORD_NOT_NULL);
        }

        Users existUser = userRepository.findByEmail(email);
        if (existUser != null){
            throw new DuplicateUserException(Message.EXIST_EMAIL);
        }

    }

	@Override
    public ResponseDto<JwtUser> getUser(HttpServletRequest request) {
        String token = request.getHeader(tokenHeader);
        String email = jwtTokenUtil.getUsernameFromToken(token);
        JwtUser jwtUser = (JwtUser) userDetailsService.loadUserByUsername(email);
        return apiResponse.response(ValueConstant.SUCCESS,ValueConstant.TRUE, jwtUser);
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
	public void removeUserById(Long userId) {
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
		
		List<String> existsRoles = roleRepository.findByNameIn(roleMap.keySet())
				.stream().map(r -> r.getName()).collect(Collectors.toList());
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
		
		Long count = ((Number)queryCount.getSingleResult()).longValue();
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
			
			UserDto dto = UserDto.builder()
	                .id(user.getUserId())
	                .username(user.getUsername())
	                .email(user.getEmail())
	                .fullName(user.getFullName())
	                .firstName(user.getFirstName())
	                .lastName(user.getLastName())
	                .phoneNumber(user.getPhoneNumber())
	                .avatar(user.getAvatar())
	                .fullName(user.getFirstName() + " " + user.getLastName())
	                .status(user.getStatus())
	                .roleDescs(
                		user.getRoles().stream()
                         .map(authority -> {
                        	 roles.add(authority.getRole().getName());
                        	 return SimpleMap.init("name", authority.getRole().getName()).more("desc", authority.getRole().getDesc());
                         })
                         .collect(Collectors.toList())	
            		)
					.build();
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
        for(UserRole userRole : user.getRoles()) {
        	if(StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN")) {
        		for(Permission per : permissionRepository.findAll()) {
        			PermissionDto permiss = new PermissionDto();
        			permiss.setId(per.getId());
        			permiss.setName(per.getName());
        			permiss.setDescription(per.getDescription());
        			permissionsDto.add(permiss);
        		}
        	} else {
	        	Iterator<Map.Entry<String, List<Permission>>> itr = map.entrySet().iterator();
	        	while(itr.hasNext()) {
	        		Map.Entry<String, List<Permission>> it = itr.next();
	        		List<PermissionDto> permissionss = new ArrayList();
	        		for(Permission permission : it.getValue()) {
	        			PermissionDto perDto = new PermissionDto(); 
	        			perDto.setId(permission.getId());
	        			perDto.setName(permission.getName());
	        			perDto.setDescription(permission.getDescription());
	        			if(permissionsDto.isEmpty()) {
	        				permissionss.add(perDto);
	        			} else {
	        				boolean check = false;
	        				for (PermissionDto per : permissionsDto) {
	        					if(per.getId() == perDto.getId()) {
	        						check = true;
	        					}
	        				}
	        				if(check != true) {
	    						permissionss.add(perDto);
	    					}
	        			}
	        		}
	        		if(StringUtils.equals(userRole.getRole().getName(), it.getKey())) {       			
	        			permissionsDto.addAll(permissionss);
	        		}        			
	        	}
	        }
	        List<UserPermission> userPermissions = userPermissionRepository.findAll();
	        for(UserPermission userPer : userPermissions) {
	        	if(userPer.getUser().getUserId() == user.getUserId()) {
	        		List<PermissionDto> permissionss = new ArrayList();
	        		boolean check = false;
					for (PermissionDto per : permissionsDto) {
						if(per.getId() == userPer.getPermission().getId()) {
							check = true;
						}
					}
					if(check != true) {
						PermissionDto PerDto = new PermissionDto();
						PerDto.setId(userPer.getPermission().getId());
						PerDto.setName(userPer.getPermission().getName());
						PerDto.setDescription(userPer.getPermission().getDescription());
						permissionsDto.add(PerDto);
					}
	        	}
	        }
        }
        UserDto dto = UserDto.builder()
                .permissions(permissionsDto)
				.build();
		pagin.getResults().add(dto);
	}
	
	@Override
	public void getRoleOfUser(PaginDto<RoleDto> pagin) {
		
        Map<String, Object> map = pagin.getOptions();

        Object userId = (Object) map.get("userId");
        
        Optional<Users> user = userRepository.findById(Long.parseLong(userId.toString()));

        if(user.isPresent()) {
        	boolean check = false;
        	for(UserRole userRole : user.get().getRoles()) {
            	if(StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN")) {
            		check = true;
            	}
            }
        	if(check == true) {
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
        		
        		Long count = ((Number)queryCount.getSingleResult()).longValue();
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
        			
        			RoleDto dto = RoleDto.builder()
        	                .id(role.getId())
        	                .name(role.getName())
        	                .desc(role.getDesc())
        					.build();
        			pagin.getResults().add(dto);
        		});
        	} else {
        		StringBuilder sqlBuilder = new StringBuilder("FROM UserRole ur");
        		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM UserRole ur");
        		
        		StringBuilder sqlCommonBuilder = new StringBuilder();
        		sqlCommonBuilder.append(" WHERE ur.user.userId = "  + userId);
        		sqlBuilder.append(" WHERE ur.user.userId = "  + userId);
        		sqlCountBuilder.append(sqlCommonBuilder);
        		
        		if (pagin.getOffset() == null || pagin.getOffset() < 0) {
        			pagin.setOffset(0);
        		}
        		
        		if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
        			pagin.setLimit(20);
        		}
        		
        		Query queryCount = em.createQuery(sqlCountBuilder.toString());
        		
        		Long count = ((Number)queryCount.getSingleResult()).longValue();
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
        			
        			RoleDto dto = RoleDto.builder()
        	                .id(userRole.getRole().getId())
        	                .name(userRole.getRole().getName())
        	                .desc(userRole.getRole().getDesc())
        					.build();
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
		sqlCommonBuilder.append(" WHERE ur.user.userId = "  + userId);
		sqlBuilder.append(" WHERE ur.user.userId = "  + userId);
		sqlCountBuilder.append(sqlCommonBuilder);
		
		if (pagin.getOffset() == null || pagin.getOffset() < 0) {
			pagin.setOffset(0);
		}
		
		if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
			pagin.setLimit(20);
		}
		
		Query queryCount = em.createQuery(sqlCountBuilder.toString());
		
		Long count = ((Number)queryCount.getSingleResult()).longValue();
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
			GroupUserDto dto = GroupUserDto.builder()
	                .id(userGroup.getGroupUser().getId())
	                .name(userGroup.getGroupUser().getName())
	                .description(userGroup.getGroupUser().getDescription())
					.build();
			pagin.getResults().add(dto);
		});
       
	}
	
	@Override
	public void getPermissionsEachUser(PaginDto<PermissionDto> pagin) {
		Map<String, Object> map = pagin.getOptions();

        Object userId = (Object) map.get("userId");
        
        Optional<Users> user = userRepository.findById(Long.parseLong(userId.toString()));

        if(user.isPresent()) {
        	boolean check = false;
        	for(UserRole userRole : user.get().getRoles()) {
            	if(StringUtils.equals(userRole.getRole().getName(), "SUPER_ADMIN")) {
            		check = true;
            	}
            }
        	if(check == true) {
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
        		
        		Long count = ((Number)queryCount.getSingleResult()).longValue();
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
        			
        			PermissionDto dto = PermissionDto.builder()
        	                .id(permission.getId())
        	                .name(permission.getName())
        	                .description(permission.getDescription())
        					.build();
        			pagin.getResults().add(dto);
        		});
        	} else {
                StringBuilder sqlBuilder = new StringBuilder("FROM UserPermission up");
        		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM UserPermission up");
        		
        		StringBuilder sqlCommonBuilder = new StringBuilder();
        		sqlCommonBuilder.append(" WHERE up.user.userId = "  + userId);
        		sqlBuilder.append(" WHERE up.user.userId = "  + userId);
        		sqlCountBuilder.append(sqlCommonBuilder);
        		
        		if (pagin.getOffset() == null || pagin.getOffset() < 0) {
        			pagin.setOffset(0);
        		}
        		
        		if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
        			pagin.setLimit(20);
        		}
        		
        		Query queryCount = em.createQuery(sqlCountBuilder.toString());
        		
        		Long count = ((Number)queryCount.getSingleResult()).longValue();
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
        			
        			PermissionDto dto = PermissionDto.builder()
        	                .id(permission.getPermission().getId())
        	                .name(permission.getPermission().getName())
        	                .description(permission.getPermission().getDescription())
        					.build();
        			pagin.getResults().add(dto);
        		});
        	}
        }
	}
}
