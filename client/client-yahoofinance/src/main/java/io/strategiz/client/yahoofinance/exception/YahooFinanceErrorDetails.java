package io.strategiz.client.yahoofinance.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details enum for Yahoo Finance client module.
 * Follows Strategiz error handling patterns.
 */
public enum YahooFinanceErrorDetails implements ErrorDetails {

    // API Connection Errors
    API_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "api-connection-error"),
    API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "api-timeout"),
    API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "api-rate-limit"),

    // API Response Errors
    INVALID_RESPONSE_FORMAT(HttpStatus.BAD_GATEWAY, "invalid-response-format"),
    EMPTY_RESPONSE(HttpStatus.NO_CONTENT, "empty-response"),
    API_ERROR_RESPONSE(HttpStatus.BAD_GATEWAY, "api-error-response"),

    // Data Processing Errors
    SYMBOL_NOT_FOUND(HttpStatus.NOT_FOUND, "symbol-not-found"),
    PRICE_DATA_UNAVAILABLE(HttpStatus.NOT_FOUND, "price-data-unavailable"),
    INVALID_SYMBOL_FORMAT(HttpStatus.BAD_REQUEST, "invalid-symbol-format"),

    // Configuration Errors
    INVALID_CONFIGURATION(HttpStatus.INTERNAL_SERVER_ERROR, "invalid-configuration"),
    MISSING_API_KEY(HttpStatus.INTERNAL_SERVER_ERROR, "missing-api-key");

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