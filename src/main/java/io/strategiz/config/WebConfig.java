package io.strategiz.config;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web configuration for CORS and other web-related settings
 */
@Configuration
public class WebConfig {
    
    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);
    
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds

    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;
    
    @Value("${proxy.host:}")
    private String proxyHost;
    
    @Value("${proxy.port:0}")
    private int proxyPort;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins("http://localhost:3000", "http://localhost:3001", "https://strategiz.io", "https://api-strategiz-io.web.app")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("Content-Type", "Authorization", "X-User-Email", "X-Admin-Request", "*")
                    .exposedHeaders("Content-Type", "Authorization", "X-User-Email", "X-Admin-Request")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
    
    /**
     * Creates a RestTemplate bean with appropriate timeout and proxy settings
     * This will be used for making direct HTTP requests to external APIs
     * 
     * @return Configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        // Configure timeouts for the RestTemplate
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT);
        
        HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfigBuilder.build());
        
        // Check system properties for proxy settings
        String systemProxyHost = System.getProperty("http.proxyHost");
        String systemProxyPort = System.getProperty("http.proxyPort");
        
        if (systemProxyHost != null && !systemProxyHost.isEmpty() && systemProxyPort != null && !systemProxyPort.isEmpty()) {
            log.info("RestTemplate using system proxy settings: {}:{}", systemProxyHost, systemProxyPort);
            HttpHost proxy = new HttpHost(systemProxyHost, Integer.parseInt(systemProxyPort));
            HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            clientBuilder.setRoutePlanner(routePlanner);
        } else if (proxyEnabled && proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            log.info("RestTemplate using configured proxy settings: {}:{}", proxyHost, proxyPort);
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            clientBuilder.setRoutePlanner(routePlanner);
        }
        
        HttpClient httpClient = clientBuilder.build();
        
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        
        log.info("RestTemplate initialized with connection timeout: {}ms, read timeout: {}ms", 
                CONNECTION_TIMEOUT, READ_TIMEOUT);
                
        return restTemplate;
    }
    
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
