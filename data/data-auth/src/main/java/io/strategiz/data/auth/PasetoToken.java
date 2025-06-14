package io.strategiz.data.auth;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model for PASETO token data
 */
public class PasetoToken {
    /**
     * Unique identifier for the token
     */
    private String id;
    
    /**
     * User ID associated with this token
     */
    private String userId;
    
    /**
     * Token type (e.g., "access", "refresh")
     */
    private String tokenType;
    
    /**
     * The actual PASETO token value
     */
    private String tokenValue;
    
    /**
     * Token issued at timestamp (seconds since epoch)
     */
    private long issuedAt;
    
    /**
     * Token expiration timestamp (seconds since epoch)
     */
    private long expiresAt;
    
    /**
     * Optional device ID that generated this token
     */
    private String deviceId;
    
    /**
     * IP address where token was issued
     */
    private String issuedFrom;
    
    /**
     * Whether the token has been revoked
     */
    private boolean revoked;
    
    /**
     * Optional reason for revocation
     */
    private String revocationReason;
    
    /**
     * Timestamp when token was revoked (seconds since epoch)
     */
    private Long revokedAt;
    
    /**
     * Additional claims stored in the token
     */
    private Map<String, Object> claims = new HashMap<>();

    // Constructors
    public PasetoToken() {
        this.claims = new HashMap<>();
    }

    public PasetoToken(String id, String userId, String tokenType, String tokenValue, long issuedAt, 
                       long expiresAt, String deviceId, String issuedFrom, boolean revoked, 
                       String revocationReason, Long revokedAt, Map<String, Object> claims) {
        this.id = id;
        this.userId = userId;
        this.tokenType = tokenType;
        this.tokenValue = tokenValue;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.deviceId = deviceId;
        this.issuedFrom = issuedFrom;
        this.revoked = revoked;
        this.revocationReason = revocationReason;
        this.revokedAt = revokedAt;
        this.claims = claims != null ? claims : new HashMap<>();
    }

    // Getters and Setters
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

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
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

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public Long getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Long revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = claims != null ? claims : new HashMap<>();
    }

    /**
     * Check if the token is expired
     * 
     * @return true if the token is expired
     */
    public boolean isExpired() {
        return Instant.now().getEpochSecond() > expiresAt;
    }
    
    /**
     * Check if the token is valid (not expired and not revoked)
     * 
     * @return true if the token is valid
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }

    // Builder pattern
    public static PasetoTokenBuilder builder() {
        return new PasetoTokenBuilder();
    }

    public static class PasetoTokenBuilder {
        private String id;
        private String userId;
        private String tokenType;
        private String tokenValue;
        private long issuedAt;
        private long expiresAt;
        private String deviceId;
        private String issuedFrom;
        private boolean revoked;
        private String revocationReason;
        private Long revokedAt;
        private Map<String, Object> claims = new HashMap<>();

        public PasetoTokenBuilder id(String id) {
            this.id = id;
            return this;
        }

        public PasetoTokenBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public PasetoTokenBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public PasetoTokenBuilder tokenValue(String tokenValue) {
            this.tokenValue = tokenValue;
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

        public PasetoTokenBuilder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public PasetoTokenBuilder issuedFrom(String issuedFrom) {
            this.issuedFrom = issuedFrom;
            return this;
        }

        public PasetoTokenBuilder revoked(boolean revoked) {
            this.revoked = revoked;
            return this;
        }

        public PasetoTokenBuilder revocationReason(String revocationReason) {
            this.revocationReason = revocationReason;
            return this;
        }

        public PasetoTokenBuilder revokedAt(Long revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public PasetoTokenBuilder claims(Map<String, Object> claims) {
            this.claims = claims != null ? claims : new HashMap<>();
            return this;
        }

        public PasetoToken build() {
            return new PasetoToken(id, userId, tokenType, tokenValue, issuedAt, expiresAt, 
                                 deviceId, issuedFrom, revoked, revocationReason, revokedAt, claims);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasetoToken that = (PasetoToken) o;
        return issuedAt == that.issuedAt &&
                expiresAt == that.expiresAt &&
                revoked == that.revoked &&
                Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(tokenType, that.tokenType) &&
                Objects.equals(tokenValue, that.tokenValue) &&
                Objects.equals(deviceId, that.deviceId) &&
                Objects.equals(issuedFrom, that.issuedFrom) &&
                Objects.equals(revocationReason, that.revocationReason) &&
                Objects.equals(revokedAt, that.revokedAt) &&
                Objects.equals(claims, that.claims);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, tokenType, tokenValue, issuedAt, expiresAt, 
                          deviceId, issuedFrom, revoked, revocationReason, revokedAt, claims);
    }

    @Override
    public String toString() {
        return "PasetoToken{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", tokenValue='" + tokenValue + '\'' +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", deviceId='" + deviceId + '\'' +
                ", issuedFrom='" + issuedFrom + '\'' +
                ", revoked=" + revoked +
                ", revocationReason='" + revocationReason + '\'' +
                ", revokedAt=" + revokedAt +
                ", claims=" + claims +
                '}';
    }
}
