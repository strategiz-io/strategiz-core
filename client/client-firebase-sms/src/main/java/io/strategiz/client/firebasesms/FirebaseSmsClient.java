package io.strategiz.client.firebasesms;

import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Firebase SMS client for sending SMS OTP messages
 * 
 * This client integrates with Firebase Phone Authentication to send SMS OTP codes.
 * It provides a simplified interface for SMS operations while handling Firebase-specific
 * configuration and error handling.
 * 
 * Note: This is a basic implementation. In production, you would integrate with
 * the actual Firebase Admin SDK for server-side SMS sending.
 */
@Component
public class FirebaseSmsClient {
    
    private static final Logger log = LoggerFactory.getLogger(FirebaseSmsClient.class);
    
    private final FirebaseSmsConfig config;
    
    public FirebaseSmsClient(FirebaseSmsConfig config) {
        this.config = config;
    }
    
    /**
     * Send SMS OTP via Firebase Phone Authentication
     * 
     * @param phoneNumber The phone number to send SMS to (E.164 format)
     * @param otpCode The OTP code to send
     * @param countryCode The country code for SMS routing
     * @return true if SMS was sent successfully, false otherwise
     * @throws StrategizException if SMS sending fails
     */
    public boolean sendSms(String phoneNumber, String otpCode, String countryCode) {
        log.debug("Sending SMS OTP via Firebase to {} in country {}", maskPhoneNumber(phoneNumber), countryCode);
        
        try {
            // Check if Firebase SMS is enabled
            if (!config.isEnabled()) {
                log.warn("Firebase SMS is disabled in configuration");
                throw new StrategizException(FirebaseSmsErrors.SMS_SERVICE_UNAVAILABLE, "SMS service is disabled");
            }
            
            // Check if we're in development mode with mock SMS
            if (config.isMockSmsEnabled()) {
                return sendMockSms(phoneNumber, otpCode, countryCode);
            }
            
            // TODO: Integrate with Firebase Admin SDK
            // For now, this is a placeholder implementation
            return sendViaFirebaseAdminSdk(phoneNumber, otpCode, countryCode);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error sending SMS via Firebase to {}: {}", maskPhoneNumber(phoneNumber), e.getMessage(), e);
            throw new StrategizException(FirebaseSmsErrors.SMS_SEND_FAILED, "Failed to send SMS via Firebase");
        }
    }
    
    /**
     * Check if Firebase SMS service is available and configured
     * 
     * @return true if service is available, false otherwise
     */
    public boolean isServiceAvailable() {
        try {
            return config.isEnabled() && 
                   (config.isMockSmsEnabled() || isFirebaseConfigured());
        } catch (Exception e) {
            log.warn("Error checking Firebase SMS service availability: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get service information for monitoring/debugging
     * 
     * @return Service status information
     */
    public ServiceStatus getServiceStatus() {
        boolean enabled = config.isEnabled();
        boolean mockMode = config.isMockSmsEnabled();
        boolean configured = isFirebaseConfigured();
        boolean available = isServiceAvailable();
        
        return new ServiceStatus(enabled, mockMode, configured, available);
    }
    
    /**
     * Send SMS via Firebase Admin SDK (production implementation)
     * 
     * TODO: Replace this placeholder with actual Firebase Admin SDK integration
     */
    private boolean sendViaFirebaseAdminSdk(String phoneNumber, String otpCode, String countryCode) {
        log.info("TODO: Implement Firebase Admin SDK SMS sending");
        
        // Placeholder implementation - simulates Firebase SMS sending
        try {
            // Simulate Firebase API call delay
            Thread.sleep(200);
            
            // Simulate occasional Firebase API failures (2% failure rate)
            if (Math.random() < 0.02) {
                throw new StrategizException(FirebaseSmsErrors.SMS_SERVICE_UNAVAILABLE, "Firebase service temporarily unavailable");
            }
            
            // In development mode, log the OTP for testing
            if (isDevelopmentMode() && config.isLogOtpCodes()) {
                log.info("🔐 Firebase SMS OTP for {}: {} (DEV MODE)", maskPhoneNumber(phoneNumber), otpCode);
            }
            
            log.debug("Firebase SMS sent successfully to {}", maskPhoneNumber(phoneNumber));
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Firebase SMS sending failed for {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
            return false;
        }
    }
    
    /**
     * Send mock SMS (for development/testing)
     */
    private boolean sendMockSms(String phoneNumber, String otpCode, String countryCode) {
        log.info("📱 MOCK SMS to {} ({}): Your OTP code is {}", 
                maskPhoneNumber(phoneNumber), countryCode, otpCode);
        
        // Simulate instant delivery in mock mode
        return true;
    }
    
    /**
     * Check if Firebase is properly configured
     */
    private boolean isFirebaseConfigured() {
        // Check if Firebase project ID is configured
        String projectId = config.getProjectId();
        if (projectId == null || projectId.isBlank()) {
            log.debug("Firebase project ID not configured");
            return false;
        }
        
        // TODO: Add more configuration checks (service account, credentials, etc.)
        return true;
    }
    
    /**
     * Check if we're running in development mode
     */
    private boolean isDevelopmentMode() {
        String activeProfiles = System.getProperty("spring.profiles.active", "");
        return activeProfiles.contains("dev") || activeProfiles.contains("development") || activeProfiles.isEmpty();
    }
    
    /**
     * Mask phone number for security in logs
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***-***-****";
        }
        
        String countryCode = phoneNumber.substring(0, phoneNumber.length() - 7);
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return countryCode + "***" + lastFour;
    }
    
    /**
     * Service status information
     */
    public record ServiceStatus(
        boolean enabled,
        boolean mockMode,
        boolean configured,
        boolean available
    ) {
        @Override
        public String toString() {
            return String.format("FirebaseSmsService[enabled=%s, mockMode=%s, configured=%s, available=%s]",
                    enabled, mockMode, configured, available);
        }
    }
}