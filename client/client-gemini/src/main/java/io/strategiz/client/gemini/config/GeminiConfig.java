package io.strategiz.client.gemini.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Google Gemini AI client
 */
@Configuration
public class GeminiConfig {

	@Value("${gemini.api.key:#{null}}")
	private String apiKey;

	@Value("${gemini.api.url:https://generativelanguage.googleapis.com}")
	private String apiUrl;

	@Value("${gemini.model:gemini-1.5-flash}")
	private String model;

	@Value("${gemini.temperature:0.7}")
	private Double temperature;

	@Value("${gemini.max-tokens:2048}")
	private Integer maxTokens;

	@Value("${gemini.project-id:#{null}}")
	private String projectId;

	@Value("${gemini.location:us-central1}")
	private String location;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiUrl() {
		return apiUrl;
	}

	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

}
