package io.strategiz.service.dashboard.config;

import io.strategiz.business.portfolio.config.BusinessPortfolioConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for service-dashboard module.
 * Follows the standard naming convention: Service[Module]Config
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.service.dashboard")
@Import({
    BusinessPortfolioConfig.class
})
public class ServiceDashboardConfig {
    // Configuration can be added here as needed
}
