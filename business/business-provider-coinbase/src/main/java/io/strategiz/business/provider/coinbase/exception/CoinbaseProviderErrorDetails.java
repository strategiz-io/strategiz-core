package io.strategiz.business.provider.coinbase.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Coinbase provider error definitions with HTTP status and property key mapping.
 */
public enum CoinbaseProviderErrorDetails implements ErrorDetails {

    // OAuth & Authentication errors
    OAUTH_FLOW_FAILED(HttpStatus.BAD_GATEWAY, "coinbase-oauth-flow-failed"),
    TOKEN_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "coinbase-token-storage-failed"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "coinbase-invalid-credentials"),

    // Integration errors
    INTEGRATION_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "coinbase-integration-creation-failed"),

    // Data sync errors
    TOKENS_NOT_FOUND(HttpStatus.NOT_FOUND, "coinbase-tokens-not-found"),
    ACCESS_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "coinbase-access-token-not-found"),
    SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "coinbase-sync-failed"),
    NO_DATA_AFTER_SYNC(HttpStatus.INTERNAL_SERVER_ERROR, "coinbase-no-data-after-sync"),

    // API errors
    PORTFOLIO_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "coinbase-portfolio-fetch-failed"),

    // General errors
    PROVIDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "coinbase-provider-error");

    private final HttpStatus httpStatus;
    private final String propertyKey;

    CoinbaseProviderErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
