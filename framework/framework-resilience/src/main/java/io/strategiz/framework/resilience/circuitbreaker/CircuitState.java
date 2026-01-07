package io.strategiz.framework.resilience.circuitbreaker;

/**
 * Represents the state of a circuit breaker.
 *
 * State transitions:
 * - CLOSED -> OPEN: When failure threshold is reached
 * - OPEN -> HALF_OPEN: After reset timeout expires
 * - HALF_OPEN -> CLOSED: On successful call
 * - HALF_OPEN -> OPEN: On failed call
 */
public enum CircuitState {

	/**
	 * Circuit is closed (normal operation). Requests flow through normally. Failures
	 * are counted toward the threshold.
	 */
	CLOSED,

	/**
	 * Circuit is open (failing fast). Requests are rejected immediately without
	 * attempting the operation. This protects downstream services.
	 */
	OPEN,

	/**
	 * Circuit is half-open (testing recovery). A limited number of requests are
	 * allowed through to test if the underlying issue is resolved.
	 */
	HALF_OPEN

}
