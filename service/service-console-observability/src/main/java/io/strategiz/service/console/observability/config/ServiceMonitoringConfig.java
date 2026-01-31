package io.strategiz.service.console.observability.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for the monitoring service module
 */
@Configuration
@ComponentScan(basePackages = { "io.strategiz.service.monitoring", "io.strategiz.service.exchange" })
public class ServiceMonitoringConfig {

	/**
	 * Create RestTemplate bean for HTTP requests
	 * @return RestTemplate instance
	 */
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
