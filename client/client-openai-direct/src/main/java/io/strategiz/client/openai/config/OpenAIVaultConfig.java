package io.strategiz.client.openai.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that loads OpenAI credentials from Vault and injects them into
 * OpenAIDirectConfig. This ensures all OpenAI secrets are loaded from Vault at startup.
 *
 * Vault path: secret/strategiz/openai Required field: - api-key: OpenAI API key
 * (sk-proj-xxx or sk-xxx)
 */
@Configuration
public class OpenAIVaultConfig {

	private static final Logger log = LoggerFactory.getLogger(OpenAIVaultConfig.class);

	private final SecretManager secretManager;

	private final OpenAIDirectConfig openaiConfig;

	@Autowired
	public OpenAIVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager,
			OpenAIDirectConfig openaiConfig) {
		this.secretManager = secretManager;
		this.openaiConfig = openaiConfig;
	}

	@PostConstruct
	public void loadOpenAIPropertiesFromVault() {
		try {
			log.info("Loading OpenAI configuration from Vault...");

			// Load OpenAI API key
			String apiKey = secretManager.readSecret("openai.api-key", null);
			if (apiKey != null && !apiKey.isEmpty()) {
				openaiConfig.setApiKey(apiKey);
				log.info("Loaded OpenAI API key from Vault");
			}
			else {
				log.warn("OpenAI API key not found in Vault - GPT-4o and o1 models will be unavailable");
			}

		}
		catch (Exception e) {
			log.error("Failed to load OpenAI configuration from Vault", e);
		}
	}

}
