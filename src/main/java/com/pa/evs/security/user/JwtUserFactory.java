package com.pa.evs.security.user;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.pa.evs.model.UserRole;
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
                .authorities(mapToGrantedAuthorities(user.getRoles()))
                .enabled(true)
                .changePwdRequire(user.getChangePwdRequire())
                .phoneNumber(user.getPhoneNumber())
                .lastPasswordResetDate(new Date())
                .build();

    }
    
    private static List<GrantedAuthority> mapToGrantedAuthorities(List<UserRole> authorities) {
    	System.out.println(authorities);
        return authorities.stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getRole().getName()))
                .collect(Collectors.toList());
    }
}
