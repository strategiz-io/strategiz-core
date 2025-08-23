package io.strategiz.client.schwab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Charles Schwab client.
 */
@Configuration
public class SchwabClientConfig {
    
    @Bean(name = "schwabRestTemplate")
    public RestTemplate schwabRestTemplate() {
        return new RestTemplate();
    }
}