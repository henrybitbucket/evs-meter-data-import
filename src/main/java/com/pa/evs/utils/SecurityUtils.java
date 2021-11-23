package com.pa.evs.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pa.evs.security.user.JwtUser;


public final class SecurityUtils {

	private SecurityUtils() {}
	
	public static String getEmail() {
		
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			Object obj = auth.getPrincipal();
			
			if (obj instanceof String) {
				return null;
			}
			
			if (obj instanceof JwtUser) {
				return ((JwtUser)obj).getEmail();
			}
		} catch (Exception e) {
			//
		}
		return null;
	}
}
