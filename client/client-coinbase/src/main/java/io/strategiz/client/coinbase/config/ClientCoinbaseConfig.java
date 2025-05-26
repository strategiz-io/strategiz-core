package io.strategiz.client.coinbase.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Coinbase API client
 */
@Configuration
public class ClientCoinbaseConfig {

    @Bean("coinbaseRestTemplate")
    public RestTemplate coinbaseRestTemplate() {
        return new RestTemplate();
    }
}
