package io.strategiz.framework.resilience.circuitbreaker;

import java.time.Instant;

/**
 * Interface for entities that maintain circuit breaker state.
 *
 * Implement this interface on entities that need circuit breaker protection,
 * such as AlertDeployment, BotDeployment, or API client state objects.
 *
 * The state is persisted with the entity (e.g., in Firestore), allowing
 * circuit breaker state to survive restarts and be shared across instances.
 */
public interface CircuitBreakerState {

	/**
	 * Get the number of consecutive failures.
	 */
	Integer getConsecutiveFailures();

	/**
	 * Set the number of consecutive failures.
	 */
	void setConsecutiveFailures(Integer failures);

	/**
	 * Get the number of consecutive successes (used in HALF_OPEN state).
	 */
	Integer getConsecutiveSuccesses();

	/**
	 * Set the number of consecutive successes.
	 */
	void setConsecutiveSuccesses(Integer successes);

	/**
	 * Get the current circuit state.
	 */
	CircuitState getCircuitState();

	/**
	 * Set the current circuit state.
	 */
	void setCircuitState(CircuitState state);

	/**
	 * Get when the circuit was last opened.
	 */
	Instant getCircuitOpenedAt();

	/**
	 * Set when the circuit was opened.
	 */
	void setCircuitOpenedAt(Instant openedAt);

	/**
	 * Get the last error message (for diagnostics).
	 */
	String getLastErrorMessage();

	/**
	 * Set the last error message.
	 */
	void setLastErrorMessage(String errorMessage);

	/**
	 * Get the failure threshold for this specific entity. Returns null to use default
	 * config.
	 */
	default Integer getFailureThreshold() {
		return null;
	}

	/**
	 * Set entity-specific failure threshold.
	 */
	default void setFailureThreshold(Integer threshold) {
		// Default no-op - override if entity supports custom thresholds
	}

}
