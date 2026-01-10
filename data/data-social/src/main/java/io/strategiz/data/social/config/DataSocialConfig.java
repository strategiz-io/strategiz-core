package io.strategiz.data.social.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Configuration for the data-social module.
 * Enables component scanning for social data repositories.
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.data.social")
public class DataSocialConfig {
    // Repository implementations are provided within this module
}
