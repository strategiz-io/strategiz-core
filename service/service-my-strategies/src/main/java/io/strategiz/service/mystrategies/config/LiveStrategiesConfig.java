package io.strategiz.service.mystrategies.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for Live Strategies service.
 * Enables scheduling for alert monitoring.
 */
@Configuration("myStrategiesSchedulingConfig")
@EnableScheduling
public class LiveStrategiesConfig {
    // Spring will scan and register @Scheduled methods
}
