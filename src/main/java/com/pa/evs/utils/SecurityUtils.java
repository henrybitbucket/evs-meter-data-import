package com.pa.evs.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pa.evs.model.Users;
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
	
	public static String getUsername() {
		
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			Object obj = auth.getPrincipal();
			
			if (obj instanceof String) {
				return null;
			}
			
			if (obj instanceof JwtUser) {
				return ((JwtUser)obj).getUsername();
			}
		} catch (Exception e) {
			//
		}
		return null;
	}
	
	public static boolean hasAnyRole(Users user, String... roles) {
		
		try {
			Set<String> rs = Arrays.stream(roles).map(r -> r.trim()).collect(Collectors.toSet());
			return user.getRoles()
					.stream()
					.map(ur -> ur.getRole().getName())
					.anyMatch(rs::contains)
					||
					user.getPermissions()
					.stream()
					.map(ur -> ur.getPermission().getName())
					.anyMatch(rs::contains)
					;
			
		} catch (Exception e) {
			//
		}
		return false;
	}
}
