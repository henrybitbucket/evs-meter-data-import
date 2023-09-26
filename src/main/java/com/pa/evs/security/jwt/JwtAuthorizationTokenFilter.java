package com.pa.evs.security.jwt;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pa.evs.constant.RestPath;
import com.pa.evs.security.user.JwtUser;

import io.jsonwebtoken.ExpiredJwtException;

@Component
public class JwtAuthorizationTokenFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final String tokenHeader;

    public JwtAuthorizationTokenFilter(UserDetailsService userDetailsService, JwtTokenUtil jwtTokenUtil, @Value("${jwt.header}") String tokenHeader) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenHeader = tokenHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authToken = request.getHeader(this.tokenHeader);
        String username = null;
        /*username = "xuejiang.qing@pa.com.sg";
		authToken = "Bearer " + jwtTokenUtil.generateToken(this.userDetailsService.loadUserByUsername(username));
		final String tokenHeaderName = this.tokenHeader;
		final String token = authToken;
		request = new HttpServletRequestWrapper(request) {
			@Override
			public String getHeader(String name) {
				if (name.equals(tokenHeaderName)) {
					return token;
				}
				return super.getHeader(name);
			}
		};*/
		
        if (authToken != null) {
            if (authToken.startsWith("Basic")) {
                chain.doFilter(request, response);
                return;
            }
            try {
            	// m3 module app token
            	if ("Bearer m3a3ec06e6-d8b7-4e85-aa25-0fb5a3e95ee1".equalsIgnoreCase(authToken)) {
            		username = "henry";
            		UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            		authToken = "Bearer " + jwtTokenUtil.generateToken(userDetails);
            	} else {
            		username = jwtTokenUtil.getUsernameFromToken(authToken);	
            	}
                
            } catch (IllegalArgumentException e) {
                logger.error("an error occured during getting username from token", e);
            } catch (ExpiredJwtException e) {
                logger.warn("the token is expired and not valid anymore", e);
            }
        }
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // It is not compelling necessary to load the use details from the database. You could also store the information
            // in the token and read it from it. It's up to you ;)
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            // For simple validation it is completely sufficient to just check the token integrity. You don't have to call
            // the database compellingly. Again it's up to you ;)
            if (jwtTokenUtil.validateToken(authToken, userDetails)) {
            	if (!RestPath.LOGIN.equalsIgnoreCase(request.getRequestURI())
            			&& !RestPath.LOGIN1.equalsIgnoreCase(request.getRequestURI())
            			&& "/api/otp".equalsIgnoreCase(request.getRequestURI())
            			&& "/api/user/changePassword".equalsIgnoreCase(request.getRequestURI())
            			&& !RestPath.WHOAMI.equalsIgnoreCase(request.getRequestURI())) {
            		JwtUser u = (JwtUser) userDetails;
            		if (u.getChangePwdRequire() == Boolean.TRUE) {
            			response.sendRedirect(RestPath.WHOAMI);
            			return;
            		}
            	}
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }
}
