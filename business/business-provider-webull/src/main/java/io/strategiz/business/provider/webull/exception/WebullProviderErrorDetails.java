package io.strategiz.business.provider.webull.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Webull provider error definitions with HTTP status and property key mapping.
 *
 * Each error maps to properties in business-provider-webull-errors.properties file which contains:
 * - Error code
 * - User-friendly message
 * - Developer message template with placeholders
 * - Documentation path
 *
 * Usage:
 * throw new StrategizException(WebullProviderErrorDetails.INVALID_CREDENTIALS, "business-provider-webull", userId);
 * throw new StrategizException(WebullProviderErrorDetails.POSITIONS_FETCH_FAILED, "business-provider-webull", userId, errorMessage);
 *
 * @author Strategiz Platform
 * @since 1.0
 */
public enum WebullProviderErrorDetails implements ErrorDetails {

    // Authentication & Connection errors
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "webull-invalid-credentials"),
    APP_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "webull-app-key-required"),
    APP_SECRET_REQUIRED(HttpStatus.BAD_REQUEST, "webull-app-secret-required"),
    ACCOUNT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "webull-account-id-required"),
    CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "webull-connection-failed"),
    CONNECTION_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "webull-connection-timeout"),

    // Data fetching errors
    ACCOUNT_LIST_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "webull-account-list-fetch-failed"),
    POSITIONS_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "webull-positions-fetch-failed"),
    BALANCE_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "webull-balance-fetch-failed"),
    ORDERS_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "webull-orders-fetch-failed"),
    PORTFOLIO_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "webull-portfolio-fetch-failed"),

    // Data processing errors
    DATA_INITIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "webull-data-init-failed"),
    DATA_TRANSFORMATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "webull-data-transform-failed"),
    INVALID_API_RESPONSE(HttpStatus.BAD_GATEWAY, "webull-invalid-api-response"),

    // Storage errors
    CREDENTIAL_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "webull-credential-storage-failed"),
    VAULT_ACCESS_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "webull-vault-access-failed"),
    FIRESTORE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "webull-firestore-save-failed"),

    // Rate limiting & permissions
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "webull-rate-limit"),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "webull-insufficient-permissions"),

    // Validation errors
    INVALID_APP_KEY_FORMAT(HttpStatus.BAD_REQUEST, "webull-invalid-app-key-format"),
    INVALID_APP_SECRET_FORMAT(HttpStatus.BAD_REQUEST, "webull-invalid-app-secret-format"),
    INVALID_ACCOUNT_ID(HttpStatus.BAD_REQUEST, "webull-invalid-account-id"),

    // General errors
    PROVIDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "webull-provider-error"),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "webull-unknown-error");

    private final HttpStatus httpStatus;
    private final String propertyKey;

    WebullProviderErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
