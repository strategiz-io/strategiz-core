package io.strategiz.data.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * User profile entity - core user identity information
 * Stored as embedded object in users/{userId} document
 */
public class UserProfileEntity {
    
    @NotBlank(message = "Name is required")
    @JsonProperty("name")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("photoURL")
    private String photoURL;
    
    @JsonProperty("verifiedEmail")
    private Boolean verifiedEmail = false;
    
    @NotBlank(message = "Subscription tier is required")
    @JsonProperty("subscriptionTier")
    private String subscriptionTier = "free"; // free, premium, enterprise
    
    @NotBlank(message = "Trading mode is required")
    @JsonProperty("tradingMode")
    private String tradingMode = "demo"; // demo, live
    
    @NotNull(message = "Active status is required")
    @JsonProperty("isActive")
    private Boolean isActive = true;

    // Constructors
    public UserProfileEntity() {
    }

    public UserProfileEntity(String name, String email) {
        this.name = name;
        this.email = email;
        this.verifiedEmail = false;
        this.subscriptionTier = "free";
        this.tradingMode = "demo";
        this.isActive = true;
    }

    public UserProfileEntity(String name, String email, String photoURL, Boolean verifiedEmail, String subscriptionTier, String tradingMode, Boolean isActive) {
        this.name = name;
        this.email = email;
        this.photoURL = photoURL;
        this.verifiedEmail = verifiedEmail;
        this.subscriptionTier = subscriptionTier;
        this.tradingMode = tradingMode;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }

    public Boolean getVerifiedEmail() {
        return verifiedEmail;
    }

    public void setVerifiedEmail(Boolean verifiedEmail) {
        this.verifiedEmail = verifiedEmail;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    public String getTradingMode() {
        return tradingMode;
    }

    public void setTradingMode(String tradingMode) {
        this.tradingMode = tradingMode;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfileEntity that = (UserProfileEntity) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(email, that.email) &&
               Objects.equals(photoURL, that.photoURL) &&
               Objects.equals(verifiedEmail, that.verifiedEmail) &&
               Objects.equals(subscriptionTier, that.subscriptionTier) &&
               Objects.equals(tradingMode, that.tradingMode) &&
               Objects.equals(isActive, that.isActive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, photoURL, verifiedEmail, subscriptionTier, tradingMode, isActive);
    }

    @Override
    public String toString() {
        return "UserProfileEntity{" +
               "name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", photoURL='" + photoURL + '\'' +
               ", verifiedEmail=" + verifiedEmail +
               ", subscriptionTier='" + subscriptionTier + '\'' +
               ", tradingMode='" + tradingMode + '\'' +
               ", isActive=" + isActive +
               '}';
    }
}
