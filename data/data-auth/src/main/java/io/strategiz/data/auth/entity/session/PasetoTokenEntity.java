package io.strategiz.data.auth.entity.session;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

/**
 * PASETO token entity for storing authentication tokens
 * Stored in top-level sessions collection
 */
@Entity
@Table(name = "paseto_tokens")
public class PasetoTokenEntity extends BaseEntity {

    @Id
    @DocumentId
    @PropertyName("tokenId")
    @JsonProperty("tokenId")
    @Column(name = "token_id")
    private String tokenId;

    @PropertyName("userId")
    @JsonProperty("userId")
    @NotBlank(message = "User ID is required")
    @Column(name = "user_id")
    private String userId;

    @PropertyName("tokenValue")
    @JsonProperty("tokenValue")
    @Column(name = "token_value", length = 2000)
    private String tokenValue; // The actual PASETO token

    @PropertyName("tokenType")
    @JsonProperty("tokenType")
    @Column(name = "token_type")
    private String tokenType; // ACCESS or REFRESH

    @PropertyName("issuedAt")
    @JsonProperty("issuedAt")
    @Column(name = "issued_at")
    private Instant issuedAt;

    @PropertyName("expiresAt")
    @JsonProperty("expiresAt")
    @Column(name = "expires_at")
    private Instant expiresAt;

    @PropertyName("revoked")
    @JsonProperty("revoked")
    @Column(name = "revoked")
    private Boolean revoked = false;

    @PropertyName("revokedAt")
    @JsonProperty("revokedAt")
    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PropertyName("revocationReason")
    @JsonProperty("revocationReason")
    @Column(name = "revocation_reason")
    private String revocationReason;

    @PropertyName("deviceId")
    @JsonProperty("deviceId")
    @Column(name = "device_id")
    private String deviceId;

    @PropertyName("issuedFrom")
    @JsonProperty("issuedFrom")
    @Column(name = "issued_from")
    private String issuedFrom; // IP address

    @PropertyName("claims")
    @JsonProperty("claims")
    @Column(name = "claims", columnDefinition = "TEXT")
    private Map<String, Object> claims;

    // Constructors
    public PasetoTokenEntity() {
        super();
    }

    public PasetoTokenEntity(String userId, String tokenValue, String tokenType) {
        super();
        this.userId = userId;
        this.tokenValue = tokenValue;
        this.tokenType = tokenType;
        this.issuedAt = Instant.now();
        this.revoked = false;
    }

    // Getters and Setters
    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
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

    public Map<String, Object> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = claims;
    }

    // Convenience methods
    public boolean isRevoked() {
        return Boolean.TRUE.equals(revoked);
    }

    public boolean isValid() {
        return !isRevoked() && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }

    // Required BaseEntity methods
    @Override
    public String getId() {
        return tokenId;
    }

    @Override
    public void setId(String id) {
        this.tokenId = id;
    }

    @Override
    public String getCollectionName() {
        return "sessions";
    }
}