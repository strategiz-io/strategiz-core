package io.strategiz.client.polygon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for Polygon.io API client
 * Handles API key, base URL, and HTTP client setup
 */
@Configuration
public class PolygonClientConfig {
    
    @Value("${polygon.api.key:}")
    private String apiKey;
    
    @Value("${polygon.api.base-url:https://api.polygon.io}")
    private String baseUrl;
    
    @Value("${polygon.api.timeout.connect:5000}")
    private int connectTimeout;
    
    @Value("${polygon.api.timeout.read:30000}")
    private int readTimeout;
    
    /**
     * Create RestTemplate specifically configured for Polygon API
     */
    @Bean(name = "polygonRestTemplate")
    public RestTemplate polygonRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        
        return builder
            .rootUri(baseUrl)
            .setConnectTimeout(Duration.ofMillis(connectTimeout))
            .setReadTimeout(Duration.ofMillis(readTimeout))
            .requestFactory(() -> factory)
            .build();
    }
    
    /**
     * Get the configured API key
     */
    public String getApiKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Polygon API key is not configured. " +
                "Please set polygon.api.key in application.properties or as an environment variable.");
        }
        return apiKey;
    }
    
    /**
     * Get the base URL for Polygon API
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}