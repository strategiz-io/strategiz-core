package io.strategiz.service.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for SMS OTP functionality
 * 
 * This configuration class contains settings for SMS OTP operations including
 * Firebase integration, rate limiting, and security parameters.
 */
@Configuration
@ConfigurationProperties(prefix = "strategiz.auth.sms-otp")
public class SmsOtpConfig {
    
    // Main SMS OTP settings
    private boolean enabled = true;
    private int otpExpiryMinutes = 5;
    private int maxVerificationAttempts = 5;
    private int rateLimitMinutes = 1;
    private int otpLength = 6;
    
    // Firebase-specific settings
    private boolean firebaseEnabled = true;
    private String firebaseProjectId;
    private String firebaseServiceAccountKeyPath;
    private long firebasePhoneAuthTimeoutSeconds = 60;
    private boolean firebaseAutoRetrieveEnabled = false;
    
    // Development mode settings
    private boolean devLogOtpCodes = true;
    private boolean devMockSmsEnabled = false;
    private String devFixedOtpCode = "123456";
    private boolean devSkipPhoneValidation = false;
    
    // Getters and Setters - Main settings
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getOtpExpiryMinutes() {
        return otpExpiryMinutes;
    }
    
    public void setOtpExpiryMinutes(int otpExpiryMinutes) {
        this.otpExpiryMinutes = otpExpiryMinutes;
    }
    
    public int getMaxVerificationAttempts() {
        return maxVerificationAttempts;
    }
    
    public void setMaxVerificationAttempts(int maxVerificationAttempts) {
        this.maxVerificationAttempts = maxVerificationAttempts;
    }
    
    public int getRateLimitMinutes() {
        return rateLimitMinutes;
    }
    
    public void setRateLimitMinutes(int rateLimitMinutes) {
        this.rateLimitMinutes = rateLimitMinutes;
    }
    
    public int getOtpLength() {
        return otpLength;
    }
    
    public void setOtpLength(int otpLength) {
        this.otpLength = otpLength;
    }
    
    // Firebase settings getters and setters
    
    public boolean isFirebaseEnabled() {
        return firebaseEnabled;
    }
    
    public void setFirebaseEnabled(boolean firebaseEnabled) {
        this.firebaseEnabled = firebaseEnabled;
    }
    
    public String getFirebaseProjectId() {
        return firebaseProjectId;
    }
    
    public void setFirebaseProjectId(String firebaseProjectId) {
        this.firebaseProjectId = firebaseProjectId;
    }
    
    public String getFirebaseServiceAccountKeyPath() {
        return firebaseServiceAccountKeyPath;
    }
    
    public void setFirebaseServiceAccountKeyPath(String firebaseServiceAccountKeyPath) {
        this.firebaseServiceAccountKeyPath = firebaseServiceAccountKeyPath;
    }
    
    public long getFirebasePhoneAuthTimeoutSeconds() {
        return firebasePhoneAuthTimeoutSeconds;
    }
    
    public void setFirebasePhoneAuthTimeoutSeconds(long firebasePhoneAuthTimeoutSeconds) {
        this.firebasePhoneAuthTimeoutSeconds = firebasePhoneAuthTimeoutSeconds;
    }
    
    public boolean isFirebaseAutoRetrieveEnabled() {
        return firebaseAutoRetrieveEnabled;
    }
    
    public void setFirebaseAutoRetrieveEnabled(boolean firebaseAutoRetrieveEnabled) {
        this.firebaseAutoRetrieveEnabled = firebaseAutoRetrieveEnabled;
    }
    
    // Development settings getters and setters
    
    public boolean isDevLogOtpCodes() {
        return devLogOtpCodes;
    }
    
    public void setDevLogOtpCodes(boolean devLogOtpCodes) {
        this.devLogOtpCodes = devLogOtpCodes;
    }
    
    public boolean isDevMockSmsEnabled() {
        return devMockSmsEnabled;
    }
    
    public void setDevMockSmsEnabled(boolean devMockSmsEnabled) {
        this.devMockSmsEnabled = devMockSmsEnabled;
    }
    
    public String getDevFixedOtpCode() {
        return devFixedOtpCode;
    }
    
    public void setDevFixedOtpCode(String devFixedOtpCode) {
        this.devFixedOtpCode = devFixedOtpCode;
    }
    
    public boolean isDevSkipPhoneValidation() {
        return devSkipPhoneValidation;
    }
    
    public void setDevSkipPhoneValidation(boolean devSkipPhoneValidation) {
        this.devSkipPhoneValidation = devSkipPhoneValidation;
    }
}