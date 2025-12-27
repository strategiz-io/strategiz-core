package io.strategiz.client.yahoofinance.error;

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
	INVALID_CONFIGURATION(HttpStatus.INTERNAL_SERVER_ERROR, "invalid-configuration"),
	MISSING_API_KEY(HttpStatus.INTERNAL_SERVER_ERROR, "missing-api-key"),

	// API Connection & Response Errors
	API_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "api-connection-error"),
	API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "api-timeout"),
	API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "api-rate-limit"),
	API_ERROR_RESPONSE(HttpStatus.BAD_GATEWAY, "yahoo-api-error"),
	RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "yahoo-rate-limit"),
	EMPTY_RESPONSE(HttpStatus.NO_CONTENT, "empty-response"),

	// Symbol & Data Errors
	INVALID_SYMBOL(HttpStatus.NOT_FOUND, "yahoo-invalid-symbol"),
	SYMBOL_NOT_FOUND(HttpStatus.NOT_FOUND, "symbol-not-found"),
	INVALID_SYMBOL_FORMAT(HttpStatus.BAD_REQUEST, "invalid-symbol-format"),
	NO_DATA_AVAILABLE(HttpStatus.NOT_FOUND, "yahoo-no-data"),
	PRICE_DATA_UNAVAILABLE(HttpStatus.NOT_FOUND, "price-data-unavailable"),

	// Parsing Errors
	PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "yahoo-parse-error"),
	INVALID_RESPONSE_FORMAT(HttpStatus.BAD_GATEWAY, "invalid-response-format"),
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
