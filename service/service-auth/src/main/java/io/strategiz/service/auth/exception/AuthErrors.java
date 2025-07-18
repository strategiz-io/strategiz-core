package io.strategiz.service.auth.exception;

/**
 * Authentication module error constants for use with StrategizException.
 * Simplified enum approach for type-safe error codes.
 * 
 * Usage: throw new StrategizException(AuthErrors.INVALID_CREDENTIALS, context);
 */
public enum AuthErrors {
    
    // Authentication errors
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED,
    MFA_REQUIRED,
    SESSION_EXPIRED,
    SESSION_INVALID,
    
    // Token errors
    INVALID_TOKEN,
    TOKEN_REVOKED,
    
    // Verification errors
    VERIFICATION_FAILED,
    VERIFICATION_EXPIRED,
    VALIDATION_FAILED,
    
    // Passkey errors
    PASSKEY_CHALLENGE_NOT_FOUND,
    PASSKEY_REGISTRATION_FAILED,
    
    // TOTP errors
    TOTP_INVALID_CODE,
    TOTP_VERIFICATION_FAILED,
    TOTP_REGISTRATION_FAILED,
    QR_GENERATION_FAILED,
    
    // OTP errors (Email & SMS)
    OTP_EXPIRED,
    OTP_NOT_FOUND,
    OTP_MAX_ATTEMPTS_EXCEEDED,
    OTP_RATE_LIMITED,
    OTP_ALREADY_USED,
    EMAIL_SEND_FAILED,
    SMS_SEND_FAILED,
    SMS_SERVICE_UNAVAILABLE,
    INVALID_PHONE_NUMBER,
    INVALID_EMAIL,
    INVALID_OTP_FORMAT,
    
    // OAuth errors
    OAUTH_ACCESS_DENIED,
    OAUTH_INVALID_GRANT,
    OAUTH_RATE_LIMITED,
    OAUTH_SERVICE_UNAVAILABLE,
    OAUTH_CONFIGURATION_ERROR,
    OAUTH_INVALID_STATE,
    
    // Session errors
    INVALID_REFRESH_TOKEN,
    REFRESH_TOKEN_EXPIRED,
    INVALID_ACCESS_TOKEN,
    ACCESS_TOKEN_EXPIRED,
    
    // Passkey errors (additional)
    PASSKEY_VERIFICATION_FAILED,
    PASSKEY_AUTHENTICATION_FAILED,
    PASSKEY_RETRIEVAL_FAILED,
    PASSKEY_DELETION_FAILED,
    PASSKEY_UNAUTHORIZED,
    
    // General errors
    USER_NOT_FOUND,
    INVALID_USER_ID,
    
    // Signup errors
    EMAIL_ALREADY_EXISTS,
    SIGNUP_FAILED,
    USER_CREATION_FAILED

} 