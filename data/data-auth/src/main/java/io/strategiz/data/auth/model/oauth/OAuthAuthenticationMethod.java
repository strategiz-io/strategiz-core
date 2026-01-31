package io.strategiz.data.auth.model.oauth;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth authentication method domain model This is a business domain object for
 * OAuth-based authentication
 */
public class OAuthAuthenticationMethod {

	private String id;

	private String userId;

	private String type;

	private String name;

	private String provider; // google, facebook, github, etc.

	private String providerId; // Provider-specific user ID

	private String email; // Email from OAuth provider

	private Boolean verified = false;

	private boolean enabled = true;

	private Instant createdAt;

	private Instant updatedAt;

	private Instant lastUsedAt;

	private Map<String, Object> metadata;

	// === CONSTRUCTORS ===

	public OAuthAuthenticationMethod() {
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
		this.metadata = new HashMap<>();
	}

	public OAuthAuthenticationMethod(String provider, String providerId, String email) {
		this();
		this.provider = provider;
		this.providerId = providerId;
		this.email = email;
		this.type = "OAUTH_" + provider.toUpperCase();
		this.name = provider + " OAuth";
		this.verified = true; // OAuth accounts are verified by default
	}

	// === CONVENIENCE METHODS ===

	public boolean isVerified() {
		return Boolean.TRUE.equals(verified);
	}

	public boolean isConfigured() {
		return provider != null && !provider.trim().isEmpty() && providerId != null && !providerId.trim().isEmpty()
				&& isVerified();
	}

	public boolean isActive() {
		return enabled && isVerified();
	}

	public void setProviderEmail(String providerEmail) {
		this.email = providerEmail;
		this.updatedAt = Instant.now();
	}

	public void setMethodName(String methodName) {
		this.name = methodName;
		this.updatedAt = Instant.now();
	}

	public String getAuthenticationMethodType() {
		return "OAUTH_" + (provider != null ? provider.toUpperCase() : "UNKNOWN");
	}

	public Map<String, Object> getTypeSpecificData() {
		Map<String, Object> data = new HashMap<>();
		data.put("provider", provider);
		data.put("email", email);
		data.put("verified", verified);
		data.put("hasProviderId", providerId != null && !providerId.trim().isEmpty());
		return data;
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

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
		if (provider != null) {
			setType("OAUTH_" + provider.toUpperCase());
			setName(provider + " OAuth");
		}
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getVerified() {
		return verified;
	}

	public void setVerified(Boolean verified) {
		this.verified = verified;
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