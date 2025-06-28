package io.strategiz.data.auth.model.passkey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;

/**
 * Model representing a passkey challenge for authentication
 */
@Entity
@Table(name = "passkey_challenges")
public class PasskeyChallenge {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;
    @Column(nullable = false)
    private String userId;
    @Column
    private String credentialId;
    @Column(nullable = false, length = 1000)
    private String challenge;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean used;
    @Column(nullable = false)
    private String challengeType;
    
    @Version
    private Long version;
    
    // Default constructor for deserialization
    public PasskeyChallenge() {
        // Empty constructor for deserialization
    }
    
    public PasskeyChallenge(String userId, String challenge, Instant createdAt, 
                          Instant expiresAt, String challengeType) {
        this.userId = userId;
        this.challenge = challenge;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = false;
        this.challengeType = challengeType;
    }
    
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
    
    public String getCredentialId() {
        return credentialId;
    }
    
    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }
    
    public String getChallenge() {
        return challenge;
    }
    
    public void setChallenge(String challenge) {
        this.challenge = challenge;
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
    
    public boolean isUsed() {
        return used;
    }
    
    public void setUsed(boolean used) {
        this.used = used;
    }
    
    public String getChallengeType() {
        return challengeType;
    }
    
    public void setChallengeType(String challengeType) {
        this.challengeType = challengeType;
    }
}
