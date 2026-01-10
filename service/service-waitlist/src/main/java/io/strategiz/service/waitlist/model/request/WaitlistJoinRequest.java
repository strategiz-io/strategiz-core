package io.strategiz.service.waitlist.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request model for joining the waitlist
 */
public class WaitlistJoinRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String referralSource;

    // Constructors
    public WaitlistJoinRequest() {
    }

    public WaitlistJoinRequest(String email) {
        this.email = email;
    }

    public WaitlistJoinRequest(String email, String referralSource) {
        this.email = email;
        this.referralSource = referralSource;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getReferralSource() {
        return referralSource;
    }

    public void setReferralSource(String referralSource) {
        this.referralSource = referralSource;
    }

    @Override
    public String toString() {
        return "WaitlistJoinRequest{" +
                "email='" + maskEmail(email) + '\'' +
                ", referralSource='" + referralSource + '\'' +
                '}';
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf("@");
        if (atIndex > 2) {
            return email.substring(0, 2) + "***" + email.substring(atIndex);
        }
        return "***" + email.substring(atIndex);
    }
}
