package io.strategiz.api.strategy.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the strategy API module
 * This configuration only defines component scanning for strategy-related packages.
 * CORS and other web configurations are handled by the global WebConfig in the application module.
 */
@Configuration
@ComponentScan(basePackages = {
    "io.strategiz.api.strategy",
    "io.strategiz.service.strategy",
    "io.strategiz.data.strategy"
})
public class ApiStrategyConfig {
    // Configuration is now limited to component scanning
    // CORS and other web configurations are handled by the global WebConfig
}
