package io.strategiz.service.auth.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Configuration for authentication error messages.
 * 
 * Configures MessageSource to load error messages from auth-errors.properties
 * for use by the ErrorMessageService.
 */
@Configuration
public class AuthErrorMessageConfig {
    
    /**
     * Configure MessageSource to load auth error messages
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages/auth-errors");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not found
        return messageSource;
    }
}