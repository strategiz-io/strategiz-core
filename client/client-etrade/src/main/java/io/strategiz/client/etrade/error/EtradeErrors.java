package io.strategiz.client.etrade.error;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error codes specific to E*TRADE API client operations. Implements ErrorDetails for
 * integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(EtradeErrors.ETRADE_API_ERROR, MODULE_NAME);
 */
public enum EtradeErrors implements ErrorDetails {

	ETRADE_API_ERROR(HttpStatus.BAD_GATEWAY, "etrade-api-error"),
	ETRADE_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "etrade-auth-failed"),
	ETRADE_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "etrade-invalid-response"),
	ETRADE_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "etrade-rate-limited"),
	ETRADE_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "etrade-token-expired"),
	ETRADE_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "etrade-invalid-token"),
	ETRADE_OAUTH_ERROR(HttpStatus.UNAUTHORIZED, "etrade-oauth-error"),
	ETRADE_NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "etrade-network-error"),
	ETRADE_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "etrade-configuration-error"),
	ETRADE_ACCOUNT_ERROR(HttpStatus.BAD_REQUEST, "etrade-account-error"),
	ETRADE_POSITION_ERROR(HttpStatus.BAD_REQUEST, "etrade-position-error"),
	ETRADE_TRANSACTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "etrade-transaction-error"),
	ETRADE_SIGNATURE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "etrade-signature-error");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	EtradeErrors(HttpStatus httpStatus, String propertyKey) {
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
