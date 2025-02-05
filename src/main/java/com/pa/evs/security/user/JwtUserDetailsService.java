package com.pa.evs.security.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Permission;
import com.pa.evs.model.Users;
import com.pa.evs.repository.RoleGroupRepository;
import com.pa.evs.repository.RolePermissionRepository;
import com.pa.evs.repository.UserGroupRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.SecurityUtils;

@Service
public class JwtUserDetailsService implements UserDetailsService {


    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    
    @Autowired
    private UserGroupRepository userGroupRepository;
    
    @Autowired
    private RoleGroupRepository roleGroupRepository;
    
    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    	
    	if (SecurityUtils.getByPassUser() != null) {
    		return SecurityUtils.getByPassUser();
    	}
    	
        Users user = userRepository.findByEmail(email);

        if (user == null) {
        	user = userRepository.findByUsername(email);
        }
        
        if (user == null) {
        	user = userRepository.findByPhoneNumber(email);
        }
        
        if (user == null) {
        	List<Users> us = userRepository.findByLcPhoneNumber(email);
        	if (us.size() == 1) {
        		user = us.get(0);
        	}
        }
        
        if (user == null) {
            return null;
        }
        // AppProps.getContext().getBean(PasswordEncoder.class).matches("P0wer!2", user.getPassword());
        
        Set<Long> allRoleIds = new HashSet<>();
        List<String> roles = userRepository.findRolesByUserId(user.getUserId()).stream().map(r -> {
        	allRoleIds.add(r.getId());
//        	if ("MMS".equalsIgnoreCase(r.getRole().getAppCode().getName())) {
        		return r.getName();
//        	}
//        	return r.getRole().getAppCode().getName() + "_" + r.getRole().getName();
        }).collect(Collectors.toList());
        
        List<String> allRls = user.getAllRoles();
        allRls.addAll(roles);
        List<String> groups = userGroupRepository.findGroupUserByUserIdIn(Arrays.asList(user.getUserId()))
        .stream().map(ug -> {
//        	if ("MMS".equalsIgnoreCase(ug.getGroupUser().getAppCode().getName())) {
        		return ug.getName();
//        	}
//        	return ug.getGroupUser().getAppCode().getName() + "_" + ug.getGroupUser().getName();
        }).collect(Collectors.toList());
        
        if (!groups.isEmpty()) {
        	roleGroupRepository.findRoleByGroupUserNameIn(groups)
        	.forEach(rg -> {
        		
        		allRoleIds.add(rg.getId());
        		if (!allRls.contains(rg.getName())) {
//        			if ("MMS".equalsIgnoreCase(rg.getRole().getAppCode().getName())) {
        				allRls.add(rg.getName());
//                	} else {
//                		allRls.add(rg.getRole().getAppCode().getName() + "_" + rg.getRole().getName());                		
//                	}
        		}
        	});
        }
        
        List<String> allPms = user.getAllPermissions();
        List<Permission> permissions = rolePermissionRepository.findPermissionByRoleIdIn(allRoleIds);
        for (Permission permission : permissions) {
        	if (!allPms.contains(permission.getName())) {
//        		if ("MMS".equalsIgnoreCase(rolePermission.getPermission().getAppCode().getName())) {
        			allPms.add(permission.getName());
//        		} else {
//        			allPms.add(rolePermission.getPermission().getAppCode().getName() + "_" + rolePermission.getPermission().getName());	
//        		}
        	}
        }
        
        userRepository.findPermissionsByUserId(user.getUserId()).forEach(up -> {
        	if (!allPms.contains(up.getName())) {
//        		if ("MMS".equalsIgnoreCase(up.getPermission().getAppCode().getName())) {
        			allPms.add(up.getName());
//        		} else {
//        			allPms.add(up.getPermission().getAppCode().getName() + "_" + up.getPermission().getName());	
//        		}
        	}
        });
        
        List<String> allProjects = user.getAllProjects();
        userRepository.findProjectsByUserId(user.getUserId()).forEach(pt -> allProjects.add(pt.getName()));

        List<String> allAppCodes = user.getAllAppCodes();
        userRepository.findAppCodesByUserId(user.getUserId()).forEach(ac -> allAppCodes.add(ac.getName()));
        user.setAllAppCodes(new ArrayList<>(new HashSet<>(allAppCodes)));
        return JwtUserFactory.create(user);
    }
}
