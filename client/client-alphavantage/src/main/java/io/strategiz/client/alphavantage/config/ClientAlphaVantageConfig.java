package io.strategiz.client.alphavantage.config;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for AlphaVantage API client following Synapse patterns.
 */
@Configuration
@EnableCaching
public class ClientAlphaVantageConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ClientAlphaVantageConfig.class);
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;
    private static final int CACHE_TTL_SECONDS = 60; // Cache for 1 minute

    /**
     * Creates a specialized RestTemplate for AlphaVantage API with caching and timeouts
     * 
     * @return Configured RestTemplate for AlphaVantage API
     */
    @Bean(name = "alphaVantageRestTemplate")
    public RestTemplate alphaVantageRestTemplate() {
        log.info("Initializing AlphaVantage RestTemplate with timeouts: connect={}ms, read={}ms", 
                CONNECTION_TIMEOUT, READ_TIMEOUT);
        
        // Configure timeouts for the RestTemplate
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
                .build();
        
        HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig);
        
        HttpClient httpClient = clientBuilder.build();
        
        HttpComponentsClientHttpRequestFactory requestFactory = 
                new HttpComponentsClientHttpRequestFactory(httpClient);
        
        return new RestTemplateBuilder()
                .requestFactory(() -> requestFactory)
                .setConnectTimeout(Duration.ofMillis(CONNECTION_TIMEOUT))
                .setReadTimeout(Duration.ofMillis(READ_TIMEOUT))
                .build();
    }
}
