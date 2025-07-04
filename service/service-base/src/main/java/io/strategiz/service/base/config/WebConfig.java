package io.strategiz.service.base.config;

import io.strategiz.service.base.web.StandardHeadersInterceptor;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web configuration for CORS, interceptors, and other web-related settings
 */
@Configuration("serviceWebConfig")
public class WebConfig implements WebMvcConfigurer {
    // Using specific bean name to avoid conflict with api-base WebConfig
    
    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);
    
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds

    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;
    
    @Value("${proxy.host:}")
    private String proxyHost;
    
    @Value("${proxy.port:0}")
    private int proxyPort;
    
    private final StandardHeadersInterceptor standardHeadersInterceptor;
    
    @Autowired
    public WebConfig(StandardHeadersInterceptor standardHeadersInterceptor) {
        this.standardHeadersInterceptor = standardHeadersInterceptor;
    }


    
    /**
     * Creates a RestTemplate bean with appropriate timeout and proxy settings
     * This will be used for making direct HTTP requests to external APIs
     * 
     * @return Configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        try {
            // Configure timeouts for the RestTemplate using newer API style
            // Updated to use Timeout.of() instead of deprecated methods
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
                    .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT))
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
                    .build();
            
            // Use HttpClients factory method instead of HttpClientBuilder
            HttpClientBuilder clientBuilder = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig);
            
            // Check system properties for proxy settings
            String systemProxyHost = System.getProperty("http.proxyHost");
            String systemProxyPort = System.getProperty("http.proxyPort");
            
            try {
                if (systemProxyHost != null && !systemProxyHost.isEmpty() && systemProxyPort != null && !systemProxyPort.isEmpty()) {
                    log.info("RestTemplate using system proxy settings: {}:{}", systemProxyHost, systemProxyPort);
                    HttpHost proxy = HttpHost.create(systemProxyHost + ":" + systemProxyPort);
                    HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                    clientBuilder.setRoutePlanner(routePlanner);
                } else if (proxyEnabled && proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
                    log.info("RestTemplate using configured proxy settings: {}:{}", proxyHost, proxyPort);
                    HttpHost proxy = HttpHost.create(proxyHost + ":" + proxyPort);
                    HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                    clientBuilder.setRoutePlanner(routePlanner);
                }
            } catch (Exception e) {
                log.error("Error configuring proxy settings: {}", e.getMessage());
            }
            
            CloseableHttpClient httpClient = clientBuilder.build();
            
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            
            log.info("RestTemplate initialized with connection timeout: {}ms, read timeout: {}ms", 
                    CONNECTION_TIMEOUT, READ_TIMEOUT);
                    
            return restTemplate;
        } catch (Exception e) {
            log.error("Error creating RestTemplate: {}", e.getMessage());
            throw new RuntimeException("Failed to create RestTemplate", e);
        }
    }
    
    // Note: The binanceRestTemplate has been removed during consolidation
    // as it represents domain-specific functionality that should not be in the base module.
    // This should be moved to a more appropriate module for financial integrations.
    
    /**
     * Register interceptors for standard headers
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register standard headers interceptor for ALL requests
        registry.addInterceptor(standardHeadersInterceptor)
                .addPathPatterns("/**")  // Apply to all paths
                .excludePathPatterns(
                    "/health",           // Health check endpoints
                    "/actuator/**",      // Actuator endpoints  
                    "/swagger-ui/**",    // Swagger UI
                    "/v3/api-docs/**"    // OpenAPI docs
                );
    }
}
