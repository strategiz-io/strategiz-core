package io.strategiz.client.yahoofinance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Yahoo Finance client
 */
@Configuration
public class YahooFinanceConfig {
    
    @Bean
    public RestTemplate yahooFinanceRestTemplate() {
        return new RestTemplate();
    }
}