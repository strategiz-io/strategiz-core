package io.strategiz.framework.resilience.circuitbreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Manages circuit breaker state transitions and execution.
 *
 * This class provides the core circuit breaker logic that can be used
 * with any entity implementing {@link CircuitBreakerState}.
 *
 * Thread-safe for use across multiple threads.
 */
public class CircuitBreakerManager {

	private static final Logger log = LoggerFactory.getLogger(CircuitBreakerManager.class);

	private final CircuitBreakerConfig config;

	public CircuitBreakerManager(CircuitBreakerConfig config) {
		this.config = config;
	}

	/**
	 * Check if the circuit allows the operation to proceed.
	 * @param state the circuit breaker state
	 * @return true if operation is allowed, false if circuit is open
	 */
	public boolean allowRequest(CircuitBreakerState state) {
		CircuitState currentState = getEffectiveState(state);

		switch (currentState) {
			case CLOSED:
				return true;

			case OPEN:
				// Check if reset timeout has passed
				if (shouldTransitionToHalfOpen(state)) {
					log.info("Circuit transitioning from OPEN to HALF_OPEN");
					state.setCircuitState(CircuitState.HALF_OPEN);
					state.setConsecutiveSuccesses(0);
					return true;
				}
				return false;

			case HALF_OPEN:
				// Allow limited requests in half-open state
				return true;

			default:
				return true;
		}
	}

	/**
	 * Record a successful operation.
	 * @param state the circuit breaker state
	 */
	public void recordSuccess(CircuitBreakerState state) {
		CircuitState currentState = getEffectiveState(state);

		// Reset failure count on success
		state.setConsecutiveFailures(0);
		state.setLastErrorMessage(null);

		if (currentState == CircuitState.HALF_OPEN) {
			int successes = incrementSuccesses(state);
			int threshold = config.getSuccessThreshold();

			if (successes >= threshold) {
				log.info("Circuit closing after {} consecutive successes", successes);
				state.setCircuitState(CircuitState.CLOSED);
				state.setConsecutiveSuccesses(0);
				state.setCircuitOpenedAt(null);
			}
		}
	}

	/**
	 * Record a failed operation.
	 * @param state the circuit breaker state
	 * @param error the error that occurred
	 */
	public void recordFailure(CircuitBreakerState state, Throwable error) {
		recordFailure(state, error != null ? error.getMessage() : "Unknown error");
	}

	/**
	 * Record a failed operation.
	 * @param state the circuit breaker state
	 * @param errorMessage the error message
	 */
	public void recordFailure(CircuitBreakerState state, String errorMessage) {
		CircuitState currentState = getEffectiveState(state);
		state.setLastErrorMessage(errorMessage);

		// Reset success count on failure
		state.setConsecutiveSuccesses(0);

		int failures = incrementFailures(state);
		int threshold = getEffectiveFailureThreshold(state);

		if (currentState == CircuitState.HALF_OPEN) {
			// Any failure in half-open reopens the circuit
			log.warn("Circuit reopening due to failure in HALF_OPEN state: {}", errorMessage);
			openCircuit(state);
		}
		else if (currentState == CircuitState.CLOSED && failures >= threshold) {
			log.warn("Circuit opening after {} consecutive failures (threshold: {})", failures, threshold);
			openCircuit(state);
		}
	}

	/**
	 * Force the circuit open (manual trip).
	 * @param state the circuit breaker state
	 * @param reason the reason for manual trip
	 */
	public void tripCircuit(CircuitBreakerState state, String reason) {
		log.warn("Circuit manually tripped: {}", reason);
		state.setLastErrorMessage(reason);
		openCircuit(state);
	}

	/**
	 * Force the circuit closed (manual reset).
	 * @param state the circuit breaker state
	 */
	public void resetCircuit(CircuitBreakerState state) {
		log.info("Circuit manually reset");
		state.setCircuitState(CircuitState.CLOSED);
		state.setConsecutiveFailures(0);
		state.setConsecutiveSuccesses(0);
		state.setCircuitOpenedAt(null);
		state.setLastErrorMessage(null);
	}

