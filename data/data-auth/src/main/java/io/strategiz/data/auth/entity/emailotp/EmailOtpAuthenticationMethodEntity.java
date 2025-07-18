package io.strategiz.data.auth.entity.emailotp;

import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Email OTP authentication method entity for both JPA and Firestore storage
 * 
 * JPA: Stored in email_otp_auth_methods table
 * Firestore: Stored in users/{userId}/authentication_methods/{methodId} collection
 * Represents email-based OTP authentication configuration for a user
 */
@Entity
@Table(name = "email_otp_auth_methods")
public class EmailOtpAuthenticationMethodEntity extends AuthenticationMethodEntity {
    
    // methodId is inherited from parent class
    
    @PropertyName("email")
    @JsonProperty("email")
    @NotBlank(message = "Email address is required")
    @Email(message = "Email address must be valid")
    @Column(name = "email")
    private String email;
    
    @PropertyName("verified")
    @JsonProperty("verified")
    @Column(name = "verified")
    private boolean verified = false;
    
    @PropertyName("lastOtpSentAt")
    @JsonProperty("lastOtpSentAt")
    @Column(name = "last_otp_sent_at")
    private Instant lastOtpSentAt;
    
    @PropertyName("dailyEmailCount")
    @JsonProperty("dailyEmailCount")
    @Min(0)
    @Column(name = "daily_email_count")
    private int dailyEmailCount = 0;
    
    @PropertyName("dailyCountResetAt")
    @JsonProperty("dailyCountResetAt")
    @Column(name = "daily_count_reset_at")
    private Instant dailyCountResetAt;
    
    @PropertyName("enabled")
    @JsonProperty("enabled")
    @Column(name = "enabled")
    private boolean enabled = false;
    
    // === CONSTRUCTORS ===
    
    public EmailOtpAuthenticationMethodEntity() {
        super();
        setType("EMAIL_OTP");
        this.verified = false;
        this.dailyEmailCount = 0;
        this.dailyCountResetAt = Instant.now();
    }
    
    public EmailOtpAuthenticationMethodEntity(String email) {
        this();
        this.email = email;
        setName("Email: " + maskEmail(email));
    }
    
    // === GETTERS AND SETTERS ===
    // methodId getter/setter are inherited from parent class
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
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
    
    public int getDailyEmailCount() {
        return dailyEmailCount;
    }
    
    public void setDailyEmailCount(int dailyEmailCount) {
        this.dailyEmailCount = dailyEmailCount;
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
        return "EMAIL_OTP";
    }
    
    @Override
    public boolean isConfigured() {
        return email != null && !email.trim().isEmpty() && verified && enabled;
    }
    
    @Override
    public Map<String, Object> getTypeSpecificData() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", maskEmail(email));
        data.put("verified", verified);
        data.put("enabled", enabled);
        data.put("dailyEmailCount", dailyEmailCount);
        data.put("lastOtpSentAt", lastOtpSentAt);
        return data;
    }
    
    // === UTILITY METHODS ===
    
    /**
     * Mask email address for display/logging purposes
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.***";
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "***@***.***";
        }
        
        String localPart = parts[0];
        String domain = parts[1];
        
        // Mask local part: show first char + *** + last char (if long enough)
        String maskedLocal;
        if (localPart.length() <= 2) {
            maskedLocal = "***";
        } else if (localPart.length() <= 4) {
            maskedLocal = localPart.charAt(0) + "***";
        } else {
            maskedLocal = localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
        }
        
        // Mask domain: show first char + *** + last part
        String maskedDomain;
        if (domain.contains(".")) {
            String[] domainParts = domain.split("\\.");
            maskedDomain = domainParts[0].charAt(0) + "***." + domainParts[domainParts.length - 1];
        } else {
            maskedDomain = domain.charAt(0) + "***";
        }
        
        return maskedLocal + "@" + maskedDomain;
    }
    
    @Override
    public String toString() {
        return "EmailOtpAuthenticationMethodEntity{" +
                "methodId='" + getMethodId() + '\'' +
                ", email='" + maskEmail(email) + '\'' +
                ", verified=" + verified +
                ", enabled=" + isEnabled() +
                ", lastOtpSentAt=" + lastOtpSentAt +
                ", dailyEmailCount=" + dailyEmailCount +
                '}';
    }
}