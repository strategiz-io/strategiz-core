package io.strategiz.framework.exception;

/**
 * Base exception class for all Strategiz application exceptions.
 * Enforces the use of enum error codes for consistency.
 */
public class StrategizException extends RuntimeException {
    
    private final String errorCode;
    
    /**
     * Create exception with enum error code (enforces type safety)
     */
    public StrategizException(Enum<?> errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode.name();
    }
    
    /**
     * Create exception with enum error code and custom message
     */
    public StrategizException(Enum<?> errorCode, String message) {
        super(message);
        this.errorCode = errorCode.name();
    }
    
    /**
     * Create exception with enum error code, message, and cause
     */
    public StrategizException(Enum<?> errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode.name();
    }
    
    /**
     * Create exception with enum error code and cause
     */
    public StrategizException(Enum<?> errorCode, Throwable cause) {
        super(errorCode.name(), cause);
        this.errorCode = errorCode.name();
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
