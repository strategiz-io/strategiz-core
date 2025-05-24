package io.strategiz.api.auth;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for the auth API module
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
    "io.strategiz.api.auth",
    "io.strategiz.service.auth",
    "io.strategiz.data.auth"
})
public class AuthConfig implements WebMvcConfigurer {

    /**
     * Configure CORS for auth endpoints
     * 
     * @param registry CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/auth/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:8081", "https://strategiz.io")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
