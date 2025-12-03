package io.strategiz.data.marketdata.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for data-marketdata module.
 * Follows the standard naming convention: Data[Module]Config
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.data.marketdata")
public class DataMarketDataConfig {

    /**
     * Configure MessageSource to load data marketdata error messages
     * NOTE: Commented out - using GlobalMessageSourceConfig which includes all module messages
     */
    // @Bean
    // public MessageSource messageSource() {
    //     ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    //     messageSource.setBasenames("messages/data-marketdata-errors");
    //     messageSource.setDefaultEncoding("UTF-8");
    //     messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not found
    //     return messageSource;
    // }
}
