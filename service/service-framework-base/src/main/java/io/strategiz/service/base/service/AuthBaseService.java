package io.strategiz.service.base.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Base service for all authentication-related services.
 * Provides common auth patterns following clean architecture.
 * 
 * Services should return data objects for success cases and throw StrategizException for errors.
 */
public abstract class AuthBaseService {
    
    protected final Logger authLog = LoggerFactory.getLogger("AUTH." + getClass().getSimpleName());
    
    private static final SecureRandom random = new SecureRandom();
    private static final int DEFAULT_CODE_LENGTH = 6;
    private static final int DEFAULT_CODE_EXPIRY_MINUTES = 5;
    
    /**
     * Generate a secure random code for auth purposes
     */
    protected String generateSecureCode() {
        return generateSecureCode(DEFAULT_CODE_LENGTH);
    }
    
    /**
     * Generate a secure random code with specified length
     */
    protected String generateSecureCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        
        String generatedCode = code.toString();
        authLog.debug("Generated secure code for method: {}, length: {}", getAuthMethodId(), length);
        return generatedCode;
    }
    
    /**
     * Check if a timestamp-based code has expired
     */
    protected boolean isCodeExpired(Date timestamp, int expiryMinutes) {
        if (timestamp == null) {
            return true;
        }
        
        Instant expiryTime = timestamp.toInstant().plus(expiryMinutes, ChronoUnit.MINUTES);
        boolean expired = Instant.now().isAfter(expiryTime);
        
        if (expired) {
            authLog.debug("Code expired for method: {}, timestamp: {}, expiry: {} minutes", 
                getAuthMethodId(), timestamp, expiryMinutes);
        }
        
        return expired;
    }
    
    /**
     * Log authentication attempt for audit/security purposes
     */
    protected void logAuthAttempt(String userId, String action, boolean success) {
        if (success) {
            authLog.info("AUTH_SUCCESS - User: {}, Method: {}, Action: {}", 
                userId, getAuthMethodId(), action);
        } else {
            authLog.warn("AUTH_FAILURE - User: {}, Method: {}, Action: {}", 
                userId, getAuthMethodId(), action);
        }
    }
    
    /**
     * Log security events for monitoring
     */
    protected void logSecurityEvent(String userId, String event, String details) {
        authLog.warn("SECURITY_EVENT - User: {}, Method: {}, Event: {}, Details: {}", 
            userId, getAuthMethodId(), event, details);
    }
    
    /**
     * Validate that the auth method is properly configured
     */
    protected void validateAuthMethodConfiguration() {
        authLog.debug("Validating configuration for auth method: {}", getAuthMethodId());
        // Override in subclasses for specific validation
    }
    
    /**
     * Get the authentication method ID for this service
     * Each auth service must implement this
     */
    protected abstract String getAuthMethodId();
    
    /**
     * Get the authentication method display name
     * Each auth service must implement this
     */
    protected abstract String getAuthMethodName();
} 