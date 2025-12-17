package io.strategiz.service.device.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for device-related exceptions.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(DeviceErrorDetails.DEVICE_NOT_FOUND, MODULE_NAME,
 * deviceId);
 */
public enum DeviceErrorDetails implements ErrorDetails {

	DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "device-not-found"),
	DEVICE_NOT_ANONYMOUS(HttpStatus.BAD_REQUEST, "device-not-anonymous"),
	DEVICE_NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "device-not-authenticated"),
	DEVICE_ALREADY_EXISTS(HttpStatus.CONFLICT, "device-already-exists"),
	INVALID_DEVICE_ID(HttpStatus.BAD_REQUEST, "device-invalid-id"),
	INVALID_USER_ID(HttpStatus.BAD_REQUEST, "device-invalid-user-id"),
	DEVICE_FINGERPRINT_MISMATCH(HttpStatus.FORBIDDEN, "device-fingerprint-mismatch"),
	DEVICE_TRUST_VIOLATION(HttpStatus.FORBIDDEN, "device-trust-violation"),
	DEVICE_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "device-registration-failed"),
	DEVICE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "device-update-failed"),
	DEVICE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "device-delete-failed");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	DeviceErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
