package io.strategiz.batch.base;

import io.strategiz.batch.base.config.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Executes operations with retry logic based on configuration.
 *
 * Supports:
 * - Exponential backoff with configurable multiplier
 * - Maximum delay cap
 * - Exception filtering (retry only specific exceptions)
 * - Attempt tracking for logging/metrics
 */
public class RetryExecutor {

	private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

	private final RetryConfig config;

	public RetryExecutor(RetryConfig config) {
		this.config = config;
	}

	/**
	 * Execute operation with retry logic.
	 * @param operation The operation to execute
	 * @param operationName Human-readable name for logging
	 * @param <T> Return type
	 * @return Result of successful execution
	 * @throws RuntimeException if all attempts fail
	 */
	public <T> T execute(Supplier<T> operation, String operationName) {
		Exception lastException = null;

		for (int attempt = 1; attempt <= config.getMaxAttempts(); attempt++) {
			try {
				if (attempt > 1) {
					Duration delay = config.getDelayForAttempt(attempt);
					log.info("Retry attempt {} for '{}' after {}ms delay", attempt, operationName, delay.toMillis());
					Thread.sleep(delay.toMillis());
				}

				return operation.get();

			}
			catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Retry interrupted for: " + operationName, ie);
			}
			catch (Exception e) {
				lastException = e;

				if (!config.shouldRetry(e)) {
					log.error("Non-retryable exception for '{}': {}", operationName, e.getMessage());
					throw new RuntimeException("Non-retryable failure: " + operationName, e);
				}

				if (attempt < config.getMaxAttempts()) {
					log.warn("Attempt {} failed for '{}': {} - will retry", attempt, operationName, e.getMessage());
				}
				else {
					log.error("All {} attempts failed for '{}': {}", config.getMaxAttempts(), operationName,
							e.getMessage());
				}
			}
		}

		throw new RuntimeException(
				"All " + config.getMaxAttempts() + " attempts failed for: " + operationName, lastException);
	}

	/**
	 * Execute void operation with retry logic.
	 */
	public void execute(Runnable operation, String operationName) {
		execute(() -> {
			operation.run();
			return null;
		}, operationName);
	}

	/**
	 * Execute with callback for each attempt (useful for metrics/logging).
	 */
	public <T> RetryResult<T> executeWithTracking(Supplier<T> operation, String operationName) {
		Exception lastException = null;
		int attemptsMade = 0;

		for (int attempt = 1; attempt <= config.getMaxAttempts(); attempt++) {
			attemptsMade = attempt;

			try {
				if (attempt > 1) {
					Duration delay = config.getDelayForAttempt(attempt);
					log.info("Retry attempt {} for '{}' after {}ms delay", attempt, operationName, delay.toMillis());
					Thread.sleep(delay.toMillis());
				}

				T result = operation.get();
				return RetryResult.success(result, attempt);

			}
			catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				return RetryResult.failure(attempt, new RuntimeException("Interrupted", ie));
			}
			catch (Exception e) {
				lastException = e;

				if (!config.shouldRetry(e)) {
					return RetryResult.failure(attempt, e);
				}

				if (attempt < config.getMaxAttempts()) {
					log.warn("Attempt {} failed for '{}': {} - will retry", attempt, operationName, e.getMessage());
				}
			}
		}

		return RetryResult.failure(attemptsMade, lastException);
	}

	/**
	 * Result of retry execution with tracking info.
	 */
	public record RetryResult<T>(boolean success, T result, int attemptsMade, Exception exception) {

		public static <T> RetryResult<T> success(T result, int attempts) {
			return new RetryResult<>(true, result, attempts, null);
		}

		public static <T> RetryResult<T> failure(int attempts, Exception e) {
			return new RetryResult<>(false, null, attempts, e);
		}

		public T getOrThrow() {
			if (success) {
				return result;
			}
			throw new RuntimeException("Retry failed after " + attemptsMade + " attempts", exception);
		}
	}

}
