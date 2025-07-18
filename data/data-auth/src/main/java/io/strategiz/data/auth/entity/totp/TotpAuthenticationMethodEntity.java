package io.strategiz.data.auth.entity.totp;

import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * TOTP authentication method entity for Time-based One-Time Password authentication
 * Stored in users/{userId}/authentication_methods subcollection
 */
public class TotpAuthenticationMethodEntity extends AuthenticationMethodEntity {

    @PropertyName("secret")
    @JsonProperty("secret")
    private String secret; // Encrypted TOTP secret

    @PropertyName("issuer")
    @JsonProperty("issuer")
    private String issuer = "Strategiz";

    @PropertyName("verified")
    @JsonProperty("verified")
    private Boolean verified = false;

    // Constructors
    public TotpAuthenticationMethodEntity() {
        super("TOTP", "TOTP Authenticator");
    }

    public TotpAuthenticationMethodEntity(String name, String secret) {
        super("TOTP", name);
        this.secret = secret;
    }

    // Getters and Setters
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
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

    public void markAsVerified() {
        super.markAsVerified();
        this.verified = true;
    }

    // Abstract method implementations
    @Override
    public String getAuthenticationMethodType() {
        return "TOTP";
    }

    @Override
    public boolean isConfigured() {
        return secret != null && !secret.trim().isEmpty() && isVerified();
    }

    @Override
    public Map<String, Object> getTypeSpecificData() {
        Map<String, Object> data = new HashMap<>();
        data.put("issuer", issuer);
        data.put("verified", verified);
        data.put("hasSecret", secret != null && !secret.trim().isEmpty());
        return data;
    }
}