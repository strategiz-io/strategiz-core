package io.strategiz.service.base.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the application All endpoints are permitted for debugging
 * purposes
 */
@Configuration("serviceSecurityConfig")
@EnableWebSecurity
public class SecurityConfig {

	// Using specific bean name to avoid conflict with api-base SecurityConfig

	@Bean
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.disable()) // Disable Spring Security CORS - we use custom
											// CorsFilter
			.csrf(csrf -> csrf.disable()) // Disable CSRF for API endpoints
			.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll() // Allow
																					// all
																					// requests
																					// without
																					// authentication
			)
			.httpBasic(httpBasic -> httpBasic.disable())
			.formLogin(formLogin -> formLogin.disable()); // Disable form login completely

		return http.build();
	}

}
