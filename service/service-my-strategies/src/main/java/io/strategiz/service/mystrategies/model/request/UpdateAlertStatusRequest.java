package io.strategiz.service.mystrategies.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for updating alert status (pause/resume).
 */
public class UpdateAlertStatusRequest {

	@NotBlank(message = "Status is required")
	@Pattern(regexp = "ACTIVE|PAUSED|STOPPED", message = "Status must be ACTIVE, PAUSED, or STOPPED")
	@JsonProperty("status")
	private String status;

	// Getter and Setter
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
