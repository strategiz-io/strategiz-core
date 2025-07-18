package io.strategiz.service.auth.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Authentication error definitions with HTTP status and property key mapping.
 * 
 * Each error maps to properties in auth-errors.properties file which contains:
 * - Error code
 * - User-friendly message  
 * - Developer message template with placeholders
 * - Documentation path
 * 
 * Usage:
 * throw new StrategizException(AuthErrorDetails.INVALID_CREDENTIALS, "service-auth", "password");
 * throw new StrategizException(AuthErrorDetails.SESSION_EXPIRED, "service-auth", Instant.now());
 */
public enum AuthErrorDetails implements ErrorDetails {
    
    // Authentication errors
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid-credentials"),
    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "account-locked"),
    MFA_REQUIRED(HttpStatus.UNAUTHORIZED, "mfa-required"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "authentication-failed"),
    
    // Session errors
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "session-expired"),
    SESSION_INVALID(HttpStatus.UNAUTHORIZED, "session-invalid"),
    SESSION_NOT_FOUND(HttpStatus.UNAUTHORIZED, "session-not-found"),
    
    // Token errors
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "invalid-token"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "token-expired"),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "token-revoked"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "refresh-token-invalid"),
    
    // Verification errors
    VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "verification-failed"),
    VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "verification-expired"),
    VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "verification-code-invalid"),
    
    // Passkey errors
    PASSKEY_CHALLENGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "passkey-challenge-not-found"),
    PASSKEY_REGISTRATION_FAILED(HttpStatus.BAD_REQUEST, "passkey-registration-failed"),
    PASSKEY_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "passkey-authentication-failed"),
    PASSKEY_NOT_FOUND(HttpStatus.NOT_FOUND, "passkey-not-found"),
    
    // TOTP errors
    TOTP_INVALID_CODE(HttpStatus.BAD_REQUEST, "totp-invalid-code"),
    TOTP_NOT_ENABLED(HttpStatus.BAD_REQUEST, "totp-not-enabled"),
    TOTP_ALREADY_ENABLED(HttpStatus.BAD_REQUEST, "totp-already-enabled"),
    QR_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "qr-generation-failed"),
    
    // SMS/Email OTP errors
    SMS_SEND_FAILED(HttpStatus.BAD_GATEWAY, "sms-send-failed"),
    EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "email-send-failed"),
    OTP_SEND_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "otp-send-rate-limited"),
    
    // UserEntity errors
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "user-not-found"),
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "user-already-exists"),
    
    // OAuth errors
    OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "oauth-authentication-failed"),
    OAUTH_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "oauth-user-info-failed"),
    OAUTH_STATE_MISMATCH(HttpStatus.BAD_REQUEST, "oauth-state-mismatch"),
    
    // Validation errors
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "validation-failed"),
    INVALID_REQUEST_FORMAT(HttpStatus.BAD_REQUEST, "invalid-request-format"),
    
    // Rate limiting
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "rate-limited"),
    
    // External service errors
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "external-service-error"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "database-error"),
    
    // Configuration errors
    CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "configuration-error");
    
    private final HttpStatus httpStatus;
    private final String propertyKey;
    
    AuthErrorDetails(HttpStatus httpStatus, String propertyKey) {
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