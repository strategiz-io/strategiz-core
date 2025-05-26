package io.strategiz.client.binanceus.config;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Binance US API client
 */
@Configuration
public class ClientBinanceUSConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ClientBinanceUSConfig.class);
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    /**
     * Creates a specialized RestTemplate for Binance US API
     * This template has specific configurations to bypass potential proxy issues
     * 
     * @return Configured RestTemplate for Binance US API
     */
    @Bean(name = "binanceRestTemplate")
    public RestTemplate binanceRestTemplate() {
        // Configure timeouts for the RestTemplate
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
                .setRedirectsEnabled(false) // Disable redirects to avoid login pages
                .setCircularRedirectsAllowed(false)
                .setAuthenticationEnabled(false); // Disable authentication
        
        HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfigBuilder.build())
                .disableRedirectHandling() // Explicitly disable redirect handling
                .disableAutomaticRetries();
        
        // Explicitly disable proxy for Binance US API
        clientBuilder.setRoutePlanner(null);
        
        HttpClient httpClient = clientBuilder.build();
        
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate binanceRestTemplate = new RestTemplate(requestFactory);
        
        log.info("Binance RestTemplate initialized with connection timeout: {}ms, read timeout: {}ms, redirects disabled", 
                CONNECTION_TIMEOUT, READ_TIMEOUT);
                
        return binanceRestTemplate;
    }
}
