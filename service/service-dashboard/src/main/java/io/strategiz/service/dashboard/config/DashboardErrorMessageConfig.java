package io.strategiz.service.dashboard.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Configuration for dashboard error messages.
 * 
 * Configures MessageSource to load error messages from dashboard-errors.properties
 * for use by the ErrorMessageService.
 */
@Configuration
public class DashboardErrorMessageConfig {
    
    /**
     * Configure MessageSource to load dashboard error messages
     * 
     * Note: This creates a dashboard-specific MessageSource bean.
     * In a production environment, you might want to merge all
     * error message sources into a single MessageSource.
     */
    @Bean(name = "dashboardMessageSource")
    public MessageSource dashboardMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages/dashboard-errors");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not found
        return messageSource;
    }
}