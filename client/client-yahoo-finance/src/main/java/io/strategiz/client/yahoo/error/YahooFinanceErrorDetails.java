package io.strategiz.client.yahoo.error;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details specific to Yahoo Finance API client operations.
 *
 * Yahoo Finance uses an unofficial API, so errors may vary:
 * - Rate limiting (429 Too Many Requests)
 * - Symbol not found (404 Not Found)
 * - Invalid response format (parsing errors)
 * - Network errors
 */
public enum YahooFinanceErrorDetails implements ErrorDetails {

	// Configuration Errors
	CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "yahoo-config-error"),

	// API Errors
	API_ERROR_RESPONSE(HttpStatus.BAD_GATEWAY, "yahoo-api-error"),

	RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "yahoo-rate-limit"),

	INVALID_SYMBOL(HttpStatus.NOT_FOUND, "yahoo-invalid-symbol"),

	NO_DATA_AVAILABLE(HttpStatus.NOT_FOUND, "yahoo-no-data"),

	// Parsing Errors
	PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "yahoo-parse-error"),

	INCOMPLETE_DATA(HttpStatus.PARTIAL_CONTENT, "yahoo-incomplete-data"),

	// Network Errors
	NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "yahoo-network-error"),

	TIMEOUT_ERROR(HttpStatus.GATEWAY_TIMEOUT, "yahoo-timeout");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	YahooFinanceErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
