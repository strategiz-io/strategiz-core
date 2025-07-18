package io.strategiz.data.auth.model.totp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * TOTP authentication method domain model
 * This is a business domain object for TOTP-based authentication
 */
public class TotpAuthenticationMethod {
    
    private String id;
    private String userId;
    private String type = "TOTP";
    private String name = "TOTP Authenticator";
    private String secret; // Encrypted TOTP secret
    private String issuer = "Strategiz";
    private Boolean verified = false;
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastUsedAt;
    private Map<String, Object> metadata;

    // === CONSTRUCTORS ===

    public TotpAuthenticationMethod() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.metadata = new HashMap<>();
    }

    public TotpAuthenticationMethod(String name, String secret) {
        this();
        this.name = name;
        this.secret = secret;
    }

    // === CONVENIENCE METHODS ===

    public boolean isVerified() {
        return Boolean.TRUE.equals(verified);
    }

    public void markAsVerified() {
        this.verified = true;
        this.updatedAt = Instant.now();
    }

    public boolean isConfigured() {
        return secret != null && !secret.trim().isEmpty() && isVerified();
    }

    public String getAuthenticationMethodType() {
        return "TOTP";
    }

    public Map<String, Object> getTypeSpecificData() {
        Map<String, Object> data = new HashMap<>();
        data.put("issuer", issuer);
        data.put("verified", verified);
        data.put("hasSecret", secret != null && !secret.trim().isEmpty());
        return data;
    }

    // === GETTERS AND SETTERS ===

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}