package io.strategiz.data.symbol.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for data-symbol module. Follows the standard naming convention:
 * Data[Module]Config
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.data.symbol")
public class DataSymbolConfig {

	// Module configuration - currently uses defaults
	// Add beans here if needed for symbol-specific configuration

}
