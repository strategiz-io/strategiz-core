package io.strategiz.framework.exception;

import java.text.MessageFormat;

/**
 * Enum-based error definitions for the Strategiz platform.
 * Each error definition includes a domain, error code, and message template.
 * 
 * Domains help categorize errors by functional area (auth, portfolio, etc.)
 * Message templates support parameter formatting for dynamic error messages.
 */
public enum ErrorDefinition {
    // Authentication/Authorization Errors
    TOTP_VERIFICATION_FAILED("auth", ErrorCode.AUTHENTICATION_ERROR, 
        "TOTP verification failed. Attempts remaining: {0}"),
    INVALID_CREDENTIALS("auth", ErrorCode.AUTHENTICATION_ERROR,
        "Invalid username or password"),
    SESSION_EXPIRED("auth", ErrorCode.AUTHENTICATION_ERROR,
        "Session has expired"),
    ACCOUNT_LOCKED("auth", ErrorCode.AUTHENTICATION_ERROR, 
        "Account is locked. Please contact support."),
    INSUFFICIENT_PERMISSIONS("auth", ErrorCode.AUTHORIZATION_ERROR,
        "You do not have permission to access this resource"),
        
    // Portfolio Errors
    PORTFOLIO_NOT_FOUND("portfolio", ErrorCode.RESOURCE_NOT_FOUND,
        "Portfolio with ID {0} was not found"),
    PORTFOLIO_ACCESS_DENIED("portfolio", ErrorCode.AUTHORIZATION_ERROR,
        "You do not have access to portfolio {0}"),
    INVALID_PORTFOLIO_OPERATION("portfolio", ErrorCode.VALIDATION_ERROR,
        "Invalid operation on portfolio: {0}"),
        
    // Strategy Errors
    STRATEGY_NOT_FOUND("strategy", ErrorCode.RESOURCE_NOT_FOUND,
        "Strategy with ID {0} was not found"),
    STRATEGY_EXECUTION_FAILED("strategy", ErrorCode.INTERNAL_ERROR,
        "Strategy execution failed: {0}"),
        
    // User Errors
    USER_NOT_FOUND("user", ErrorCode.RESOURCE_NOT_FOUND,
        "User with ID {0} was not found"),
    USER_ALREADY_EXISTS("user", ErrorCode.CONFLICT,
        "User with ID {0} already exists"),
        
    // Common Errors
    VALIDATION_FAILED("common", ErrorCode.VALIDATION_ERROR,
        "Validation failed: {0}"),
    RESOURCE_NOT_FOUND("common", ErrorCode.RESOURCE_NOT_FOUND,
        "Resource {0} with ID {1} was not found"),
    INTERNAL_ERROR("common", ErrorCode.INTERNAL_ERROR,
        "An internal error occurred: {0}"),
    EXTERNAL_SERVICE_ERROR("common", ErrorCode.EXTERNAL_SERVICE_ERROR,
        "External service error: {0}");
    
    private final String domain;
    private final ErrorCode errorCode;
    private final String messageTemplate;
    
    ErrorDefinition(String domain, ErrorCode errorCode, String messageTemplate) {
        this.domain = domain;
        this.errorCode = errorCode;
        this.messageTemplate = messageTemplate;
    }
    
    /**
     * Get the domain this error belongs to
     * @return the domain name
     */
    public String getDomain() {
        return domain;
    }
    
    /**
     * Get the error code associated with this error
     * @return the error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    /**
     * Get the message template for this error
     * @return the message template
     */
    public String getMessageTemplate() {
        return messageTemplate;
    }
    
    /**
     * Format the message template with the provided arguments
     * @param args arguments to format the message with
     * @return the formatted message
     */
    public String formatMessage(Object... args) {
        if (args == null || args.length == 0) {
            return messageTemplate;
        }
        return MessageFormat.format(messageTemplate, args);
    }
    
    /**
     * Create a new StrategizException with this error definition
     * @param args arguments to format the message with
     * @return a new StrategizException
     */
    public StrategizException createException(Object... args) {
        return new StrategizException(this, args);
    }
}
