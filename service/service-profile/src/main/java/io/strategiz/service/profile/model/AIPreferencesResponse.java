package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for AI preferences.
 */
public class AIPreferencesResponse {

	@JsonProperty("preferredModel")
	private String preferredModel;

	@JsonProperty("preferredProvider")
	private String preferredProvider;

	@JsonProperty("enableStreaming")
	private Boolean enableStreaming;

	@JsonProperty("saveHistory")
	private Boolean saveHistory;

	@JsonProperty("maxTokens")
	private Integer maxTokens;

	@JsonProperty("temperature")
	private Double temperature;

	// Default constructor
	public AIPreferencesResponse() {
	}

	// Getters and setters
	public String getPreferredModel() {
		return preferredModel;
	}

	public void setPreferredModel(String preferredModel) {
		this.preferredModel = preferredModel;
	}

	public String getPreferredProvider() {
		return preferredProvider;
	}

	public void setPreferredProvider(String preferredProvider) {
		this.preferredProvider = preferredProvider;
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
