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
    
    @JsonProperty("isEmailVerified")
    private Boolean isEmailVerified = false;
    
    @NotBlank(message = "Subscription tier is required")
    @JsonProperty("subscriptionTier")
    private String subscriptionTier = "free"; // free, premium, enterprise
    
    @NotBlank(message = "Trading mode is required")
    @JsonProperty("tradingMode")
    private String tradingMode = "demo"; // demo, live
    

    // Constructors
    public UserProfileEntity() {
    }

    public UserProfileEntity(String name, String email) {
        this.name = name;
        this.email = email;
        this.isEmailVerified = false;
        this.subscriptionTier = "free";
        this.tradingMode = "demo";
    }

    public UserProfileEntity(String name, String email, String photoURL, Boolean isEmailVerified, String subscriptionTier, String tradingMode) {
        this.name = name;
        this.email = email;
        this.photoURL = photoURL;
        this.isEmailVerified = isEmailVerified;
        this.subscriptionTier = subscriptionTier;
        this.tradingMode = tradingMode;
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

    public Boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    public void setIsEmailVerified(Boolean isEmailVerified) {
        this.isEmailVerified = isEmailVerified;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfileEntity that = (UserProfileEntity) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(email, that.email) &&
               Objects.equals(photoURL, that.photoURL) &&
               Objects.equals(isEmailVerified, that.isEmailVerified) &&
               Objects.equals(subscriptionTier, that.subscriptionTier) &&
               Objects.equals(tradingMode, that.tradingMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, photoURL, isEmailVerified, subscriptionTier, tradingMode);
    }

    @Override
    public String toString() {
        return "UserProfileEntity{" +
               "name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", photoURL='" + photoURL + '\'' +
               ", isEmailVerified=" + isEmailVerified +
               ", subscriptionTier='" + subscriptionTier + '\'' +
               ", tradingMode='" + tradingMode + '\'' +
               '}';
    }
}
