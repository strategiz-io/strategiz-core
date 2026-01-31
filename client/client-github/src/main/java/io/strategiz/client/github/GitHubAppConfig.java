package io.strategiz.client.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * GitHub App configuration Loads App ID and private key from application properties
 * (populated by Vault)
 */
@Configuration
public class GitHubAppConfig {

	@Value("${github.app.id:#{null}}")
	private String appId;

	@Value("${github.app.private-key:#{null}}")
	private String privateKey;

	@Value("${github.app.installation-id:#{null}}")
	private String installationId;

	@Value("${github.app.enabled:false}")
	private boolean enabled;

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getInstallationId() {
		return installationId;
	}

	public void setInstallationId(String installationId) {
		this.installationId = installationId;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Check if all required credentials are present (used by VaultConfig during loading).
	 */
	public boolean hasAllCredentials() {
		return appId != null && !appId.isEmpty() && installationId != null && !installationId.isEmpty()
				&& privateKey != null && !privateKey.isEmpty();
	}

	/**
	 * Check if GitHub App is fully configured and enabled.
	 */
	public boolean isConfigured() {
		return enabled && hasAllCredentials();
	}

}
