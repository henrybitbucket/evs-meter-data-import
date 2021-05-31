package com.pa.evs.security.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

public final class JwtUserFactory {

    private JwtUserFactory() {
    }
    
    @SuppressWarnings("unused")
	static List<GrantedAuthority> mapToGrantedAuthorities(List<?> authorities) {
        return new ArrayList<>();
    }
}
