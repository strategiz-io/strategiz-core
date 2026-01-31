package io.strategiz.service.auth.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Authentication Service error details enum that maps to service-auth-errors.properties.
 * Each enum constant references a property key in the properties file.
 *
 * The actual error messages (code, message, developer, more-info) are defined in the
 * properties file for easy modification without code changes.
 */
public enum ServiceAuthErrorDetails implements ErrorDetails {

	// Authentication errors
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid-credentials"),
	ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "account-locked"),
	MFA_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "mfa-required"),
	SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "session-expired"),
	SESSION_INVALID(HttpStatus.UNAUTHORIZED, "session-invalid"),

	// Token errors
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "invalid-token"), TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "token-revoked"),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "invalid-refresh-token"),
	REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "refresh-token-expired"),

	// Passkey errors
	PASSKEY_CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "passkey-challenge-not-found"),
	PASSKEY_REGISTRATION_FAILED(HttpStatus.BAD_REQUEST, "passkey-registration-failed"),
	PASSKEY_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "passkey-verification-failed"),
	PASSKEY_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "passkey-authentication-failed"),
	PASSKEY_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "passkey-retrieval-failed"),
	PASSKEY_DELETION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "passkey-deletion-failed"),
	PASSKEY_UNAUTHORIZED(HttpStatus.FORBIDDEN, "passkey-unauthorized"),

	// TOTP errors
	TOTP_INVALID_CODE(HttpStatus.BAD_REQUEST, "totp-invalid-code"),
	TOTP_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "totp-verification-failed"),
	TOTP_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "totp-registration-failed"),
	QR_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "qr-generation-failed"),

	// OTP errors
	OTP_EXPIRED(HttpStatus.BAD_REQUEST, "otp-expired"), OTP_NOT_FOUND(HttpStatus.NOT_FOUND, "otp-not-found"),
	OTP_MAX_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "otp-max-attempts-exceeded"),
	OTP_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "otp-rate-limited"),
	OTP_ALREADY_USED(HttpStatus.BAD_REQUEST, "otp-already-used"),
	EMAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "email-send-failed"),
	SMS_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "sms-send-failed"),
	SMS_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "sms-service-unavailable"),
	INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "invalid-phone-number"),
	INVALID_EMAIL(HttpStatus.BAD_REQUEST, "invalid-email"),

	// OAuth errors
	OAUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "oauth-access-denied"),
	OAUTH_INVALID_GRANT(HttpStatus.BAD_REQUEST, "oauth-invalid-grant"),
	OAUTH_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "oauth-rate-limited"),
	OAUTH_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "oauth-service-unavailable"),
	OAUTH_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "oauth-configuration-error"),
	OAUTH_INVALID_STATE(HttpStatus.BAD_REQUEST, "oauth-invalid-state"),

	// User errors
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "user-not-found"), INVALID_USER_ID(HttpStatus.BAD_REQUEST, "invalid-user-id"),
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "email-already-exists"),
	SIGNUP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "signup-failed"),
	USER_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "user-creation-failed"),

	// Verification errors
	VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "verification-failed"),
	VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "verification-expired"),
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "validation-failed"),

	// System errors
	EXTERNAL_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "external-service-error"),
	CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "configuration-error"),

	// Device trust errors
	DEVICE_TRUST_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "device-trust-verification-failed"),
	DEVICE_TRUST_CHALLENGE_FAILED(HttpStatus.BAD_REQUEST, "device-trust-challenge-failed"),
	DEVICE_TRUST_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "device-trust-authentication-failed"),
	DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "device-not-found"),

	// Additional token error (for SessionController)
	REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "invalid-refresh-token");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	ServiceAuthErrorDetails(HttpStatus httpStatus, String propertyKey) {
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