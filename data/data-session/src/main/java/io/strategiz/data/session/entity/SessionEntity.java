package io.strategiz.data.session.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

/**
 * Session entity for storing authentication sessions and tokens
 * Combines token and session data in a single collection
 * Stored in the 'sessions' collection in Firestore
 */
@Collection("sessions")
public class SessionEntity extends BaseEntity {

    /**
     * Unique session/token identifier
     */
    @DocumentId
    @PropertyName("sessionId")
    @JsonProperty("sessionId")
    @NotBlank
    private String sessionId;

    /**
     * ID of the authenticated user
     */
    @PropertyName("userId")
    @JsonProperty("userId")
    @NotBlank
    private String userId;

    /**
     * The actual PASETO token value
     */
    @PropertyName("tokenValue")
    @JsonProperty("tokenValue")
    @NotBlank
    private String tokenValue;

    /**
     * Token type: ACCESS or REFRESH
     */
    @PropertyName("tokenType")
    @JsonProperty("tokenType")
    @NotBlank
    private String tokenType;

    /**
     * Reference to device in devices collection
     */
    @PropertyName("deviceId")
    @JsonProperty("deviceId")
    private String deviceId;

    /**
     * When the token was issued
     */
    @PropertyName("issuedAt")
    @JsonProperty("issuedAt")
    @NotNull
    private Instant issuedAt;

    /**
     * When the token expires
     */
    @PropertyName("expiresAt")
    @JsonProperty("expiresAt")
    @NotNull
    private Instant expiresAt;

    /**
     * Last time this session was accessed
     */
    @PropertyName("lastAccessedAt")
    @JsonProperty("lastAccessedAt")
    private Instant lastAccessedAt;

    /**
     * IP address from which the session was created
     */
    @PropertyName("ipAddress")
    @JsonProperty("ipAddress")
    private String ipAddress;

    /**
     * Whether this token has been revoked
     */
    @PropertyName("revoked")
    @JsonProperty("revoked")
    private Boolean revoked = false;

    /**
     * When the token was revoked (if applicable)
     */
    @PropertyName("revokedAt")
    @JsonProperty("revokedAt")
    private Instant revokedAt;

    /**
     * Reason for revocation (if applicable)
     */
    @PropertyName("revocationReason")
    @JsonProperty("revocationReason")
    private String revocationReason;

    /**
     * Additional token claims (ACR, AMR, scope, etc.)
     */
    @PropertyName("claims")
    @JsonProperty("claims")
    private Map<String, Object> claims;

    // === CONSTRUCTORS ===

    public SessionEntity() {
        super();
        this.revoked = false;
        this.issuedAt = Instant.now();
        this.lastAccessedAt = Instant.now();
    }

    public SessionEntity(String userId) {
        super(userId);
        this.userId = userId;
        this.revoked = false;
        this.issuedAt = Instant.now();
        this.lastAccessedAt = Instant.now();
    }

    // === REQUIRED OVERRIDES FROM BaseEntity ===

    @Override
    public String getId() {
        return sessionId;
    }

    @Override
    public void setId(String id) {
        this.sessionId = id;
    }

    // === VALIDATION METHODS ===

    /**
     * Check if the session is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the session is valid (not revoked and not expired)
     */
    public boolean isValid() {
        return !Boolean.TRUE.equals(revoked) && !isExpired();
    }

    /**
     * Update the last accessed time
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = Instant.now();
    }

    /**
     * Revoke the session
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }

    // === GETTERS AND SETTERS ===

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
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
        this.claims = claims;
    }
}