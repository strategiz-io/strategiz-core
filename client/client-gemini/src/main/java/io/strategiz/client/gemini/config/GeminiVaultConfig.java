package io.strategiz.client.gemini.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that loads Gemini API key from Vault and directly injects it into
 * GeminiConfig. This ensures the API key is available regardless of bean initialization order.
 */
@Configuration
public class GeminiVaultConfig {

	private static final Logger log = LoggerFactory.getLogger(GeminiVaultConfig.class);

	private final SecretManager secretManager;

	private final GeminiConfig geminiConfig;

	@Autowired
	public GeminiVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager, GeminiConfig geminiConfig) {
		this.secretManager = secretManager;
		this.geminiConfig = geminiConfig;
	}

	@PostConstruct
	public void loadGeminiPropertiesFromVault() {
		try {
			log.info("Loading Gemini API properties from Vault...");

			// Load Gemini API key from Vault
			// Vault path: secret/strategiz/gemini with field "api-key"
			String geminiApiKey = secretManager.readSecret("gemini.api-key");

			if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
				// Directly set the API key on GeminiConfig
				geminiConfig.setApiKey(geminiApiKey);
				log.info("Loaded and set Gemini API key from Vault");
			}
			else {
				log.warn("Gemini API key not found in Vault at path: gemini.api-key");
			}

		}
		catch (Exception e) {
			log.error("Failed to load Gemini properties from Vault", e);
		}
	}

}
