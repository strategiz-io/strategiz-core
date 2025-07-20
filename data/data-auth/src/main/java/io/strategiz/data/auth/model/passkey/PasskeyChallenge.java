package io.strategiz.data.auth.model.passkey;

import io.strategiz.data.base.entity.BaseEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Passkey challenge entity
 * This is a database entity for WebAuthn challenges
 */
public class PasskeyChallenge extends BaseEntity {
    
    private String id;
    private String challenge;
    private String userId;
    private String sessionId;
    private String credentialId; // Associated credential ID for authentication challenges
    private String type; // "registration" or "authentication"
    private boolean used = false;
    private Instant createdAt;
    private Instant expiresAt;
    private Map<String, Object> metadata;

    // === CONSTRUCTORS ===

    public PasskeyChallenge() {
        super(); // Initialize BaseEntity
        this.createdAt = Instant.now();
        this.expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES); // 5 minute expiry
        this.metadata = new HashMap<>();
    }

    public PasskeyChallenge(String challenge, String userId, String type) {
        super(userId); // Initialize BaseEntity with userId for audit fields
        this.challenge = challenge;
        this.userId = userId;
        this.type = type;
        this.createdAt = Instant.now();
        this.expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);
        this.metadata = new HashMap<>();
    }
    
    public PasskeyChallenge(String userId, String challenge, Instant createdAt, Instant expiresAt, String type) {
        super(userId); // Initialize BaseEntity with userId for audit fields
        this.id = null; // Will be set when saved
        this.userId = userId;
        this.challenge = challenge;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.type = type;
        this.used = false;
        this.metadata = new HashMap<>();
    }

    // === CONVENIENCE METHODS ===

    public boolean hasExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !hasExpired();
    }

    public void markAsUsed() {
        this.used = true;
    }

    // === STATIC FACTORY METHODS ===

    public static PasskeyChallenge createRegistrationChallenge(String challenge, String userId) {
        return new PasskeyChallenge(challenge, userId, "registration");
    }

    public static PasskeyChallenge createAuthenticationChallenge(String challenge, String userId) {
        return new PasskeyChallenge(challenge, userId, "authentication");
    }

    // === GETTERS AND SETTERS ===

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
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

    @Override
    public String getCollectionName() {
        return "passkey_challenges";
    }

    // === ADDITIONAL GETTERS/SETTERS FOR FIRESTORE MAPPING ===
    
    // Firestore expects these field names based on what's stored in the database
    public String getChallengeType() {
        return type;
    }
    
    public void setChallengeType(String challengeType) {
        this.type = challengeType;
    }
}