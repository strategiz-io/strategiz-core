package io.strategiz.business.provider.kraken.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Kraken provider error definitions with HTTP status and property key mapping.
 *
 * Each error maps to properties in business-provider-kraken-errors.properties file which
 * contains: - Error code - User-friendly message - Developer message template with
 * placeholders - Documentation path
 *
 * Usage: throw new StrategizException(KrakenProviderErrorDetails.INVALID_CREDENTIALS,
 * "business-provider-kraken", userId); throw new
 * StrategizException(KrakenProviderErrorDetails.BALANCE_FETCH_FAILED,
 * "business-provider-kraken", userId, errorMessage);
 *
 * @author Strategiz Platform
 * @since 1.0
 */
public enum KrakenProviderErrorDetails implements ErrorDetails {

	// Authentication & Connection errors
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "kraken-invalid-credentials"),
	API_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "kraken-api-key-required"),
	API_SECRET_REQUIRED(HttpStatus.BAD_REQUEST, "kraken-api-secret-required"),
	CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "kraken-connection-failed"),
	CONNECTION_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "kraken-connection-timeout"),

	// Data fetching errors
	BALANCE_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "kraken-balance-fetch-failed"),
	TRADES_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "kraken-trades-fetch-failed"),
	TICKER_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "kraken-ticker-fetch-failed"),
	PORTFOLIO_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "kraken-portfolio-fetch-failed"),

	// Data processing errors
	DATA_INITIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "kraken-data-init-failed"),
	DATA_TRANSFORMATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "kraken-data-transform-failed"),
	INVALID_API_RESPONSE(HttpStatus.BAD_GATEWAY, "kraken-invalid-api-response"),

	// Storage errors
	CREDENTIAL_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "kraken-credential-storage-failed"),
	VAULT_ACCESS_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "kraken-vault-access-failed"),
	FIRESTORE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "kraken-firestore-save-failed"),

	// Rate limiting & permissions
	RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "kraken-rate-limit"),
	INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "kraken-insufficient-permissions"),

	// Validation errors
	INVALID_API_KEY_FORMAT(HttpStatus.BAD_REQUEST, "kraken-invalid-api-key-format"),
	INVALID_API_SECRET_FORMAT(HttpStatus.BAD_REQUEST, "kraken-invalid-api-secret-format"),
	INVALID_OTP(HttpStatus.BAD_REQUEST, "kraken-invalid-otp"),

	// General errors
	PROVIDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "kraken-provider-error"),
	UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "kraken-unknown-error");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	KrakenProviderErrorDetails(HttpStatus httpStatus, String propertyKey) {
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