package io.strategiz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for the application
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Disable CSRF for API endpoints
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/api/binanceus/raw-data")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/binanceus/admin/raw-data")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/binanceus/admin/debug-keys")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/binanceus/admin/debug-user-document")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/binanceus/public-admin-raw-data")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/binanceus/public-test")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/binanceus/test-api")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/binanceus/debug-connectivity")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/binanceus/sample-data")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/kraken/raw-data")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/health")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/raw-binanceus/**")).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {}) // Use HTTP Basic instead of form login
            .formLogin(formLogin -> formLogin.disable()); // Disable form login completely
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://strategiz.io"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true); // Set to true to allow credentials
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        
        UserDetails user = User.builder()
            .username("user")
            .password(encoder.encode("test"))
            .roles("USER")
            .build();
            
        return new InMemoryUserDetailsManager(user);
    }
}
