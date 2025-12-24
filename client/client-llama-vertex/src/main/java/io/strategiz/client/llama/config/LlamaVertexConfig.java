package io.strategiz.client.llama.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Meta Llama via Google Vertex AI
 */
@Configuration
public class LlamaVertexConfig {

	@Value("${llama.vertex.project-id:#{null}}")
	private String projectId;

	@Value("${llama.vertex.location:us-east1}")
	private String location;

	@Value("${llama.vertex.model:llama-3.1-8b-instruct-maas}")
	private String defaultModel;

	@Value("${llama.vertex.temperature:0.7}")
	private Double temperature;

	@Value("${llama.vertex.max-tokens:8192}")
	private Integer maxTokens;

	@Value("${llama.vertex.enabled:true}")
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
	 * Get the Vertex AI endpoint for Llama models
	 */
	public String getEndpoint() {
		return String.format("%s-aiplatform.googleapis.com", location);
	}

}
