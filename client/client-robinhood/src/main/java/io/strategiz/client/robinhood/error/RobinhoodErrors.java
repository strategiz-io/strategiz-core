package io.strategiz.client.robinhood.error;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error codes specific to Robinhood API client operations.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(RobinhoodErrors.ROBINHOOD_API_ERROR, MODULE_NAME);
 */
public enum RobinhoodErrors implements ErrorDetails {

	ROBINHOOD_API_ERROR(HttpStatus.BAD_GATEWAY, "robinhood-api-error"),
	ROBINHOOD_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "robinhood-auth-failed"),
	ROBINHOOD_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "robinhood-invalid-credentials"),
	ROBINHOOD_MFA_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "robinhood-mfa-required"),
	ROBINHOOD_MFA_FAILED(HttpStatus.UNAUTHORIZED, "robinhood-mfa-failed"),
	ROBINHOOD_DEVICE_APPROVAL_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "robinhood-device-approval-required"),
	ROBINHOOD_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "robinhood-invalid-response"),
	ROBINHOOD_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "robinhood-rate-limited"),
	ROBINHOOD_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "robinhood-token-expired"),
	ROBINHOOD_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "robinhood-invalid-token"),
	ROBINHOOD_NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "robinhood-network-error"),
	ROBINHOOD_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "robinhood-configuration-error"),
	ROBINHOOD_ACCOUNT_ERROR(HttpStatus.BAD_REQUEST, "robinhood-account-error"),
	ROBINHOOD_POSITION_ERROR(HttpStatus.BAD_REQUEST, "robinhood-position-error"),
	ROBINHOOD_CHALLENGE_EXPIRED(HttpStatus.GONE, "robinhood-challenge-expired");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	RobinhoodErrors(HttpStatus httpStatus, String propertyKey) {
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

