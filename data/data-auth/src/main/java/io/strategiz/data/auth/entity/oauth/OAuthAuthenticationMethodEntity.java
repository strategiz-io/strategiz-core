package io.strategiz.data.auth.entity.oauth;

import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth authentication method entity for OAuth provider authentication
 * Stored in users/{userId}/authentication_methods subcollection
 */
public class OAuthAuthenticationMethodEntity extends AuthenticationMethodEntity {

    @PropertyName("provider")
    @JsonProperty("provider")
    private String provider; // google, facebook, github, etc.

    @PropertyName("providerId")
    @JsonProperty("providerId")
    private String providerId; // Provider-specific user ID

    @PropertyName("email")
    @JsonProperty("email")
    private String email; // Email from OAuth provider

    @PropertyName("verified")
    @JsonProperty("verified")
    private Boolean verified = false;

    // Constructors
    public OAuthAuthenticationMethodEntity() {
        super();
    }

    public OAuthAuthenticationMethodEntity(String provider, String providerId, String email) {
        super("OAUTH_" + provider.toUpperCase(), provider + " OAuth");
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.verified = true; // OAuth accounts are verified by default
    }

    // Getters and Setters
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
        // Update type and name when provider changes
        if (provider != null) {
            setType("OAUTH_" + provider.toUpperCase());
            setName(provider + " OAuth");
        }
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    // Convenience methods
    public boolean isVerified() {
        return Boolean.TRUE.equals(verified);
    }

    // Abstract method implementations
    @Override
    public String getAuthenticationMethodType() {
        return "OAUTH_" + (provider != null ? provider.toUpperCase() : "UNKNOWN");
    }

    @Override
    public boolean isConfigured() {
        return provider != null && !provider.trim().isEmpty() && 
               providerId != null && !providerId.trim().isEmpty() &&
               isVerified();
    }

    @Override
    public Map<String, Object> getTypeSpecificData() {
        Map<String, Object> data = new HashMap<>();
        data.put("provider", provider);
        data.put("email", email);
        data.put("verified", verified);
        data.put("hasProviderId", providerId != null && !providerId.trim().isEmpty());
        return data;
    }
}