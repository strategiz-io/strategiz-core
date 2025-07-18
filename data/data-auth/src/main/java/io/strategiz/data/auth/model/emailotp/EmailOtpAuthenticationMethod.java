package io.strategiz.data.auth.model.emailotp;

import io.strategiz.data.auth.model.AuthenticationMethod;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Email OTP authentication method domain model
 * 
 * Represents email-based OTP authentication for a user account.
 * This stores the email address and email OTP configuration.
 */
public class EmailOtpAuthenticationMethod extends AuthenticationMethod {
    
    private String email; // Email address
    private boolean verified;
    private Instant lastOtpSentAt;
    private int dailyEmailCount;
    private Instant dailyCountResetAt;
    private boolean enabled = false;
    
    // === CONSTRUCTORS ===
    
    public EmailOtpAuthenticationMethod() {
        super();
        setType("EMAIL_OTP");
        this.verified = false;
        this.dailyEmailCount = 0;
        this.dailyCountResetAt = Instant.now();
    }
    
    public EmailOtpAuthenticationMethod(String email) {
        this();
        this.email = email;
        setName("Email: " + maskEmail(email));
    }
    
    // === GETTERS AND SETTERS ===
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
        if (email != null) {
            setName("Email: " + maskEmail(email));
        }
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
    
    // === BUSINESS METHODS ===
    
    /**
     * Mark this method as recently used for email OTP
     */
    public void markOtpSent() {
        setLastOtpSentAt(Instant.now());
        setLastUsedAt(Instant.now());
        incrementDailyEmailCount();
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
     * Check if daily email limit has been reset (24 hours)
     */
    public boolean shouldResetDailyCount() {
        if (dailyCountResetAt == null) {
            return true;
        }
        return Instant.now().isAfter(dailyCountResetAt.plusSeconds(24 * 60 * 60));
    }
    
    /**
     * Reset daily email count
     */
    public void resetDailyCount() {
        this.dailyEmailCount = 0;
        this.dailyCountResetAt = Instant.now();
    }
    
    /**
     * Increment daily email count
     */
    private void incrementDailyEmailCount() {
        if (shouldResetDailyCount()) {
            resetDailyCount();
        }
        this.dailyEmailCount++;
    }
    
    /**
     * Check if email address can receive more emails today
     */
    public boolean canSendEmailToday(int dailyLimit) {
        if (shouldResetDailyCount()) {
            return true;
        }
        return dailyEmailCount < dailyLimit;
    }
    
    /**
     * Mask email address for display/logging
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
        return "EmailOtpAuthenticationMethod{" +
                "id='" + getId() + '\'' +
                ", email='" + maskEmail(email) + '\'' +
                ", verified=" + verified +
                ", enabled=" + isEnabled() +
                ", lastOtpSentAt=" + lastOtpSentAt +
                ", dailyEmailCount=" + dailyEmailCount +
                '}';
    }
}