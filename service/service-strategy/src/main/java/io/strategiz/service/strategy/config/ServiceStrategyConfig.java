package io.strategiz.service.strategy.config;

import io.strategiz.business.strategy.execution.config.BusinessStrategyExecutionConfig;
import io.strategiz.data.strategy.config.DataStrategyConfig;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Service Strategy Configuration
 * 
 * Configures MessageSource to load error messages from service-strategy-errors.properties
 * for use by the ErrorMessageService.
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.service.strategy")
@Import({DataStrategyConfig.class, BusinessStrategyExecutionConfig.class})
public class ServiceStrategyConfig {
    
    /**
     * Configure MessageSource to load strategy error messages
     * NOTE: Commented out - using GlobalMessageSourceConfig which includes all module messages
     */
    // @Bean
    // public MessageSource messageSource() {
    //     ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    //     messageSource.setBasenames("messages/service-strategy-errors");
    //     messageSource.setDefaultEncoding("UTF-8");
    //     messageSource.setUseCodeAsDefaultMessage(true); // Fallback to code if message not found
    //     return messageSource;
    // }
}