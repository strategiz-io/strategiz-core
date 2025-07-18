package io.strategiz.data.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Configuration for the data-user module.
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.data.user")
public class DataUserConfig {
    // Repository implementations are provided by client-firebase module
} 