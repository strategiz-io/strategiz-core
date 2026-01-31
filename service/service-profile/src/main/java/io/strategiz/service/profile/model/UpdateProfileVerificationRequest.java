package io.strategiz.service.profile.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for email verification
 */
public class UpdateProfileVerificationRequest {

	@NotBlank(message = "Verification code is required")
	@Size(min = 6, max = 10, message = "Verification code must be 6-10 characters")
	private String verificationCode;

	public UpdateProfileVerificationRequest() {
	}

	public UpdateProfileVerificationRequest(String verificationCode) {
		this.verificationCode = verificationCode;
	}

	public String getVerificationCode() {
		return verificationCode;
	}

	public void setVerificationCode(String verificationCode) {
		this.verificationCode = verificationCode;
	}

}