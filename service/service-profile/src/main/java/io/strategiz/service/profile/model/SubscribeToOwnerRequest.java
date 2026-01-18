package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for subscribing to a strategy owner.
 */
public class SubscribeToOwnerRequest {

	@JsonProperty("stripeCustomerId")
	private String stripeCustomerId;

	// Default constructor
	public SubscribeToOwnerRequest() {
	}

	// Constructor with all fields
	public SubscribeToOwnerRequest(String stripeCustomerId) {
		this.stripeCustomerId = stripeCustomerId;
	}

	// Getters and setters
	public String getStripeCustomerId() {
		return stripeCustomerId;
	}

	public void setStripeCustomerId(String stripeCustomerId) {
		this.stripeCustomerId = stripeCustomerId;
	}

}
