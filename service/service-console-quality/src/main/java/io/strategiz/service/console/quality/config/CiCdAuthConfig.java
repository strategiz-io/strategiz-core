package io.strategiz.service.console.quality.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.VaultTemplate;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for CI/CD authentication tokens loaded from Vault.
 *
 * Vault path: secret/strategiz/ci-cd
 * Required key: quality-api-token
 */
@Configuration
public class CiCdAuthConfig {

	private static final Logger log = LoggerFactory.getLogger(CiCdAuthConfig.class);

	private static final String VAULT_PATH = "secret/strategiz/ci-cd";

	private static final String TOKEN_KEY = "quality-api-token";

	@Autowired(required = false)
	private VaultTemplate vaultTemplate;

	private String qualityApiToken;

	@PostConstruct
	public void loadFromVault() {
		if (vaultTemplate == null) {
			log.warn("VaultTemplate not available - CI/CD authentication will be disabled");
			return;
		}

		try {
			var secrets = vaultTemplate.read(VAULT_PATH);
			if (secrets != null && secrets.getData() != null) {
				this.qualityApiToken = (String) secrets.getData().get(TOKEN_KEY);

				if (this.qualityApiToken != null && !this.qualityApiToken.isEmpty()) {
					log.info("CI/CD quality API token loaded successfully from Vault");
				}
				else {
					log.warn("CI/CD quality API token not found in Vault at {}/{}", VAULT_PATH, TOKEN_KEY);
				}
			}
			else {
				log.warn("No secrets found in Vault at path: {}", VAULT_PATH);
			}
		}
		catch (Exception e) {
			log.error("Failed to load CI/CD token from Vault: {}", e.getMessage());
		}
	}

	/**
	 * Validate if the provided token matches the CI/CD quality API token.
	 * @param token the token to validate
	 * @return true if valid, false otherwise
	 */
	public boolean isValidToken(String token) {
		if (this.qualityApiToken == null || this.qualityApiToken.isEmpty()) {
			return false;
		}

		return this.qualityApiToken.equals(token);
	}

	/**
	 * Check if CI/CD authentication is configured.
	 * @return true if token is loaded, false otherwise
	 */
	public boolean isConfigured() {
		return this.qualityApiToken != null && !this.qualityApiToken.isEmpty();
	}

}
