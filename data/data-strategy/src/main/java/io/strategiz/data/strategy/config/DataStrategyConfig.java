package io.strategiz.data.strategy.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Configuration for data-strategy module.
 * Follows the standard naming convention: Data[Module]Config
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.data.strategy")
public class DataStrategyConfig {
    
    /**
     * Configure MessageSource to load data strategy error messages
     * NOTE: Commented out - using GlobalMessageSourceConfig which includes all module messages
     */
    // @Bean
    // public MessageSource messageSource() {
    //     ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    //     messageSource.setBasenames("messages/data-strategy-errors");
    //     messageSource.setDefaultEncoding("UTF-8");
    //     messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not found
    //     return messageSource;
    // }
}