package com.pa.evs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.pa.evs.constant.RestPath;
import com.pa.evs.security.jwt.JwtAuthenticationEntryPoint;
import com.pa.evs.security.jwt.JwtAuthorizationTokenFilter;
import com.pa.evs.security.user.JwtUserDetailsService;

@Configuration
@EnableWebSecurity
class SecurityConfig {

	@Autowired
	private JwtAuthenticationEntryPoint unauthorizedHandler;

	@Autowired
	private JwtUserDetailsService jwtUserDetailsService;

	// Custom JWT based security filter
	@Autowired
	JwtAuthorizationTokenFilter authenticationTokenFilter;

	@Value("${jwt.header}")
	private String tokenHeader;

	@Value("${jwt.route.authentication.path}")
	private String authenticationPath;
	
	HttpSecurity httpSecurity;

	@Bean
	public PasswordEncoder passwordEncoderBean() {
		return new BCryptPasswordEncoder();
	}

	@Order(1)
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		httpSecurity = http;
		http.cors(cm -> cm.disable());
		http.csrf(cm -> cm.disable());
		http.exceptionHandling(a -> {
			a.authenticationEntryPoint(unauthorizedHandler);
		});
		http.sessionManagement(a -> {
			a.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		});
		http.authorizeHttpRequests(authorizeRequests -> {
			authorizeRequests
			.requestMatchers(RestPath.LOGIN).permitAll()
			.requestMatchers(RestPath.LOGIN1).permitAll()
			.requestMatchers("/api//message/**").permitAll()
			.requestMatchers("/v2/api-docs").permitAll()
			.requestMatchers("/v2/api-docs**").permitAll()
			.requestMatchers("/v2/api-docs/**").permitAll()
			.requestMatchers("/api/v2/api-docs").permitAll()
			.requestMatchers("/api/v2/api-docs**").permitAll()
			.requestMatchers("/api/v2/api-docs/**").permitAll()
			.requestMatchers("/swagger-resources/configuration/ui").permitAll()
			.requestMatchers("/swagger-resources/**").permitAll()
			.requestMatchers("/swagger-resources**").permitAll()
			.requestMatchers("/swagger-ui/").permitAll()
			.requestMatchers("/swagger-ui**").permitAll()
			.requestMatchers("/api/link-msn").permitAll()
			.requestMatchers("/api/firm-ware/upload/**").permitAll()
			.requestMatchers("/api/firm-wares**").permitAll()
			.requestMatchers("/api/firm-ware**").permitAll()
			.requestMatchers("/api/device-csr/upload/**").permitAll()
			.requestMatchers("/api/meter/logs").permitAll()
			.requestMatchers("/api//ca-request-logs").permitAll()
			.requestMatchers("/api/device-groups").permitAll()
			.requestMatchers("/api/device-group**").permitAll()
			.requestMatchers("/api/ping").permitAll()
			.requestMatchers("/api/ping**").permitAll()
			.requestMatchers("/api/pis").permitAll()
			.requestMatchers("/api/pis**").permitAll()
			.requestMatchers("/api/pi/log**").permitAll()
			.requestMatchers("/api/logs/**").permitAll()
			.requestMatchers("/api/logs**").permitAll()
			.requestMatchers("/api/report").permitAll()
			.requestMatchers("/api/report**").permitAll()
			.requestMatchers("/api/test-link-msn").permitAll()
            .requestMatchers("/actuator/shutdown").permitAll()
            .requestMatchers("/shutdownContext").permitAll()
            .requestMatchers("/api/file-name/**").permitAll()
            .requestMatchers("/api/download-meter-file/**").permitAll()
            .requestMatchers("/api/settings").permitAll()
            .requestMatchers("/api/buildings").permitAll()
            .requestMatchers("/api/setting/**").permitAll()
            .requestMatchers("/api/getMDTMessage**").permitAll()
            .requestMatchers("/api/address/upload").permitAll()
            .requestMatchers("/api/vendors").permitAll()
            .requestMatchers("/api/address-logs").permitAll()
            .requestMatchers("/api/address-logs").permitAll()
            .requestMatchers("/api/firm-ware/get/**").permitAll()
            .requestMatchers("/api/file/*/*").permitAll()
            .requestMatchers("/api/otp", "/api/user/resetPassword").permitAll()
            .requestMatchers("/api/user/credential-type").permitAll()
            .requestMatchers("/api/submit-meter-commission").permitAll()
            .requestMatchers("/api/last-submitted-meter-commission**").permitAll()
            .requestMatchers("/api/add-device-test/**").permitAll()
            .requestMatchers("/api/user/save").permitAll()
//            .requestMatchers("/api/app_savelog").permitAll()
            .requestMatchers("/api/send-sms").permitAll()
            .requestMatchers("/api/app_getlog").permitAll()
            .requestMatchers("/api/lock/addresses").permitAll()
            .requestMatchers("/api/countries").permitAll()
            .requestMatchers("/api/dms/projects").permitAll()
            .requestMatchers("/api/dms/sites/*").permitAll()
            .requestMatchers("/api/sites").permitAll()
            .requestMatchers("/api/getLocks").permitAll()
            .requestMatchers("/api/dms/getAllApplications").permitAll()
            .requestMatchers("/api/ca-request").permitAll()
            .requestMatchers("/api/couple-decouple-msn/template").permitAll()
            .requestMatchers("/api/meter/template", 
            		"/api/ca-request", 
            		"/api/address-upload-template", 
            		"/api/device-csr/template",
            		"/api/telco-msisdn/template",
            		"/api/upload-dmslock/template",
            		"/api/upload-user/template",
            		"/health").permitAll()
            
            .requestMatchers(HttpMethod.POST, "/api/dms-assigned-locks2**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/lock/*/code2**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/lock/*/save-log").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/dms/project/*/application-guest").permitAll()
            .requestMatchers(HttpMethod.GET, "/google/oauth/get-code").permitAll()
			.anyRequest().authenticated();
		});

		http.addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	@DependsOn("org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration")
	public AuthenticationManager authenticationManager() throws Exception {
		ProviderManager providerManager = (ProviderManager) httpSecurity.getSharedObject(org.springframework.security.authentication.AuthenticationManager.class);
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(jwtUserDetailsService);
		provider.setPasswordEncoder(passwordEncoderBean());
		List<AuthenticationProvider> providers = new ArrayList<>();
		providers.addAll(providerManager.getProviders());
		providers.add(provider);
		return new ProviderManager(providers);
	}
}
