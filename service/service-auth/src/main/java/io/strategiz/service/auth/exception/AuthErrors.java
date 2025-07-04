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
    QR_GENERATION_FAILED,
    
    // User errors  
    USER_NOT_FOUND

} 