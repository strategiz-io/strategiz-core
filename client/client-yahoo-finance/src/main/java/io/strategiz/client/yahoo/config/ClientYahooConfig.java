package io.strategiz.client.yahoo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Spring configuration for Yahoo Finance client.
 *
 * <p>
 * Provides:
 * - RestTemplate configured with appropriate timeouts for Yahoo Finance API
 * - Component scanning for Yahoo Finance client beans
 * - Conditional activation based on property: strategiz.yahoo-finance.enabled
 * </p>
 *
 * <p>
 * Enable in application.properties:
 * <pre>
 * strategiz.yahoo-finance.enabled=true
 * yahoo.finance.base-url=https://query2.finance.yahoo.com
 * yahoo.finance.delay-ms=150
 * yahoo.finance.max-retries=5
 * yahoo.finance.timeout-ms=10000
 * </pre>
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "strategiz.yahoo-finance.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "io.strategiz.client.yahoo")
public class ClientYahooConfig {

	/**
	 * RestTemplate configured for Yahoo Finance API calls.
	 *
	 * @param builder Spring's RestTemplateBuilder
	 * @return Configured RestTemplate
	 */
	@Bean(name = "yahooFinanceRestTemplate")
	public RestTemplate yahooFinanceRestTemplate(RestTemplateBuilder builder) {
		return builder.setConnectTimeout(Duration.ofSeconds(10))
			.setReadTimeout(Duration.ofSeconds(30))
			.build();
	}

}
