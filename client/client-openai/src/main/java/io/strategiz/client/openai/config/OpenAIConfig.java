package io.strategiz.client.openai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAI API client
 */
@Configuration
public class OpenAIConfig {

	@Value("${openai.api.key:#{null}}")
	private String apiKey;

	@Value("${openai.api.url:https://api.openai.com/v1}")
	private String apiUrl;

	@Value("${openai.model:gpt-4o-mini}")
	private String model;

	@Value("${openai.temperature:0.7}")
	private Double temperature;

	@Value("${openai.max-tokens:4096}")
	private Integer maxTokens;

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

}
