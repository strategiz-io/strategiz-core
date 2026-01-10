package io.strategiz.data.waitlist.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Configuration for the data-waitlist module.
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.data.waitlist")
public class DataWaitlistConfig {
    // Repository implementation is provided by client-firebase module
}
