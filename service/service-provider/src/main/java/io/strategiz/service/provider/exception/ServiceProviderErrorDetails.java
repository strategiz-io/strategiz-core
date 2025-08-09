package io.strategiz.service.provider.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Enum containing all provider service error details.
 * 
 * Each error has a code, HTTP status, and message key for internationalization.
 * Messages are resolved from service-provider-errors.properties file.
 */
public enum ServiceProviderErrorDetails implements ErrorDetails {
    
    // Provider Connection Errors
    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "provider-not-found"),
    PROVIDER_CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "provider-connection-failed"),
    PROVIDER_ALREADY_EXISTS(HttpStatus.CONFLICT, "provider-already-exists"),
    PROVIDER_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "provider-invalid-credentials"),
    PROVIDER_DISCONNECTED(HttpStatus.SERVICE_UNAVAILABLE, "provider-disconnected"),
    
    // OAuth Errors
    OAUTH_STATE_INVALID(HttpStatus.BAD_REQUEST, "oauth-state-invalid"),
    INVALID_OAUTH_STATE(HttpStatus.BAD_REQUEST, "invalid-oauth-state"), // Alias for consistency
    OAUTH_CODE_INVALID(HttpStatus.BAD_REQUEST, "oauth-code-invalid"),
    OAUTH_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY, "oauth-token-exchange-failed"),
    OAUTH_TOKEN_REFRESH_FAILED(HttpStatus.BAD_GATEWAY, "oauth-token-refresh-failed"),
    OAUTH_TOKEN_REVOCATION_FAILED(HttpStatus.BAD_GATEWAY, "oauth-token-revocation-failed"),
    
    // API Key Errors
    API_KEY_INVALID(HttpStatus.UNAUTHORIZED, "api-key-invalid"),
    API_KEY_EXPIRED(HttpStatus.UNAUTHORIZED, "api-key-expired"),
    API_KEY_REVOKED(HttpStatus.UNAUTHORIZED, "api-key-revoked"),
    API_KEY_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "api-key-rate-limited"),
    
    // Provider Data Errors
    PROVIDER_DATA_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "provider-data-unavailable"),
    PROVIDER_DATA_SYNC_FAILED(HttpStatus.BAD_GATEWAY, "provider-data-sync-failed"),
    PROVIDER_DATA_PARSING_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "provider-data-parsing-failed"),
    
    // Validation Errors
    INVALID_PROVIDER_TYPE(HttpStatus.BAD_REQUEST, "invalid-provider-type"),
    INVALID_PROVIDER_CONFIG(HttpStatus.BAD_REQUEST, "invalid-provider-config"),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "missing-required-field"),
    PROVIDER_NOT_SUPPORTED(HttpStatus.NOT_IMPLEMENTED, "provider-not-supported"),
    
    // Provider Specific Errors
    COINBASE_API_ERROR(HttpStatus.BAD_GATEWAY, "coinbase-api-error"),
    BINANCE_API_ERROR(HttpStatus.BAD_GATEWAY, "binance-api-error"),
    KRAKEN_API_ERROR(HttpStatus.BAD_GATEWAY, "kraken-api-error"),
    
    // System Errors
    PROVIDER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "provider-service-unavailable"),
    PROVIDER_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "provider-database-error"),
    PROVIDER_CACHE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "provider-cache-error"),
    PROVIDER_ENCRYPTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "provider-encryption-error"),
    PROVIDER_VAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "provider-vault-error"),
    
    // Webhook Errors
    WEBHOOK_REGISTRATION_FAILED(HttpStatus.BAD_GATEWAY, "webhook-registration-failed"),
    WEBHOOK_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "webhook-verification-failed"),
    WEBHOOK_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "webhook-processing-failed");
    
    private final HttpStatus httpStatus;
    private final String messageKey;
    
    ServiceProviderErrorDetails(HttpStatus httpStatus, String messageKey) {
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
    
    
    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    @Override
    public String getPropertyKey() {
        return messageKey;
    }
}