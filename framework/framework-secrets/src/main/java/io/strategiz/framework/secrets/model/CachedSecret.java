package io.strategiz.framework.secrets.model;

import java.time.Instant;

/**
 * Model class representing a cached secret with expiry time
 */
public class CachedSecret {
    private final String value;
    private final Instant expiryTime;
    
    public CachedSecret(String value, Instant expiryTime) {
        this.value = value;
        this.expiryTime = expiryTime;
    }
    
    public String getValue() {
        return value;
    }
    
    public Instant getExpiryTime() {
        return expiryTime;
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiryTime);
    }
}