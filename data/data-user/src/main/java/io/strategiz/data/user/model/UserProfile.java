package io.strategiz.data.user.model;

import java.util.Objects;

/**
 * Represents the profile object within a user document.
 * Contains user identity and status information.
 */
public class UserProfile {
    private String name;
    private String email;
    private String photoURL;
    private Boolean verifiedEmail = false;
    private String subscriptionTier = "free"; // premium, basic, free, etc.
    private String tradingMode = "demo"; // demo or real
    private Boolean isActive = true;

    // Constructors
    public UserProfile() {
    }

    public UserProfile(String name, String email, String photoURL, Boolean verifiedEmail, String subscriptionTier, String tradingMode, Boolean isActive) {
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
        UserProfile that = (UserProfile) o;
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
        return "UserProfile{" +
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
