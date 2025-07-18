package io.strategiz.data.auth.model.smsotp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * SMS OTP session domain model for temporary OTP storage
 * 
 * This represents a temporary SMS OTP session with the actual OTP code,
 * expiration, and verification tracking. This is separate from the
 * SmsOtpAuthenticationMethod which stores the phone number registration.
 */
public class SmsOtpSession {
    
    private String sessionId;
    private String phoneNumber; // E.164 format
    private String otpCode;
    private String ipAddress;
    private String countryCode;
    private Instant createdAt;
    private Instant expiresAt;
    private int verificationAttempts;
    private boolean verified;
    private String userId; // Optional: set when user is identified
    
    // === CONSTRUCTORS ===
    
    public SmsOtpSession() {
        this.createdAt = Instant.now();
        this.verificationAttempts = 0;
        this.verified = false;
    }
    
    public SmsOtpSession(String sessionId, String phoneNumber, String otpCode, String ipAddress, String countryCode, int expiryMinutes) {
        this();
        this.sessionId = sessionId;
        this.phoneNumber = phoneNumber;
        this.otpCode = otpCode;
        this.ipAddress = ipAddress;
        this.countryCode = countryCode;
        this.expiresAt = createdAt.plus(expiryMinutes, ChronoUnit.MINUTES);
    }
    
    // === GETTERS AND SETTERS ===
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getOtpCode() {
        return otpCode;
    }
    
    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public int getVerificationAttempts() {
        return verificationAttempts;
    }
    
    public void setVerificationAttempts(int verificationAttempts) {
        this.verificationAttempts = verificationAttempts;
    }
    
    public boolean isVerified() {
        return verified;
    }
    
    public void setVerified(boolean verified) {
        this.verified = verified;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    // === BUSINESS METHODS ===
    
    /**
     * Check if this OTP session has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if this OTP session is still valid (not expired and not verified)
     */
    public boolean isValid() {
        return !isExpired() && !verified;
    }
    
    /**
     * Increment verification attempts
     */
    public void incrementAttempts() {
        this.verificationAttempts++;
    }
    
    /**
     * Check if maximum attempts have been exceeded
     */
    public boolean hasExceededMaxAttempts(int maxAttempts) {
        return verificationAttempts >= maxAttempts;
    }
    
    /**
     * Mark this session as verified
     */
    public void markVerified() {
        this.verified = true;
    }
    
    /**
     * Get remaining attempts
     */
    public int getRemainingAttempts(int maxAttempts) {
        return Math.max(0, maxAttempts - verificationAttempts);
    }
    
    /**
     * Get time remaining until expiration in seconds
     */
    public long getSecondsUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(Instant.now(), expiresAt);
    }
    
    /**
     * Mask phone number for logging
     */
    public String getMaskedPhoneNumber() {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***-***-****";
        }
        
        String countryPrefix = phoneNumber.substring(0, phoneNumber.length() - 7);
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return countryPrefix + "***" + lastFour;
    }
    
    @Override
    public String toString() {
        return "SmsOtpSession{" +
                "sessionId='" + sessionId + '\'' +
                ", phoneNumber='" + getMaskedPhoneNumber() + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", verified=" + verified +
                ", verificationAttempts=" + verificationAttempts +
                ", expiresAt=" + expiresAt +
                ", isExpired=" + isExpired() +
                '}';
    }
}