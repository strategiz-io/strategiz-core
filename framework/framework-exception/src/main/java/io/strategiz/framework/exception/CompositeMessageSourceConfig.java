package io.strategiz.framework.exception;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Composite message source configuration that aggregates all error message sources.
 * 
 * This configuration creates a primary MessageSource bean that loads error messages
 * from all service modules, allowing the ErrorMessageService to resolve messages
 * from any module.
 */
@Configuration
public class CompositeMessageSourceConfig {
    
    /**
     * Create a composite MessageSource that loads error messages from all modules.
     * 
     * This is marked as @Primary so it will be the default MessageSource injected
     * into the ErrorMessageService.
     * 
     * To add a new module's error messages:
     * 1. Create a messages/[module]-errors.properties file in the module
     * 2. Add the basename to the array below
     */
    @Bean
    @Primary
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        
        // Add all error message bundles here
        messageSource.setBasenames(
            "messages/auth-errors",          // Authentication service errors
            "messages/dashboard-errors",     // Dashboard service errors
            "messages/provider-errors",      // Provider service errors
            "messages/profile-errors",       // Profile service errors (future)
            "messages/portfolio-errors",     // Portfolio service errors (future)
            "messages/marketplace-errors",   // Marketplace service errors (future)
            "messages/common-errors"         // Common/framework errors (future)
        );
        
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not found
        messageSource.setCacheSeconds(3600); // Cache messages for 1 hour
        
        return messageSource;
    }
}