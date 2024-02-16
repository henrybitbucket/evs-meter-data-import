package com.pa.evs.security.user;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.pa.evs.model.Users;

public final class JwtUserFactory {

	private JwtUserFactory() {
    }

    public static JwtUser create(Users user) {

        return JwtUser.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhoneNumber())
                .avatar(user.getAvatar())
                .password(user.getPassword())
                .appCodes(user.getAllAppCodes())
                .authorities(mapToGrantedAuthorities(user.getAllRoles()))
                .permissions(user.getAllPermissions())
                .projects(user.getAllProjects())
                .enabled(true)
                .changePwdRequire(user.getChangePwdRequire())
                .phoneNumber(user.getPhoneNumber())
                .lastPasswordResetDate(new Date())
                .build();

    }
    
    private static List<GrantedAuthority> mapToGrantedAuthorities(List<String> authorities) {
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
