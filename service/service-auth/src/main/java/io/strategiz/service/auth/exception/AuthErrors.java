package io.strategiz.service.auth.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Authentication module error constants for use with StrategizException.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * @deprecated Consider using ServiceAuthErrorDetails for new code. This enum is
 *             maintained for backwards compatibility.
 *
 *             Usage: throw new StrategizException(AuthErrors.INVALID_CREDENTIALS,
 *             MODULE_NAME);
 */
public enum AuthErrors implements ErrorDetails {

	// Authentication errors
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "auth-invalid-credentials"),
	ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "auth-account-locked"),
	MFA_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "auth-mfa-required"),
	SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "auth-session-expired"),
	SESSION_INVALID(HttpStatus.UNAUTHORIZED, "auth-session-invalid"),

	// Token errors
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "auth-invalid-token"),
	TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "auth-token-revoked"),

	// Verification errors
	VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "auth-verification-failed"),
	VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "auth-verification-expired"),
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "auth-validation-failed"),

	// Passkey errors
	PASSKEY_CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "auth-passkey-challenge-not-found"),
	PASSKEY_REGISTRATION_FAILED(HttpStatus.BAD_REQUEST, "auth-passkey-registration-failed"),
	PASSKEY_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "auth-passkey-verification-failed"),
	PASSKEY_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "auth-passkey-authentication-failed"),
	PASSKEY_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "auth-passkey-retrieval-failed"),
	PASSKEY_DELETION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "auth-passkey-deletion-failed"),
	PASSKEY_UNAUTHORIZED(HttpStatus.FORBIDDEN, "auth-passkey-unauthorized"),

	// TOTP errors
	TOTP_INVALID_CODE(HttpStatus.BAD_REQUEST, "auth-totp-invalid-code"),
	TOTP_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "auth-totp-verification-failed"),
	TOTP_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "auth-totp-registration-failed"),
	QR_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "auth-qr-generation-failed"),

	// OTP errors (Email & SMS)
	OTP_EXPIRED(HttpStatus.BAD_REQUEST, "auth-otp-expired"),
	OTP_NOT_FOUND(HttpStatus.NOT_FOUND, "auth-otp-not-found"),
	OTP_MAX_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "auth-otp-max-attempts"),
	OTP_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "auth-otp-rate-limited"),
	OTP_ALREADY_USED(HttpStatus.BAD_REQUEST, "auth-otp-already-used"),
	EMAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "auth-email-send-failed"),
	SMS_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "auth-sms-send-failed"),
	SMS_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "auth-sms-service-unavailable"),
	INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "auth-invalid-phone-number"),
	INVALID_EMAIL(HttpStatus.BAD_REQUEST, "auth-invalid-email"),
	INVALID_OTP_FORMAT(HttpStatus.BAD_REQUEST, "auth-invalid-otp-format"),

	// OAuth errors
	OAUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "auth-oauth-access-denied"),
	OAUTH_INVALID_GRANT(HttpStatus.BAD_REQUEST, "auth-oauth-invalid-grant"),
	OAUTH_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "auth-oauth-rate-limited"),
	OAUTH_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "auth-oauth-service-unavailable"),
	OAUTH_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "auth-oauth-configuration-error"),
	OAUTH_INVALID_STATE(HttpStatus.BAD_REQUEST, "auth-oauth-invalid-state"),

	// Session errors
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "auth-invalid-refresh-token"),
	REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "auth-refresh-token-expired"),
	INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "auth-invalid-access-token"),
	ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "auth-access-token-expired"),

	// General errors
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "auth-user-not-found"),
	INVALID_USER_ID(HttpStatus.BAD_REQUEST, "auth-invalid-user-id"),

	// Signup errors
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "auth-email-already-exists"),
	SIGNUP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "auth-signup-failed"),
	USER_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "auth-user-creation-failed"),

	// Fraud detection errors
	FRAUD_DETECTED(HttpStatus.FORBIDDEN, "auth-fraud-detected"),
	RECAPTCHA_FAILED(HttpStatus.BAD_REQUEST, "auth-recaptcha-failed");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	AuthErrors(HttpStatus httpStatus, String propertyKey) {
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
 