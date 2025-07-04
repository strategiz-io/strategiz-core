package io.strategiz.service.auth.example;

import io.strategiz.framework.exception.StrategizException;

/**
 * Example showing the clean properties-based exception approach
 */
public class CleanExceptionExample {

    // Example usage - framework will be available at runtime
    public void demonstrateCleanExceptions() {
        String email = "user@example.com";
        
        // Invalid credentials - module prefix + error code + parameters
        // throw new StrategizException("AUTH_INVALID_CREDENTIALS", email);
        
        // Account locked
        // throw new StrategizException("AUTH_ACCOUNT_LOCKED", email);
        
        // MFA required  
        // throw new StrategizException("AUTH_MFA_REQUIRED", email);
        
        // Invalid TOTP code
        // throw new StrategizException("AUTH_TOTP_INVALID_CODE", email);
    }
}

/*
 * Usage is now super clean:
 * 
 * throw new StrategizException("AUTH_INVALID_CREDENTIALS", userEmail);
 * 
 * Results in:
 * HTTP 401 Unauthorized
 * {
 *   "code": "AUTH_INVALID_CREDENTIALS",
 *   "message": "The email or password you entered is incorrect",
 *   "developerMessage": "Authentication failed for user: user@example.com", 
 *   "moreInfo": "https://docs.strategiz.io/errors/authentication"
 * }
 * 
 * Benefits:
 * - Centralized error messages in properties files
 * - Module prefixes (AUTH_, PROVIDER_, DASHBOARD_)
 * - Message formatting with parameters {0}, {1}, etc.
 * - Internationalization ready
 * - Clean, minimal code
 * 
 * Error message properties format:
 * AUTH_INVALID_CREDENTIALS.message=The email or password you entered is incorrect
 * AUTH_INVALID_CREDENTIALS.developerMessage=Authentication failed for user: {0}
 * AUTH_INVALID_CREDENTIALS.moreInfo=https://docs.strategiz.io/errors/authentication
 */ 