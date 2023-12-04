package com.pa.evs.security.user;

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
import com.pa.evs.repository.RolePermissionRepository;
import com.pa.evs.repository.UserRepository;

@Service
public class JwtUserDetailsService implements UserDetailsService {


    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    
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
        
        try {
            List<String> allPms = user.getAllPermissions();
            List<String> roles = user.getRoles().stream().map(r -> r.getRole().getName()).collect(Collectors.toList());
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleNameIn(roles);
            for (RolePermission rolePermission : rolePermissions) {
            	allPms.add(rolePermission.getPermission().getName());
            }
            user.getPermissions().forEach(up -> allPms.add(up.getPermission().getName()));
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        return JwtUserFactory.create(user);
    }
}
