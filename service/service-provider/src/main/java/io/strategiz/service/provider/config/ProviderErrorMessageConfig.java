package io.strategiz.service.provider.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Configuration for provider error messages.
 * 
 * Configures MessageSource to load error messages from provider-errors.properties
 * for use by the ErrorMessageService.
 */
@Configuration
public class ProviderErrorMessageConfig {
    
    /**
     * Configure MessageSource to load provider error messages
     * 
     * Note: This creates a provider-specific MessageSource bean.
     * In a production environment, you might want to merge all
     * error message sources into a single MessageSource.
     */
    @Bean(name = "providerMessageSource")
    public MessageSource providerMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages/provider-errors");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not found
        return messageSource;
    }
}