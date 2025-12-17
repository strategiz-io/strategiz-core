package io.strategiz.client.alpaca.error;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error codes specific to Alpaca API client operations.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(AlpacaErrors.ALPACA_API_ERROR, MODULE_NAME);
 */
public enum AlpacaErrors implements ErrorDetails {

	ALPACA_API_ERROR(HttpStatus.BAD_GATEWAY, "alpaca-api-error"),
	ALPACA_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "alpaca-auth-failed"),
	ALPACA_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "alpaca-invalid-response"),
	ALPACA_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "alpaca-rate-limited"),
	ALPACA_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "alpaca-token-expired"),
	ALPACA_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "alpaca-invalid-token"),
	ALPACA_OAUTH_ERROR(HttpStatus.UNAUTHORIZED, "alpaca-oauth-error"),
	ALPACA_NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "alpaca-network-error"),
	ALPACA_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "alpaca-configuration-error"),
	ALPACA_ACCOUNT_ERROR(HttpStatus.BAD_REQUEST, "alpaca-account-error"),
	ALPACA_POSITION_ERROR(HttpStatus.BAD_REQUEST, "alpaca-position-error");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	AlpacaErrors(HttpStatus httpStatus, String propertyKey) {
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

