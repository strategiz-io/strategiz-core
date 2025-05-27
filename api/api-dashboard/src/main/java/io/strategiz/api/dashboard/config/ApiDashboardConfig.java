package io.strategiz.api.dashboard.config;

import io.strategiz.service.dashboard.config.ServiceDashboardConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for api-dashboard module.
 * Follows the standard naming convention: Api[Module]Config
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.api.dashboard")
@Import({
    ServiceDashboardConfig.class
})
public class ApiDashboardConfig {
    // Configuration can be added here as needed
}
