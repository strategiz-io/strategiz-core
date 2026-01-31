package io.strategiz.service.provider.controller;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Test configuration for provider controller tests. Minimal Spring configuration for
 * testing.
 */
@Configuration
@SpringBootApplication
@ComponentScan(basePackages = "io.strategiz.service.provider",
		excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
				pattern = "io.strategiz.service.provider.ProviderServiceApplication"))
public class TestProviderConfiguration {

	// Empty configuration class for test context

}