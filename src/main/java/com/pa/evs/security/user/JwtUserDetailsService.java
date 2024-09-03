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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.RolePermission;
import com.pa.evs.model.Users;
import com.pa.evs.repository.RoleGroupRepository;
import com.pa.evs.repository.RolePermissionRepository;
import com.pa.evs.repository.UserGroupRepository;
import com.pa.evs.repository.UserRepository;
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
        
        Set<Long> allRoleIds = new HashSet<>();
        List<String> roles = user.getRoles().stream().map(r -> {
        	allRoleIds.add(r.getRole().getId());
//        	if ("MMS".equalsIgnoreCase(r.getRole().getAppCode().getName())) {
        		return r.getRole().getName();
//        	}
//        	return r.getRole().getAppCode().getName() + "_" + r.getRole().getName();
        }).collect(Collectors.toList());
        
        List<String> allRls = user.getAllRoles();
        allRls.addAll(roles);
        List<String> groups = userGroupRepository.findByUserUserIdIn(Arrays.asList(user.getUserId()))
        .stream().map(ug -> {
//        	if ("MMS".equalsIgnoreCase(ug.getGroupUser().getAppCode().getName())) {
        		return ug.getGroupUser().getName();
//        	}
//        	return ug.getGroupUser().getAppCode().getName() + "_" + ug.getGroupUser().getName();
        }).collect(Collectors.toList());
        
        if (!groups.isEmpty()) {
        	roleGroupRepository.findByGroupUserNameIn(groups)
        	.forEach(rg -> {
        		
        		allRoleIds.add(rg.getRole().getId());
        		if (!allRls.contains(rg.getRole().getName())) {
//        			if ("MMS".equalsIgnoreCase(rg.getRole().getAppCode().getName())) {
        				allRls.add(rg.getRole().getName());
//                	} else {
//                		allRls.add(rg.getRole().getAppCode().getName() + "_" + rg.getRole().getName());                		
//                	}
        		}
        	});
        }
        
        List<String> allPms = user.getAllPermissions();
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIdIn(allRoleIds);
        for (RolePermission rolePermission : rolePermissions) {
        	if (!allPms.contains(rolePermission.getPermission().getName())) {
//        		if ("MMS".equalsIgnoreCase(rolePermission.getPermission().getAppCode().getName())) {
        			allPms.add(rolePermission.getPermission().getName());
//        		} else {
//        			allPms.add(rolePermission.getPermission().getAppCode().getName() + "_" + rolePermission.getPermission().getName());	
//        		}
        	}
        }
        
        user.getPermissions().forEach(up -> {
        	if (!allPms.contains(up.getPermission().getName())) {
//        		if ("MMS".equalsIgnoreCase(up.getPermission().getAppCode().getName())) {
        			allPms.add(up.getPermission().getName());
//        		} else {
//        			allPms.add(up.getPermission().getAppCode().getName() + "_" + up.getPermission().getName());	
//        		}
        	}
        });
        
        List<String> allProjects = user.getAllProjects();
        user.getProjects().forEach(pt -> allProjects.add(pt.getProject().getName()));

        List<String> allAppCodes = user.getAllAppCodes();
        user.getAppCodes().forEach(ac -> allAppCodes.add(ac.getAppCode().getName()));
        user.setAllAppCodes(new ArrayList<>(new HashSet<>(allAppCodes)));
        return JwtUserFactory.create(user);
    }
}
