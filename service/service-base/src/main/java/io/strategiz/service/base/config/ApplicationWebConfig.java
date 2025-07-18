package io.strategiz.service.base.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global web configuration for the Strategiz application
 * This configuration applies to all endpoints across the application
 */
@Configuration
public class ApplicationWebConfig implements WebMvcConfigurer {

    @Value("${strategiz.cors.allowed-origins:http://localhost:3000,http://localhost:8080,https://strategiz.io}")
    private String[] allowedOrigins;

    /**
     * Configure CORS globally for all endpoints
     * 
     * @param registry CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:[*]", "https://*.strategiz.io", "https://strategiz.io", "https://*.web.app", "https://*.firebaseapp.com", "https://*.run.app")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * Configure resource handlers to serve static resources
     * 
     * @param registry ResourceHandlerRegistry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Explicitly define static resource paths to avoid conflicting with API endpoints
        // Only map specific static resource paths - NEVER use /** pattern that could intercept API requests
        registry.addResourceHandler(
                "/static/**", 
                "/assets/**", 
                "/css/**", 
                "/js/**", 
                "/images/**", 
                "/favicon.ico")
                .addResourceLocations("classpath:/static/");
                
        // Handle root path requests for SPA frontend
        registry.addResourceHandler("/", "/index.html")
                .addResourceLocations("classpath:/static/index.html");
    }
}
