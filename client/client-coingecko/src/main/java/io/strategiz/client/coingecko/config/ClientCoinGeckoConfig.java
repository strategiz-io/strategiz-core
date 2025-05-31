package io.strategiz.client.coingecko.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for CoinGecko API client following Synapse patterns.
 */
@Configuration
@EnableCaching
public class ClientCoinGeckoConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ClientCoinGeckoConfig.class);
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;
    private static final int CACHE_TTL_SECONDS = 60; // Cache for 1 minute

    /**
     * Creates a specialized RestTemplate for CoinGecko API with caching and timeouts
     * 
     * @return Configured RestTemplate for CoinGecko API
     */
    @Bean(name = "coinGeckoRestTemplate")
    public RestTemplate coinGeckoRestTemplate() {
        log.info("Initializing CoinGecko RestTemplate with timeouts: connect={}ms, read={}ms", 
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
        
        HttpComponentsClientHttpRequestFactory requestFactory = 
                new HttpComponentsClientHttpRequestFactory(httpClient);
        
        return new RestTemplateBuilder()
                .requestFactory(() -> requestFactory)
                .setConnectTimeout(Duration.ofMillis(CONNECTION_TIMEOUT))
                .setReadTimeout(Duration.ofMillis(READ_TIMEOUT))
                .build();
    }
}
