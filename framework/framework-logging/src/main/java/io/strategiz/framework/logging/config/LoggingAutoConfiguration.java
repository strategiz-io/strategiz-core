package io.strategiz.framework.logging.config;

import io.strategiz.framework.logging.LoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for Strategiz Framework Logging.
 * 
 * This configuration automatically sets up:
 * - Structured JSON logging
 * - Request correlation and tracing
 * - Performance monitoring
 * - Logging context management
 * 
 * The configuration is automatically enabled when the framework-logging
 * dependency is present on the classpath.
 */
@AutoConfiguration
@ComponentScan(basePackages = "io.strategiz.framework.logging")
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnWebApplication
public class LoggingAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingAutoConfiguration.class);
    
    /**
     * Register the logging filter if not already present
     */
    @Bean
    public LoggingFilter loggingFilter() {
        log.info("Initializing Strategiz Framework Logging with structured JSON logging");
        return new LoggingFilter();
    }
} 