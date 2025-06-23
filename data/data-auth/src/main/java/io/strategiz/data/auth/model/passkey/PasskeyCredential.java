package io.strategiz.data.auth.model.passkey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Base64;
import org.hibernate.annotations.GenericGenerator;

/**
 * Represents a passkey credential stored in the database
 */
@Entity
@Table(name = "passkey_credentials")
public class PasskeyCredential {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;
    
    @Column(nullable = false, unique = true)
    private String credentialId;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, length = 4000)
    private String publicKeyBase64;
    
    @Column
    private String authenticatorName;
    
    @Column(nullable = false)
    private Instant registrationTime;
    
    @Column(nullable = false)
    private Instant lastUsedTime;
    
    @Column
    private String aaguid;
    
    @Column
    private String deviceName;
    
    @Column
    private String userAgent;
    
    @Column(length = 4000)
    private String attestationObject;
    
    @Column(length = 4000)
    private String clientDataJSON;
    
    @Column(nullable = false)
    private boolean trusted = false;
    
    /**
     * Default constructor for JPA
     */
    public PasskeyCredential() {
        // Empty constructor for JPA
    }

    /**
     * Constructor with required fields
     */
    public PasskeyCredential(String credentialId, String userId, byte[] publicKey, String authenticatorName, 
                           Instant registrationTime, String aaguid) {
        this.credentialId = credentialId;
        this.userId = userId;
        this.publicKeyBase64 = publicKey != null ? Base64.getEncoder().encodeToString(publicKey) : null;
        this.authenticatorName = authenticatorName;
        this.registrationTime = registrationTime;
        this.lastUsedTime = registrationTime; // Initially the same as registration time
        this.aaguid = aaguid;
    }
    

    
    /**
     * Convenience method to update the last used time
     */
    public void updateLastUsedTime() {
        this.lastUsedTime = Instant.now();
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

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String publicKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
    }

    /**
     * Convenience method for storing a raw public key as Base64
     */
    public void setPublicKey(byte[] publicKey) {
        if (publicKey != null) {
            this.publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey);
        } else {
            this.publicKeyBase64 = null;
        }
    }

    /**
     * Convenience method for retrieving the decoded public key
     */
    public byte[] getPublicKey() {
        if (publicKeyBase64 != null) {
            return Base64.getDecoder().decode(publicKeyBase64);
        }
        return null;
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

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getAttestationObject() {
        return attestationObject;
    }

    public void setAttestationObject(String attestationObject) {
        this.attestationObject = attestationObject;
    }

    public String getClientDataJSON() {
        return clientDataJSON;
    }

    public void setClientDataJSON(String clientDataJSON) {
        this.clientDataJSON = clientDataJSON;
    }
    
    public boolean isTrusted() {
        return trusted;
    }
    
    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }
}