	/**
	 * Check if the circuit is currently open (failing fast).
	 * @param state the circuit breaker state
	 * @return true if circuit is open
	 */
	public boolean isOpen(CircuitBreakerState state) {
		return getEffectiveState(state) == CircuitState.OPEN;
	}

	/**
	 * Check if the circuit is currently closed (normal operation).
	 * @param state the circuit breaker state
	 * @return true if circuit is closed
	 */
	public boolean isClosed(CircuitBreakerState state) {
		return getEffectiveState(state) == CircuitState.CLOSED;
	}

	/**
	 * Execute an operation with circuit breaker protection.
	 * @param state the circuit breaker state
	 * @param operation the operation to execute
	 * @param <T> the return type
	 * @return the result of the operation
	 * @throws CircuitBreakerOpenException if circuit is open
	 */
	public <T> T execute(CircuitBreakerState state, Supplier<T> operation) {
		if (!allowRequest(state)) {
			throw new CircuitBreakerOpenException("Circuit breaker is open");
		}

		try {
			T result = operation.get();
			recordSuccess(state);
			return result;
		}
		catch (Exception e) {
			recordFailure(state, e);
			throw e;
		}
	}

	/**
	 * Execute an operation with circuit breaker protection (void return).
	 * @param state the circuit breaker state
	 * @param operation the operation to execute
	 * @throws CircuitBreakerOpenException if circuit is open
	 */
	public void execute(CircuitBreakerState state, Runnable operation) {
		if (!allowRequest(state)) {
			throw new CircuitBreakerOpenException("Circuit breaker is open");
		}

		try {
			operation.run();
			recordSuccess(state);
		}
		catch (Exception e) {
			recordFailure(state, e);
			throw e;
		}
	}

	/**
	 * Get time remaining until circuit can transition to HALF_OPEN.
	 * @param state the circuit breaker state
	 * @return duration until reset, or Duration.ZERO if already elapsed
	 */
	public Duration getTimeUntilReset(CircuitBreakerState state) {
		if (getEffectiveState(state) != CircuitState.OPEN) {
			return Duration.ZERO;
		}

		Instant openedAt = state.getCircuitOpenedAt();
		if (openedAt == null) {
			return Duration.ZERO;
		}

		Instant resetAt = openedAt.plus(config.getResetTimeout());
		Duration remaining = Duration.between(Instant.now(), resetAt);

		return remaining.isNegative() ? Duration.ZERO : remaining;
	}

	// Private helper methods

	private CircuitState getEffectiveState(CircuitBreakerState state) {
		CircuitState currentState = state.getCircuitState();
		return currentState != null ? currentState : CircuitState.CLOSED;
	}

	private int getEffectiveFailureThreshold(CircuitBreakerState state) {
		Integer entityThreshold = state.getFailureThreshold();
		return entityThreshold != null ? entityThreshold : config.getFailureThreshold();
	}

	private boolean shouldTransitionToHalfOpen(CircuitBreakerState state) {
		Instant openedAt = state.getCircuitOpenedAt();
		if (openedAt == null) {
			return true;
		}
		return Instant.now().isAfter(openedAt.plus(config.getResetTimeout()));
	}

	private void openCircuit(CircuitBreakerState state) {
		state.setCircuitState(CircuitState.OPEN);
		state.setCircuitOpenedAt(Instant.now());
	}

	private int incrementFailures(CircuitBreakerState state) {
		int current = state.getConsecutiveFailures() != null ? state.getConsecutiveFailures() : 0;
		int newValue = current + 1;
		state.setConsecutiveFailures(newValue);
		return newValue;
	}

	private int incrementSuccesses(CircuitBreakerState state) {
		int current = state.getConsecutiveSuccesses() != null ? state.getConsecutiveSuccesses() : 0;
		int newValue = current + 1;
		state.setConsecutiveSuccesses(newValue);
		return newValue;
	}

}
