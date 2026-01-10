package io.strategiz.service.social.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import io.strategiz.data.social.config.DataSocialConfig;

/**
 * Configuration for the Social Service.
 * Imports DataSocialConfig for repository beans.
 */
@Configuration
@Import({DataSocialConfig.class})
@ComponentScan(basePackages = "io.strategiz.service.social")
public class ServiceSocialConfig {
    // Configuration is handled by annotations
}
