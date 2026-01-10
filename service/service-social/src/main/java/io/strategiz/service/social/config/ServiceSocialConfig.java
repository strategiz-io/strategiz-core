package io.strategiz.service.social.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Social Service.
 * Configures component scanning for social service and related data packages.
 */
@Configuration
@ComponentScan(basePackages = {
    "io.strategiz.service.social",
    "io.strategiz.data.social"
})
public class ServiceSocialConfig {
    // Configuration is handled by annotations
}
