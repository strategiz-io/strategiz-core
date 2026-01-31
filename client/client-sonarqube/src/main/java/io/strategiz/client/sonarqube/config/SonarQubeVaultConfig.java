package io.strategiz.client.sonarqube.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;

/**
 * Loads SonarQube configuration from Vault at application startup.
 *
 * Vault path: secret/strategiz/sonarqube Required fields: - url: SonarQube instance URL -
 * token: API token for authentication - project-key: SonarQube project key
 */
@Configuration
public class SonarQubeVaultConfig {

	private static final Logger log = LoggerFactory.getLogger(SonarQubeVaultConfig.class);

	private final SecretManager secretManager;

	private final SonarQubeConfig sonarQubeConfig;

	@Autowired
	public SonarQubeVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager,
			SonarQubeConfig sonarQubeConfig) {
		this.secretManager = secretManager;
		this.sonarQubeConfig = sonarQubeConfig;
	}

	@PostConstruct
	public void loadSonarQubePropertiesFromVault() {
		try {
			log.info("Loading SonarQube configuration from Vault...");

			// Load SonarQube URL
			String url = secretManager.readSecret("sonarqube.url", null);
			if (url != null && !url.isEmpty()) {
				sonarQubeConfig.setUrl(url);
				log.info("Loaded SonarQube URL from Vault: {}", url);
			}
			else {
				log.warn("SonarQube URL not found in Vault - using application.properties fallback");
			}

			// Load SonarQube token
			String token = secretManager.readSecret("sonarqube.token", null);
			if (token != null && !token.isEmpty()) {
				sonarQubeConfig.setToken(token);
				log.info("Loaded SonarQube token from Vault");
			}
			else {
				log.warn("SonarQube token not found in Vault - using application.properties fallback");
			}

			// Load SonarQube project key
			String projectKey = secretManager.readSecret("sonarqube.project-key", null);
			if (projectKey != null && !projectKey.isEmpty()) {
				sonarQubeConfig.setProjectKey(projectKey);
				log.info("Loaded SonarQube project key from Vault: {}", projectKey);
			}
			else {
				log.warn("SonarQube project key not found in Vault - using application.properties fallback");
			}

			log.info("SonarQube configuration loaded successfully");
		}
		catch (Exception e) {
			log.error("Failed to load SonarQube configuration from Vault", e);
		}
	}

}
