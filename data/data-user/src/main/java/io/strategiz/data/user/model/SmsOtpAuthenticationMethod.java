package io.strategiz.data.user.model;

import java.util.Date;
import java.util.Objects;

/**
 * SMS One-Time Password authentication method.
 */
public class SmsOtpAuthenticationMethod extends AuthenticationMethod {
    private String phoneNumber;
    private Boolean verified = false;

    // No-argument constructor
    public SmsOtpAuthenticationMethod() {
        super();
    }

    // Custom constructor (preserved and updated)
    public SmsOtpAuthenticationMethod(String name, String phoneNumber, Boolean verified, String createdBy) {
        super(null, "SMS_OTP", name, null, createdBy);
        this.phoneNumber = phoneNumber;
        if (verified != null) { // Preserve original logic of allowing explicit set
            this.verified = verified;
        }
        // If 'verified' is null in the constructor, it will retain its default 'false' value.
    }

    // All-arguments constructor (including inherited fields)
    public SmsOtpAuthenticationMethod(String id, String type, String name, Date lastVerifiedAt, String createdBy, String phoneNumber, Boolean verified) {
        super(id, type, name, lastVerifiedAt, createdBy);
        this.phoneNumber = phoneNumber;
        this.verified = verified != null ? verified : false; // Ensure 'verified' is not null
    }

    // Getters and Setters
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified != null ? verified : false; // Ensure 'verified' is not set to null
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SmsOtpAuthenticationMethod that = (SmsOtpAuthenticationMethod) o;
        return Objects.equals(phoneNumber, that.phoneNumber) &&
               Objects.equals(verified, that.verified);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), phoneNumber, verified);
    }

    @Override
    public String toString() {
        String superString = super.toString();
        String content = superString.substring(0, superString.length() - 1);
        return content +
               ", phoneNumber='" + phoneNumber + '\'' +
               ", verified=" + verified +
               '}';
    }
}
