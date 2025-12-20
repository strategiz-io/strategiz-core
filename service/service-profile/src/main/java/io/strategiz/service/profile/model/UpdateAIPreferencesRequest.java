package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request model for updating AI preferences.
 */
public class UpdateAIPreferencesRequest {

	@JsonProperty("preferredModel")
	private String preferredModel;

	@JsonProperty("enableStreaming")
	private Boolean enableStreaming;

	@JsonProperty("saveHistory")
	private Boolean saveHistory;

	@Min(value = 1, message = "Max tokens must be at least 1")
	@Max(value = 100000, message = "Max tokens must be at most 100000")
	@JsonProperty("maxTokens")
	private Integer maxTokens;

	@DecimalMin(value = "0.0", message = "Temperature must be at least 0")
	@DecimalMax(value = "2.0", message = "Temperature must be at most 2")
	@JsonProperty("temperature")
	private Double temperature;

	// Default constructor
	public UpdateAIPreferencesRequest() {
	}

	// Getters and setters
	public String getPreferredModel() {
		return preferredModel;
	}

	public void setPreferredModel(String preferredModel) {
		this.preferredModel = preferredModel;
	}

	public Boolean getEnableStreaming() {
		return enableStreaming;
	}

	public void setEnableStreaming(Boolean enableStreaming) {
		this.enableStreaming = enableStreaming;
	}

	public Boolean getSaveHistory() {
		return saveHistory;
	}

	public void setSaveHistory(Boolean saveHistory) {
		this.saveHistory = saveHistory;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

}
