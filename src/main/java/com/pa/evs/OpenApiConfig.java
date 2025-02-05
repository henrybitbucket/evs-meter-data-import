package com.pa.evs;

import java.util.Enumeration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pa.evs.TopFilter.HttpServletRequestHolder;
import com.pa.evs.utils.AppProps;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
    	if (HttpServletRequestHolder.get() != null) {
	    	Enumeration<String> ens = HttpServletRequestHolder.get().getHeaderNames();
	    	while (ens.hasMoreElements()) {
				String hd = (String) ens.nextElement();
			}
    	}
        return new OpenAPI().addSecurityItem(new SecurityRequirement().addList("Authentication"))
                .components(new Components().addSecuritySchemes("Authentication", createCustomApiKeyScheme()))
                .addServersItem(new Server().url(AppProps.get("springdoc.server.url", "http://localhost:" + AppProps.get("server.port", "8080"))).description("API Server"))
                .info(new Info().title("REST API")
                        .description("API Services")
                        .version("1.0.0").contact(new Contact().name("PA").email("PA@support.com"))
                        .license(new License().name("© 2024 PA LLC")
                                .url("https://PA.support.com")));
    }

    private SecurityScheme createCustomApiKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");
    }
}