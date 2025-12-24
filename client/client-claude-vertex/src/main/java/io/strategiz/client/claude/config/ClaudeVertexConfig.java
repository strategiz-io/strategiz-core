package io.strategiz.client.claude.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Claude AI via Google Vertex AI
 */
@Configuration
public class ClaudeVertexConfig {

	@Value("${claude.vertex.project-id:#{null}}")
	private String projectId;

	@Value("${claude.vertex.location:us-east1}")
	private String location;

	@Value("${claude.vertex.model:claude-haiku-4-5}")
	private String defaultModel;

	@Value("${claude.vertex.temperature:0.7}")
	private Double temperature;

	@Value("${claude.vertex.max-tokens:4096}")
	private Integer maxTokens;

	@Value("${claude.vertex.enabled:true}")
	private boolean enabled;

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

	public String getDefaultModel() {
		return defaultModel;
	}

	public void setDefaultModel(String defaultModel) {
		this.defaultModel = defaultModel;
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

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Get the Vertex AI endpoint for Claude models
	 */
	public String getEndpoint() {
		return String.format("%s-aiplatform.googleapis.com", location);
	}

}
