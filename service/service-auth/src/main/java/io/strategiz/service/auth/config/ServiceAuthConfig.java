package io.strategiz.service.auth.config;

import io.strategiz.data.auth.config.DataAuthConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for service-auth module.
 * Imports data module configurations to ensure repositories and entities are available.
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.service.auth")
@Import({DataAuthConfig.class})
public class ServiceAuthConfig {
    // Service configuration goes here
}
