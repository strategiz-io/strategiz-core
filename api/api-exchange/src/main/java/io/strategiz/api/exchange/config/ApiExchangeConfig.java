package io.strategiz.api.exchange.config;

import io.strategiz.service.exchange.config.ServiceExchangeConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for the exchange API module
 * This configuration only defines component scanning for exchange-related packages.
 * CORS and other web configurations are handled by the global WebConfig in the application module.
 */
@Configuration
@ComponentScan(basePackages = {
    "io.strategiz.api.exchange",
    // Legacy packages for backward compatibility
    "io.strategiz.coinbase.controller",
    "io.strategiz.binanceus",
    "io.strategiz.kraken"
})
@Import({
    ServiceExchangeConfig.class
})
public class ApiExchangeConfig {
    // Configuration is limited to component scanning
    // CORS and other web configurations are handled by the global WebConfig
}
