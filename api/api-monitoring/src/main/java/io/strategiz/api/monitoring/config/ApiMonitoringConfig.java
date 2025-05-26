package io.strategiz.api.monitoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for the monitoring API module
 * This configuration defines component scanning for monitoring-related packages
 * and provides beans required for monitoring functionality.
 */
@Configuration
@ComponentScan(basePackages = {
    "io.strategiz.api.monitoring"
})
public class ApiMonitoringConfig {
    
    /**
     * Creates a RestTemplate bean for making HTTP requests to other services
     * 
     * @return RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
