package io.strategiz.service.profile.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * User profile request DTO for creating/updating user profiles
 */
public class ProfileRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    private String photoURL;
    
    private String subscriptionTier;
    
    private String tradingMode;

    // Default constructor
    public ProfileRequest() {
    }

    // Constructor with all fields
    public ProfileRequest(String name, String email, String photoURL,
                          String subscriptionTier, String tradingMode) {
        this.name = name;
        this.email = email;
        this.photoURL = photoURL;
        this.subscriptionTier = subscriptionTier;
        this.tradingMode = tradingMode;
    }

    // Getters and setters
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
}
