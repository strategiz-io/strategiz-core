package io.strategiz.client.recaptcha;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Google reCAPTCHA Enterprise client.
 *
 * Properties loaded from application.properties or Vault.
 */
@Configuration
@ConfigurationProperties(prefix = "recaptcha")
public class RecaptchaConfig {

	private boolean enabled = true;

	private boolean mockEnabled = false;

	private String projectId;

	private String siteKey;

	private String apiKey;

	private double threshold = 0.5;

	private int timeoutMs = 500;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isMockEnabled() {
		return mockEnabled;
	}

	public void setMockEnabled(boolean mockEnabled) {
		this.mockEnabled = mockEnabled;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public int getTimeoutMs() {
		return timeoutMs;
	}

	public void setTimeoutMs(int timeoutMs) {
		this.timeoutMs = timeoutMs;
	}

	/**
	 * Check if reCAPTCHA is properly configured.
	 */
	public boolean isConfigured() {
		return projectId != null && !projectId.isEmpty() && siteKey != null && !siteKey.isEmpty();
	}

}
