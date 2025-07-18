package io.strategiz.data.auth.entity.smsotp;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * SMS OTP session entity for temporary OTP storage in Firestore
 * 
 * Stored in sms_otp_sessions/{sessionId} collection
 * Contains temporary OTP codes with expiration and verification tracking
 */
public class SmsOtpSessionEntity extends BaseEntity {
    
    @DocumentId
    @PropertyName("sessionId")
    @JsonProperty("sessionId")
    private String sessionId;
    
    @PropertyName("phoneNumber")
    @JsonProperty("phoneNumber")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format")
    private String phoneNumber;
    
    @PropertyName("otpCode")
    @JsonProperty("otpCode")
    @NotBlank(message = "OTP code is required")
    private String otpCode;
    
    @PropertyName("ipAddress")
    @JsonProperty("ipAddress")
    private String ipAddress;
    
    @PropertyName("countryCode")
    @JsonProperty("countryCode")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be a valid ISO 3166-1 alpha-2 code")
    private String countryCode;
    
    @PropertyName("createdAt")
    @JsonProperty("createdAt")
    @NotNull(message = "Created at timestamp is required")
    private Instant createdAt;
    
    @PropertyName("expiresAt")
    @JsonProperty("expiresAt")
    @NotNull(message = "Expiration timestamp is required")
    private Instant expiresAt;
    
    @PropertyName("verificationAttempts")
    @JsonProperty("verificationAttempts")
    @Min(0)
    private int verificationAttempts = 0;
    
    @PropertyName("verified")
    @JsonProperty("verified")
    private boolean verified = false;
    
    @PropertyName("userId")
    @JsonProperty("userId")
    private String userId; // Optional: set when user is identified
    
    // === CONSTRUCTORS ===
    
    public SmsOtpSessionEntity() {
        super();
        this.createdAt = Instant.now();
        this.verificationAttempts = 0;
        this.verified = false;
    }
    
    public SmsOtpSessionEntity(String sessionId, String phoneNumber, String otpCode, String ipAddress, String countryCode) {
        this();
        this.sessionId = sessionId;
        this.phoneNumber = phoneNumber;
        this.otpCode = otpCode;
        this.ipAddress = ipAddress;
        this.countryCode = countryCode;
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
    
    // === ABSTRACT METHOD IMPLEMENTATIONS ===
    
    @Override
    public String getId() {
        return sessionId;
    }
    
    @Override
    public void setId(String id) {
        this.sessionId = id;
    }
    
    @Override
    public String getCollectionName() {
        return "sms_otp_sessions";
    }
    
    // === UTILITY METHODS ===
    
    /**
     * Mask phone number for display/logging purposes
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
        return "SmsOtpSessionEntity{" +
                "sessionId='" + sessionId + '\'' +
                ", phoneNumber='" + getMaskedPhoneNumber() + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", verified=" + verified +
                ", verificationAttempts=" + verificationAttempts +
                ", expiresAt=" + expiresAt +
                '}';
    }
}