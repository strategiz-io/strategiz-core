package io.strategiz.business.preferences.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for user preferences operations. Implements ErrorDetails for integration
 * with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(PreferencesErrorDetails.XXX,
 * "business-preferences");
 */
public enum PreferencesErrorDetails implements ErrorDetails {

	// === Alert Preferences Errors ===
	INVALID_NOTIFICATION_CHANNEL(HttpStatus.BAD_REQUEST, "prefs-invalid-notification-channel"),
	INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "prefs-invalid-phone-number"),
	SMS_NOT_CONFIGURED(HttpStatus.BAD_REQUEST, "prefs-sms-not-configured"),
	INVALID_QUIET_HOURS(HttpStatus.BAD_REQUEST, "prefs-invalid-quiet-hours"),
	INVALID_TIME_WINDOW(HttpStatus.BAD_REQUEST, "prefs-invalid-time-window"),
	INVALID_TIME_FORMAT(HttpStatus.BAD_REQUEST, "prefs-invalid-time-format"),
	INVALID_TIMEZONE(HttpStatus.BAD_REQUEST, "prefs-invalid-timezone"),
	INVALID_FREQUENCY(HttpStatus.BAD_REQUEST, "prefs-invalid-frequency"),
	PREFERENCES_NOT_FOUND(HttpStatus.NOT_FOUND, "prefs-not-found"),

	// === AI Preferences Errors ===
	INVALID_MODEL(HttpStatus.BAD_REQUEST, "prefs-invalid-model"),
	INVALID_TEMPERATURE(HttpStatus.BAD_REQUEST, "prefs-invalid-temperature"),
	INVALID_MAX_TOKENS(HttpStatus.BAD_REQUEST, "prefs-invalid-max-tokens"),
	MODEL_NOT_ALLOWED(HttpStatus.FORBIDDEN, "prefs-model-not-allowed"),

	// === General Preference Errors ===
	PREFERENCE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "prefs-update-failed"),
	PREFERENCE_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "prefs-creation-failed"),
	PREFERENCE_DELETION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "prefs-deletion-failed"),
	INVALID_PREFERENCE_VALUE(HttpStatus.BAD_REQUEST, "prefs-invalid-value"),

	// === Subscription Preferences Errors ===
	INVALID_TIER(HttpStatus.BAD_REQUEST, "prefs-invalid-tier"),
	TIER_DOWNGRADE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "prefs-tier-downgrade-not-allowed"),
	SUBSCRIPTION_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "prefs-subscription-limit-exceeded"),

	// === Credit/Usage Errors ===
	INSUFFICIENT_CREDITS(HttpStatus.PAYMENT_REQUIRED, "prefs-insufficient-credits"),
	CREDITS_EXHAUSTED(HttpStatus.PAYMENT_REQUIRED, "prefs-credits-exhausted"),
	USAGE_LIMIT_WARNING(HttpStatus.OK, "prefs-usage-limit-warning"),
	USAGE_LIMIT_CRITICAL(HttpStatus.OK, "prefs-usage-limit-critical"),
	TRIAL_EXPIRED(HttpStatus.PAYMENT_REQUIRED, "prefs-trial-expired");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	PreferencesErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
