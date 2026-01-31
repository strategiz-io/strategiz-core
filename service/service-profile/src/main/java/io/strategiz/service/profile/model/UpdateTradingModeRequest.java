package io.strategiz.service.profile.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request to update user's trading mode
 */
public class UpdateTradingModeRequest {

	@NotNull(message = "Trading mode is required")
	@Pattern(regexp = "^(demo|live)$", message = "Trading mode must be 'demo' or 'live'")
	@JsonProperty("mode")
	private String mode;

	// Default constructor for Jackson
	public UpdateTradingModeRequest() {
	}

	public UpdateTradingModeRequest(String mode) {
		this.mode = mode;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	@Override
	public String toString() {
		return "UpdateTradingModeRequest{" + "mode='" + mode + '\'' + '}';
	}

}