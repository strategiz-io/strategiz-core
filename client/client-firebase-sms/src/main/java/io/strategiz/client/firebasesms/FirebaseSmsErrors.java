package io.strategiz.client.firebasesms;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error codes for Firebase SMS client operations.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(FirebaseSmsErrors.SMS_SEND_FAILED, MODULE_NAME);
 */
public enum FirebaseSmsErrors implements ErrorDetails {

	SMS_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "sms-send-failed"),
	SMS_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "sms-service-unavailable"),
	SMS_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "sms-configuration-error"),
	SMS_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "sms-rate-limited"),
	INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "sms-invalid-phone-number"),
	NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "sms-network-error");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	FirebaseSmsErrors(HttpStatus httpStatus, String propertyKey) {
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
