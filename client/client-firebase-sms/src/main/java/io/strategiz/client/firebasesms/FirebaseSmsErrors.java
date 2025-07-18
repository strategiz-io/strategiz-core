package io.strategiz.client.firebasesms;

/**
 * Error codes for Firebase SMS client operations
 * 
 * Usage: throw new StrategizException(FirebaseSmsErrors.SMS_SEND_FAILED);
 */
public enum FirebaseSmsErrors {
    SMS_SEND_FAILED,
    SMS_SERVICE_UNAVAILABLE,
    SMS_CONFIGURATION_ERROR,
    SMS_RATE_LIMITED,
    INVALID_PHONE_NUMBER,
    NETWORK_ERROR
}