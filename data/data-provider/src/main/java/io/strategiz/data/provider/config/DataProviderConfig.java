package io.strategiz.data.provider.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for data-provider module Ensures that repository implementations in
 * this module are scanned
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.data.provider")
public class DataProviderConfig {

	// Configuration to ensure Spring scans the data-provider package

}