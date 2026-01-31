package io.strategiz.client.fmp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Financial Modeling Prep API.
 */
@Configuration
@ConfigurationProperties(prefix = "fmp")
public class FmpConfig {

	private String apiKey;

	private String baseUrl = "https://financialmodelingprep.com/api/v3";

	private int maxRetries = 3;

	private long retryDelayMs = 1000;

	private int rateLimitPerMinute = 300; // Free tier: 250, Starter: 300, Premium: 750,
											// Ultimate: 3000

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public long getRetryDelayMs() {
		return retryDelayMs;
	}

	public void setRetryDelayMs(long retryDelayMs) {
		this.retryDelayMs = retryDelayMs;
	}

	public int getRateLimitPerMinute() {
		return rateLimitPerMinute;
	}

	public void setRateLimitPerMinute(int rateLimitPerMinute) {
		this.rateLimitPerMinute = rateLimitPerMinute;
	}

	/**
	 * Check if the client is properly configured with an API key.
	 */
	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank();
	}

}
