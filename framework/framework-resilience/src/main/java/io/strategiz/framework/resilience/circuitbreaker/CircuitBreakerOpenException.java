package io.strategiz.framework.resilience.circuitbreaker;

/**
 * Exception thrown when an operation is rejected because the circuit breaker is open.
 *
 * This is a fail-fast exception indicating that the underlying service/operation
 * is known to be unhealthy and requests are being rejected to protect the system.
 */
public class CircuitBreakerOpenException extends RuntimeException {

	public CircuitBreakerOpenException(String message) {
		super(message);
	}

	public CircuitBreakerOpenException(String message, Throwable cause) {
		super(message, cause);
	}

}
