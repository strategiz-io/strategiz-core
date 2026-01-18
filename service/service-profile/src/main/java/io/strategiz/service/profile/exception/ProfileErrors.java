package io.strategiz.service.profile.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Profile service error details for use with StrategizException.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, MODULE_NAME, userId);
 */
public enum ProfileErrors implements ErrorDetails {

	// Profile validation errors
	PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "profile-already-exists"),
	PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "profile-not-found"),
	PROFILE_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "profile-creation-failed"),
	PROFILE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "profile-update-failed"),
	PROFILE_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "profile-validation-failed"),
	PROFILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "profile-access-denied"),

	// Market data errors
	MARKET_DATA_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "profile-market-data-fetch-failed"),

	// Owner subscription errors
	SUBSCRIPTION_SETTINGS_NOT_FOUND(HttpStatus.NOT_FOUND, "subscription-settings-not-found"),
	SUBSCRIPTION_NOT_ENABLED(HttpStatus.BAD_REQUEST, "subscription-not-enabled"),
	SUBSCRIPTION_ALREADY_ENABLED(HttpStatus.CONFLICT, "subscription-already-enabled"),
	STRIPE_NOT_CONNECTED(HttpStatus.BAD_REQUEST, "stripe-not-connected"),
	INVALID_SUBSCRIPTION_PRICE(HttpStatus.BAD_REQUEST, "invalid-subscription-price"),
	INVALID_PROFILE_PITCH(HttpStatus.BAD_REQUEST, "invalid-profile-pitch"),
	SUBSCRIPTION_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "subscription-update-failed");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	ProfileErrors(HttpStatus httpStatus, String propertyKey) {
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
 