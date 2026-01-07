package io.strategiz.batch.base.config;

import java.time.Duration;

/**
 * Configuration for retry behavior in batch jobs.
 *
 * Immutable - use builder pattern to construct.
 */
public class RetryConfig {

	private final int maxAttempts;

	private final Duration initialDelay;

	private final Duration maxDelay;

	private final double backoffMultiplier;

	private final boolean retryOnAllExceptions;

	private final Class<? extends Throwable>[] retryableExceptions;

	private RetryConfig(Builder builder) {
		this.maxAttempts = builder.maxAttempts;
		this.initialDelay = builder.initialDelay;
		this.maxDelay = builder.maxDelay;
		this.backoffMultiplier = builder.backoffMultiplier;
		this.retryOnAllExceptions = builder.retryOnAllExceptions;
		this.retryableExceptions = builder.retryableExceptions;
	}

	/**
	 * Maximum number of attempts (including initial attempt).
	 */
	public int getMaxAttempts() {
		return maxAttempts;
	}

	/**
	 * Initial delay before first retry.
	 */
	public Duration getInitialDelay() {
		return initialDelay;
	}

	/**
	 * Maximum delay between retries (caps exponential backoff).
	 */
	public Duration getMaxDelay() {
		return maxDelay;
	}

	/**
	 * Multiplier for exponential backoff (e.g., 2.0 doubles delay each retry).
	 */
	public double getBackoffMultiplier() {
		return backoffMultiplier;
	}

	/**
	 * Whether to retry on all exceptions or only specified ones.
	 */
	public boolean isRetryOnAllExceptions() {
		return retryOnAllExceptions;
	}

	/**
	 * Specific exceptions to retry (if retryOnAllExceptions is false).
	 */
	public Class<? extends Throwable>[] getRetryableExceptions() {
		return retryableExceptions;
	}

	/**
	 * Calculate delay for a specific attempt (1-indexed).
	 */
	public Duration getDelayForAttempt(int attempt) {
		if (attempt <= 1) {
			return Duration.ZERO;
		}
		// Exponential backoff: initialDelay * (multiplier ^ (attempt - 2))
		double delayMs = initialDelay.toMillis() * Math.pow(backoffMultiplier, attempt - 2);
		long cappedMs = Math.min((long) delayMs, maxDelay.toMillis());
		return Duration.ofMillis(cappedMs);
	}

	/**
	 * Check if the given exception should trigger a retry.
	 */
	public boolean shouldRetry(Throwable t) {
		if (retryOnAllExceptions) {
			return true;
		}
		if (retryableExceptions == null || retryableExceptions.length == 0) {
			return false;
		}
		for (Class<? extends Throwable> retryable : retryableExceptions) {
			if (retryable.isInstance(t)) {
				return true;
			}
		}
		return false;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Default configuration for batch jobs (3 attempts, 1s initial, 30s max).
	 */
	public static RetryConfig defaultConfig() {
		return builder().build();
	}

	/**
	 * No retry configuration.
	 */
	public static RetryConfig noRetry() {
		return builder().maxAttempts(1).build();
	}

	/**
	 * Aggressive retry for critical jobs (5 attempts, shorter delays).
	 */
	public static RetryConfig aggressive() {
		return builder().maxAttempts(5).initialDelay(Duration.ofMillis(500)).maxDelay(Duration.ofSeconds(10)).build();
	}

	/**
	 * Conservative retry for expensive operations (3 attempts, longer delays).
	 */
	public static RetryConfig conservative() {
		return builder().maxAttempts(3).initialDelay(Duration.ofSeconds(5)).maxDelay(Duration.ofMinutes(2)).build();
	}

	@SuppressWarnings("unchecked")
	public static class Builder {

		private int maxAttempts = 3;

		private Duration initialDelay = Duration.ofSeconds(1);

		private Duration maxDelay = Duration.ofSeconds(30);

		private double backoffMultiplier = 2.0;

		private boolean retryOnAllExceptions = true;

		private Class<? extends Throwable>[] retryableExceptions;

		public Builder maxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
			return this;
		}

		public Builder initialDelay(Duration initialDelay) {
			this.initialDelay = initialDelay;
			return this;
		}

		public Builder maxDelay(Duration maxDelay) {
			this.maxDelay = maxDelay;
			return this;
		}

		public Builder backoffMultiplier(double backoffMultiplier) {
			this.backoffMultiplier = backoffMultiplier;
			return this;
		}

		public Builder retryOnAllExceptions(boolean retryOnAllExceptions) {
			this.retryOnAllExceptions = retryOnAllExceptions;
			return this;
		}

		@SafeVarargs
		public final Builder retryOn(Class<? extends Throwable>... exceptions) {
			this.retryableExceptions = exceptions;
			this.retryOnAllExceptions = false;
			return this;
		}

		public RetryConfig build() {
			return new RetryConfig(this);
		}

	}

}
