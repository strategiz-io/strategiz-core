package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for initiating phone verification.
 */
public class PhoneVerificationRequest {

	@JsonProperty("phoneNumber")
	@NotBlank(message = "Phone number is required")
	@Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format (e.g., +14155551234)")
	private String phoneNumber;

	// Default constructor
	public PhoneVerificationRequest() {
	}

	public PhoneVerificationRequest(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

}
