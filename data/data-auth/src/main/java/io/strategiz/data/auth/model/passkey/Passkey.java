package io.strategiz.data.auth.model.passkey;

import java.time.Instant;

/**
 * Model representing a registered passkey (WebAuthn credential)
 * in the authentication data layer
 */
public class Passkey {
    
    private String id;
    private String credentialId;
    private String userId;
    private String authenticatorName;
    private Instant registrationTime;
    private Instant lastUsedTime;
    private String aaguid;
    private byte[] publicKey;
    
    // Default constructor for deserialization
    public Passkey() {
        // Empty constructor for deserialization
    }

    public Passkey(String credentialId, String userId, String authenticatorName,
                 Instant registrationTime, String aaguid, byte[] publicKey) {
        this.credentialId = credentialId;
        this.userId = userId;
        this.authenticatorName = authenticatorName;
        this.registrationTime = registrationTime;
        this.lastUsedTime = registrationTime; // Initially same as registration time
        this.aaguid = aaguid;
        this.publicKey = publicKey;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthenticatorName() {
        return authenticatorName;
    }

    public void setAuthenticatorName(String authenticatorName) {
        this.authenticatorName = authenticatorName;
    }

    public Instant getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(Instant registrationTime) {
        this.registrationTime = registrationTime;
    }

    public Instant getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(Instant lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }

    public String getAaguid() {
        return aaguid;
    }

    public void setAaguid(String aaguid) {
        this.aaguid = aaguid;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }
}
