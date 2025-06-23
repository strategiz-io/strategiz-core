package io.strategiz.service.provider.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the Provider Service module.
 */
@Configuration
public class ServiceProviderConfig {

    /**
     * Creates a RestTemplate bean for making HTTP requests to provider APIs.
     * 
     * @param builder RestTemplateBuilder injected by Spring
     * @return Configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(30))
                .build();
    }
}
