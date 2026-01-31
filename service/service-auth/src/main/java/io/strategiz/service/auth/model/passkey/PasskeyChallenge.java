package io.strategiz.service.auth.model.passkey;

import java.time.Instant;
import java.util.UUID;

/**
 * Service layer model representing a WebAuthn passkey challenge for authentication or
 * registration
 */
public class PasskeyChallenge {

	private String id;

	private String userId;

	private String challenge;

	private String credentialId;

	private PasskeyChallengeType type;

	private Instant createdAt;

	private Instant expiresAt;

	private boolean used;

	/**
	 * Default constructor
	 */
	public PasskeyChallenge() {
	}

	/**
	 * Create a new challenge
	 * @param userId UserEntity ID (email or internal ID)
	 * @param challenge The challenge string
	 * @param type Challenge type (registration or authentication)
	 * @param expirationSeconds Expiration time in seconds
	 */
	public PasskeyChallenge(String userId, String challenge, PasskeyChallengeType type, long expirationSeconds) {
		this.id = UUID.randomUUID().toString();
		this.userId = userId;
		this.challenge = challenge;
		this.type = type;
		this.createdAt = Instant.now();
		this.expiresAt = createdAt.plusSeconds(expirationSeconds);
		this.used = false;
	}

	/**
	 * Check if challenge is expired
	 * @return true if expired
	 */
	public boolean isExpired() {
		return Instant.now().isAfter(expiresAt);
	}

	// Getters and setters
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

	public String getChallenge() {
		return challenge;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public PasskeyChallengeType getType() {
		return type;
	}

	public void setType(PasskeyChallengeType type) {
		this.type = type;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public String getCredentialId() {
		return credentialId;
	}

	public void setCredentialId(String credentialId) {
		this.credentialId = credentialId;
	}

	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	/**
	 * Convert this service model to a data model
	 * @return Data layer PasskeyChallenge entity
	 */
	public io.strategiz.data.auth.model.passkey.PasskeyChallenge toDataModel() {
		io.strategiz.data.auth.model.passkey.PasskeyChallenge dataModel = new io.strategiz.data.auth.model.passkey.PasskeyChallenge(
				userId, challenge, createdAt, expiresAt, type.name());
		dataModel.setId(id);
		dataModel.setUsed(used);
		dataModel.setCredentialId(credentialId);
		return dataModel;
	}

	/**
	 * Create a service model from a data model
	 * @param dataModel Data layer PasskeyChallenge entity
	 * @return Service layer PasskeyChallenge model
	 */
	public static PasskeyChallenge fromDataModel(io.strategiz.data.auth.model.passkey.PasskeyChallenge dataModel) {
		PasskeyChallenge serviceModel = new PasskeyChallenge();
		serviceModel.setId(dataModel.getId());
		serviceModel.setUserId(dataModel.getUserId());
		serviceModel.setChallenge(dataModel.getChallenge());
		serviceModel.setType(PasskeyChallengeType.valueOf(dataModel.getChallengeType()));
		serviceModel.setCreatedAt(dataModel.getCreatedAt());
		serviceModel.setExpiresAt(dataModel.getExpiresAt());
		serviceModel.setCredentialId(dataModel.getCredentialId());
		serviceModel.setUsed(dataModel.isUsed());
		return serviceModel;
	}

}
