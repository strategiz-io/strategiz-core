package io.strategiz.service.strategy.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the strategy service module
 * This configuration defines component scanning for strategy-related packages.
 * CORS and other web configurations are handled by the global WebConfig in the application module.
 */
@Configuration
@ComponentScan(basePackages = {
    "io.strategiz.service.strategy",
    "io.strategiz.data.strategy"
})
public class ServiceStrategyConfig {
    // Configuration is limited to component scanning
    // CORS and other web configurations are handled by the global WebConfig
}
