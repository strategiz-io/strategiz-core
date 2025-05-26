package io.strategiz.service.exchange.config;

import io.strategiz.client.binanceus.config.ClientBinanceUSConfig;
import io.strategiz.client.coinbase.config.ClientCoinbaseConfig;
import io.strategiz.client.kraken.config.ClientKrakenConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for Exchange service module
 * This configuration imports client module configurations and sets up component scanning
 */
@Configuration
@ComponentScan(basePackages = {
    "io.strategiz.service.exchange",
    "io.strategiz.data.exchange"
})
@Import({
    // Client module configurations
    ClientBinanceUSConfig.class,
    ClientCoinbaseConfig.class,
    ClientKrakenConfig.class
})
public class ServiceExchangeConfig {
    // Configuration is mainly handled through imports and component scanning
}
