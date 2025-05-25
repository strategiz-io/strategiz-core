package io.strategiz.api.auth;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the auth API module
 * This configuration only defines component scanning for auth-related packages.
 * CORS and other web configurations are handled by the global WebConfig in the application module.
 */
@Configuration
@ComponentScan(basePackages = {
    "io.strategiz.api.auth",
    "io.strategiz.service.auth",
    "io.strategiz.data.auth"
})
public class AuthConfig {
    // Configuration is now limited to component scanning
    // CORS and other web configurations are handled by the global WebConfig
}
