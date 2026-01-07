package io.strategiz.client.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Firebase Cloud Messaging client.
 *
 * Properties loaded from application.properties or Vault.
 */
@Configuration
@ConfigurationProperties(prefix = "fcm")
public class FcmConfig {

	private boolean enabled = true;

	private boolean mockEnabled = false;

	private String projectId;

	// Android notification channel
	private String defaultAndroidChannel = "alerts";

	// iOS sound
	private String defaultIosSound = "default";

	// Default TTL in seconds (24 hours)
	private int timeToLive = 86400;

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

	public String getDefaultAndroidChannel() {
		return defaultAndroidChannel;
	}

	public void setDefaultAndroidChannel(String defaultAndroidChannel) {
		this.defaultAndroidChannel = defaultAndroidChannel;
	}

	public String getDefaultIosSound() {
		return defaultIosSound;
	}

	public void setDefaultIosSound(String defaultIosSound) {
		this.defaultIosSound = defaultIosSound;
	}

	public int getTimeToLive() {
		return timeToLive;
	}

	public void setTimeToLive(int timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * Check if FCM is properly configured. Firebase Admin SDK uses application default
	 * credentials, so we just need the project ID.
	 */
	public boolean isConfigured() {
		return projectId != null && !projectId.isEmpty();
	}

}
