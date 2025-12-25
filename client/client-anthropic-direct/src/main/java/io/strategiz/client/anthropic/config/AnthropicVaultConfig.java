package io.strategiz.client.anthropic.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that loads Anthropic credentials from Vault and injects them into
 * AnthropicDirectConfig. This ensures all Anthropic secrets are loaded from Vault at
 * startup.
 *
 * Vault path: secret/strategiz/anthropic Required field: - api-key: Anthropic API key
 * (sk-ant-xxx)
 */
@Configuration
public class AnthropicVaultConfig {

	private static final Logger log = LoggerFactory.getLogger(AnthropicVaultConfig.class);

	private final SecretManager secretManager;

	private final AnthropicDirectConfig anthropicConfig;

	@Autowired
	public AnthropicVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager,
			AnthropicDirectConfig anthropicConfig) {
		this.secretManager = secretManager;
		this.anthropicConfig = anthropicConfig;
	}

	@PostConstruct
	public void loadAnthropicPropertiesFromVault() {
		try {
			log.info("Loading Anthropic configuration from Vault...");

			// Load Anthropic API key
			String apiKey = secretManager.readSecret("anthropic.api-key", null);
			if (apiKey != null && !apiKey.isEmpty()) {
				anthropicConfig.setApiKey(apiKey);
				log.info("Loaded Anthropic API key from Vault");
			}
			else {
				log.warn("Anthropic API key not found in Vault - Claude 4.5 models will be unavailable");
			}

		}
		catch (Exception e) {
			log.error("Failed to load Anthropic configuration from Vault", e);
		}
	}

}
