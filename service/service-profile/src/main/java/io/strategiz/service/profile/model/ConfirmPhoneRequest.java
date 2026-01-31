package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for confirming phone verification with OTP code.
 */
public class ConfirmPhoneRequest {

	@JsonProperty("code")
	@NotBlank(message = "Verification code is required")
	@Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
	private String code;

	// Default constructor
	public ConfirmPhoneRequest() {
	}

	public ConfirmPhoneRequest(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
