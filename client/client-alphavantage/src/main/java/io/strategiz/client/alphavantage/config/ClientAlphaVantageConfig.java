package io.strategiz.client.alphavantage.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;



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
        
        // Configure timeouts for the RestTemplate using the modern API
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
                .build();
        
        HttpClientBuilder clientBuilder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig);
        
        CloseableHttpClient httpClient = clientBuilder.build();
        
        // Create request factory with HttpClient 5.x
        HttpComponentsClientHttpRequestFactory requestFactory = 
                new HttpComponentsClientHttpRequestFactory(httpClient);
        
        // Create RestTemplate directly instead of using deprecated builder methods
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        
        log.info("AlphaVantage RestTemplate created with custom request factory");
        return restTemplate;
    }
}
