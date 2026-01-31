package io.strategiz.client.coinbase;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error codes for Coinbase client operations. Implements ErrorDetails for integration
 * with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(CoinbaseErrors.API_CONNECTION_FAILED, MODULE_NAME);
 */
public enum CoinbaseErrors implements ErrorDetails {

	API_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "coinbase-connection-failed"),
	API_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "coinbase-auth-failed"),
	AUTHENTICATION_ERROR(HttpStatus.UNAUTHORIZED, "coinbase-authentication-error"),
	API_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "coinbase-rate-limited"),
	API_ERROR(HttpStatus.BAD_GATEWAY, "coinbase-api-error"),
	INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "coinbase-invalid-response"),
	ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "coinbase-account-not-found"),
	INSUFFICIENT_FUNDS(HttpStatus.PAYMENT_REQUIRED, "coinbase-insufficient-funds"),
	CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "coinbase-configuration-error"),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "coinbase-invalid-credentials"),
	TRANSACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "coinbase-transaction-failed"),
	NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "coinbase-network-error"),
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "coinbase-service-unavailable");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	CoinbaseErrors(HttpStatus httpStatus, String propertyKey) {
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
