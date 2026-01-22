package io.strategiz.data.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.constants.SubscriptionTierConstants;
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
    private String subscriptionTier = SubscriptionTierConstants.DEFAULT;
    
    @NotNull(message = "Demo mode is required")
    @JsonProperty("demoMode")
    private Boolean demoMode = true; // true for demo, false for live

    @JsonProperty("role")
    private String role = "USER"; // USER, ADMIN

    // Constructors
    public UserProfileEntity() {
    }

    public UserProfileEntity(String name, String email) {
        this.name = name;
        this.email = email;
        this.isEmailVerified = false;
        this.subscriptionTier = SubscriptionTierConstants.DEFAULT;
        this.demoMode = true;
    }

    public UserProfileEntity(String name, String email, String photoURL, Boolean isEmailVerified, String subscriptionTier, Boolean demoMode) {
        this.name = name;
        this.email = email;
        this.photoURL = photoURL;
        this.isEmailVerified = isEmailVerified;
        this.subscriptionTier = subscriptionTier;
        this.demoMode = demoMode;
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

    public Boolean getDemoMode() {
        return demoMode;
    }

    public void setDemoMode(Boolean demoMode) {
        this.demoMode = demoMode;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
               Objects.equals(demoMode, that.demoMode) &&
               Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, photoURL, isEmailVerified, subscriptionTier, demoMode, role);
    }

    @Override
    public String toString() {
        return "UserProfileEntity{" +
               "name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", photoURL='" + photoURL + '\'' +
               ", isEmailVerified=" + isEmailVerified +
               ", subscriptionTier='" + subscriptionTier + '\'' +
               ", demoMode=" + demoMode +
               ", role='" + role + '\'' +
               '}';
    }
}
