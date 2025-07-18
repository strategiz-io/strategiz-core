package io.strategiz.data.auth.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

/**
 * Base entity for authentication methods stored in users/{userId}/authentication_methods subcollection
 * This is the base class for all authentication method types (TOTP, OAuth, Passkey, etc.)
 */
@MappedSuperclass
public abstract class AuthenticationMethodEntity extends BaseEntity {

    @Id
    @DocumentId
    @PropertyName("methodId")
    @JsonProperty("methodId")
    @Column(name = "method_id")
    private String methodId;

    @PropertyName("type")
    @JsonProperty("type")
    @NotBlank(message = "Method type is required")
    private String type; // TOTP, OAUTH_GOOGLE, OAUTH_FACEBOOK, PASSKEY, SMS_OTP, EMAIL_OTP

    @PropertyName("name")
    @JsonProperty("name")
    private String name; // User-friendly name

    @PropertyName("lastVerifiedAt")
    @JsonProperty("lastVerifiedAt")
    private Instant lastVerifiedAt;

    @PropertyName("userId")
    @JsonProperty("userId")
    @NotBlank(message = "User ID is required")
    private String userId; // The user this authentication method belongs to

    @PropertyName("lastAccessedAt")
    @JsonProperty("lastAccessedAt")
    private Instant lastAccessedAt; // Last time this method was used

    @PropertyName("metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Type-specific metadata

    // Constructors
    public AuthenticationMethodEntity() {
        super();
    }

    public AuthenticationMethodEntity(String type, String name) {
        super();
        this.type = type;
        this.name = name;
    }

    // Getters and Setters
    public String getMethodId() {
        return methodId;
    }

    public void setMethodId(String methodId) {
        this.methodId = methodId;
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

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    // Convenience methods
    public void markAsVerified() {
        this.lastVerifiedAt = Instant.now();
    }

    public boolean isRecentlyVerified(long withinSeconds) {
        if (lastVerifiedAt == null) {
            return false;
        }
        return lastVerifiedAt.isAfter(Instant.now().minusSeconds(withinSeconds));
    }

    // Required BaseEntity methods
    @Override
    public String getId() {
        return methodId;
    }

    @Override
    public void setId(String id) {
        this.methodId = id;
    }

    @Override
    public String getCollectionName() {
        return "authentication_methods";
    }

    // Abstract methods for type-specific behavior
    public abstract String getAuthenticationMethodType();
    public abstract boolean isConfigured();
    public abstract Map<String, Object> getTypeSpecificData();
}