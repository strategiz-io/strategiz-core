package io.strategiz.client.alpaca.error;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details enum for Alpaca API client errors
 */
public enum AlpacaErrorDetails implements ErrorDetails {

    // API Communication Errors
    API_ERROR_RESPONSE(HttpStatus.BAD_GATEWAY, "api-error-response"),
    API_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "api-rate-limit-exceeded"),
    API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "api-unauthorized"),
    API_NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "api-network-error"),
    API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "api-timeout"),

    // Data Errors
    INVALID_SYMBOL(HttpStatus.BAD_REQUEST, "invalid-symbol"),
    NO_DATA_AVAILABLE(HttpStatus.NOT_FOUND, "no-data-available"),
    INVALID_RESPONSE_FORMAT(HttpStatus.INTERNAL_SERVER_ERROR, "invalid-response-format"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "invalid-date-range"),

    // Pagination Errors
    PAGINATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "pagination-error"),

    // Configuration Errors
    CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "configuration-error");

    private final HttpStatus httpStatus;
    private final String propertyKey;

    AlpacaErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
