package io.strategiz.application.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration for Spring Boot Actuator and Prometheus metrics
 */
@Configuration
public class ActuatorConfig {

    /**
     * Customizes the meter registry with application information
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
        return registry -> registry.config()
                .commonTags("application", "strategiz-core")
                .commonTags("environment", environment.getActiveProfiles().length > 0 ? 
                        environment.getActiveProfiles()[0] : "default");
    }
}
