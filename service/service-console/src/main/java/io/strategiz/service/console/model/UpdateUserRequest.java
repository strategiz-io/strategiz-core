package io.strategiz.service.console.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request model for updating user profile via admin console.
 */
public class UpdateUserRequest {

	@JsonProperty("name")
	private String name;

	@JsonProperty("email")
	private String email;

	@JsonProperty("phoneNumber")
	private String phoneNumber;

	@JsonProperty("subscriptionTier")
	private String subscriptionTier;

	@JsonProperty("demoMode")
	private Boolean demoMode;

	// Getters and Setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getSubscriptionTier() {
		return subscriptionTier;
	}

	public void setSubscriptionTier(String subscriptionTier) {
		this.subscriptionTier = subscriptionTier;
	}

	public Boolean getDemoMode() {
		return demoMode;
	}

	public void setDemoMode(Boolean demoMode) {
		this.demoMode = demoMode;
	}

}
