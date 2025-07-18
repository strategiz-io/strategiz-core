package io.strategiz.data.auth.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the data-auth module.
 * Scans for components in the package structure.
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.data.auth")
public class DataAuthConfig {
    // Repository implementations are provided by client-firebase module
}
