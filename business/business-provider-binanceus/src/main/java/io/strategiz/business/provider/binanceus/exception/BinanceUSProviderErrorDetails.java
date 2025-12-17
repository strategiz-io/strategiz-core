package io.strategiz.business.provider.binanceus.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Binance US provider error definitions with HTTP status and property key mapping.
 */
public enum BinanceUSProviderErrorDetails implements ErrorDetails {

    // Authentication errors
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "binanceus-invalid-credentials"),
    API_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "binanceus-api-key-required"),
    API_SECRET_REQUIRED(HttpStatus.BAD_REQUEST, "binanceus-api-secret-required"),

    // Integration errors
    INTEGRATION_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "binanceus-integration-creation-failed"),
    CREDENTIAL_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "binanceus-credential-storage-failed"),

    // Connection errors
    CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "binanceus-connection-failed"),

    // Data errors
    BALANCE_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "binanceus-balance-fetch-failed"),

    // General errors
    PROVIDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "binanceus-provider-error");

    private final HttpStatus httpStatus;
    private final String propertyKey;

    BinanceUSProviderErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
