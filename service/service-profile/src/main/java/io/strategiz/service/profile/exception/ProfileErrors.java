package io.strategiz.service.profile.exception;

/**
 * Profile service error constants for use with StrategizException.
 * Simplified enum approach for type-safe error codes.
 * 
 * Usage: throw new StrategizException(ProfileErrors.PROFILE_ALREADY_EXISTS, context);
 */
public enum ProfileErrors {
    
    // Profile validation errors
    PROFILE_ALREADY_EXISTS,
    PROFILE_NOT_FOUND,
    PROFILE_CREATION_FAILED,
    PROFILE_UPDATE_FAILED,
    PROFILE_VALIDATION_FAILED,
    PROFILE_ACCESS_DENIED

} 