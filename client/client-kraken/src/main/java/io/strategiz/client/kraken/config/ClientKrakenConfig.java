package io.strategiz.client.kraken.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the Kraken client module
 */
@Configuration
public class ClientKrakenConfig {

    /**
     * Create RestTemplate bean for Kraken API requests
     * 
     * @return RestTemplate instance
     */
    @Bean
    public RestTemplate krakenRestTemplate() {
        return new RestTemplate();
    }
}
