package io.strategiz.service.console.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Response model for admin user listing.
 */
public class AdminUserResponse {

	@JsonProperty("id")
	private String id;

	@JsonProperty("email")
	private String email;

	@JsonProperty("name")
	private String name;

	@JsonProperty("role")
	private String role;

	@JsonProperty("subscriptionTier")
	private String subscriptionTier;

	@JsonProperty("isEmailVerified")
	private Boolean isEmailVerified;

	@JsonProperty("demoMode")
	private Boolean demoMode;

	@JsonProperty("status")
	private String status; // ACTIVE, DISABLED

	@JsonProperty("createdAt")
	private Instant createdAt;

	@JsonProperty("lastLoginAt")
	private Instant lastLoginAt;

	@JsonProperty("activeSessions")
	private Integer activeSessions;

	// Constructors
	public AdminUserResponse() {
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getSubscriptionTier() {
		return subscriptionTier;
	}

	public void setSubscriptionTier(String subscriptionTier) {
		this.subscriptionTier = subscriptionTier;
	}

	public Boolean getIsEmailVerified() {
		return isEmailVerified;
	}

	public void setIsEmailVerified(Boolean isEmailVerified) {
		this.isEmailVerified = isEmailVerified;
	}

	public Boolean getDemoMode() {
		return demoMode;
	}

	public void setDemoMode(Boolean demoMode) {
		this.demoMode = demoMode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(Instant lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	public Integer getActiveSessions() {
		return activeSessions;
	}

	public void setActiveSessions(Integer activeSessions) {
		this.activeSessions = activeSessions;
	}

}
