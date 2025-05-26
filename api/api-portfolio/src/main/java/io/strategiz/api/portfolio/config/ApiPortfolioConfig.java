package io.strategiz.api.portfolio.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the portfolio API module
 * This configuration only defines component scanning for portfolio-related packages.
 * CORS and other web configurations are handled by the global WebConfig in the application module.
 */
@Configuration
@ComponentScan(basePackages = {
    "io.strategiz.api.portfolio",
    "io.strategiz.service.portfolio",
    "io.strategiz.data.portfolio"
})
public class ApiPortfolioConfig {
    // Configuration is now limited to component scanning
    // CORS and other web configurations are handled by the global WebConfig
}
