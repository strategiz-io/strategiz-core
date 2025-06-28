package io.strategiz.data.auth.model.session;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a PASETO authentication token.
 */
public class PasetoToken {
    private String id;
    private String userId;
    private String tokenValue;
    private String tokenType; // ACCESS or REFRESH
    private long issuedAt;
    private long expiresAt;
    private boolean revoked;
    private String deviceId;
    private String issuedFrom; // IP address
    private long revokedAt;
    private String revocationReason;
    private Map<String, Object> claims;
    
    // Default constructor
    public PasetoToken() {
        this.claims = new HashMap<>();
    }
    
    // All-args constructor 
    public PasetoToken(String id, String userId, String tokenValue, String tokenType,
                      long issuedAt, long expiresAt, boolean revoked, String deviceId,
                      String issuedFrom, long revokedAt, String revocationReason,
                      Map<String, Object> claims) {
        this.id = id;
        this.userId = userId;
        this.tokenValue = tokenValue;
        this.tokenType = tokenType;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.deviceId = deviceId;
        this.issuedFrom = issuedFrom;
        this.revokedAt = revokedAt;
        this.revocationReason = revocationReason;
        this.claims = claims != null ? claims : new HashMap<>();
    }
    
    /**
     * Check if the token is valid (not revoked and not expired)
     */
    public boolean isValid() {
        return !isRevoked() && expiresAt > Instant.now().getEpochSecond();
    }
    
    // Getters and setters
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
    
    public String getTokenValue() {
        return tokenValue;
    }
    
    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public long getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isRevoked() {
        return revoked;
    }
    
    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getIssuedFrom() {
        return issuedFrom;
    }
    
    public void setIssuedFrom(String issuedFrom) {
        this.issuedFrom = issuedFrom;
    }
    
    public long getRevokedAt() {
        return revokedAt;
    }
    
    public void setRevokedAt(long revokedAt) {
        this.revokedAt = revokedAt;
    }
    
    public String getRevocationReason() {
        return revocationReason;
    }
    
    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }
    
    public Map<String, Object> getClaims() {
        return claims;
    }
    
    public void setClaims(Map<String, Object> claims) {
        this.claims = claims != null ? claims : new HashMap<>();
    }
    
    // Builder pattern implementation
    public static PasetoTokenBuilder builder() {
        return new PasetoTokenBuilder();
    }
    
    public static class PasetoTokenBuilder {
        private String id;
        private String userId;
        private String tokenValue;
        private String tokenType;
        private long issuedAt;
        private long expiresAt;
        private boolean revoked;
        private String deviceId;
        private String issuedFrom;
        private long revokedAt;
        private String revocationReason;
        private Map<String, Object> claims = new HashMap<>();
        
        public PasetoTokenBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public PasetoTokenBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public PasetoTokenBuilder tokenValue(String tokenValue) {
            this.tokenValue = tokenValue;
            return this;
        }
        
        public PasetoTokenBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }
        
        public PasetoTokenBuilder issuedAt(long issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }
        
        public PasetoTokenBuilder expiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public PasetoTokenBuilder revoked(boolean revoked) {
            this.revoked = revoked;
            return this;
        }
        
        public PasetoTokenBuilder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }
        
        public PasetoTokenBuilder issuedFrom(String issuedFrom) {
            this.issuedFrom = issuedFrom;
            return this;
        }
        
        public PasetoTokenBuilder revokedAt(long revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }
        
        public PasetoTokenBuilder revocationReason(String revocationReason) {
            this.revocationReason = revocationReason;
            return this;
        }
        
        public PasetoTokenBuilder claims(Map<String, Object> claims) {
            if (claims != null) {
                this.claims = claims;
            }
            return this;
        }
        
        public PasetoToken build() {
            return new PasetoToken(id, userId, tokenValue, tokenType, issuedAt, expiresAt, 
                                 revoked, deviceId, issuedFrom, revokedAt, revocationReason, claims);
        }
    }
}
