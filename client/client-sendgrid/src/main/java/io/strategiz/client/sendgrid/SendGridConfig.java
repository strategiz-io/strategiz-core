package io.strategiz.client.sendgrid;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for SendGrid email client.
 *
 * Properties loaded from application.properties or Vault.
 */
@Configuration
@ConfigurationProperties(prefix = "sendgrid")
public class SendGridConfig {

	private boolean enabled = true;

	private boolean mockEnabled = false;

	private String apiKey;

	private String fromEmail;

	private String fromName;

	// Template IDs for different alert types
	private String alertTemplateId;

	private String signalTemplateId;

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

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getFromEmail() {
		return fromEmail;
	}

	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public String getAlertTemplateId() {
		return alertTemplateId;
	}

	public void setAlertTemplateId(String alertTemplateId) {
		this.alertTemplateId = alertTemplateId;
	}

	public String getSignalTemplateId() {
		return signalTemplateId;
	}

	public void setSignalTemplateId(String signalTemplateId) {
		this.signalTemplateId = signalTemplateId;
	}

	/**
	 * Check if SendGrid is properly configured.
	 */
	public boolean isConfigured() {
		return apiKey != null && !apiKey.isEmpty() && fromEmail != null && !fromEmail.isEmpty();
	}

}
