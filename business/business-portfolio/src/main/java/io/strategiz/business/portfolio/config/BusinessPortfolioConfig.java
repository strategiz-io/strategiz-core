package io.strategiz.business.portfolio.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for business-portfolio module.
 * Follows the standard naming convention: Business[Module]Config
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.business.portfolio")
public class BusinessPortfolioConfig {
    // Configuration can be added here as needed
}
