package com.pa.evs;

import com.pa.evs.constant.RestPath;
import com.pa.evs.security.jwt.JwtAuthenticationEntryPoint;
import com.pa.evs.security.jwt.JwtAuthorizationTokenFilter;
import com.pa.evs.security.user.JwtUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

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

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(jwtUserDetailsService).passwordEncoder(passwordEncoderBean());
	}

	@Bean
	public PasswordEncoder passwordEncoderBean() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure(HttpSecurity httpSecurity) throws Exception {
		httpSecurity
				// we don't need CSRF because our token is invulnerable
				.cors().and().csrf().disable()

				.exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()

				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()

				.authorizeRequests()
				.antMatchers(RestPath.LOGIN).permitAll()
				.antMatchers(RestPath.LOGIN1).permitAll()
				.antMatchers("/api//message/**").permitAll()
				.antMatchers("/v2/api-docs").permitAll()
				.antMatchers("/v2/api-docs**").permitAll()
				.antMatchers("/v2/api-docs/**").permitAll()
				.antMatchers("/api/v2/api-docs").permitAll()
				.antMatchers("/api/v2/api-docs**").permitAll()
				.antMatchers("/api/v2/api-docs/**").permitAll()
				.antMatchers("/swagger-resources/configuration/ui").permitAll()
				.antMatchers("/swagger-resources/**").permitAll()
				.antMatchers("/swagger-resources**").permitAll()
				.antMatchers("/swagger-ui/").permitAll()
				.antMatchers("/swagger-ui**").permitAll()
				.antMatchers("/api/link-msn").permitAll()
				.antMatchers("/api/firm-ware/upload/**").permitAll()
				.antMatchers("/api/firm-wares**").permitAll()
				.antMatchers("/api/firm-ware**").permitAll()
				.antMatchers("/api/device-csr/upload/**").permitAll()
				.antMatchers("/api/meter/logs").permitAll()
				.antMatchers("/api//ca-request-logs").permitAll()
				.antMatchers("/api/device-groups").permitAll()
				.antMatchers("/api/device-group**").permitAll()
				.antMatchers("/api/ping").permitAll()
				.antMatchers("/api/ping**").permitAll()
				.antMatchers("/api/pis").permitAll()
				.antMatchers("/api/pis**").permitAll()
				.antMatchers("/api/pi/log**").permitAll()
				.antMatchers("/api/logs/**").permitAll()
				.antMatchers("/api/logs**").permitAll()
				.antMatchers("/api/report").permitAll()
				.antMatchers("/api/report**").permitAll()
				.antMatchers("/api/test-link-msn").permitAll()
	            .antMatchers("/actuator/shutdown").permitAll()
	            .antMatchers("/shutdownContext").permitAll()
	            .antMatchers("/api/file-name/**").permitAll()
	            .antMatchers("/api/download-meter-file/**").permitAll()
	            .antMatchers("/api/settings").permitAll()
	            .antMatchers("/api/buildings").permitAll()
	            .antMatchers("/api/setting/**").permitAll()
	            .antMatchers("/api/getMDTMessage**").permitAll()
	            .antMatchers("/api/address/upload").permitAll()
	            .antMatchers("/api/vendors").permitAll()
	            .antMatchers("/api/address-logs").permitAll()
	            .antMatchers("/api/address-logs").permitAll()
	            .antMatchers("/api/firm-ware/get/**").permitAll()
	            .antMatchers("/api/file/*/*").permitAll()
	            .antMatchers("/api/otp", "/api/user/resetPassword").permitAll()
	            .antMatchers("/api/user/credential-type").permitAll()
	            .antMatchers("/api/submit-meter-commission").permitAll()
	            .antMatchers("/api/last-submitted-meter-commission**").permitAll()
	            .antMatchers("/api/add-device-test/**").permitAll()
	            .antMatchers("/api/user/save").permitAll()
//	            .antMatchers("/api/app_savelog").permitAll()
	            .antMatchers("/api/send-sms").permitAll()
	            .antMatchers("/api/app_getlog").permitAll()
	            .antMatchers("/api/lock/addresses").permitAll()
	            
	            .antMatchers("/api/countries").permitAll()
	            .antMatchers("/api/dms/projects").permitAll()
	            .antMatchers("/api/dms/sites/*").permitAll()
	            .antMatchers("/api/sites").permitAll()
	            .antMatchers("/api/getLocks").permitAll()
	            .antMatchers("/api/dms/getAllApplications").permitAll()
	            .antMatchers("/api/couple-decouple-msn/template").permitAll()
	            .antMatchers("/api/meter/template").permitAll()
	            
	            
	            .antMatchers(HttpMethod.POST, "/api/dms-assigned-locks2**").permitAll()
	            .antMatchers(HttpMethod.POST, "/api/lock/*/code2**").permitAll()
	            .antMatchers(HttpMethod.POST, "/api/lock/*/save-log").permitAll()
	            .antMatchers(HttpMethod.POST, "/api/dms/project/*/application-guest").permitAll()
	            
				.anyRequest().authenticated();

		httpSecurity.addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

		// disable page caching
		httpSecurity.headers().frameOptions().sameOrigin() // required to set for H2 else H2 Console will be blank.
				.cacheControl();
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
//        // AuthenticationTokenFilter will ignore the below paths
//        web
//            .ignoring()
//            .antMatchers(
//                HttpMethod.POST,
//                authenticationPath
//            )
//
//            // allow anonymous resource requests
//            .and()
//            .ignoring()
//            .antMatchers(
//                HttpMethod.GET,
//                "/",
//                "/*.html",
//                "/favicon.ico",
//                "/**/*.html",
//                "/**/*.css",
//                "/**/*.js"
//            )
//
//            // Un-secure H2 Database (for testing purposes, H2 console shouldn't be unprotected in production)
//            .and()
//            .ignoring()
//            .antMatchers("/h2-console/**/**");
		web.ignoring().antMatchers(HttpMethod.GET, "/", "/webjars/**", "/*.html", "/favicon.ico", "/**/*.html",
				"/**/*.css", "/**/*.js", "/fonts/**");
		web.ignoring().antMatchers("/v2/api-docs", "/configuration/ui", "/swagger-resources/**", "/configuration/**",
				"/swagger-ui.html", "/webjars/**");
	}
}
