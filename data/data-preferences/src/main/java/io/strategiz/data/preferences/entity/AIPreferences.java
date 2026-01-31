package io.strategiz.data.preferences.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

/**
 * AI preferences for users/{userId}/preferences/ai Contains user's AI model preferences
 * and settings.
 */
@Collection("preferences")
public class AIPreferences extends BaseEntity {

	public static final String PREFERENCE_ID = "ai";

	@DocumentId
	@PropertyName("preferenceId")
	@JsonProperty("preferenceId")
	private String preferenceId = PREFERENCE_ID;

	@PropertyName("preferredModel")
	@JsonProperty("preferredModel")
	private String preferredModel; // e.g., "gemini-3-flash-preview", "claude-opus-4-5"

	@PropertyName("preferredProvider")
	@JsonProperty("preferredProvider")
	private String preferredProvider; // e.g., "google", "anthropic"

	@PropertyName("enableStreaming")
	@JsonProperty("enableStreaming")
	private Boolean enableStreaming = true;

	@PropertyName("saveHistory")
	@JsonProperty("saveHistory")
	private Boolean saveHistory = true;

	@PropertyName("maxTokens")
	@JsonProperty("maxTokens")
	private Integer maxTokens;

	@PropertyName("temperature")
	@JsonProperty("temperature")
	private Double temperature;

	// Constructors
	public AIPreferences() {
		super();
	}

	public AIPreferences(String preferredModel) {
		super();
		this.preferredModel = preferredModel;
	}

	// Getters and Setters
	public String getPreferenceId() {
		return preferenceId;
	}

	public void setPreferenceId(String preferenceId) {
		this.preferenceId = preferenceId;
	}

	public String getPreferredModel() {
		return preferredModel;
	}

	public void setPreferredModel(String preferredModel) {
		this.preferredModel = preferredModel;
		// Auto-set provider based on model
		if (preferredModel != null) {
			if (preferredModel.startsWith("gemini")) {
				this.preferredProvider = "google";
			}
			else if (preferredModel.startsWith("claude")) {
				this.preferredProvider = "anthropic";
			}
		}
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

	// Required BaseEntity methods
	@Override
	public String getId() {
		return preferenceId;
	}

	@Override
	public void setId(String id) {
		this.preferenceId = id;
	}

}
