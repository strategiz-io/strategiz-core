package io.strategiz.service.auth.model.passkey;

import java.time.Instant;

/**
 * Service layer domain model for a passkey credential. This is separate from the data
 * layer entity to maintain proper separation of concerns.
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

	private String deviceName;

	private String userAgent;

	private boolean trusted;

	public Passkey() {
		// Default constructor
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

	public boolean isTrusted() {
		return trusted;
	}

	public void setTrusted(boolean trusted) {
		this.trusted = trusted;
	}

}
