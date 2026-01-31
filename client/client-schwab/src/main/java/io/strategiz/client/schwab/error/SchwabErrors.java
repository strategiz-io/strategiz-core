package io.strategiz.client.schwab.error;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error codes specific to Charles Schwab API client operations. Implements ErrorDetails
 * for integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(SchwabErrors.SCHWAB_API_ERROR, MODULE_NAME);
 */
public enum SchwabErrors implements ErrorDetails {

	SCHWAB_API_ERROR(HttpStatus.BAD_GATEWAY, "schwab-api-error"),
	SCHWAB_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "schwab-auth-failed"),
	SCHWAB_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "schwab-invalid-response"),
	SCHWAB_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "schwab-rate-limited"),
	SCHWAB_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "schwab-token-expired"),
	SCHWAB_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "schwab-invalid-token"),
	SCHWAB_OAUTH_ERROR(HttpStatus.UNAUTHORIZED, "schwab-oauth-error"),
	SCHWAB_NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "schwab-network-error"),
	SCHWAB_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "schwab-configuration-error"),
	SCHWAB_ACCOUNT_ERROR(HttpStatus.BAD_REQUEST, "schwab-account-error"),
	SCHWAB_POSITION_ERROR(HttpStatus.BAD_REQUEST, "schwab-position-error"),
	SCHWAB_TRANSACTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "schwab-transaction-error");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	SchwabErrors(HttpStatus httpStatus, String propertyKey) {
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
