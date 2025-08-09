package io.strategiz.service.dashboard.config;

import io.strategiz.business.portfolio.config.BusinessPortfolioConfig;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Configuration class for service-dashboard module.
 * Follows the standard naming convention: Service[Module]Config
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.service.dashboard")
@Import({
    BusinessPortfolioConfig.class
})
public class ServiceDashboardConfig {
    
    /**
     * Configure MessageSource to load dashboard error messages
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages/dashboard-errors");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not found
        return messageSource;
    }
}
