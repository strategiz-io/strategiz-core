package io.strategiz.framework.resilience.circuitbreaker;

import java.time.Duration;

/**
 * Configuration for circuit breaker behavior.
 *
 * Immutable configuration object - use builder pattern to construct.
 */
public class CircuitBreakerConfig {

	private final int failureThreshold;

	private final int successThreshold;

	private final Duration resetTimeout;

	private final Duration halfOpenTimeout;

	private CircuitBreakerConfig(Builder builder) {
		this.failureThreshold = builder.failureThreshold;
		this.successThreshold = builder.successThreshold;
		this.resetTimeout = builder.resetTimeout;
		this.halfOpenTimeout = builder.halfOpenTimeout;
	}

	/**
	 * Number of consecutive failures before opening the circuit.
	 */
	public int getFailureThreshold() {
		return failureThreshold;
	}

	/**
	 * Number of consecutive successes in HALF_OPEN state before closing the circuit.
	 */
	public int getSuccessThreshold() {
		return successThreshold;
	}

	/**
	 * Time to wait in OPEN state before transitioning to HALF_OPEN.
	 */
	public Duration getResetTimeout() {
		return resetTimeout;
	}

	/**
	 * Max time to stay in HALF_OPEN state before forcing a decision.
	 */
	public Duration getHalfOpenTimeout() {
		return halfOpenTimeout;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Default configuration for alerts (more tolerant).
	 */
	public static CircuitBreakerConfig forAlerts() {
		return builder().failureThreshold(5).successThreshold(2).resetTimeout(Duration.ofMinutes(5)).build();
	}

	/**
	 * Default configuration for bots (stricter - real money involved).
	 */
	public static CircuitBreakerConfig forBots() {
		return builder().failureThreshold(3).successThreshold(3).resetTimeout(Duration.ofMinutes(15)).build();
	}

	/**
	 * Default configuration for external API clients.
	 */
	public static CircuitBreakerConfig forApiClient() {
		return builder().failureThreshold(5).successThreshold(1).resetTimeout(Duration.ofSeconds(30)).build();
	}

	/**
	 * Default configuration for batch jobs.
	 */
	public static CircuitBreakerConfig forBatchJob() {
		return builder().failureThreshold(3).successThreshold(1).resetTimeout(Duration.ofMinutes(10)).build();
	}

	public static class Builder {

		private int failureThreshold = 5;

		private int successThreshold = 1;

		private Duration resetTimeout = Duration.ofMinutes(1);

		private Duration halfOpenTimeout = Duration.ofMinutes(5);

		public Builder failureThreshold(int failureThreshold) {
			this.failureThreshold = failureThreshold;
			return this;
		}

		public Builder successThreshold(int successThreshold) {
			this.successThreshold = successThreshold;
			return this;
		}

		public Builder resetTimeout(Duration resetTimeout) {
			this.resetTimeout = resetTimeout;
			return this;
		}

		public Builder halfOpenTimeout(Duration halfOpenTimeout) {
			this.halfOpenTimeout = halfOpenTimeout;
			return this;
		}

		public CircuitBreakerConfig build() {
			return new CircuitBreakerConfig(this);
		}

	}

}
