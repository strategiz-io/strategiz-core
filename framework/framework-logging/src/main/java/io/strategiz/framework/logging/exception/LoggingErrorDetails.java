package io.strategiz.framework.logging.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error codes for logging framework operations.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(LoggingErrorDetails.INVALID_ARGUMENT, MODULE_NAME);
 */
public enum LoggingErrorDetails implements ErrorDetails {

	INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "logging-invalid-argument"),
	CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "logging-configuration-error");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	LoggingErrorDetails(HttpStatus httpStatus, String propertyKey) {
		this.httpStatus = httpStatus;
		this.propertyKey = propertyKey;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	@Override
	public String getPropertyKey() {
		return propertyKey;
	}

}
