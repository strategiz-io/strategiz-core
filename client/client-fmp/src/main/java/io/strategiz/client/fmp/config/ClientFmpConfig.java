package io.strategiz.client.fmp.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Spring configuration for Financial Modeling Prep client.
 *
 * <p>
 * Provides: - RestTemplate configured with appropriate timeouts for FMP API - Rate limiter based
 * on API tier - Component scanning for FMP client beans - Conditional activation based on
 * property: strategiz.fmp.enabled
 * </p>
 *
 * <p>
 * Enable in application.properties:
 *
 * <pre>
 * strategiz.fmp.enabled=true
 * fmp.api-key=your-api-key
 * fmp.base-url=https://financialmodelingprep.com/api/v3
 * fmp.rate-limit-per-minute=300
 * </pre>
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "strategiz.fmp.enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan(basePackages = "io.strategiz.client.fmp")
public class ClientFmpConfig {

	/**
	 * RestTemplate configured for FMP API calls.
	 * @param builder Spring's RestTemplateBuilder
	 * @return Configured RestTemplate
	 */
	@Bean(name = "fmpRestTemplate")
	public RestTemplate fmpRestTemplate(RestTemplateBuilder builder) {
		return builder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration.ofSeconds(30)).build();
	}

	/**
	 * Rate limiter for FMP API based on configured rate limit per minute.
	 * @param config FMP configuration
	 * @return Bucket for rate limiting
	 */
	@Bean(name = "fmpRateLimiter")
	public Bucket fmpRateLimiter(FmpConfig config) {
		// Create rate limiter based on configured calls per minute
		Bandwidth limit = Bandwidth.classic(config.getRateLimitPerMinute(),
				Refill.intervally(config.getRateLimitPerMinute(), Duration.ofMinutes(1)));
		return Bucket.builder().addLimit(limit).build();
	}

}
