package io.strategiz.service.profile.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a Stripe checkout session.
 */
public class CheckoutRequest {

	@NotBlank(message = "Tier ID is required")
	private String tierId;

	public CheckoutRequest() {
	}

	public CheckoutRequest(String tierId) {
		this.tierId = tierId;
	}

	public String getTierId() {
		return tierId;
	}

	public void setTierId(String tierId) {
		this.tierId = tierId;
	}

}
