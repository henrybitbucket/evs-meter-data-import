package com.pa.evs;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * 
	<pre>
		<!-- https://mvnrepository.com/artifact/io.springfox/springfox-boot-starter -->
		<dependency>
			<groupId>io.springfox</groupId>
			<artifactId>springfox-boot-starter</artifactId>
			<version>3.0.0</version>
		</dependency>
		<dependency>
			<groupId>io.springfox</groupId>
			<artifactId>springfox-swagger2</artifactId>
			<version>3.0.0</version>
		</dependency>
		<!-- https://mvnrepository.com/artifact/io.springfox/springfox-swagger-ui -->
		<dependency>
			<groupId>io.springfox</groupId>
			<artifactId>springfox-swagger-ui</artifactId>
			<version>3.0.0</version>
		</dependency>
		###
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
		###
		http://localhost:8080/swagger-ui/#/
	</pre>
 *
 */
@Configuration
public class SpringFoxConfig {                                    
    
	@Bean
    public Docket api() { 
        return new Docket(DocumentationType.SWAGGER_2)  
          .securityContexts(Arrays.asList(securityContext()))
          .securitySchemes(Arrays.asList(apiKey()))
          .select()
          .apis(RequestHandlerSelectors.basePackage("com.pa.evs"))              
          .paths(PathSelectors.any())                          
          .build();                                           
    }
    
    
    private ApiKey apiKey() { 
        return new ApiKey("JWT", "Authorization", "header"); 
    }
    
    private SecurityContext securityContext() { 
        return SecurityContext.builder().securityReferences(defaultAuth()).build(); 
    } 

    private List<SecurityReference> defaultAuth() { 
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything"); 
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1]; 
        authorizationScopes[0] = authorizationScope; 
        return Arrays.asList(new SecurityReference("JWT", authorizationScopes)); 
    }
}
