package io.strategiz.data.auth.entity.passkey;

import io.strategiz.data.base.entity.BaseEntity;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Firestore entity for passkey credentials
 * Stores individual WebAuthn/FIDO2 credentials
 */
@Entity
@Table(name = "passkey_credentials")
public class PasskeyCredentialEntity extends BaseEntity {

    @PropertyName("userId")
    @JsonProperty("userId")
    @NotBlank
    private String userId;

    @PropertyName("credentialId")
    @JsonProperty("credentialId")
    @NotBlank
    private String credentialId;

    @PropertyName("publicKey")
    @JsonProperty("publicKey")
    private byte[] publicKey;

    @PropertyName("publicKeyBase64")
    @JsonProperty("publicKeyBase64")
    private String publicKeyBase64;

    @PropertyName("signatureCount")
    @JsonProperty("signatureCount")
    private int signatureCount;

    @PropertyName("authenticatorData")
    @JsonProperty("authenticatorData")
    private String authenticatorData;

    @PropertyName("clientDataJSON")
    @JsonProperty("clientDataJSON")
    private String clientDataJSON;

    @PropertyName("attestationObject")
    @JsonProperty("attestationObject")
    private String attestationObject;

    @PropertyName("name")
    @JsonProperty("name")
    private String name;

    @PropertyName("authenticatorName")
    @JsonProperty("authenticatorName")
    private String authenticatorName;

    @PropertyName("deviceName")
    @JsonProperty("deviceName")
    private String deviceName;

    @PropertyName("device")
    @JsonProperty("device")
    private String device;

    @PropertyName("aaguid")
    @JsonProperty("aaguid")
    private String aaguid;

    @PropertyName("userAgent")
    @JsonProperty("userAgent")
    private String userAgent;

    @PropertyName("verified")
    @JsonProperty("verified")
    private boolean verified = false;

    @PropertyName("trusted")
    @JsonProperty("trusted")
    private boolean trusted = false;

    @PropertyName("lastUsedAt")
    @JsonProperty("lastUsedAt")
    private Instant lastUsedAt;

    @PropertyName("registrationTime")
    @JsonProperty("registrationTime")
    private Instant registrationTime;

    @PropertyName("lastUsedTime")
    @JsonProperty("lastUsedTime")
    private Instant lastUsedTime;

    @PropertyName("metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    private String id;

    // === CONSTRUCTORS ===

    public PasskeyCredentialEntity() {
        this.metadata = new HashMap<>();
        this.registrationTime = Instant.now();
    }

    public PasskeyCredentialEntity(String userId, String credentialId, String publicKeyBase64) {
        this();
        this.userId = userId;
        this.credentialId = credentialId;
        this.publicKeyBase64 = publicKeyBase64;
        if (publicKeyBase64 != null) {
            this.publicKey = java.util.Base64.getDecoder().decode(publicKeyBase64);
        }
    }

    // === ABSTRACT METHOD IMPLEMENTATIONS ===

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getCollectionName() {
        return "passkey_credentials";
    }

    // === CONVENIENCE METHODS ===

    public boolean isVerified() {
        return verified;
    }

    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
        this.lastUsedTime = this.lastUsedAt;
        this.signatureCount++;
    }

    public boolean isConfigured() {
        return credentialId != null && !credentialId.trim().isEmpty() && 
               publicKey != null && publicKey.length > 0;
    }

    public Instant getCreatedAt() {
        if (getAuditFields() != null && getAuditFields().getCreatedDate() != null) {
            return getAuditFields().getCreatedDate().toDate().toInstant();
        }
        return registrationTime;
    }

    // === GETTERS AND SETTERS ===

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

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String publicKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
        if (publicKeyBase64 != null) {
            this.publicKey = java.util.Base64.getDecoder().decode(publicKeyBase64);
        }
    }

    public int getSignatureCount() {
        return signatureCount;
    }

    public void setSignatureCount(int signatureCount) {
        this.signatureCount = signatureCount;
    }

    public String getAuthenticatorData() {
        return authenticatorData;
    }

    public void setAuthenticatorData(String authenticatorData) {
        this.authenticatorData = authenticatorData;
    }

    public String getClientDataJSON() {
        return clientDataJSON;
    }

    public void setClientDataJSON(String clientDataJSON) {
        this.clientDataJSON = clientDataJSON;
    }

    public String getAttestationObject() {
        return attestationObject;
    }

    public void setAttestationObject(String attestationObject) {
        this.attestationObject = attestationObject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public boolean getVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
        this.lastUsedTime = lastUsedAt;
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

    public String getAuthenticatorName() {
        return authenticatorName;
    }

    public void setAuthenticatorName(String authenticatorName) {
        this.authenticatorName = authenticatorName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getAaguid() {
        return aaguid;
    }

    public void setAaguid(String aaguid) {
        this.aaguid = aaguid;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    public Instant getRegistrationTime() {
        return registrationTime != null ? registrationTime : getCreatedAt();
    }

    public void setRegistrationTime(Instant registrationTime) {
        this.registrationTime = registrationTime;
    }

    public Instant getLastUsedTime() {
        return lastUsedTime != null ? lastUsedTime : lastUsedAt;
    }

    public void setLastUsedTime(Instant lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }
}