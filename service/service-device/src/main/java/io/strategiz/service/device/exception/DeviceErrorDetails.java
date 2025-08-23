package io.strategiz.service.device.exception;

/**
 * Error details for device-related exceptions
 */
public enum DeviceErrorDetails {
    DEVICE_NOT_FOUND("Device not found", "DEVICE_001"),
    DEVICE_NOT_ANONYMOUS("Device is not anonymous", "DEVICE_002"),
    DEVICE_NOT_AUTHENTICATED("Device is not authenticated", "DEVICE_003"),
    DEVICE_ALREADY_EXISTS("Device already exists", "DEVICE_004"),
    INVALID_DEVICE_ID("Invalid device ID", "DEVICE_005"),
    INVALID_USER_ID("Invalid user ID", "DEVICE_006"),
    DEVICE_FINGERPRINT_MISMATCH("Device fingerprint mismatch", "DEVICE_007"),
    DEVICE_TRUST_VIOLATION("Device trust violation", "DEVICE_008"),
    DEVICE_REGISTRATION_FAILED("Device registration failed", "DEVICE_009"),
    DEVICE_UPDATE_FAILED("Device update failed", "DEVICE_010");
    
    private final String message;
    private final String code;
    
    DeviceErrorDetails(String message, String code) {
        this.message = message;
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String toString() {
        return code + ": " + message;
    }
}