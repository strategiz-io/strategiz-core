package io.strategiz.api.exchange.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the exchange API module
 * This configuration only defines component scanning for exchange-related packages.
 * CORS and other web configurations are handled by the global WebConfig in the application module.
 */
@Configuration
@ComponentScan(basePackages = {
    "strategiz.api.exchange",
    "strategiz.service.exchange",
    "strategiz.data.exchange",
    "strategiz.client.coinbase",
    "strategiz.client.kraken",
    // Legacy packages for backward compatibility
    "io.strategiz.coinbase",
    "io.strategiz.coinbase.controller",
    "io.strategiz.coinbase.service",
    "io.strategiz.binanceus",
    "io.strategiz.kraken"
})
public class ExchangeConfig {
    // Configuration is limited to component scanning
    // CORS and other web configurations are handled by the global WebConfig
}
