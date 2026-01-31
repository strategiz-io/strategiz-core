package io.strategiz.data.auth.model.passkey;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Passkey credential domain model This is a business domain object for individual passkey
 * credentials
 */
public class PasskeyCredential {

	private String id;

	private String userId; // The user this credential belongs to

	private String credentialId; // Base64-encoded credential ID

	private byte[] publicKey; // Binary public key

	private String publicKeyBase64; // Base64-encoded public key for compatibility

	private int signatureCount;

	private String authenticatorData;

	private String clientDataJSON;

	private String attestationObject;

	private String name; // User-given name for the credential

	private String authenticatorName; // Name of the authenticator device

	private String deviceName; // Device identifier/name

	private String device; // Legacy device field for compatibility

	private String aaguid; // Authenticator Attestation GUID

	private String userAgent; // User agent string from registration

	private boolean verified = false;

	private boolean trusted = false; // Whether this credential is trusted

	private Instant createdAt;

	private Instant registrationTime; // Alias for createdAt

	private Instant lastUsedAt;

	private Instant lastUsedTime; // Alias for lastUsedAt

	private Map<String, Object> metadata;

	// === CONSTRUCTORS ===

	public PasskeyCredential() {
		this.createdAt = Instant.now();
		this.metadata = new HashMap<>();
	}

	public PasskeyCredential(String credentialId, String publicKeyBase64) {
		this();
		this.credentialId = credentialId;
		this.publicKeyBase64 = publicKeyBase64;
		// Convert base64 to bytes if needed
		if (publicKeyBase64 != null) {
			this.publicKey = java.util.Base64.getDecoder().decode(publicKeyBase64);
		}
	}

	// === CONVENIENCE METHODS ===

	public boolean isVerified() {
		return verified;
	}

	public void markAsUsed() {
		this.lastUsedAt = Instant.now();
		this.signatureCount++;
	}

	public boolean isConfigured() {
		return credentialId != null && !credentialId.trim().isEmpty() && publicKey != null && publicKey.length > 0;
	}

	// === GETTERS AND SETTERS ===

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

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public void setLastUsedAt(Instant lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
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

	// Missing getter/setter methods for new fields

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
		return registrationTime != null ? registrationTime : createdAt;
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