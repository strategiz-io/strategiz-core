package io.strategiz.business.historicalinsights.exception;

/**
 * Exception thrown when there is insufficient historical data to perform analysis.
 * This typically occurs when a symbol has less than the minimum required number of bars.
 */
public class InsufficientDataException extends RuntimeException {

	public InsufficientDataException(String message) {
		super(message);
	}

	public InsufficientDataException(String message, Throwable cause) {
		super(message, cause);
	}

}
