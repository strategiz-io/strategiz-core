package io.strategiz.data.auth.model.smsotp;

import io.strategiz.data.auth.model.AuthenticationMethod;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * SMS OTP authentication method domain model
 * 
 * Represents SMS-based OTP authentication for a user account.
 * This stores the phone number and SMS OTP configuration.
 */
public class SmsOtpAuthenticationMethod extends AuthenticationMethod {
    
    private String phoneNumber; // E.164 format
    private String countryCode; // ISO 3166-1 alpha-2 country code
    private boolean verified;
    private Instant lastOtpSentAt;
    private int dailySmsCount;
    private Instant dailyCountResetAt;
    private boolean enabled = false;
    
    // === CONSTRUCTORS ===
    
    public SmsOtpAuthenticationMethod() {
        super();
        setType("SMS_OTP");
        this.verified = false;
        this.dailySmsCount = 0;
        this.dailyCountResetAt = Instant.now();
    }
    
    public SmsOtpAuthenticationMethod(String phoneNumber, String countryCode) {
        this();
        this.phoneNumber = phoneNumber;
        this.countryCode = countryCode;
        setName("SMS: " + maskPhoneNumber(phoneNumber));
    }
    
    // === GETTERS AND SETTERS ===
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        if (phoneNumber != null) {
            setName("SMS: " + maskPhoneNumber(phoneNumber));
        }
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    public boolean isVerified() {
        return verified;
    }
    
    public void setVerified(boolean verified) {
        this.verified = verified;
        if (verified) {
            setLastVerifiedAt(Instant.now());
        }
    }
    
    public Instant getLastOtpSentAt() {
        return lastOtpSentAt;
    }
    
    public void setLastOtpSentAt(Instant lastOtpSentAt) {
        this.lastOtpSentAt = lastOtpSentAt;
    }
    
    public int getDailySmsCount() {
        return dailySmsCount;
    }
    
    public void setDailySmsCount(int dailySmsCount) {
        this.dailySmsCount = dailySmsCount;
    }
    
    public Instant getDailyCountResetAt() {
        return dailyCountResetAt;
    }
    
    public void setDailyCountResetAt(Instant dailyCountResetAt) {
        this.dailyCountResetAt = dailyCountResetAt;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    // === ABSTRACT METHOD IMPLEMENTATIONS ===
    
    @Override
    public String getAuthenticationMethodType() {
        return "SMS_OTP";
    }
    
    @Override
    public boolean isConfigured() {
        return phoneNumber != null && !phoneNumber.trim().isEmpty() && verified && enabled;
    }
    
    @Override
    public Map<String, Object> getTypeSpecificData() {
        Map<String, Object> data = new HashMap<>();
        data.put("phoneNumber", maskPhoneNumber(phoneNumber));
        data.put("countryCode", countryCode);
        data.put("verified", verified);
        data.put("enabled", enabled);
        data.put("dailySmsCount", dailySmsCount);
        data.put("lastOtpSentAt", lastOtpSentAt);
        return data;
    }
    
    // === BUSINESS METHODS ===
    
    /**
     * Mark this method as recently used for SMS OTP
     */
    public void markOtpSent() {
        setLastOtpSentAt(Instant.now());
        setLastUsedAt(Instant.now());
        incrementDailySmsCount();
    }
    
    /**
     * Mark this method as successfully verified
     */
    public void markVerified() {
        setVerified(true);
        setLastVerifiedAt(Instant.now());
        setLastUsedAt(Instant.now());
    }
    
    /**
     * Check if daily SMS limit has been reset (24 hours)
     */
    public boolean shouldResetDailyCount() {
        if (dailyCountResetAt == null) {
            return true;
        }
        return Instant.now().isAfter(dailyCountResetAt.plusSeconds(24 * 60 * 60));
    }
    
    /**
     * Reset daily SMS count
     */
    public void resetDailyCount() {
        this.dailySmsCount = 0;
        this.dailyCountResetAt = Instant.now();
    }
    
    /**
     * Increment daily SMS count
     */
    private void incrementDailySmsCount() {
        if (shouldResetDailyCount()) {
            resetDailyCount();
        }
        this.dailySmsCount++;
    }
    
    /**
     * Check if phone number can receive more SMS today
     */
    public boolean canSendSmsToday(int dailyLimit) {
        if (shouldResetDailyCount()) {
            return true;
        }
        return dailySmsCount < dailyLimit;
    }
    
    /**
     * Mask phone number for display/logging
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***-***-****";
        }
        
        String countryCode = phoneNumber.substring(0, phoneNumber.length() - 7);
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return countryCode + "***" + lastFour;
    }
    
    @Override
    public String toString() {
        return "SmsOtpAuthenticationMethod{" +
                "id='" + getId() + '\'' +
                ", phoneNumber='" + maskPhoneNumber(phoneNumber) + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", verified=" + verified +
                ", enabled=" + isEnabled() +
                ", lastOtpSentAt=" + lastOtpSentAt +
                ", dailySmsCount=" + dailySmsCount +
                '}';
    }
}