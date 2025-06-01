package io.strategiz.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for Strategiz Core
 * Handles static resource mapping for documentation and UI resources
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /docs/** URL path to the physical location of the docs directory
        registry.addResourceHandler("/docs/**")
                .addResourceLocations("file:docs/");
                
        // Add other resource handlers as needed
    }
}
