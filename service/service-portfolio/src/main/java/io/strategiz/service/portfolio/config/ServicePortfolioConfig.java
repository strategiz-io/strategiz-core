package io.strategiz.service.portfolio.config;

import io.strategiz.business.portfolio.config.BusinessPortfolioConfig;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Configuration class for the Portfolio Service module.
 * Follows Single Responsibility Principle - only handles module configuration.
 */
@Configuration
@Import(BusinessPortfolioConfig.class)  // Import business portfolio config which includes enhancer components
public class ServicePortfolioConfig {

    /**
     * Configure message source for error messages
     */
    @Bean
    public MessageSource portfolioMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages/service-portfolio-errors");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
    
    /**
     * Portfolio service configuration properties
     */
    @Bean
    public PortfolioProperties portfolioProperties() {
        return new PortfolioProperties();
    }
    
    /**
     * Configuration properties class
     */
    public static class PortfolioProperties {
        private int maxProvidersPerUser = 10;
        private int topHoldingsLimit = 10;
        private int cacheTimeoutMinutes = 5;
        private boolean enableRealTimeUpdates = false;
        
        // Getters and setters
        public int getMaxProvidersPerUser() {
            return maxProvidersPerUser;
        }
        
        public void setMaxProvidersPerUser(int maxProvidersPerUser) {
            this.maxProvidersPerUser = maxProvidersPerUser;
        }
        
        public int getTopHoldingsLimit() {
            return topHoldingsLimit;
        }
        
        public void setTopHoldingsLimit(int topHoldingsLimit) {
            this.topHoldingsLimit = topHoldingsLimit;
        }
        
        public int getCacheTimeoutMinutes() {
            return cacheTimeoutMinutes;
        }
        
        public void setCacheTimeoutMinutes(int cacheTimeoutMinutes) {
            this.cacheTimeoutMinutes = cacheTimeoutMinutes;
        }
        
        public boolean isEnableRealTimeUpdates() {
            return enableRealTimeUpdates;
        }
        
        public void setEnableRealTimeUpdates(boolean enableRealTimeUpdates) {
            this.enableRealTimeUpdates = enableRealTimeUpdates;
        }
    }
}