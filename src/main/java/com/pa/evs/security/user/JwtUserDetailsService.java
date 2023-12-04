package com.pa.evs.security.user;

import java.util.Arrays;
import java.util.List;
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
        Users user = userRepository.findByEmail(email);

        if (user == null) {
        	user = userRepository.findByUsername(email);
        }
        
        if (user == null) {
            return null;
        }
        
        List<String> roles = user.getRoles().stream().map(r -> r.getRole().getName()).collect(Collectors.toList());
        
        List<String> allRls = user.getAllRoles();
        allRls.addAll(roles);
        List<String> groups = userGroupRepository.findByUserUserIdIn(Arrays.asList(user.getUserId()))
        .stream().map(ug -> ug.getGroupUser().getName()).collect(Collectors.toList());
        
        if (!groups.isEmpty()) {
        	roleGroupRepository.findByGroupUserNameIn(groups)
        	.forEach(rg -> {
        		if (!allRls.contains(rg.getRole().getName())) {
        			allRls.add(rg.getRole().getName());
        		}
        	});
        }
        
        List<String> allPms = user.getAllPermissions();
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleNameIn(allRls);
        for (RolePermission rolePermission : rolePermissions) {
        	if (!allPms.contains(rolePermission.getPermission().getName())) {
        		allPms.add(rolePermission.getPermission().getName());
        	}
        }
        user.getPermissions().forEach(up -> {
        	if (!allPms.contains(up.getPermission().getName())) {
        		allPms.add(up.getPermission().getName());
        	}
        });
        
        return JwtUserFactory.create(user);
    }
}
