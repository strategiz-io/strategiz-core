package io.strategiz.data.auth.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Base authentication method domain model This is a business domain object for all
 * authentication methods
 */
public abstract class AuthenticationMethod {

	private String id;

	private String userId;

	private String type;

	private String name;

	private boolean enabled = true;

	private Instant createdAt;

	private Instant updatedAt;

	private Instant lastUsedAt;

	private Instant lastVerifiedAt;

	private Map<String, Object> metadata;

	// === CONSTRUCTORS ===

	public AuthenticationMethod() {
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
		this.metadata = new HashMap<>();
	}

	public AuthenticationMethod(String type, String name) {
		this();
		this.type = type;
		this.name = name;
	}

	// === ABSTRACT METHODS ===

	public abstract String getAuthenticationMethodType();

	public abstract boolean isConfigured();

	public abstract Map<String, Object> getTypeSpecificData();

	// === CONVENIENCE METHODS ===

	public void markAsVerified() {
		this.lastVerifiedAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public void markAsUsed() {
		this.lastUsedAt = Instant.now();
	}

	public void setMethodName(String methodName) {
		this.name = methodName;
		this.updatedAt = Instant.now();
	}

	// === GETTERS AND SETTERS ===

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

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public void setLastUsedAt(Instant lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
	}

	public Instant getLastVerifiedAt() {
		return lastVerifiedAt;
	}

	public void setLastVerifiedAt(Instant lastVerifiedAt) {
		this.lastVerifiedAt = lastVerifiedAt;
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

}