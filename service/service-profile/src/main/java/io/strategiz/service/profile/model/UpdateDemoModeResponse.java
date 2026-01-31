package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response after updating demo mode Includes new JWT tokens with updated demo mode claim
 */
public class UpdateDemoModeResponse {

	@JsonProperty("demoMode")
	private Boolean demoMode;

	@JsonProperty("accessToken")
	private String accessToken;

	@JsonProperty("refreshToken")
	private String refreshToken;

	@JsonProperty("message")
	private String message;

	// Default constructor for Jackson
	public UpdateDemoModeResponse() {
	}

	public UpdateDemoModeResponse(Boolean demoMode, String accessToken, String refreshToken) {
		this.demoMode = demoMode;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.message = "Demo mode updated successfully";
	}

	public Boolean getDemoMode() {
		return demoMode;
	}

	public void setDemoMode(Boolean demoMode) {
		this.demoMode = demoMode;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}