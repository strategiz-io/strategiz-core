package io.strategiz.business.livestrategies.config;

import io.strategiz.framework.resilience.circuitbreaker.CircuitBreakerConfig;
import io.strategiz.framework.resilience.circuitbreaker.CircuitBreakerManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for live strategies business logic. Sets up circuit breaker, retry
 * policies, and other resilience patterns.
 */
@Configuration("liveStrategiesBusinessConfig")
public class LiveStrategiesConfig {

	/**
	 * Circuit breaker manager for bot trading operations. Uses the stricter "forBots"
	 * configuration: - 3 failure threshold (real money involved) - 3 success threshold to
	 * recover - 15 minute reset timeout
	 */
	@Bean
	public CircuitBreakerManager circuitBreakerManager() {
		return new CircuitBreakerManager(CircuitBreakerConfig.forBots());
	}

}
