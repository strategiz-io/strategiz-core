package io.strategiz.data.marketdata.timescale.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the data-marketdata-timescale module.
 * Only active when strategiz.timescale.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "strategiz.timescale.enabled", havingValue = "true")
@EnableConfigurationProperties(TimescaleProperties.class)
@ComponentScan(basePackages = "io.strategiz.data.marketdata.timescale")
public class DataMarketDataTimescaleConfig {

}
