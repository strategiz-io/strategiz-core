package io.strategiz.client.alpaca.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Alpaca API client
 */
@Configuration
public class ClientAlpacaConfig {

    @Bean("alpacaRestTemplate")
    public RestTemplate alpacaRestTemplate() {
        return new RestTemplate();
    }
}
