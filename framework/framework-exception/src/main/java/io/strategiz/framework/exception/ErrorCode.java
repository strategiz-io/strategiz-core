package io.strategiz.framework.exception;

/**
 * Standard error codes with associated HTTP status codes for the Strategiz platform.
 * These are used to create consistent error responses across all services.
 */
public enum ErrorCode {
    // 4xx Client Errors
    VALIDATION_ERROR(400, "Validation failed"),
    AUTHENTICATION_ERROR(401, "Authentication failed"),
    AUTHORIZATION_ERROR(403, "Access denied"),
    RESOURCE_NOT_FOUND(404, "Resource not found"),
    METHOD_NOT_ALLOWED(405, "Method not allowed"),
    CONFLICT(409, "Resource conflict"),
    PRECONDITION_FAILED(412, "Precondition failed"),
    TOO_MANY_REQUESTS(429, "Too many requests"),
    
    // 5xx Server Errors
    INTERNAL_ERROR(500, "Internal server error"),
    NOT_IMPLEMENTED(501, "Not implemented"),
    SERVICE_UNAVAILABLE(503, "Service unavailable"),
    
    // Custom error codes
    BUSINESS_RULE_VIOLATION(400, "Business rule violation"),
    DATA_INTEGRITY_ERROR(400, "Data integrity error"),
    EXTERNAL_SERVICE_ERROR(502, "External service error");
    
    private final int httpStatus;
    private final String defaultMessage;
    
    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
    
    /**
     * Get the HTTP status code associated with this error code
     * @return the HTTP status code as an integer
     */
    public int getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * Get the default message for this error code
     * @return the default message
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
