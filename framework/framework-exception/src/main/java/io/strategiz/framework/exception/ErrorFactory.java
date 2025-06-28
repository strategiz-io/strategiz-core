package io.strategiz.framework.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory for creating standardized exceptions across the application.
 * Provides domain-specific convenience methods for common error scenarios.
 */
@Component
public class ErrorFactory {
    private static final Logger log = LoggerFactory.getLogger(ErrorFactory.class);
    
    // Authentication errors
    
    /**
     * Create an exception for failed TOTP verification
     * 
     * @param username the username
     * @param attemptsRemaining remaining verification attempts
     * @return a StrategizException
     */
    public StrategizException totpVerificationFailed(String username, int attemptsRemaining) {
        log.debug("TOTP verification failed for user {}, attempts remaining: {}", 
                 username, attemptsRemaining);
                 
        return ErrorDefinition.TOTP_VERIFICATION_FAILED
            .createException(attemptsRemaining)
            .withContext("username", username);
    }
    
    /**
     * Create an exception for invalid credentials
     * 
     * @param username the username
     * @return a StrategizException
     */
    public StrategizException invalidCredentials(String username) {
        log.debug("Invalid credentials for user: {}", username);
        
        return ErrorDefinition.INVALID_CREDENTIALS
            .createException()
            .withContext("username", username);
    }
    
    // Portfolio errors
    
    /**
     * Create an exception for a portfolio not found
     * 
     * @param portfolioId the portfolio ID
     * @return a StrategizException
     */
    public StrategizException portfolioNotFound(String portfolioId) {
        log.debug("Portfolio not found: {}", portfolioId);
        
        return ErrorDefinition.PORTFOLIO_NOT_FOUND
            .createException(portfolioId);
    }
    
    /**
     * Create an exception for unauthorized portfolio access
     * 
     * @param portfolioId the portfolio ID
     * @param username the username attempting access
     * @return a StrategizException
     */
    public StrategizException portfolioAccessDenied(String portfolioId, String username) {
        log.debug("Portfolio access denied. User: {}, Portfolio: {}", username, portfolioId);
        
        return ErrorDefinition.PORTFOLIO_ACCESS_DENIED
            .createException(portfolioId)
            .withContext("username", username);
    }
    
    // User errors
    
    /**
     * Create an exception for a user not found
     * 
     * @param userId the user ID
     * @return a StrategizException
     */
    public StrategizException userNotFound(String userId) {
        log.debug("User not found: {}", userId);
        
        return ErrorDefinition.USER_NOT_FOUND
            .createException(userId);
    }
    
    /**
     * Create an exception for a user that already exists
     * 
     * @param userId the user ID
     * @return a StrategizException
     */
    public StrategizException userAlreadyExists(String userId) {
        log.debug("User already exists: {}", userId);
        
        return ErrorDefinition.USER_ALREADY_EXISTS
            .createException(userId);
    }
    
    // Generic errors
    
    /**
     * Create an exception for validation failures
     * 
     * @param details validation error details
     * @return a StrategizException
     */
    public StrategizException validationFailed(String details) {
        log.debug("Validation failed: {}", details);
        
        return ErrorDefinition.VALIDATION_FAILED
            .createException(details);
    }
    
    /**
     * Create an exception for resource not found
     * 
     * @param resourceType the type of resource
     * @param resourceId the resource ID
     * @return a StrategizException
     */
    public StrategizException resourceNotFound(String resourceType, String resourceId) {
        log.debug("Resource not found. Type: {}, ID: {}", resourceType, resourceId);
        
        return ErrorDefinition.RESOURCE_NOT_FOUND
            .createException(resourceType, resourceId);
    }
    
    /**
     * Create an exception for internal errors
     * 
     * @param details error details
     * @param cause the underlying cause
     * @return a StrategizException
     */
    public StrategizException internalError(String details, Throwable cause) {
        log.error("Internal error: {}", details, cause);
        
        return new StrategizException(ErrorDefinition.INTERNAL_ERROR, cause, details);
    }
}
