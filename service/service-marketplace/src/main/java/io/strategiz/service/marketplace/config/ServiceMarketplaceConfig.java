package io.strategiz.service.marketplace.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Marketplace Service
 * Configures component scanning for marketplace service and related data packages
 */
@Configuration
@ComponentScan(basePackages = {
    "io.strategiz.service.marketplace",
    "io.strategiz.data.marketplace"
})
public class ServiceMarketplaceConfig {
    // Configuration is handled by annotations
}
