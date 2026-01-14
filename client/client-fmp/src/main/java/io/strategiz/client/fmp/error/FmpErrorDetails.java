package io.strategiz.client.fmp.error;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for FMP API errors.
 */
public enum FmpErrorDetails implements ErrorDetails {

	API_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "fmp-api-key-missing"),
	API_ERROR_RESPONSE(HttpStatus.BAD_GATEWAY, "fmp-api-error-response"),
	RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "fmp-rate-limit-exceeded"),
	NO_DATA_AVAILABLE(HttpStatus.NOT_FOUND, "fmp-no-data-available"),
	INVALID_SYMBOL(HttpStatus.BAD_REQUEST, "fmp-invalid-symbol"),
	PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "fmp-parsing-error");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	FmpErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
