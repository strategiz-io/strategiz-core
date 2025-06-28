package io.strategiz.service.portfolio.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the portfolio service module
 * This configuration handles component scanning for portfolio-related packages.
 */
@Configuration
@ComponentScan(basePackages = {
    "io.strategiz.service.portfolio",
    "io.strategiz.data.portfolio"
})
public class ServicePortfolioConfig {
    // Configuration is limited to component scanning
    // CORS and other web configurations are handled by the global WebConfig
}
