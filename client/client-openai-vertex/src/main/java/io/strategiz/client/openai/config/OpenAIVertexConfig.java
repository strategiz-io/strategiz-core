package io.strategiz.client.openai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAI via Google Vertex AI
 */
@Configuration
public class OpenAIVertexConfig {

	@Value("${openai.vertex.project-id:#{null}}")
	private String projectId;

	@Value("${openai.vertex.location:us-east5}")
	private String location;

	@Value("${openai.vertex.model:gpt-4o-mini}")
	private String defaultModel;

	@Value("${openai.vertex.temperature:0.7}")
	private Double temperature;

	@Value("${openai.vertex.max-tokens:4096}")
	private Integer maxTokens;

	@Value("${openai.vertex.enabled:true}")
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
	 * Get the Vertex AI endpoint for OpenAI models
	 */
	public String getEndpoint() {
		return String.format("%s-aiplatform.googleapis.com", location);
	}

}
