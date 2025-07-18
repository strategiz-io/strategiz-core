package io.strategiz.data.auth.entity.smsotp;

import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * SMS OTP authentication method entity for Firestore storage
 * 
 * Stored in users/{userId}/authentication_methods/{methodId} collection
 * Represents SMS-based OTP authentication configuration for a user
 */
public class SmsOtpAuthenticationMethodEntity extends AuthenticationMethodEntity {
    
    @PropertyName("phoneNumber")
    @JsonProperty("phoneNumber")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format")
    private String phoneNumber;
    
    @PropertyName("countryCode")
    @JsonProperty("countryCode")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be a valid ISO 3166-1 alpha-2 code")
    private String countryCode;
    
    @PropertyName("verified")
    @JsonProperty("verified")
    private boolean verified = false;
    
    @PropertyName("lastOtpSentAt")
    @JsonProperty("lastOtpSentAt")
    private Instant lastOtpSentAt;
    
    @PropertyName("dailySmsCount")
    @JsonProperty("dailySmsCount")
    @Min(0)
    private int dailySmsCount = 0;
    
    @PropertyName("dailyCountResetAt")
    @JsonProperty("dailyCountResetAt")
    private Instant dailyCountResetAt;
    
    @PropertyName("enabled")
    @JsonProperty("enabled")
    private boolean enabled = false;
    
    // === CONSTRUCTORS ===
    
    public SmsOtpAuthenticationMethodEntity() {
        super();
        setType("SMS_OTP");
        this.verified = false;
        this.dailySmsCount = 0;
        this.dailyCountResetAt = Instant.now();
    }
    
    public SmsOtpAuthenticationMethodEntity(String phoneNumber, String countryCode) {
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
    
    // === UTILITY METHODS ===
    
    /**
     * Mask phone number for display/logging purposes
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
        return "SmsOtpAuthenticationMethodEntity{" +
                "methodId='" + getMethodId() + '\'' +
                ", phoneNumber='" + maskPhoneNumber(phoneNumber) + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", verified=" + verified +
                ", enabled=" + isEnabled() +
                ", lastOtpSentAt=" + lastOtpSentAt +
                ", dailySmsCount=" + dailySmsCount +
                '}';
    }
}